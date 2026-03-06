import os
import glob
import json
from typing import List, Dict, Optional, Any
from datetime import datetime
from collections import deque

class RawLogReader:
    def __init__(self, log_dir):
        self.log_dir = log_dir

    def find_latest_log_file(self):
        pattern = os.path.join(self.log_dir, "aggregated*.ndjson")
        files = glob.glob(pattern)

        if not files:
            return None
        
        return max(files, key = os.path.getctime)
    
    def read_last_n_lines(
            self,
            filepath: str,
            n: int = 100,
            max_bytes: int = 10 * 1024 * 1024
    ):
        if not os.path.exists(filepath):
            return []
        
        file_size = os.path.getsize(filepath)

        if file_size == 0:
            return []
        
        lines = deque(maxlen = n)

        with open(filepath, "rb") as f:
            offset = min(file_size, max_bytes)
            f.seek(-offset, os.SEEK_END)

            chunk = f.read(offset)

            text = chunk.decode("utf-8", errors = "ignore")
            raw_lines = text.split("\n")

            if offset < file_size:
                raw_lines = raw_lines[1:]

            for line in raw_lines:
                line = line.strip()
                if not line:
                    continue

                try:
                    parsed = json.loads(line) 
                    lines.append(parsed)
                except json.JSONDecodeError:
                    continue
        
        return list(lines)
    
    def get_recent_logs(
            self,
            n: int = 100,
            log_type: Optional[str] = None,
            source_host: Optional[str] = None
    ):
        latest_file = self.find_latest_log_file()

        if not latest_file:
            return {
                "total_returned": 0,
                "source_file": None,
                "logs": [],
                "message": "No log files found"
            }
        
        all_logs = self.read_last_n_lines(latest_file, n = n*2)

        filtered_logs = []
        for log in all_logs:
            fields = log.get("fields", {})

            if log_type and fields.get("log_type") != log_type:
                continue

            if source_host and fields.get("source_host") != source_host:
                continue

            filtered_logs.append(log)

            if len(filtered_logs) >= n:
                break

        return {
            "total_returned": len(filtered_logs),
            "source_file": os.path.basename(latest_file),
            "logs": filtered_logs
        }
    
    def get_statistics(self):
        pattern = os.path.join(self.log_dir, "aggregated*.ndjson")
        files = glob.glob(pattern)

        if not files:
            return {
                "total_files": 0,
                "total_size_bytes": 0,
                "latest_file": None
            }
        
        total_size = sum(os.path.getsize(f) for f in files)
        latest = max(files, key = os.path.getctime)

        return {
            "total_files": len(files),
            "total_size_bytes": total_size,
            "total_size_mb": round(total_size / (1024 * 1024), 2),
            "latest_file": {
                "name": os.path.basename(latest),
                "size_bytes": os.path.getsize(latest),
                "created_at": datetime.fromtimestamp(os.path.getctime(latest)).isoformat()     
            }
        }

LOG_DIR = os.getenv("LOG_INPUT_PATH")
raw_log_reader = RawLogReader(LOG_DIR)   