package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestAgentRepository(t *testing.T) {
	dbClient, err := utils.InitTestDb(true)
	assert.NoError(t, err)
	defer utils.RemoveTestDb()

	agentRepo := db.NewAgentRepository(dbClient.DB)

	t.Run("Create", func(t *testing.T) {
		agent := &db.Agent{
			Name:    "TestAgent",
			AgentID: "unique-agent-1",
		}
		err := agentRepo.Create(agent)
		assert.NoError(t, err)
		assert.NotZero(t, agent.ID)
	})

	t.Run("Get", func(t *testing.T) {
		agent := &db.Agent{
			Name:    "TestAgent",
			AgentID: "unique-agent-2",
		}
		err := agentRepo.Create(agent)
		assert.NoError(t, err)

		fetchedAgent, err := agentRepo.Get(scopes.ByAgentUUID("unique-agent-2"))
		assert.NoError(t, err)
		assert.NotNil(t, fetchedAgent)
		assert.Equal(t, "TestAgent", fetchedAgent.Name)
	})

	t.Run("Update", func(t *testing.T) {
		agent := &db.Agent{
			Name:    "OldName",
			AgentID: "unique-agent-3",
		}
		err := agentRepo.Create(agent)
		assert.NoError(t, err)

		agent.Name = "NewName"
		err = agentRepo.Save(agent)
		assert.NoError(t, err)

		updatedAgent, err := agentRepo.Get(scopes.ByAgentUUID("unique-agent-3"))
		assert.NoError(t, err)
		assert.Equal(t, "NewName", updatedAgent.Name)
	})

	t.Run("Delete", func(t *testing.T) {
		agent := &db.Agent{
			Name:    "TestAgent",
			AgentID: "unique-agent-4",
		}
		err := agentRepo.Create(agent)
		assert.NoError(t, err)

		err = agentRepo.Delete(scopes.ByID(agent.ID))
		assert.NoError(t, err)

		deletedAgent, err := agentRepo.Get(scopes.ByAgentUUID("unique-agent-4"))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, deletedAgent.IsEmpty())
	})

	t.Run("Find", func(t *testing.T) {
		// Ensure a clean state by removing existing records
		dbClient.DB.Exec("DELETE FROM agents")

		agentRepo.Create(&db.Agent{Name: "Agent1", AgentID: "unique-agent-5"})
		agentRepo.Create(&db.Agent{Name: "Agent2", AgentID: "unique-agent-6"})
		agentRepo.Create(&db.Agent{Name: "Agent3", AgentID: "unique-agent-7"})

		agents, err := agentRepo.Find([]string{})
		assert.NoError(t, err)
		assert.Len(t, agents, 3) // Should match only the newly inserted records
	})
}
