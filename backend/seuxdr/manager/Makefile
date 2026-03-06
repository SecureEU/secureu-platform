migration_up:
	migrate -path database/migrations/ -database "sqlite3://storage/manager.db" -verbose up

migration_down:
	migrate -path database/migrations/ -database "sqlite3://storage/manager.db" -verbose down

migration_fix:
	migrate -path database/migrations/ -database "sqlite3://storage/manager.db" force 2

