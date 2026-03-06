package rbac

import (
	"strings"
)

// Scope is a permission to execute an action, often at a very granular level, that can be
// included in a role.
//
// When implementing features that require restriction, add a scope and provide it to the
// appropriate scopes.
type Scope struct {
	Name        string
	Description string
}

var (
	ScopeOmnipotence = Scope{"omnipotence", "Omnipotence"}

	ScopeOrgView   = Scope{"org.view", "View organisations"}
	ScopeOrgCreate = Scope{"org.create", "Create organisations"}
	ScopeOrgDelete = Scope{"org.delete", "Delete organisations"}

	ScopeGroupCreate = Scope{"group.create", "Create groups"}
	ScopeGroupView   = Scope{"group.view", "View groups"}
	ScopeGroupDelete = Scope{"group.delete", "Delete groups"}

	ScopeAgentDeploy  = Scope{"agent.deploy", "Deploy agents"}
	ScopeAgentDisable = Scope{"agent.disable", "Disable agents"}
	ScopeAgentView    = Scope{"agent.view", "View agents"}

	ScopeUserView           = Scope{"user.view", "View users"}
	ScopeUserCreateAdmin    = Scope{"user.create.admin", "Create admin users"}
	ScopeUserCreateManager  = Scope{"user.create.manager", "Create manager users"}
	ScopeUserCreateEmployee = Scope{"user.create.employee", "Create employee users"}
	ScopeUserUpdateAdmin    = Scope{"user.update.admin", "Update admin users"}
	ScopeUserUpdateManager  = Scope{"user.update.manager", "Update manager users"}
	ScopeUserUpdateEmployee = Scope{"user.update.employee", "Update employee users"}
	ScopeUserUpdateRole     = Scope{"user.update.role", "Update user roles"}

	ScopeUserAssignAdminRole    = Scope{"user.assign.admin", "Assign admin role"}
	ScopeUserAssignManagerRole  = Scope{"user.assign.manager", "Assign manager role"}
	ScopeUserAssignEmployeeRole = Scope{"user.assign.employee", "Assign employee role"}

	ScopeUserDelete = Scope{"user.delete", "Delete users"}
)

// Role is a named collection of scopes, representing a predefined role within the
// product.
type Role struct {
	Name        string
	Description string
	Rank        uint
}

var (
	RoleEmployee = Role{Name: "employee", Description: "Can deploy agents for their group", Rank: 1}
	RoleManager  = Role{Name: "manager", Description: "Manage group users and view org", Rank: 2}
	RoleAdmin    = Role{Name: "admin", Description: "Manage org and users", Rank: 3}
	RoleEUAdmin  = Role{Name: "eu_admin", Description: "All access", Rank: 4}
)

var rolesByName = map[string]Role{
	RoleEmployee.Name: RoleEmployee,
	RoleManager.Name:  RoleManager,
	RoleAdmin.Name:    RoleAdmin,
	RoleEUAdmin.Name:  RoleEUAdmin,
}

func GetRoleByName(name string) (Role, bool) {
	role, ok := rolesByName[name]
	return role, ok
}

// Used to retrieve the permissions that a user needs to be able to assign the passed role.
// Pass the role you want to assign to the user
func GetAssignRoleCoverage(role string) []Scope {
	scopes := []Scope{}

	switch role {
	// if we are trying to make the user an admin
	case RoleAdmin.Name:
		// we need to have the permission to update users to admin
		scopes = append(scopes, ScopeUserAssignAdminRole)
	case RoleManager.Name:
		scopes = append(scopes, ScopeUserAssignManagerRole)
	case RoleEmployee.Name:
		scopes = append(scopes, ScopeUserAssignEmployeeRole)
	}
	return scopes
}

// Used to retrieve the permissions that a user needs to be able to update a user with the passed role type.
// Pass the role of the user you are trying to update to it
func GetUpdateRoleCoverage(role string) []Scope {
	scopes := []Scope{}
	//if we want to update an admin user
	switch role {
	case RoleAdmin.Name:
		// we need to have the permission to update admin users
		scopes = append(scopes, ScopeUserUpdateAdmin)
	case RoleManager.Name:
		scopes = append(scopes, ScopeUserUpdateManager)
	case RoleEmployee.Name:
		scopes = append(scopes, ScopeUserUpdateEmployee)
	}
	return scopes
}

func (role *Role) HasPermissions(requiredScopes []Scope) (bool, []Scope, string) {
	rolePermissions := ScopesFor(*role)
	permissionSet := make(map[string]struct{}, len(rolePermissions))

	for _, p := range rolePermissions {
		permissionSet[p.Name] = struct{}{}
	}

	var missing []Scope
	for _, required := range requiredScopes {
		if _, ok := permissionSet[required.Name]; !ok {
			missing = append(missing, required)
		}
	}

	var names []string
	for _, m := range missing {
		names = append(names, m.Name)
	}
	message := "Missing permissions: " + strings.Join(names, ", ")

	return len(missing) == 0, missing, message
}

func ScopesFor(role Role) []Scope {
	var scopes []Scope

	if role.Rank >= RoleEmployee.Rank {
		scopes = append(scopes, ScopeAgentDeploy)
		scopes = append(scopes, ScopeAgentDisable)
		scopes = append(scopes, ScopeGroupView)
		scopes = append(scopes, ScopeOrgView)
		scopes = append(scopes, ScopeAgentView)
	}
	if role.Rank >= RoleManager.Rank {
		scopes = append(scopes, ScopeGroupCreate)
		scopes = append(scopes, ScopeGroupDelete)
		scopes = append(scopes, ScopeAgentDisable)
		scopes = append(scopes, ScopeUserView, ScopeUserCreateEmployee)
		scopes = append(scopes, ScopeUserUpdateEmployee)
	}
	if role.Rank >= RoleAdmin.Rank {
		scopes = append(scopes, ScopeOrgCreate)
		scopes = append(scopes, ScopeOrgDelete)
		scopes = append(scopes, ScopeUserUpdateRole)
		scopes = append(scopes, ScopeUserCreateManager)
		scopes = append(scopes, ScopeUserUpdateManager)
		scopes = append(scopes, ScopeUserAssignEmployeeRole)
		scopes = append(scopes, ScopeUserAssignManagerRole)
		scopes = append(scopes, ScopeUserDelete)
	}
	if role.Rank >= RoleEUAdmin.Rank {
		scopes = append(scopes, ScopeOmnipotence)
	}

	return scopes
}
