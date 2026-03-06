package crons

import (
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"log"
	"time"
)

func StartSessionCleanup(repo db.SessionRepository) {
	err := repo.Delete(scopes.ByValid(false))
	if err != nil {
		log.Printf("Failed to clean invalidated sessions: %v\n", err)
	}
	err = repo.Delete(scopes.ByExpiresAtLessThan(time.Now()))
	if err != nil {
		log.Printf("Failed to clean expired sessions: %v\n", err)
	}
}
