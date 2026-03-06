package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestMain(m *testing.M) {
	// Change the working directory to the project root
	os.Chdir("../") // Adjust accordingly
	os.Exit(m.Run())
}

func TestOrganisationsRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(true)
	assert.NoError(t, err)
	defer utils.RemoveTestDb()

	orgRepo := db.NewOrganisationsRepository(dbClient.DB)

	t.Run("Create Organisation", func(t *testing.T) {
		org := db.Organisation{Name: "Test Org", Code: "ORG123", ApiKey: "test-api-key"}
		err := orgRepo.Create(&org)
		assert.NoError(t, err)
		assert.NotZero(t, org.ID) // Ensure ID is set after creation
	})

	t.Run("Get By ID", func(t *testing.T) {
		org := db.Organisation{Name: "Another Org", Code: "ORG456", ApiKey: "another-api-key"}
		err := orgRepo.Create(&org)
		assert.NoError(t, err)

		got, err := orgRepo.Get(scopes.ByID(org.ID))
		assert.NoError(t, err)
		assert.NotNil(t, got)
		assert.Equal(t, org.Name, got.Name)
	})

	t.Run("Find By API Key", func(t *testing.T) {
		org := db.Organisation{Name: "Org With API", Code: "ORG789", ApiKey: "api-key"}
		err := orgRepo.Create(&org)
		assert.NoError(t, err)

		got, err := orgRepo.Get(scopes.ByApiKey("api-key"))
		assert.NoError(t, err)
		assert.Equal(t, org.Name, got.Name)
	})

	t.Run("Update Organisation", func(t *testing.T) {
		org := db.Organisation{Name: "Update Org", Code: "ORG000", ApiKey: "update-api-key"}
		err := orgRepo.Create(&org)
		assert.NoError(t, err)

		org.Name = "Updated Org"
		err = orgRepo.Save(org)
		assert.NoError(t, err)

		got, err := orgRepo.Get(scopes.ByID(org.ID))
		assert.NoError(t, err)
		assert.Equal(t, "Updated Org", got.Name)
	})

	t.Run("Delete Organisation", func(t *testing.T) {
		org := db.Organisation{Name: "Delete Org", Code: "ORG111", ApiKey: "delete-api-key"}
		err := orgRepo.Create(&org)
		assert.NoError(t, err)

		err = orgRepo.Delete(scopes.ByID(org.ID))
		assert.NoError(t, err)

		got, err := orgRepo.Get(scopes.ByID(org.ID))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, got.IsEmpty())
	})

	t.Run("Get By Non-Existing ID", func(t *testing.T) {
		got, err := orgRepo.Get(scopes.ByID(999999)) // Non-existent ID
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, got.IsEmpty()) // Expect nil
	})
}
