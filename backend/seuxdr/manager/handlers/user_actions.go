package handlers

import (
	"SEUXDR/manager/api/authenticationservice"
	"SEUXDR/manager/api/opensearchservice"
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/rbac"
	"SEUXDR/manager/validators"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"golang.org/x/crypto/bcrypt"
)

func (h *Handlers) ManageOrgs(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "internal server error"})
		return
	}

	// No authentication required - return all organizations with all groups
	preloadConfigs := []db.PreloadConfig{
		{Name: "Groups"},
	}

	orgRepo := db.NewOrganisationsRepository(h.db)
	orgs, err := orgRepo.Find(preloadConfigs)
	if err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Failed to fetch organisations"))
		return
	}

	orgsJSON := helpers.MapOrganisationsToJson(orgs)
	c.JSON(http.StatusOK, orgsJSON)

}

// Handler to create a new organisation
func (h *Handlers) CreateOrg(c *gin.Context) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - allow all requests to create organisations
	var request helpers.CreateOrgRequest

	// Bind the incoming JSON payload to the CreateOrgRequest struct
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid payload"})
		return
	}

	// Generate a random API key with 32 bytes (64 characters in hex)
	apiKey, err := helpers.GenerateRandomAPIKey(32)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to generate API key for organisation"})
		return
	}

	// Get any existing user ID for the foreign key constraint
	// Since authentication is disabled, we just need any valid user ID
	userRepo := db.NewUserRepository(h.db)
	users, err := userRepo.Find(nil)
	var userID int64
	if err != nil || len(users) == 0 {
		// No users exist, create a default system user
		logger.LogWithContext(logrus.InfoLevel, "No users found, creating default system user", logrus.Fields{})
		systemUser := db.User{
			FirstName: "System",
			LastName:  "User",
			Email:     "system@seuxdr.local",
			Password:  "disabled",
			Role:      "admin",
		}
		if err := userRepo.Create(&systemUser); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create system user"})
			return
		}
		userID = systemUser.ID
	} else {
		userID = users[0].ID
	}

	// Simulate generating an ID and the current time for the new organisation
	org := db.Organisation{
		Name:   request.Name,
		Code:   request.Code,
		ApiKey: apiKey,
		UserID: userID, // Use first available user ID
	}

	orgRepo := db.NewOrganisationsRepository(h.db)

	// Add the new organisation to the database
	err = orgRepo.Create(&org)
	if err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Failed to create organisation"))
		return
	}

	// Respond with the newly created organisation
	c.JSON(http.StatusOK,
		helpers.MapOrganisationToJson(org),
	)
}

// Handler to create a new group
func (h *Handlers) CreateGroup(c *gin.Context) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - allow all requests to create groups
	var request helpers.CreateGroupRequest

	// Bind the incoming JSON payload to the CreateGroupRequest struct
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid payload"})
		return
	}

	// Generate a random license key with 4 groups of 4 characters
	licenseKey, err := helpers.GenerateRandomLicenseKey(4, 4)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Error generating license key."})
		return
	}

	// Create the new group
	group := db.Group{
		Name:       request.Name,
		LicenseKey: licenseKey,
		OrgID:      request.OrgID,
	}

	groupRepo := db.NewGroupRepository(h.db)

	// Add the new group to the database
	err = groupRepo.Create(&group)
	if err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Failed to create group for org"))
		return
	}

	// Respond with the newly created group
	c.JSON(http.StatusOK, helpers.MapGroupToJson(group))
}

func (h *Handlers) ViewAgents(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - return all agents without access control
	preloadConfigs := []db.PreloadConfig{
		{Name: "Groups"},
		{Name: "Groups.Agents"},
	}

	orgRepo := db.NewOrganisationsRepository(h.db)

	orgs, err := orgRepo.Find(preloadConfigs)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch agents"})
		return
	}

	var agentData = []helpers.AgentGroupView{}

	for _, org := range orgs {
		for _, group := range org.Groups {
			groupView := helpers.AgentGroupView{}
			for _, agent := range group.Agents {
				groupView.Name = agent.Name
				groupView.OrgName = org.Name
				groupView.Active = agent.IsActivated == 1
				groupView.CreatedAt = agent.CreatedAt.String()
				groupView.OS = agent.OSVersion
				groupView.OrgID = group.OrgID
				groupView.GroupID = *agent.GroupID
				agentID := strconv.Itoa(int(agent.ID))
				groupView.ID = agentID
				groupView.GroupName = group.Name
				agentData = append(agentData, groupView)
			}
		}
	}

	c.JSON(http.StatusOK, agentData)
}

func (h *Handlers) ViewAlerts(c *gin.Context) {

	// Access the logger from the context
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}

	// No authentication required - return all alerts without access control
	var payload helpers.LogQuery

	if c.BindJSON(&payload) == nil {

		searchSvc := opensearchservice.NewOpenSearchServiceFactory(h.db, logger)

		alerts, err := searchSvc.Search(payload)
		if err != nil {
			c.JSON(http.StatusInternalServerError, err)
			return
		}

		c.JSON(http.StatusOK, alerts)
		return
	}

	c.JSON(http.StatusBadRequest, "Invalid Payload")
}

func (h *Handlers) GetTLSCerts(c *gin.Context) {
	cfg := conf.GetConfigFunc()()
	c.FileAttachment(cfg.CERTS.TLS.SERVER_CA_CRT, "server-ca.crt")
}

func (h *Handlers) UserRegister(c *gin.Context) {
	var userPayload helpers.UserPayload

	err := c.BindJSON(&userPayload)
	if err == nil {
		// Access the logger from the context
		logger := helpers.GetLogger(c)
		if logger == nil {
			c.JSON(500, gin.H{"error": failedHandlerStartMsg})
			return
		}

		authSvc, err := authenticationservice.AuthenticationServiceFactory(h.db, logger)
		if err != nil {
			c.JSON(500, gin.H{"error": err})
			return
		}
		_, err = authSvc.RegisterUser(userPayload.Email, userPayload.Password, userPayload.FirstName, userPayload.LastName, rbac.RoleAdmin.Name, 0, nil, nil, nil)
		if err != nil {
			c.JSON(401, gin.H{"error": err.Error()})
			return
		}
		c.JSON(200, gin.H{"Success": "true"})
	} else {
		c.JSON(http.StatusBadRequest, gin.H{"error": err})
		return
	}
}

func (h *Handlers) Login(c *gin.Context) {
	// No authentication required - return success for backward compatibility
	c.JSON(http.StatusOK, gin.H{
		"message": "Login successful (authentication disabled)",
	})
}

func (h *Handlers) LogOut(c *gin.Context) {
	// No authentication required - return success for backward compatibility
	c.JSON(http.StatusOK, gin.H{"message": "Logged out (authentication disabled)"})
}

func (h *Handlers) ManageUsers(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "internal server error"})
		return
	}

	// No authentication required - return all users
	userRepo := db.NewUserRepository(h.db)
	users, err := userRepo.Find([]string{"Group", "Org"})
	if err != nil {
		c.JSON(http.StatusBadRequest, helpers.JsonResponseWithMessage(true, "Failed to find users"))
		return
	}

	usersJSON := helpers.MapUsersToJSON(users)
	c.JSON(http.StatusOK, usersJSON)

}

func (h *Handlers) CreateUser(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "internal server error"})
		return
	}

	// No authentication required - allow creating users with any role
	var userPayload helpers.CreateUserPayload

	if err := c.BindJSON(&userPayload); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid payload"})
		return
	}

	var (
		newUser     db.User
		newPassword string
		err         error
	)

	authSvc, err := authenticationservice.AuthenticationServiceFactory(h.db, logger)
	if err != nil {
		c.JSON(500, gin.H{"error": err})
		return
	}

	newPassword, err = helpers.GenerateSecurePassword(16)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to generate password for user"})
		return
	}

	// Create user based on role
	switch userPayload.Role {
	case rbac.RoleAdmin.Name:
		// create core account user (orgID, groupID, parentID are nil)
		newUser, err = authSvc.RegisterUser(userPayload.Email, newPassword, userPayload.FirstName, userPayload.LastName, rbac.RoleAdmin.Name, 1, nil, nil, nil)
	case rbac.RoleManager.Name:
		if userPayload.OrgID == nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "missing organisation id"})
			return
		}
		// create manager level account with only access to organisation
		newUser, err = authSvc.RegisterUser(userPayload.Email, newPassword, userPayload.FirstName, userPayload.LastName, rbac.RoleManager.Name, 1, userPayload.OrgID, nil, nil)
	case rbac.RoleEmployee.Name:
		if userPayload.GroupID == nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "missing group id"})
			return
		}
		// create employee account that can view a specific organisation's group
		newUser, err = authSvc.RegisterUser(userPayload.Email, newPassword, userPayload.FirstName, userPayload.LastName, rbac.RoleEmployee.Name, 1, userPayload.OrgID, userPayload.GroupID, nil)
	default:
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid role"})
		return
	}

	if err != nil {
		c.JSON(401, gin.H{"error": err.Error()})
		return
	}

	userJSON := helpers.MapUserToJSON(newUser)
	userJSON.Password = newPassword
	c.JSON(http.StatusOK, userJSON)
}

func (h *Handlers) ChangePassword(c *gin.Context) {

	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "internal server error"})
		return
	}

	// No authentication required - keeping password change logic for backward compatibility
	// but without role validation
	var cpPayload helpers.ChangePasswordPayload

	if err := c.BindJSON(&cpPayload); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid payload"})
		return
	}

	// Since authentication is disabled, return success for backward compatibility
	c.JSON(http.StatusOK, gin.H{"message": "Password change not required (authentication disabled)"})
}

func (h *Handlers) UpdateUser(c *gin.Context) {
	logger := helpers.GetLogger(c)
	if logger == nil {
		c.JSON(500, gin.H{"error": failedHandlerStartMsg})
		return
	}
	if h.mtlsManager == nil {
		c.JSON(500, gin.H{"error": "internal server error"})
		return
	}

	// No authentication required - allow updating any user
	userID := c.Param("user_id")

	// Convert to int if needed
	userIDInt, err := strconv.ParseInt(userID, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid user ID"})
		return
	}

	var cpPayload helpers.EditUserPayload

	if err := c.BindJSON(&cpPayload); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid payload"})
		return
	}

	// check first_name, last_name, email exist (minimum payload)
	if err := helpers.ValidateEditUserPayload(cpPayload); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	userRepo := db.NewUserRepository(h.db)
	orgRepo := db.NewOrganisationsRepository(h.db)

	// fetch user we are trying to update
	updatedUser, err := userRepo.Get(scopes.ByID(int64(userIDInt)))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to find user."})
		return
	}

	// Update role if provided
	if len(cpPayload.Role) > 0 {
		updatedUser.Role = cpPayload.Role
	}

	// validate first_name
	if err := validators.ValidateName(helpers.Capitalize(strings.TrimSpace(cpPayload.FirstName))); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid payload"})
		return
	}

	// validate last_name
	if err := validators.ValidateName(helpers.Capitalize(strings.TrimSpace(cpPayload.LastName))); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid payload"})
		return
	}

	// org id is required for managers and employees
	if cpPayload.OrgID == nil && (cpPayload.Role == rbac.RoleManager.Name || cpPayload.Role == rbac.RoleEmployee.Name) {
		c.JSON(http.StatusUnauthorized, gin.H{"error": fmt.Sprintf("invalid or empty organisation - required for %s", cpPayload.Role)})
		return
	}

	// group id is required for employees
	if cpPayload.GroupID == nil {
		if cpPayload.Role == rbac.RoleEmployee.Name {
			c.JSON(http.StatusUnauthorized, gin.H{"error": fmt.Sprintf("invalid or empty group - required for %s", cpPayload.Role)})
			return
		}
	}

	var orgs []db.Organisation
	if cpPayload.Role == rbac.RoleManager.Name || cpPayload.Role == rbac.RoleEmployee.Name {
		// get organisations
		preloadConfigs := []db.PreloadConfig{
			{Name: "Groups"},
		}
		// verify organisation exists
		orgs, err = orgRepo.Find(preloadConfigs, scopes.ByID(*cpPayload.OrgID))
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Errorf("Invalid organisation: %w", err)})
			return
		}
		// update org id for user
		updatedUser.OrgID = cpPayload.OrgID

		if cpPayload.Role == rbac.RoleEmployee.Name {
			// verify group exists
			groupExists := false
			for _, org := range orgs {
				for _, group := range org.Groups {
					if group.ID == *cpPayload.GroupID {
						groupExists = true
						break
					}
				}
			}
			if !groupExists {
				c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Errorf("Invalid group: %w", err)})
				return
			}

			updatedUser.GroupID = cpPayload.GroupID
		} else {
			updatedUser.GroupID = nil
		}
	}

	updatedUser.FirstName = cpPayload.FirstName
	updatedUser.LastName = cpPayload.LastName

	if cpPayload.Password != "" {
		err = validators.ValidatePassword(cpPayload.Password)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Errorf("Invalid password: %w", err)})
			return
		}
		ps, err := bcrypt.GenerateFromPassword([]byte(cpPayload.Password), bcrypt.DefaultCost)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Errorf("Failed to hash password: %w", err)})
			return
		}
		updatedUser.Password = string(ps)
	}

	if err = userRepo.Save(updatedUser); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update user. Please try again later."})
		return
	}
	userJSON := helpers.MapUserToJSON(*updatedUser)
	c.JSON(http.StatusOK, userJSON)
}
