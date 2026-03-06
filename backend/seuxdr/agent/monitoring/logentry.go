package monitoring

import (
	"fmt"
	"regexp"
	"time"
)

// LogEntry represents a parsed log entry.
type LogEntry struct {
	Timestamp time.Time
	Hostname  string
	Program   string
	Message   string
	Username  string
	IPAddress string
	Port      int
}

// ParseLogEntry parses a syslog entry into a LogEntry struct.
func ParseLogEntry(log string) (*LogEntry, error) {
	// Example syslog regex pattern to extract fields
	re := regexp.MustCompile(`(?P<Date>\w{3}\s+\d{1,2}\s+\d{2}:\d{2}:\d{2})\s+(?P<Hostname>\S+)\s+(?P<Program>\S+)\[\d+\]:\s+(?P<Message>.*)`)

	// Extract fields using the regex
	matches := re.FindStringSubmatch(log)
	if matches == nil {
		return nil, fmt.Errorf("log entry did not match expected format")
	}

	entry := &LogEntry{
		Timestamp: time.Now().UTC(), // Simplified: you'd convert the "Date" match to a time.Time
		Hostname:  matches[2],
		Program:   matches[3],
		Message:   matches[4],
	}

	// Additional parsing based on the log message
	reMessage := regexp.MustCompile(`Failed password for (invalid user )?(\S+) from (\S+) port (\d+)`)
	messageMatches := reMessage.FindStringSubmatch(entry.Message)
	if messageMatches != nil {
		entry.Username = messageMatches[2]
		entry.IPAddress = messageMatches[3]
		entry.Port = 22 // Default port for SSH
	}

	return entry, nil
}
