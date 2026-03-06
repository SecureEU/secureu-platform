import psycopg2
from psycopg2.extras import Json
from datetime import datetime
from typing import Dict, Any, Optional
import config

class PostgresStorage:
    def __init__(self):
        try:
            self.conn = psycopg2.connect(
                host = config.POSTGRES_HOST,
                port = config.POSTGRES_PORT,
                database = config.POSTGRES_DB,
                user = config.POSTGRES_USER,
                password = config.POSTGRES_PASSWORD
            )
            self.conn.autocommit = True
            print(f"Connected to PostgreSQL: {config.POSTGRES_DB}")
        except Exception as e:
            print("Failed to connect...")
            raise
        
    def store_anomaly(self, log_data: Dict[str, Any], analysis_result: Dict[str, Any]):
        try:
            cursor = self.conn.cursor()

            # Extract fields from JSON imported log object
            log_type = log_data.get('fields', {}).get('log_type', 'unknown')
            source_host = log_data.get('fields', {}).get('source', 'unknown')
            raw_message = log_data.get('message', '')

            # Extract fields from LLM output
            severity = analysis_result.get('severity', 'UNKNOWN')

            # Parse event timestamp
            event_timestamp = analysis_result.get('timestamp')
            if event_timestamp:
                try:
                    event_timestamp = datetime.fromisoformat(event_timestamp.replace('Z', '+00:00'))
                except:
                    event_timestamp = None

            # Insert into DB
            query = """
                INSERT INTO security_incidents
                (log_type, source_host, raw_log_message, analysis_result, severity, event_timestamp)
                VALUES (%s, %s, %s, %s, %s, %s)
                RETURNING id
            """
            cursor.execute(query, (
                log_type,
                source_host,
                raw_message,
                Json(analysis_result),
                severity,
                event_timestamp
            ))

            incident_id = cursor.fetchone()[0]
            cursor.close()

            print(f"Stored incident #{incident_id}: [{log_type}] {severity}")
            return incident_id
        except Exception as e:
            print(f" Database error: {e}")
            return None
        
    def get_incident_count(self):
        try:
            cursor = self.conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM security_incidents")
            count = cursor.fetchone()[0]
            cursor.close()
            return count
        except Exception as e:
            print(f"Error getting count: {e}")
            return 0
        
    def close(self):
        if self.conn:
            self.conn.close()
            print("PosrgreSQL connection closed")