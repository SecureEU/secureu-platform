package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestCreateExecutable(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	repo := db.NewExecutableRepository(dbClient.DB)

	exe := db.Executable{
		OS:                  "linux",
		Architecture:        "amd64",
		RawExecutable:       []byte("fake binary data"),
		InstallationPackage: []byte("fake package data"),
		FileName:            "test_executable",
		RawFileName:         "test_executable_raw",
		AgentVersionID:      1,
		GroupID:             1,
	}

	err = repo.Create(&exe)
	assert.NoError(t, err)
	assert.Greater(t, exe.ID, int64(0), "Executable ID should be greater than zero")

	// Verify that the executable was created.
	savedExe, err := repo.Get(scopes.ByID(exe.ID))
	assert.NoError(t, err)
	assert.NotNil(t, savedExe, "Executable should be found")
	assert.Equal(t, exe.OS, savedExe.OS)
	assert.Equal(t, exe.Architecture, savedExe.Architecture)
	assert.Equal(t, exe.FileName, savedExe.FileName)
	assert.Equal(t, exe.RawExecutable, savedExe.RawExecutable)
	assert.Equal(t, exe.InstallationPackage, savedExe.InstallationPackage)
}

func TestGetExecutableByID(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	repo := db.NewExecutableRepository(dbClient.DB)

	exe := db.Executable{
		OS:                  "windows",
		Architecture:        "amd64",
		RawExecutable:       []byte("fake binary data"),
		InstallationPackage: []byte("fake package data"),
		FileName:            "test_executable",
		RawFileName:         "test_executable_raw",
		AgentVersionID:      1,
		GroupID:             1,
	}

	_ = repo.Create(&exe)

	t.Run("should return executable when ID exists", func(t *testing.T) {
		savedExe, err := repo.Get(scopes.ByID(exe.ID))
		assert.NoError(t, err)
		assert.NotNil(t, savedExe)
		assert.Equal(t, exe.OS, savedExe.OS)
		assert.Equal(t, exe.Architecture, savedExe.Architecture)
	})

	t.Run("should return nil when ID does not exist", func(t *testing.T) {
		savedExe, err := repo.Get(scopes.ByID(9999))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, savedExe.IsEmpty())
	})
}

func TestGetExecutablesByGroupID(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	repo := db.NewExecutableRepository(dbClient.DB)

	var groupID int64 = 1

	exe1 := db.Executable{
		OS:                  "linux",
		Architecture:        "amd64",
		RawExecutable:       []byte("fake binary data 1"),
		InstallationPackage: []byte("fake package data 1"),
		FileName:            "test_executable_1",
		RawFileName:         "test_executable_1_raw",
		AgentVersionID:      1,
		GroupID:             groupID,
	}
	exe2 := db.Executable{
		OS:                  "windows",
		Architecture:        "amd64",
		RawExecutable:       []byte("fake binary data 2"),
		InstallationPackage: []byte("fake package data 2"),
		FileName:            "test_executable_2",
		RawFileName:         "test_executable_2_raw",
		AgentVersionID:      1,
		GroupID:             groupID,
	}

	err = repo.Create(&exe1)
	assert.NoError(t, err)
	assert.Equal(t, exe1.ID, int64(1), "Executable ID should be equal to 1")

	err = repo.Create(&exe2)
	assert.NoError(t, err)
	assert.Equal(t, exe2.ID, int64(2), "Executable ID should be equal to 2")

	executables, err := repo.Find(scopes.ByGroupID(1))
	assert.NoError(t, err)
	assert.Len(t, executables, 2, "Expected 2 executables for group ID 1")
}

func TestDeleteExecutable(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	repo := db.NewExecutableRepository(dbClient.DB)

	exe := db.Executable{
		OS:                  "macos",
		Architecture:        "amd64",
		RawExecutable:       []byte("fake binary data"),
		InstallationPackage: []byte("fake package data"),
		FileName:            "test_executable",
		RawFileName:         "test_executable_raw",
		AgentVersionID:      1,
		GroupID:             1,
	}

	_ = repo.Create(&exe)

	err = repo.Delete(scopes.ByID(exe.ID))
	assert.NoError(t, err)

	// Verify that the executable no longer exists.
	savedExe, err := repo.Get(scopes.ByID(exe.ID))
	assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
	assert.True(t, savedExe.IsEmpty())
}

func TestGetExecutablesByCriteria(t *testing.T) {
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.Nil(t, err)

	var groupID int64 = 2

	repo := db.NewExecutableRepository(dbClient.DB)

	exe := db.Executable{
		OS:                  "linux",
		Architecture:        "arm64",
		RawExecutable:       []byte("fake binary data"),
		InstallationPackage: []byte("fake package data"),
		FileName:            "test_executable",
		RawFileName:         "test_executable_raw",
		AgentVersionID:      1,
		GroupID:             groupID,
	}

	_ = repo.Create(&exe)

	executables, err := repo.Find(scopes.ByArchitecture("arm64"), scopes.ByOS("linux"), scopes.ByGroupID(2))
	assert.NoError(t, err)
	assert.Len(t, executables, 1)
	assert.Equal(t, exe.FileName, executables[0].FileName)
}
