package db

import (
	"embed"
	"fmt"
	"io/fs"
	"log"
	"sort"
	"strings"

	"github.com/pkg/errors"

	gormSqlite "github.com/glebarez/sqlite" // Alias for GORM driver
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

type DBClient struct {
	DB             *gorm.DB
	Path           string
	MigrationsPath string
}

func NewDBClient(dbPath string, migrationsPath string, migrationsFS *embed.FS) (DBClient, error) {
	var dbClient DBClient
	dsn := "file:" + dbPath + "?cache=shared&mode=rwc&_pragma=foreign_keys(1)"

	// Use GORM with modernc.org/sqlite driver
	gormDB, err := gorm.Open(gormSqlite.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Silent),
	}) // use silent logger so that gorm doesn't log its logs to the journal

	// Updated alias
	if err != nil {
		return dbClient, errors.Wrap(err, "failed to open sqlite DB")
	}

	// Use modernc.org/sqlite driver
	migrationFiles, err := getSQLFiles(migrationsPath, migrationsFS)
	if err != nil {
		return dbClient, errors.Wrap(err, "failed to open sqlite DB")
	}

	for _, mFile := range migrationFiles {
		if err := runMigration(gormDB, mFile, migrationsFS); err != nil {
			return dbClient, err
		}
	}

	dbClient = DBClient{DB: gormDB, Path: dbPath, MigrationsPath: migrationsPath}

	return dbClient, nil
}

func getSQLFiles(directory string, migrationsFS *embed.FS) ([]string, error) {
	var sqlFiles []string

	// Read directory contents
	files, err := fs.ReadDir(migrationsFS, directory)
	if err != nil {
		log.Fatalf("Error reading directory: %v", err)
	}

	// Loop through the directory contents
	for _, file := range files {

		// If the file is not a directory and has a .sql extension
		if !file.IsDir() && file.Name() != "" && len(file.Name()) > 4 && file.Name()[len(file.Name())-4:] == ".sql" {
			sqlFiles = append(sqlFiles, fmt.Sprintf("%s/%s", directory, file.Name()))

		}
	}
	sort.Strings(sqlFiles)
	return sqlFiles, nil
}

func runMigration(db *gorm.DB, migrationFile string, migrationsFS *embed.FS) error {
	// Read the migration file (SQL)
	migrationSQL, err := migrationsFS.ReadFile(migrationFile)
	if err != nil {
		return fmt.Errorf("failed to read migration file: %w", err)
	}

	// Split the migration file into individual statements (assuming each statement ends with a semicolon)
	statements := strings.Split(string(migrationSQL), ";")

	// Execute each SQL statement in the migration
	err = db.Transaction(func(tx *gorm.DB) error {
		for _, stmt := range statements {
			stmt = strings.TrimSpace(stmt)
			// stmt = removeNewlines(stmt)
			if stmt == "" {
				continue
			}

			if err := tx.Exec(stmt).Error; err != nil {
				return fmt.Errorf("failed to execute migration statement: %w", err)
			}
		}
		return nil
	})
	if err != nil {
		return fmt.Errorf("migration failed: %w", err)
	}

	return nil
}
