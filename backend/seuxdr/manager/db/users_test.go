package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestUserRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(true)
	assert.NoError(t, err)
	defer utils.RemoveTestDb()

	userRepo := db.NewUserRepository(dbClient.DB)

	orgRepo := db.NewOrganisationsRepository(dbClient.DB)
	groupRepo := db.NewGroupRepository(dbClient.DB)
	newOrg := db.Organisation{Name: "Clone Systems", Code: "CS", ApiKey: "HjGw7ISLn_3namBGewQe"}

	err = orgRepo.Create(&newOrg)
	assert.Nil(t, err)

	newGroup := db.Group{Name: "Clone Systems Office", LicenseKey: "123456789012345678901234567890123456", OrgID: newOrg.ID}
	err = groupRepo.Create(&newGroup)
	assert.Nil(t, err)

	rolesRepo := db.NewRoleRepository(dbClient.DB)
	role := db.Role{Name: "User", Description: "Has user priviledges"}
	err = rolesRepo.Create(&role)
	assert.Nil(t, err)

	t.Run("Create User", func(t *testing.T) {
		user := db.User{FirstName: "John", LastName: "Doe", Email: "john.doe@example.com", OrgID: &newOrg.ID}
		err = userRepo.Create(&user)
		assert.NoError(t, err)
		assert.NotZero(t, user.ID) // Ensure ID is set after creation
	})

	t.Run("Get By ID", func(t *testing.T) {
		user := db.User{FirstName: "Jane", LastName: "Doe", Email: "jane.doe@example.com", OrgID: &newOrg.ID}
		err = userRepo.Create(&user)
		assert.NoError(t, err)

		got, err := userRepo.Get(scopes.ByID(user.ID))
		assert.NoError(t, err)
		assert.NotNil(t, got)
		assert.Equal(t, user.FirstName, got.FirstName)
	})

	t.Run("Update User", func(t *testing.T) {
		user := db.User{FirstName: "Alice", LastName: "Smith", Email: "alice.smith@example.com", OrgID: &newOrg.ID}
		err = userRepo.Create(&user)
		assert.NoError(t, err)

		user.FirstName = "Alice Updated"
		err = userRepo.Save(&user)
		assert.NoError(t, err)

		got, err := userRepo.Get(scopes.ByID(user.ID))
		assert.NoError(t, err)
		assert.Equal(t, "Alice Updated", got.FirstName)
	})

	t.Run("Delete User", func(t *testing.T) {
		user := db.User{FirstName: "Bob", LastName: "Johnson", Email: "bob.johnson@example.com", OrgID: &newOrg.ID}
		err = userRepo.Create(&user)
		assert.NoError(t, err)

		err = userRepo.Delete(scopes.ByID(user.ID))
		assert.NoError(t, err)

		got, err := userRepo.Get(scopes.ByID(user.ID))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, got.IsEmpty())
	})

	t.Run("Get By Non-Existing ID", func(t *testing.T) {
		got, err := userRepo.Get(scopes.ByID(999999)) // Non-existent ID
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, got.IsEmpty()) // Expect nil
	})
}
