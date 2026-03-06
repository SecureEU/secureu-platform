// Updated Executable model for single version approach

package db

import (
	"time"

	"gorm.io/gorm"
)

// Update your Executable model
type Executable struct {
	ID             int64     `gorm:"primaryKey;autoIncrement"`
	CreatedAt      time.Time `gorm:"autoCreateTime"`
	UpdatedAt      time.Time `gorm:"autoUpdateTime"`
	AgentVersionID int64     `gorm:"not null"`
	OS             string    `gorm:"type:varchar(50);not null"`
	Architecture   string    `gorm:"type:varchar(50);not null"`
	Distro         string    `gorm:"type:varchar(50)"`

	// Store BOTH the raw executable and the installation package
	RawExecutable       []byte `gorm:"type:longblob;not null"` // NEW: Raw agent binary
	InstallationPackage []byte `gorm:"type:longblob;not null"` // Existing: ZIP/TAR.GZ with installers

	GroupID         int64  `gorm:"not null"`
	FileName        string `gorm:"type:varchar(255);not null"` // Installation package filename
	RawFileName     string `gorm:"type:varchar(255);not null"` // NEW: Raw executable filename
	Checksum        string `gorm:"type:varchar(128)"`          // Checksum of raw executable
	PackageChecksum string `gorm:"type:varchar(128)"`          // NEW: Checksum of installation package
	FileSize        int64  `gorm:"default:0"`                  // Size of raw executable
	PackageSize     int64  `gorm:"default:0"`                  // NEW: Size of installation package

	// Relationships
	Group        Group        `gorm:"foreignKey:GroupID"`
	AgentVersion AgentVersion `gorm:"foreignKey:AgentVersionID"`
}

// IsEmpty checks if the Executable struct is effectively empty
func (e *Executable) IsEmpty() bool {
	return e.ID == 0 &&
		e.AgentVersionID == 0 &&
		e.OS == "" &&
		e.Architecture == "" &&
		e.Distro == "" &&
		len(e.RawExecutable) == 0 &&
		len(e.InstallationPackage) == 0 &&
		e.GroupID == 0 &&
		e.FileName == "" &&
		e.RawFileName == "" &&
		e.CreatedAt.IsZero() &&
		e.UpdatedAt.IsZero()
}

// Unique constraint prevents duplicate executables:
// CREATE UNIQUE INDEX idx_executables_unique ON executables(agent_version_id, group_id, os, architecture, distro);

type ExecutableRepository interface {
	Create(executable *Executable) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*Executable, error)
	Save(executable *Executable) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Executable, error)
	FindByGroupVersionAndPlatform(groupID, versionID int64, os, architecture, distro string) (*Executable, error)
	DeleteByGroupAndVersion(groupID, versionID int64) error
	FindByVersion(versionID int64) ([]Executable, error)
	// New methods for raw executable lookup
	FindRawExecutableByVersionAndPlatform(versionID int64, os, architecture string) (*Executable, error)
}

type executableRepository struct {
	db *gorm.DB
}

func NewExecutableRepository(db *gorm.DB) ExecutableRepository {
	return &executableRepository{db: db}
}

func (repo *executableRepository) Create(executable *Executable) error {
	return repo.db.Create(executable).Error
}

func (repo *executableRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*Executable, error) {
	var executable Executable
	err := repo.db.Scopes(scopes...).Take(&executable).Error
	return &executable, err
}

func (repo *executableRepository) Save(executable *Executable) error {
	return repo.db.Save(executable).Error
}

func (repo *executableRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := repo.db.Scopes(scopes...).Delete(&Executable{})
	return result.Error
}

func (repo *executableRepository) Find(scopes ...func(*gorm.DB) *gorm.DB) ([]Executable, error) {
	var executables []Executable
	err := repo.db.Scopes(scopes...).Find(&executables).Error
	return executables, err
}

// FindByGroupVersionAndPlatform finds executable for specific group, version and platform
func (repo *executableRepository) FindByGroupVersionAndPlatform(groupID, versionID int64, os, architecture, distro string) (*Executable, error) {
	var executable Executable
	query := repo.db.Where("group_id = ? AND agent_version_id = ? AND os = ? AND architecture = ?",
		groupID, versionID, os, architecture)

	if distro != "" {
		query = query.Where("distro = ?", distro)
	} else {
		query = query.Where("distro IS NULL OR distro = ''")
	}

	err := query.Take(&executable).Error
	return &executable, err
}

// DeleteByGroupAndVersion deletes all executables for a group and version
func (repo *executableRepository) DeleteByGroupAndVersion(groupID, versionID int64) error {
	return repo.db.Where("group_id = ? AND agent_version_id = ?", groupID, versionID).Delete(&Executable{}).Error
}

// FindByVersion returns all executables for a specific version
func (repo *executableRepository) FindByVersion(versionID int64) ([]Executable, error) {
	var executables []Executable
	err := repo.db.Where("agent_version_id = ?", versionID).Find(&executables).Error
	return executables, err
}

// Add new method to repository
func (repo *executableRepository) FindRawExecutableByVersionAndPlatform(versionID int64, os, architecture string) (*Executable, error) {
	var executable Executable
	err := repo.db.Where("agent_version_id = ? AND os = ? AND architecture = ?",
		versionID, os, architecture).Take(&executable).Error
	return &executable, err
}
