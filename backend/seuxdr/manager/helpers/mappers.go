package helpers

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/rbac"
)

func MapGroupToJson(group db.Group) GroupJSON {
	return GroupJSON{
		ID:        group.ID,
		CreatedAt: group.CreatedAt,
		Name:      group.Name,
		OrgID:     group.OrgID,
	}
}

func MapGroupsToJson(groups []db.Group) []GroupJSON {
	mappedGroups := make([]GroupJSON, len(groups))
	for i, group := range groups {
		mappedGroups[i] = MapGroupToJson(group)
	}
	return mappedGroups
}

func MapOrganisationToJson(org db.Organisation) OrganisationJSON {
	return OrganisationJSON{
		ID:        org.ID,
		CreatedAt: org.CreatedAt,
		Name:      org.Name,
		Code:      org.Code,
		Groups:    MapGroupsToJson(org.Groups), // Convert Groups to JSON format
	}
}

func MapOrganisationsToJson(orgs []db.Organisation) []OrganisationJSON {
	mappedOrgs := make([]OrganisationJSON, len(orgs))
	for i, org := range orgs {
		mappedOrgs[i] = MapOrganisationToJson(org)
	}
	return mappedOrgs
}

// MapScopesToPermissions converts a slice of Scope to a slice of Permission.
func MapScopesToPermissions(scopes []rbac.Scope) []Permission {
	permissions := make([]Permission, 0, len(scopes))
	for _, scope := range scopes {
		permissions = append(permissions, Permission{Name: scope.Name})
	}
	return permissions
}

func MapUsersToJSON(users []db.User) []UserResponse {
	responses := make([]UserResponse, len(users))
	for i, user := range users {
		responses[i] = MapUserToJSON(user)
	}
	return responses
}

func MapUserToJSON(user db.User) UserResponse {
	return UserResponse{
		ID:        user.ID,
		CreatedAt: user.CreatedAt,
		FirstName: user.FirstName,
		LastName:  user.LastName,
		Email:     user.Email,
		Role:      user.Role,
		OrgID:     user.OrgID,
		GroupID:   user.GroupID,
	}
}

func MapRoleScopesToPermissionResponse(rolePerms []rbac.Scope) []Permission {
	perms := make([]Permission, len(rolePerms))
	for i, perm := range rolePerms {
		perms[i] = MapRoleScopeToPermissionResponse(perm)
	}
	return perms
}

func MapRoleScopeToPermissionResponse(rolePerm rbac.Scope) Permission {
	return Permission{Name: rolePerm.Name}
}
