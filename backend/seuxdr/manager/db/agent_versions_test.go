//go:build manager
// +build manager

package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/utils"
	"testing"

	_ "github.com/mattn/go-sqlite3"
	"github.com/stretchr/testify/assert"
)

func TestAgentVersionRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(false)
	assert.Nil(t, err)
	defer utils.RemoveTestDb()

	repo := db.NewAgentVersionRepository(dbClient.DB)

	// Test for Create
	t.Run("Scenario: Successfully create a new agent version", func(t *testing.T) {
		agentVersion := db.AgentVersion{
			AgentVersion: "v1.0.0",
		}

		id, err := repo.Create(agentVersion)
		assert.NoError(t, err)
		assert.NotZero(t, id)
	})

	t.Run("Scenario: Fail to create an agent version with empty data", func(t *testing.T) {
		invalidVersion := db.AgentVersion{}

		_, err := repo.Create(invalidVersion)
		assert.Error(t, err)
	})

	// Test for GetByID
	t.Run("Scenario: Successfully retrieve an agent version by ID", func(t *testing.T) {
		agentVersion := db.AgentVersion{
			AgentVersion: "v1.0.1",
		}
		id, err := repo.Create(agentVersion)
		assert.NoError(t, err)

		retrievedVersion, err := repo.GetByID(id)
		assert.NoError(t, err)
		assert.Equal(t, agentVersion.AgentVersion, retrievedVersion.AgentVersion)
	})

	t.Run("Scenario: Attempt to retrieve a non-existing agent version by ID", func(t *testing.T) {
		retrievedVersion, err := repo.GetByID(999)
		assert.Error(t, err)
		assert.True(t, retrievedVersion.IsEmpty())
	})

	// Test for Update
	t.Run("Scenario: Successfully update an existing agent version", func(t *testing.T) {
		agentVersion := db.AgentVersion{
			AgentVersion: "v1.0.2",
		}
		id, err := repo.Create(agentVersion)
		assert.NoError(t, err)

		agentVersion.ID = id
		agentVersion.AgentVersion = "v1.0.3"
		err = repo.Save(agentVersion)
		assert.NoError(t, err)

		updatedVersion, _ := repo.GetByID(id)
		assert.Equal(t, "v1.0.3", updatedVersion.AgentVersion)
	})

	t.Run("Scenario: Attempt to update a non-existing agent version", func(t *testing.T) {
		nonExistentVersion := db.AgentVersion{
			ID:           999,
			AgentVersion: "non-existing",
		}
		err := repo.Save(nonExistentVersion)
		assert.Error(t, err) // Typically, no error is thrown for non-updates, check affected rows if needed
	})

	// Test for Delete
	t.Run("Scenario: Successfully delete an agent version by ID", func(t *testing.T) {
		agentVersion := db.AgentVersion{
			AgentVersion: "v1.0.4",
		}
		id, err := repo.Create(agentVersion)
		assert.NoError(t, err)

		err = repo.Delete(id)
		assert.NoError(t, err)

		deletedVersion, err := repo.GetByID(id)
		assert.Error(t, err)
		assert.True(t, deletedVersion.IsEmpty())
	})

	t.Run("Scenario: Attempt to delete a non-existing agent version", func(t *testing.T) {
		err := repo.Delete(999)
		assert.NoError(t, err) // Deleting a non-existent row should not fail
	})
}
