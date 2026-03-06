package authenticationservice

import (
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"SEUXDR/manager/rbac"
	"SEUXDR/manager/validators"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

type AuthenticationService interface {
	RegisterUser(email, password, firstName, surname, role string, IsPasswordTemp int, orgID *int64, groupID *int64, parentID *int64) (db.User, error)
	LoginUser(email, password string) (helpers.PermissionsResponse, int)
	AuthenticateTokens(token string) (string, error)
	LogOut(token string) error
}

type authenticationService struct {
	config       conf.Configuration
	AuthProvider conf.AuthConfiger
	UserRepo     db.UserRepository
	SessionsRepo db.SessionRepository
	logger       logging.EULogger
}

func AuthenticationServiceFactory(dbConn *gorm.DB, logger logging.EULogger) (AuthenticationService, error) {
	config := conf.GetConfigFunc()()
	authProvider, err := conf.LoadConfig(config.CERTS.JWT.PRIVATE_KEY, config.CERTS.JWT.PUBLIC_KEY)
	if err != nil {
		return nil, err
	}
	userRepo := db.NewUserRepository(dbConn)
	sessionRepo := db.NewSessionRepository(dbConn)
	return NewAuthenticationService(config, &authProvider, userRepo, sessionRepo, logger), nil
}

func NewAuthenticationService(config conf.Configuration, authProvider conf.AuthConfiger, userRepo db.UserRepository, sessionRepo db.SessionRepository, logger logging.EULogger) AuthenticationService {
	return &authenticationService{AuthProvider: authProvider, config: config, UserRepo: userRepo, SessionsRepo: sessionRepo, logger: logger}
}

func (authService *authenticationService) RegisterUser(email, password, firstName, surname, role string, isPasswordTemp int, orgID *int64, groupID *int64, parentID *int64) (db.User, error) {
	var (
		err     error
		newUser db.User
	)
	if err = validators.ValidateName(helpers.Capitalize(strings.TrimSpace(firstName))); err != nil {
		return newUser, fmt.Errorf("Invalid first name: %w", err)
	}
	if err := validators.ValidateName(helpers.Capitalize(strings.TrimSpace(surname))); err != nil {
		return newUser, fmt.Errorf("Invalid last name: %w", err)
	}

	usr, err := authService.UserRepo.Get(scopes.ByEmailEquals(email))
	if err == nil {
		return newUser, fmt.Errorf("user with email %s already exists", usr.Email)
	}
	if !errors.Is(errors.Cause(err), gorm.ErrRecordNotFound) {
		return newUser, fmt.Errorf("failed to check if user exists: %w", err)
	}

	err = validators.ValidatePassword(password)
	if err != nil {
		return newUser, fmt.Errorf("Invalid password: %w", err)
	}

	hashedPasswordBytes, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return newUser, fmt.Errorf("failed to hash password: %w", err)
	}
	if isPasswordTemp != 0 && isPasswordTemp != 1 {
		return newUser, errors.New("invalid password setting")
	}
	newUser = db.User{
		FirstName:      firstName,
		LastName:       surname,
		Email:          email,
		Role:           role,
		Password:       string(hashedPasswordBytes),
		IsPasswordTemp: &isPasswordTemp,
		OrgID:          orgID,
		GroupID:        groupID,
		ParentID:       parentID,
	}

	if err := authService.UserRepo.Create(&newUser); err != nil {
		return newUser, fmt.Errorf("failed to create user: %w", err)
	}

	return newUser, nil
}

func (authService *authenticationService) LoginUser(email, password string) (helpers.PermissionsResponse, int) {
	user, err := authService.UserRepo.Get(scopes.ByEmailEquals(email))
	if errors.Is(errors.Cause(err), gorm.ErrRecordNotFound) {
		return helpers.PermissionsResponse{Error: true, Message: "Incorrect Email or Password"}, http.StatusNotFound
	}
	if err != nil {
		return helpers.PermissionsResponse{Error: true, Message: "Failed to validate credentials. Please Try again later"}, http.StatusInternalServerError
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		return helpers.PermissionsResponse{Error: true, Message: "Incorrect Email or Password"}, http.StatusBadRequest
	}

	session, err := authService.SessionsRepo.Get(scopes.ByUserID(user.ID), scopes.OrderBy("created_at", "DESC"))
	if err == nil && session.Valid == 1 {
		session.Valid = 0
		if err = authService.SessionsRepo.Save(session); err != nil {
			return helpers.PermissionsResponse{Error: true, Message: "Failed to end previous session. Please try logging out of other devices and try again."}, http.StatusForbidden
		}
	}
	now := time.Now()

	token, err := authService.AuthProvider.GenerateToken(uint(user.ID), now)
	if err != nil {
		return helpers.PermissionsResponse{Error: true, Message: "Failed to generate token"}, http.StatusInternalServerError
	}

	sesh := db.UserSession{UserID: user.ID, JWTToken: token, Valid: 1, ExpiresAt: now.Add(time.Hour * 24)}

	if err = authService.SessionsRepo.Create(&sesh); err != nil {
		return helpers.PermissionsResponse{Error: true, Message: "Failed to create session"}, http.StatusInternalServerError
	}

	type loginResponse struct {
		Username       string `json:"username"`
		Role           string `json:"role"`
		Token          string `json:"token"`
		IsPasswordTemp bool   `json:"is_password_temp"`
	}
	usrName := loginResponse{Username: fmt.Sprintf("%s %s", user.FirstName, user.LastName), Role: user.Role, Token: token, IsPasswordTemp: *user.IsPasswordTemp == 1}
	role, ok := rbac.GetRoleByName(user.Role)
	if !ok {
		return helpers.PermissionsResponse{Error: true, Message: "Failed to get permissions"}, http.StatusInternalServerError
	}
	permissions := rbac.ScopesFor(role)
	perms := helpers.MapRoleScopesToPermissionResponse(permissions)
	resp := helpers.PermissionsResponse{Error: false, Message: "Login Successful", Data: usrName, Permissions: perms}

	return resp, http.StatusAccepted
}

func (authService *authenticationService) AuthenticateTokens(token string) (string, error) {
	var (
		err    error
		userID string
	)

	userID, err = authService.AuthProvider.Validate(token)
	if err != nil {
		return userID, fmt.Errorf("Invalid auth token %w", err)
	}
	usrInt, err := strconv.ParseInt(userID, 10, 64)
	if err != nil {
		return userID, fmt.Errorf("invalid user ID in token: %w", err)
	}

	session, err := authService.SessionsRepo.Get(scopes.ByJWT(token), scopes.ByValid(true), scopes.OrderBy("created_at", "DESC"))
	if err != nil || session.Valid == 0 {
		return userID, fmt.Errorf("Invalid session or token: %w", err)
	}
	if session.UserID != int64(usrInt) {
		return userID, fmt.Errorf("Invalid issuer id %w", err)
	}
	if session.IsExpired() {
		return userID, fmt.Errorf("Expired session found")
	}

	return userID, nil
}

func (authService *authenticationService) LogOut(token string) error {
	// Look for a valid session with this token
	session, err := authService.SessionsRepo.Get(scopes.ByJWT(token), scopes.ByValid(true), scopes.OrderBy("created_at", "DESC"))
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return fmt.Errorf("no active session found for token")
		}
		return fmt.Errorf("error querying session: %w", err)
	}

	if session.Valid == 0 {
		return nil
	}

	// Soft-revoke the session instead of deleting
	session.Valid = 0
	if err := authService.SessionsRepo.Save(session); err != nil {
		return fmt.Errorf("failed to revoke session: %w", err)
	}

	return nil
}
