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

func TestCARepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(false)
	assert.Nil(t, err)
	defer utils.RemoveTestDb()

	repo := db.NewCARepository(dbClient.DB)

	// Test for Create
	t.Run("Scenario: Successfully create a new CA", func(t *testing.T) {
		ca := &db.CA{
			CAKeyName:  "path/to/ca_key.pem",
			CACertName: "path/to/ca_cert.pem",
			ValidUntil: time.Now().UTC().AddDate(1, 0, 0), // 1 year validity
		}

		err := repo.Create(ca)
		assert.NoError(t, err)
		assert.NotZero(t, ca.ID)
	})

	t.Run("Scenario: Fail to create CA with invalid data", func(t *testing.T) {
		invalidCA := &db.CA{
			CAKeyName:  "",
			CACertName: "",
			ValidUntil: time.Now().UTC(),
		}

		err := repo.Create(invalidCA)
		assert.Error(t, err)
	})

	// Test for GetByID
	t.Run("Scenario: Successfully retrieve a CA by ID", func(t *testing.T) {
		ca := &db.CA{
			CAKeyName:  "path/to/ca_key2.pem",
			CACertName: "path/to/ca_cert2.pem",
			ValidUntil: time.Now().UTC().AddDate(1, 0, 0),
		}
		err := repo.Create(ca)
		assert.NoError(t, err)

		retrievedCA, err := repo.Get(scopes.ByID(ca.ID))
		assert.NoError(t, err)
		assert.Equal(t, ca.CAKeyName, retrievedCA.CAKeyName)
		assert.Equal(t, ca.CACertName, retrievedCA.CACertName)
		assert.Equal(t, ca.ValidUntil.Unix(), retrievedCA.ValidUntil.Unix())
	})

	t.Run("Scenario: Attempt to retrieve non-existing CA by ID", func(t *testing.T) {
		retrievedCA, err := repo.Get(scopes.ByID(999))
		assert.Error(t, err)
		assert.True(t, retrievedCA.IsEmpty())
	})

	// Test for Update
	t.Run("Scenario: Successfully update an existing CA", func(t *testing.T) {
		ca := &db.CA{
			CAKeyName:  "path/to/ca_key3.pem",
			CACertName: "path/to/ca_cert3.pem",
			ValidUntil: time.Now().UTC().AddDate(1, 0, 0),
		}
		err := repo.Create(ca)
		assert.NoError(t, err)

		ca.CAKeyName = "path/to/ca_key3_updated.pem"
		err = repo.Save(ca)
		assert.NoError(t, err)

		updatedCA, err := repo.Get(scopes.ByID(ca.ID))
		assert.NoError(t, err)
		assert.Equal(t, "path/to/ca_key3_updated.pem", updatedCA.CAKeyName)
	})

	// Test for Delete
	t.Run("Scenario: Successfully delete a CA by ID", func(t *testing.T) {
		ca := &db.CA{
			CAKeyName:  "path/to/ca_key4.pem",
			CACertName: "path/to/ca_cert4.pem",
			ValidUntil: time.Now().UTC().AddDate(1, 0, 0),
		}
		err := repo.Create(ca)
		assert.NoError(t, err)

		err = repo.Delete(scopes.ByID(ca.ID))
		assert.NoError(t, err)

		deletedCA, err := repo.Get(scopes.ByID(ca.ID))
		assert.ErrorIs(t, gorm.ErrRecordNotFound, err)
		assert.Zero(t, deletedCA.ID)
	})

	t.Run("Scenario: Attempt to delete a non-existing CA", func(t *testing.T) {
		err := repo.Delete(scopes.ByID(999))
		assert.Error(t, err)
	})

	// Test for GetLatestCA
	t.Run("Scenario: Successfully retrieve the latest CA", func(t *testing.T) {
		ca1 := &db.CA{
			CAKeyName:  "path/to/ca_key_early.pem",
			CACertName: "path/to/ca_cert_early.pem",
			ValidUntil: time.Now().UTC().AddDate(0, 1, 0), // 1 month from now
		}
		ca2 := &db.CA{
			CAKeyName:  "path/to/ca_key_latest.pem",
			CACertName: "path/to/ca_cert_latest.pem",
			ValidUntil: time.Now().UTC().AddDate(1, 0, 0), // 1 year from now
		}
		_ = repo.Create(ca1)
		_ = repo.Create(ca2)

		// get latest CA
		latestCA, err := repo.Get(scopes.OrderBy("valid_until", "DESC"))
		assert.NoError(t, err)
		assert.Equal(t, ca2.CAKeyName, latestCA.CAKeyName)
	})

	// Test for GetValidCAs
	t.Run("Scenario: Successfully retrieve all valid CAs", func(t *testing.T) {
		validCAs, err := repo.Find(scopes.ByValidUntilAfter(time.Now().UTC()))
		assert.NoError(t, err)
		assert.GreaterOrEqual(t, len(validCAs), 1)
	})

	// Test for GetInvalidCAs
	t.Run("Scenario: Successfully retrieve all invalid CAs", func(t *testing.T) {
		ca := &db.CA{
			CAKeyName:  "path/to/expired_ca_key.pem",
			CACertName: "path/to/expired_ca_cert.pem",
			ValidUntil: time.Now().UTC().AddDate(-1, 0, 0), // expired 1 year ago
		}
		_ = repo.Create(ca)

		invalidCAs, err := repo.Find(scopes.ByValidUntilBeforeOrEqual(time.Now().UTC()))
		assert.NoError(t, err)
		assert.GreaterOrEqual(t, len(invalidCAs), 1)
	})
}
