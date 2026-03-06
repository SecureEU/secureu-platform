import os
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import List, Dict, Optional, Any
from datetime import datetime, timedelta

class Database:
    def __init__(self):
        self.conn = psycopg2.connect(
            host = os.getenv("POSTGRES_HOST", "postgres"),
            port = os.getenv("POSTGRES_PORT", "5432"),
            database = os.getenv("POSTGRES_DB", "security_inc"),
            user = os.getenv("POSTGRES_USER", "user123"),
            password = os.getenv("POSTGRES_PASSWORD", "password123"),
            cursor_factory = RealDictCursor
        )
        print(f"API connected to PostgreSQL: {os.getenv('POSTGRES_DB')}")

    def get_incidents(
            self,
            limit: int = 10,
            offset: int = 0,
            severity: Optional[str] = None,
            log_type: Optional[str] = None,
            source_host: Optional[str] = None,
            date_from: Optional[datetime] = None,
            date_to: Optional[datetime] = None
    ):
        
        # Query builder
        query = "SELECT * FROM security_incidents WHERE 1=1"
        params = []

        if severity:
            query += " AND severity = %s"
            params.append(severity)

        if log_type:
            query += " AND log_type = %s"
            params.append(log_type)
        
        if source_host:
            query += " AND source_host = %s"
            params.append(source_host)

        if date_from:
            query += " AND created_at >= %s"
            params.append(date_from)
        
        if date_to:
            query += " AND created_at <= %s"
            params.append(date_to)

        # Order by newest entries
        query += " ORDER BY created_at DESC"

        limit = min(limit, 50)
        query += " LIMIT %s OFFSET %s"
        params.extend([limit, offset])

        cursor = self.conn.cursor()
        cursor.execute(query, params)
        results = cursor.fetchall()
        cursor.close()

        return results
    
    def get_incidents_by_timerange(
            self,
            hours: int = 2,
            log_type: Optional[str] = None,
            severity: Optional[str] = None,
            anomaly_only: bool = False
    ):
        cursor = self.conn.cursor()

        query = """
            SELECT id, raw_log_message, severity, log_type, created_at
            FROM security_incidents
            WHERE created_at >= NOW() - INTERVAL '%s hours'
            """
        params = [hours]

        if log_type:
            query += " AND log_type = %s"
            params.append(log_type)

        if severity:
            query += " AND severity = %s"
            params.append(severity)
        
        if anomaly_only:
            query += " AND (analysis_result->>'is_anomaly')::boolean = TRUE"
        
        query += " ORDER BY created_at ASC"

        cursor.execute(query, params)
        results = cursor.fetchall()
        cursor.close()

        return results
    
    def get_single_incident(self, incident_id: int):
        cursor = self.conn.cursor()
        cursor.execute(
            "SELECT * FROM security_incidents WHERE id = %s",
            (incident_id, )
        )
        result = cursor.fetchall()
        cursor.close()

        return result
    
    def get_stats(self,
                  hours: int = 24
    ):
        cursor = self.conn.cursor()

        where_clauses = []
        params = []

        if hours and hours > 0:
            time_filter = "WHERE created_at >= NOW() - INTERVAL '%s hours'" % hours
            time_params = (hours, )
            time_range_key = f"last_{hours}h"
        else:
            time_filter = ""
            time_params = ()
            time_range_key = "all_time"

        # Total incidents within time range
        query = f"SELECT COUNT(*) as total FROM security_incidents {time_filter}"
        if time_params:
            cursor.execute(query, time_params)
        else:
            cursor.execute(query)
        total = cursor.fetchone()["total"]

        # Filter by assigned severity (within time range)
        query = f"""
            SELECT severity, COUNT(*) as count
            FROM security_incidents
            {time_filter}
            GROUP BY severity
            ORDER BY
                CASE severity
                    WHEN 'CRITICAL' THEN 1
                    WHEN 'HIGH' THEN 2
                    WHEN 'MEDIUM' THEN 3
                    WHEN 'LOW' THEN 4
                    WHEN 'INFO' THEN 5
                    ELSE 6
                END   
            """
        if time_params:
            cursor.execute(query, time_params)
        else:
            cursor.execute(query)
        by_severity = {row["severity"]: row["count"] for row in cursor.fetchall()}

        # Filter by log type (within time range)
        query = f"""
            SELECT log_type, COUNT(*) as count
            FROM security_incidents
            {time_filter}
            GROUP BY log_type
        """
        if time_params:
            cursor.execute(query, time_params)
        else:
            cursor.execute(query)
        by_log_type = {row["log_type"]: row["count"] for row in cursor.fetchall()}

        # Top source hosts (within time range)
        query = f"""
            SELECT source_host, COUNT(*) as count
            FROM security_incidents
            {time_filter}
            GROUP BY source_host
            ORDER BY count DESC
            LIMIT 5
        """
        if time_params:
            cursor.execute(query, time_params)
        else:
            cursor.execute(query)
        top_hosts = [
            {"host": row["source_host"], "count": row["count"]}
            for row in cursor.fetchall()
        ]

        cursor.close()

        return {
            "total_incidents": total,
            "by_severity": by_severity,
            "by_log_type": by_log_type,
            "time_range_hours": hours if hours else None,
            "top_source_hosts": top_hosts
        }
    
    def search_by_ip(self, ip_address):
        cursor = self.conn.cursor()
        cursor.execute("""
                SELECT * FROM security_incidents
                WHERE analysis_result->>'source_ip' = %s
                ORDER BY created_at DESC
        """, (ip_address, ))
        results = cursor.fetchall()
        cursor.close()

        return results
    
    def get_recent(self, hours: int = 24):
        cursor = self.conn.cursor()
        cursor.execute("""
                SELECT * FROM security_incidents
                WHERE created_at >= NOW() - INTERVAL '%s hours'
                ORDER BY created_at DESC
        """, (hours, ))
        results = cursor.fetchall()
        cursor.close()

        return results
    
    def close(self):
        if self.conn:
            self.conn.close()
            print("API database connection closed")

db = Database()



