package db

import (
	"errors"
	"time"

	"gorm.io/gorm"
)

type userRepository struct {
	db *gorm.DB
}

type User struct {
	ID             int64         `gorm:"primaryKey;autoIncrement"`
	CreatedAt      time.Time     `gorm:"not null;default:CURRENT_TIMESTAMP"`
	UpdatedAt      time.Time     `gorm:"default:CURRENT_TIMESTAMP"`
	FirstName      string        `gorm:"size:100;not null"`
	LastName       string        `gorm:"size:100;not null"`
	Email          string        `gorm:"size:255;unique;not null"`
	Password       string        `gorm:"size:60;not null"` // Assumes hashed password (bcrypt etc.)
	IsPasswordTemp *int          `gorm:"default:1"`
	Role           string        `gorm:"not null;check:role IN ('employee','manager','admin','eu_admin')"`
	OrgID          *int64        // Nullable foreign key to organisation
	Org            *Organisation `gorm:"foreignKey:OrgID"` // Association
	GroupID        *int64        // Nullable foreign key to group
	Group          *Group        `gorm:"foreignKey:GroupID"` // Association
	ParentID       *int64        `gorm:"index"`              // References another user (creator, manager etc.)
}

func (u *User) IsEmpty() bool {
	return u.ID == 0 &&
		u.CreatedAt.IsZero() &&
		u.UpdatedAt.IsZero() &&
		u.FirstName == "" &&
		u.LastName == "" &&
		u.Email == "" &&
		u.OrgID == nil &&
		u.GroupID == nil &&
		u.Role == ""
}

// UserRepository defines the interface for interacting with the users table
type UserRepository interface {
	Create(user *User) error
	Get(scopes ...func(*gorm.DB) *gorm.DB) (*User, error)
	Save(user *User) error
	Delete(scopes ...func(*gorm.DB) *gorm.DB) error
	FindTopLevelParent(userID int64) (*User, error)
	GetUserAncestry(userID int64) ([]User, error)
	GetUserDescendants(userID int64) ([]User, error)
	Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]User, error)
}

func NewUserRepository(db *gorm.DB) UserRepository {
	return &userRepository{db: db}
}

func (userRepo *userRepository) Get(scopes ...func(*gorm.DB) *gorm.DB) (*User, error) {
	var user User

	err := userRepo.db.Scopes(scopes...).Take(&user).Error

	return &user, err
}

// Create inserts a new user record into the database
func (userRepo *userRepository) Create(user *User) error {
	return userRepo.db.Create(user).Error
}

// Update modifies an existing user record
func (userRepo *userRepository) Save(user *User) error {
	return userRepo.db.Save(user).Error
}

// Delete marks a user as deleted by setting the deleted_at field
func (userRepo *userRepository) Delete(scopes ...func(*gorm.DB) *gorm.DB) error {
	result := userRepo.db.Scopes(scopes...).Delete(&User{})
	if result.RowsAffected == 0 {
		return errors.New("no record found to delete")
	}
	return result.Error
}

func (userRepo *userRepository) Find(preloads []string, scopes ...func(*gorm.DB) *gorm.DB) ([]User, error) {
	var users []User
	// var orgs []Organisation
	// Apply scopes
	query := userRepo.db.Scopes(scopes...)

	// Dynamically apply preloads
	for _, preload := range preloads {
		query = query.Preload(preload)
	}

	// Use the modified query with preloads
	return users, query.Find(&users).Error
}

func (userRepo *userRepository) FindTopLevelParent(userID int64) (*User, error) {
	var current User
	if err := userRepo.db.
		Preload("Org").
		Preload("Group").
		First(&current, userID).Error; err != nil {
		return nil, err
	}

	for current.ParentID != nil {
		var parent User
		if err := userRepo.db.
			Preload("Org").
			Preload("Group").
			First(&parent, *current.ParentID).Error; err != nil {
			return nil, err
		}
		current = parent
	}

	return &current, nil
}

func (userRepo *userRepository) GetUserAncestry(userID int64) ([]User, error) {
	var ancestry []User

	var current User
	if err := userRepo.db.First(&current, userID).Error; err != nil {
		return nil, err
	}

	ancestry = append(ancestry, current)

	for current.ParentID != nil {
		var parent User
		if err := userRepo.db.First(&parent, *current.ParentID).Error; err != nil {
			return nil, err
		}
		ancestry = append(ancestry, parent)
		current = parent
	}

	// reverse the order so top-level is first
	for i, j := 0, len(ancestry)-1; i < j; i, j = i+1, j-1 {
		ancestry[i], ancestry[j] = ancestry[j], ancestry[i]
	}

	return ancestry, nil
}

func (userRepo *userRepository) GetUserDescendants(userID int64) ([]User, error) {
	var descendants []User

	query := `
	WITH RECURSIVE user_tree AS (
		SELECT * FROM users WHERE id = ?
		UNION ALL
		SELECT u.* FROM users u
		INNER JOIN user_tree ut ON u.parent_id = ut.id
	)
	SELECT * FROM user_tree WHERE id != ?
	`

	if err := userRepo.db.Raw(query, userID, userID).Scan(&descendants).Error; err != nil {
		return nil, err
	}

	return descendants, nil
}
