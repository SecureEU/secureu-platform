//go:build manager
// +build manager

package db_test

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/utils"
	"testing"
	"time"

	_ "github.com/mattn/go-sqlite3"
	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestGroupCertificateRepository(t *testing.T) {
	// Initialize the test database
	dbClient, err := utils.InitTestDb(true)
	assert.Nil(t, err)
	defer utils.RemoveTestDb()

	orgRepo := db.NewOrganisationsRepository(dbClient.DB)
	groupRepo := db.NewGroupRepository(dbClient.DB)
	newOrg := db.Organisation{Name: "Clone Systems", Code: "CS", ApiKey: "HjGw7ISLn_3namBGewQe"}

	err = orgRepo.Create(&newOrg)
	assert.Nil(t, err)

	newGroup := db.Group{Name: "Clone Systems Office", LicenseKey: "123456789012345678901234567890123456", OrgID: &newOrg.ID}
	err = groupRepo.Create(&newGroup)
	assert.Nil(t, err)
	repo := db.NewGroupCertificateRepository(dbClient.DB)

	// Test for Create
	t.Run("Scenario: Successfully create a new group certificate", func(t *testing.T) {
		groupCert := db.GroupCertificate{
			RegistrationCertificate: []byte("cert_data"),
			RegistrationKey:         []byte("key_data"),
			GroupID:                 newGroup.ID,
			ValidUntil:              time.Now().UTC().Add(24 * time.Hour),
		}

		id, err := repo.Create(groupCert)
		assert.NoError(t, err)
		assert.NotZero(t, id)
	})

	t.Run("Scenario: Fail to create a group certificate with invalid data", func(t *testing.T) {
		invalidCert := db.GroupCertificate{}

		_, err := repo.Create(invalidCert)
		assert.Error(t, err)
	})

	// Test for GetByID
	t.Run("Scenario: Successfully retrieve a group certificate by ID", func(t *testing.T) {
		groupCert := db.GroupCertificate{
			RegistrationCertificate: []byte("cert_data"),
			RegistrationKey:         []byte("key_data"),
			GroupID:                 newGroup.ID,
			ValidUntil:              time.Now().UTC().Add(24 * time.Hour),
		}
		id, _ := repo.Create(groupCert)

		retrievedCert, err := repo.Get(scopes.ByID(id))
		assert.NoError(t, err)
		assert.Equal(t, groupCert.GroupID, retrievedCert.GroupID)
	})

	t.Run("Scenario: Attempt to retrieve non-existing certificate by ID", func(t *testing.T) {
		retrievedCert, err := repo.Get(scopes.ByID(999))
		assert.Error(t, err)
		assert.True(t, retrievedCert.IsEmpty())
	})

	// Test for FindByGroupID
	t.Run("Scenario: Retrieve multiple certificates for a group", func(t *testing.T) {
		group2 := db.Group{Name: "Clone Systems Office", LicenseKey: "123456789012345678901234567890123459", OrgID: &newOrg.ID}

		err = groupRepo.Create(&group2)
		assert.Nil(t, err)
		groupCert1 := db.GroupCertificate{
			RegistrationCertificate: []byte("cert_data_1"),
			RegistrationKey:         []byte("key_data_1"),
			GroupID:                 group2.ID,
			ValidUntil:              time.Now().UTC().Add(24 * time.Hour),
		}
		groupCert2 := db.GroupCertificate{
			RegistrationCertificate: []byte("cert_data_2"),
			RegistrationKey:         []byte("key_data_2"),
			GroupID:                 group2.ID,
			ValidUntil:              time.Now().UTC().Add(48 * time.Hour),
		}
		repo.Create(groupCert1)
		repo.Create(groupCert2)

		certs, err := repo.Find(scopes.ByGroupID(group2.ID))
		assert.NoError(t, err)
		assert.Equal(t, 2, len(certs.Certs))
	})

	t.Run("Scenario: Attempt to retrieve certificates for a non-existing group", func(t *testing.T) {
		certs, err := repo.Find(scopes.ByGroupID(999))
		assert.NoError(t, err)
		assert.Equal(t, 0, len(certs.Certs))
	})

	// Test for Delete
	t.Run("Scenario: Successfully delete a certificate by ID", func(t *testing.T) {
		groupCert := db.GroupCertificate{
			RegistrationCertificate: []byte("cert_data"),
			RegistrationKey:         []byte("key_data"),
			GroupID:                 newGroup.ID,
			ValidUntil:              time.Now().UTC().Add(24 * time.Hour),
		}
		id, _ := repo.Create(groupCert)

		err := repo.Delete(id)
		assert.NoError(t, err)

		retrievedCert, err := repo.Get(scopes.ByID(id))
		assert.Error(t, err)
		assert.Equal(t, gorm.ErrRecordNotFound, err)
		assert.True(t, retrievedCert.IsEmpty())
	})

	t.Run("Scenario: Attempt to delete a non-existing certificate", func(t *testing.T) {
		err := repo.Delete(999)
		assert.NoError(t, err) // Deleting a non-existent row should not fail
	})

	// Test for IsEmpty
	t.Run("Scenario: Check if an empty certificate returns true", func(t *testing.T) {
		emptyCert := db.GroupCertificate{}
		assert.True(t, emptyCert.IsEmpty())
	})

	t.Run("Scenario: Check if a non-empty certificate returns false", func(t *testing.T) {
		nonEmptyCert := db.GroupCertificate{
			ID: 1,
		}
		assert.False(t, nonEmptyCert.IsEmpty())
	})
}
