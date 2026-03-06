package db

import (
	"database/sql"
	"fmt"
	"log"
	"path/filepath"

	"github.com/glebarez/sqlite"
	"github.com/golang-migrate/migrate"
	"github.com/golang-migrate/migrate/database/sqlite3"
	"github.com/golang-migrate/migrate/source/file"
	"github.com/pkg/errors"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

type DBClient struct {
	DB             *gorm.DB
	Path           string
	MigrationsPath string
}

func NewDBClient(dbPath string, migrationsPath string, foreignKeysEnabled bool) (DBClient, error) {
	var dbClient DBClient

	fEnabled := "0"
	if foreignKeysEnabled {
		fEnabled = "1"
	}

	dsn := "file:" + dbPath + "?cache=shared&mode=rwc&_pragma=foreign_keys(" + fEnabled + ")"

	// Use GORM with sqlite driver
	sqliteDb, err := sql.Open("sqlite3", dsn)

	// Updated alias
	if err != nil {
		return dbClient, errors.Wrap(err, "failed to open sqlite DB")
	}
	driver, err := sqlite3.WithInstance(sqliteDb, &sqlite3.Config{})
	if err != nil {
		return dbClient, fmt.Errorf("creating sqlite3 db driver failed %s", err)
	}

	absPath, err := filepath.Abs(migrationsPath)
	if err != nil {
		log.Fatalf("Error getting absolute path: %v", err)
	}

	fSrc, err := (&file.File{}).Open(absPath)
	if err != nil {
		log.Fatal(err)
	}

	log.Println(migrationsPath)

	m, err := migrate.NewWithInstance("file", fSrc, "sqlite3", driver)
	if err != nil {
		return dbClient, fmt.Errorf("initializing db migration failed %s", err)
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		return dbClient, fmt.Errorf("migrating database failed %s", err)
	}

	gormDB, err := gorm.Open(sqlite.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		return dbClient, err
	}

	dbClient = DBClient{DB: gormDB, Path: dbPath, MigrationsPath: migrationsPath}

	return dbClient, nil
}
