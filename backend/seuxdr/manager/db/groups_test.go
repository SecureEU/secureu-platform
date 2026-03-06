package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestGroupRepository(t *testing.T) {
	// Initialize Test Database
	dbClient, err := utils.InitTestDb(false)
	assert.NoError(t, err)
	defer utils.RemoveTestDb()

	// Create GroupRepository instance
	groupRepo := db.NewGroupRepository(dbClient.DB)

	// Test data
	testGroup := &db.Group{
		Name:       "Test Group",
		LicenseKey: "test-license-key",
	}

	// Test Create
	t.Run("Create Group", func(t *testing.T) {
		err := groupRepo.Create(testGroup)
		assert.NoError(t, err)
		assert.NotEqual(t, 0, testGroup.ID)
	})

	// Test GetByID
	t.Run("Get Group by ID", func(t *testing.T) {
		group, err := groupRepo.Get(scopes.ByID(testGroup.ID))
		assert.NoError(t, err)
		assert.Equal(t, testGroup.Name, group.Name)
		assert.Equal(t, testGroup.LicenseKey, group.LicenseKey)
	})

	// Test FindByLicenseKey
	t.Run("Find Group by LicenseKey", func(t *testing.T) {
		group, err := groupRepo.Get(scopes.ByLicenseKey(testGroup.LicenseKey))
		assert.NoError(t, err)
		assert.Equal(t, testGroup.Name, group.Name)
		assert.Equal(t, testGroup.LicenseKey, group.LicenseKey)
	})

	// Test Update
	t.Run("Update Group", func(t *testing.T) {
		testGroup.Name = "Updated Group Name"
		err := groupRepo.Save(*testGroup)
		assert.NoError(t, err)

		updatedGroup, err := groupRepo.Get(scopes.ByID(testGroup.ID))
		assert.NoError(t, err)
		assert.Equal(t, "Updated Group Name", updatedGroup.Name)
	})

	// Test Delete (Soft Delete)
	t.Run("Delete Group", func(t *testing.T) {
		err := groupRepo.Delete(scopes.ByID(testGroup.ID))
		assert.NoError(t, err)

		deletedGroup, err := groupRepo.Get(scopes.ByID(testGroup.ID))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, deletedGroup.IsEmpty())
	})

}
