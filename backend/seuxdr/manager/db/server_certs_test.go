package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestServerCertsRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(false)
	defer utils.RemoveTestDb()
	assert.NoError(t, err)

	repo := db.NewServerCertsRepository(dbClient.DB)

	// Test for Create
	t.Run("Scenario: Successfully create a new server certificate", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_1",
			ServerCertName: "cert_path_1",
			ValidUntil:     time.Now().UTC().Add(24 * time.Hour),
		}

		err := repo.Create(serverCert)
		assert.NoError(t, err)
		assert.NotZero(t, serverCert.ID)
	})

	t.Run("Scenario: Fail to create a server certificate with empty data", func(t *testing.T) {
		invalidCert := &db.ServerCert{}

		err := repo.Create(invalidCert)
		assert.Error(t, err)
	})

	// Test for GetByID
	t.Run("Scenario: Successfully retrieve a server certificate by ID", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_2",
			ServerCertName: "cert_path_2",
			ValidUntil:     time.Now().UTC().Add(24 * time.Hour),
		}
		err := repo.Create(serverCert)
		assert.NoError(t, err)

		retrievedCert, err := repo.Get(scopes.ByID(serverCert.ID))
		assert.NoError(t, err)
		assert.Equal(t, serverCert.ServerKeyName, retrievedCert.ServerKeyName)
		assert.Equal(t, serverCert.ServerCertName, retrievedCert.ServerCertName)
		assert.Equal(t, serverCert.ValidUntil.Unix(), retrievedCert.ValidUntil.Unix())
	})

	t.Run("Scenario: Attempt to retrieve a non-existing server certificate by ID", func(t *testing.T) {
		retrievedCert, err := repo.Get(scopes.ByID(999))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, retrievedCert.IsEmpty())
	})

	// Test for Update
	t.Run("Scenario: Successfully update an existing server certificate", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_3",
			ServerCertName: "cert_path_3",
			ValidUntil:     time.Now().UTC().Add(24 * time.Hour),
		}
		err := repo.Create(serverCert)
		assert.NoError(t, err)

		serverCert.ServerKeyName = "updated_key_path"
		serverCert.ServerCertName = "updated_cert_path"
		err = repo.Save(serverCert)
		assert.NoError(t, err)

		updatedCert, _ := repo.Get(scopes.ByID(serverCert.ID))
		assert.Equal(t, "updated_key_path", updatedCert.ServerKeyName)
		assert.Equal(t, "updated_cert_path", updatedCert.ServerCertName)
	})

	// Test for Delete
	t.Run("Scenario: Successfully delete a server certificate by ID", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_4",
			ServerCertName: "cert_path_4",
			ValidUntil:     time.Now().UTC().Add(24 * time.Hour),
		}
		err := repo.Create(serverCert)
		assert.NoError(t, err)

		err = repo.Delete(scopes.ByID(serverCert.ID))
		assert.NoError(t, err)

		deletedCert, err := repo.Get(scopes.ByID(serverCert.ID))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.True(t, deletedCert.IsEmpty())
	})

	t.Run("Scenario: Attempt to delete a non-existing server certificate", func(t *testing.T) {
		err := repo.Delete(scopes.ByID(999))
		assert.Error(t, err)
	})

	// Test for GetValidCerts
	t.Run("Scenario: Successfully retrieve valid server certificates", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_5",
			ServerCertName: "cert_path_5",
			ValidUntil:     time.Now().UTC().Add(24 * time.Hour),
		}
		err := repo.Create(serverCert)
		assert.NoError(t, err)

		// GetValidCerts()
		validCerts, err := repo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
		assert.NoError(t, err)
		assert.NotEmpty(t, validCerts)
	})

	// Test for GetInvalidCerts
	t.Run("Scenario: Successfully retrieve invalid server certificates", func(t *testing.T) {
		serverCert := &db.ServerCert{
			ServerKeyName:  "key_path_6",
			ServerCertName: "cert_path_6",
			ValidUntil:     time.Now().UTC().Add(-24 * time.Hour),
		}
		err := repo.Create(serverCert)
		assert.NoError(t, err)

		// GetInvalidCerts()
		invalidCerts, err := repo.Find(scopes.ByValidUntilBeforeOrEqual(time.Now().UTC()))
		assert.NoError(t, err)
		assert.NotEmpty(t, invalidCerts)
	})
}
