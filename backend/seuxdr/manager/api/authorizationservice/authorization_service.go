package authorizationservice

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/rbac"
	"fmt"

	"gorm.io/gorm"
)

type AuthorizationService struct {
	db *gorm.DB
}

func NewAuthorizationService(database *gorm.DB) *AuthorizationService {
	return &AuthorizationService{
		db: database,
	}
}

// ValidateUserGroupAccess validates if a user has access to a specific group
func (s *AuthorizationService) ValidateUserGroupAccess(user *db.User, groupID int64, requiredScopes []rbac.Scope) error {
	// Check role permissions
	role, found := rbac.GetRoleByName(user.Role)
	if !found {
		return fmt.Errorf("invalid user role")
	}

	ok, _, message := role.HasPermissions(requiredScopes)
	if !ok {
		return fmt.Errorf("insufficient permissions: %s", message)
	}

	// Get top-level parent user
	userRepo := db.NewUserRepository(s.db)
	headUser, err := userRepo.FindTopLevelParent(user.ID)
	if err != nil {
		return fmt.Errorf("failed to validate user hierarchy: %w", err)
	}

	// Build scopes and preload configs based on user role
	scps := []func(db *gorm.DB) *gorm.DB{scopes.ByUserID(int64(headUser.ID))}
	var preloadConfigs []db.PreloadConfig

	switch user.Role {
	case rbac.RoleManager.Name:
		scps = append(scps, scopes.ByID(int64(*user.OrgID)))
		preloadConfigs = []db.PreloadConfig{{Name: "Groups"}}
	case rbac.RoleEmployee.Name:
		scps = append(scps, scopes.ByChildGroupID(*user.GroupID))
		preloadConfigs = []db.PreloadConfig{
			{Name: "Groups", Conditions: []interface{}{"id = ?", *user.GroupID}},
		}
	default:
		if user.Role != rbac.RoleAdmin.Name {
			return fmt.Errorf("unsupported user role")
		}
	}

	// Get user's organizations
	orgRepo := db.NewOrganisationsRepository(s.db)
	orgs, err := orgRepo.Find(preloadConfigs, scps...)
	if err != nil {
		return fmt.Errorf("failed to validate permissions: %w", err)
	}

	if len(orgs) == 0 {
		return fmt.Errorf("no accessible organizations found")
	}

	// Validate user access to the requested group
	switch user.Role {
	case rbac.RoleManager.Name:
		if orgs[0].ID != *user.OrgID {
			return fmt.Errorf("manager does not have access to this organization")
		}
		// Check if the group belongs to this org
		for _, group := range orgs[0].Groups {
			if group.ID == groupID {
				return nil // Access granted
			}
		}
		return fmt.Errorf("group not found in user's organization")

	case rbac.RoleEmployee.Name:
		if orgs[0].Groups[0].ID != *user.GroupID || *user.GroupID != groupID {
			return fmt.Errorf("employee does not have access to this group")
		}
		return nil // Access granted

	default:
		if user.Role != rbac.RoleAdmin.Name {
			return fmt.Errorf("unsupported user role")
		}
		return nil
	}
}
