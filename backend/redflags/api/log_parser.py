import re
from typing import Dict, Optional, List
from dataclasses import dataclass
from datetime import datetime

@dataclass
class ParsedLog:
    '''Structure of a parsed log line.'''
    raw_line: str
    timestamp: Optional[str]
    severity: Optional[str]
    hostname: Optional[str]
    process: Optional[str]
    pid: Optional[str]
    # Extracted information
    ip_addresses: List[str]
    file_paths: List[str]
    ports: List[int]
    urls: List[str]
    # Original message
    original_msg: str
    # Masked semantic message
    semantic_msg: str

class LogParser:
    def __init__(self):
        # ========================================== #
        #    E X T R A C T I O N  P A T T E R N S    #
        # ========================================== #

        # Pattern 1: Standard syslog
        # Example line: May 13 10:16:25 server01 nginx[5157]: Message
        self.syslog_pattern = re.compile(
            r'^(?P<timestamp>\w{3}\s+\d{1,2}\s+\d{2}:\d{2}:\d{2})\s+'
            r'(?P<hostname>\S+)\s+'
            r'(?P<process>\S+?)(?:\[(?P<pid>\d+)\])?:\s+'
            r'(?P<message>.*)$',
            re.IGNORECASE
        )

        # Pattern 2: ISO timestamp format
        # Example: 2025-05-13 10:16:25 ERROR Message
        self.iso_pattern = re.compile(
            r'^(?P<timestamp>\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})'
            r'(?:[.,]\d+)?\s+'
            r'(?:\[(?P<severity>DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\]\s+)?'
            r'(?P<message>.*)$',
            re.IGNORECASE
        )

        # Pattern 3: Windows Event Log style
        # Example: [2025-05-13 10:16:25] [ERROR] Application: Message
        self.windows_pattern = re.compile(
            r'^\[(?P<timestamp>[\d\-:\s]+)\]\s+'
            r'\[(?P<severity>DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\]\s+'
            r'(?:(?P<process>\S+):\s+)?'
            r'(?P<message>.*)$',
            re.IGNORECASE           
        )

        # ============================================ #
        # C T I  E X T R A C T I O N   P A T T E R N S #
        # ============================================ #

        # IP address (IPv4)
        self.ip_pattern = re.compile(
            r'\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}'
            r'(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b'
        )

        # File paths (Unix and Windows)
        self.path_pattern = re.compile(
            r'(?:'
            r'(?:/[\w\.\-]+)+/?|'  # Unix: /path/to/file
            r'(?:[A-Z]:\\[\w\.\-\\]+)'  # Windows: C:\path\to\file
            r')'
        )

        # Ports
        self.port_pattern = re.compile(r'\bport\s+(\d{1,5})\b', re.IGNORECASE)

        # URLs
        self.url_pattern = re.compile(
            r'https?://[^\s<>"{}|\\^`\[\]]+'
        )

        # Email addresses
        self.email_pattern = re.compile(
            r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b'
        )

        # =============================== #
        # M A S K I N G   P A T T E R N S #
        # =============================== #

        self.masking_rules = [
            # IP adress
            (r'\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}'
             r'(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b', '<IP>'),

            # Email
            (r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', '<EMAIL>'),

            # URLs
            (r'https?://[^\s<>"{}|\\^`\[\]]+', '<URL>'),

            # File paths
            (r'(?:/[\w\.\-]+){2,}/?|(?:[A-Z]:\\[\w\.\-\\]+)', '<PATH>'),

            # Hex values
            (r'\b0x[0-9a-fA-F]+\b', '<HEX>'),

            # UUIDs
            (r'\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b', '<UUID>'),

            # Dates
            (r'\b\d{4}[-/]\d{2}[-/]\d{2}\b', '<DATE>'),
            (r'\b\d{2}[-/]\d{2}[-/]\d{4}\b', '<DATE>'),

            # Times
            (r'\b\d{2}:\d{2}:\d{2}(?:\.\d+)?\b', '<TIME>'),

            # Generic numbers
            (r'\b\d+\b', '<NUM>')
        ]

        self.masking_patterns = [
            (re.compile(pattern), replacement)
            for pattern, replacement in self.masking_rules
        ]

        self.severity_pattern = re.compile(
            r'\b(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\b',
            re.IGNORECASE
        )

    def parse(self, log_line: str) -> ParsedLog:
        '''
        Parsing a log line and extracting CTI
        
        INPUT:
            log_line: str, log line from a log file
            
        OUTPUT:
            ParsedLog object with extracted fields
        '''
        parsed = self._try_patterns(log_line)

        if not parsed:
            parsed = {
                "timestamp": None,
                "hostname": None,
                "process": None,
                "pid": None,
                "message": log_line   
            }

        original_msg = parsed["message"]
        severity = self._extract_severity(log_line)
        indicators = self._extract_indicators(original_msg)
        semantic_msg = self._mask_parameters(original_msg)

        if severity != "UNKNOWN":
            semantic_msg = re.sub(
                r'\b' + severity + r'\b',
                '',
                semantic_msg,
                flags = re.IGNORECASE
            ).strip()

        semantic_msg = ' '.join(semantic_msg.split())

        return ParsedLog(
            raw_line = log_line,
            timestamp = parsed.get('timestamp'),
            severity = severity,
            hostname = parsed.get('hostname'),
            process = parsed.get('process'),
            pid = parsed.get('pid'),
            ip_addresses = indicators['ips'],
            file_paths = indicators['paths'],
            ports = indicators['ports'],
            urls = indicators['urls'],
            original_msg = original_msg,
            semantic_msg = semantic_msg
        )
    
    def _try_patterns(self, log_line: str):
        patterns = [
            ('syslog', self.syslog_pattern),
            ('iso', self.iso_pattern),
            ('windows', self.windows_pattern)
        ]

        for format_type, pattern in patterns:
            match = pattern.match(log_line)
            if match:
                result = match.groupdict()
                result["format_type"] = format_type

                return result
            
        return None
    
    def _extract_severity(self, text: str):
        match = self.severity_pattern.search(text)
        if match:
            return match.group(1).upper()
        
        return "UNKNOWN"
    
    def _extract_indicators(self, text: str):
        return {
            "ips": self.ip_pattern.findall(text),
            "paths": self.path_pattern.findall(text),
            "ports": [int(p) for p in self.port_pattern.findall(text)],
            "urls": self.url_pattern.findall(text)
        }
    
    def _mask_parameters(self, text: str):
        masked = text
        
        for pattern, replacement in self.masking_patterns:
            masked = pattern.sub(replacement, masked)

        return masked
    
    def batch_parse(self, log_lines: List[str]):
        return [self.parse(line) for line in log_lines if line.strip()]
    
    def to_dict(self, parsed_log: ParsedLog):
        return {
            "raw_line": parsed_log.raw_line,
            "timestamp": parsed_log.timestamp,
            "severity": parsed_log.severity,
            "hostname": parsed_log.hostname,
            "process": parsed_log.process,
            "pid": parsed_log.pid,
            "indicators": {
                "ip_addresses": parsed_log.ip_addresses,
                "file_paths": parsed_log.file_paths,
                "ports": parsed_log.ports,
                "urls": parsed_log.urls
            },
            "original_message": parsed_log.original_msg,
            "semantic_message": parsed_log.semantic_msg
        }