//go:build manager
// +build manager

package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"

	_ "github.com/mattn/go-sqlite3"
	"github.com/stretchr/testify/assert"
)

func TestRoleRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(true)
	assert.NoError(t, err)
	defer utils.RemoveTestDb()
	repo := db.NewRoleRepository(dbClient.DB)

	// Test for Create
	t.Run("Scenario: Successfully create a new role", func(t *testing.T) {
		role := db.Role{
			Name:        "Admin",
			Description: "Administrator role with full access",
		}

		id, err := repo.Create(role)
		assert.NoError(t, err)
		assert.NotZero(t, id)
	})

	t.Run("Scenario: Fail to create a role with empty name", func(t *testing.T) {
		invalidRole := db.Role{
			Description: "Role without a name",
		}

		_, err := repo.Create(invalidRole)
		assert.Error(t, err)
	})

	// Test for GetByID
	t.Run("Scenario: Successfully retrieve a role by ID", func(t *testing.T) {
		role := db.Role{
			Name:        "User",
			Description: "Regular user role",
		}
		id, _ := repo.Create(role)

		retrievedRole, err := repo.Get(scopes.ByID(id))
		assert.NoError(t, err)
		assert.Equal(t, role.Name, retrievedRole.Name)
	})

	t.Run("Scenario: Attempt to retrieve non-existing role by ID", func(t *testing.T) {
		retrievedRole, err := repo.Get(scopes.ByID(999))
		assert.Error(t, err)
		assert.Nil(t, retrievedRole)
	})

	// Test for Update
	t.Run("Scenario: Successfully update an existing role", func(t *testing.T) {
		role := db.Role{
			Name:        "Guest",
			Description: "Guest role",
		}
		id, _ := repo.Create(role)

		role.ID = id
		role.Description = "Updated guest role"
		err := repo.Save(role)
		assert.NoError(t, err)

		updatedRole, err := repo.Get(scopes.ByID(id))
		assert.NoError(t, err)
		assert.Equal(t, role.Description, updatedRole.Description)
	})

	t.Run("Scenario: Attempt to update a non-existing role", func(t *testing.T) {
		role := db.Role{
			ID:          999,
			Name:        "Non-Existent Role",
			Description: "This role does not exist",
		}
		err := repo.Save(role)
		assert.Error(t, err)
	})

	// Test for Delete
	t.Run("Scenario: Successfully delete a role by ID", func(t *testing.T) {
		role := db.Role{
			Name:        "TempRole",
			Description: "Temporary role for deletion test",
		}
		id, _ := repo.Create(role)

		err := repo.Delete(scopes.ByID(id))
		assert.NoError(t, err)

		retrievedRole, err := repo.Get(scopes.ByID(id))
		assert.Error(t, err)
		assert.Nil(t, retrievedRole)
	})

	t.Run("Scenario: Attempt to delete a non-existing role", func(t *testing.T) {
		err := repo.Delete(scopes.ByID(999))
		assert.NoError(t, err) // Deleting a non-existent row should not fail
	})
}
