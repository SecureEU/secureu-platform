from drain3 import TemplateMiner
from drain3.template_miner_config import TemplateMinerConfig
from typing import List, Dict, Any
import os

from log_parser import ParsedLog, LogParser

class DrainAnalyzer:
    def __init__(self, config_file: str = "drain3.ini"):
        self.config_file = config_file
        config = TemplateMinerConfig()
        self.template_miner = TemplateMiner(config = config)

        if os.path.exists(config_file):
            config.load(config_file)

    def extract_templates(self, logs):
        template_stats = {}
        parser = LogParser()

        for log_entry in logs:
            raw_log = log_entry.get("raw_log_message", "")
            parsed_message = parser.parse(raw_log)
            semantic_part = parsed_message.original_msg
            result = self.template_miner.add_log_message(semantic_part)

            template_id = result["cluster_id"]
            if template_id not in template_stats:
                template_stats[template_id] = {
                    "template": result["template_mined"],
                    "count": 0,
                    "example_logs": [],
                    "severity_distribution": {}
                }
            template_stats[template_id]["count"] += 1

            if len(template_stats[template_id]["example_logs"]) < 3:
                template_stats[template_id]["example_logs"].append(raw_log)

            severity = log_entry.get("severity", "UNKNOWN")
            template_stats[template_id]["severity_distribution"][severity] = \
                template_stats[template_id]["severity_distribution"].get(severity, 0) + 1
            
        sorted_templates = sorted(
            template_stats.items(),
            key = lambda x: x[1]["count"],
            reverse = True
        )

        return {
            "total_logs_processed": len(logs),
            "total_templates": len(template_stats),
            "templates": [{
                "template_id": template_id,
                "template": data["template"],
                "count": data["count"],
                "percentage": round((data["count"] / len(logs) * 100), 2) if logs else 0,
                "severity_distribution": data["severity_distribution"],
                "example_logs": data["example_logs"]
        }
        for template_id, data in sorted_templates
            ]
        }
    
drain_analyzer = DrainAnalyzer()