package helpers

// Root struct to represent the entire JSON response from opensearch plus our agent map for mapping logs to agents
type OpenSearchData struct {
	Response []Alert        `json:"data"`
	AgentMap []AgentDetails `json:"agent_map"`
}

// Root struct to represent the entire JSON response from OpenSearch
type Response struct {
	Took     int    `json:"took"`
	TimedOut bool   `json:"timed_out"`
	Shards   Shards `json:"_shards"`
	Hits     Hits   `json:"hits"`
	// ScrollID string `json:"_scroll_id,omitempty"`
}

// Shards struct represents the "_shards" field
type Shards struct {
	Total      int `json:"total"`
	Successful int `json:"successful"`
	Skipped    int `json:"skipped"`
	Failed     int `json:"failed"`
}

// Hits struct represents the "hits" field
type Hits struct {
	Total    Total   `json:"total"`
	MaxScore float64 `json:"max_score"`
	Hits     []Alert `json:"hits,omitempty"` // Keeping hits flexible since it can be empty
}

// Total struct represents the "total" object inside "hits"
type Total struct {
	Value    int    `json:"value"`
	Relation string `json:"relation"`
}

// Root struct represents the top-level JSON structure
type Alert struct {
	Index  string        `json:"_index"`
	ID     string        `json:"_id"`
	Score  float64       `json:"_score"`
	Source Source        `json:"_source"`
	Sort   []interface{} `json:"sort,omitempty"`
}

// Source contains the main alert details
type Source struct {
	Predecoder Predecoder `json:"predecoder"`
	Agent      Agent      `json:"agent"`
	Manager    Manager    `json:"manager"`
	Data       Data       `json:"data"`
	Rule       Rule       `json:"rule"`
	Decoder    Decoder    `json:"decoder"`
	FullLog    string     `json:"full_log"`
	Input      Input      `json:"input"`
	Timestamp  string     `json:"@timestamp"`
	Location   string     `json:"location"`
	ID         string     `json:"id"`
	Time       string     `json:"timestamp"`
	
	// Wazuh Active Response Fields (optional)
	ActiveResponse *ActiveResponse `json:"active_response,omitempty"` // Wazuh AR configuration
	Command        *Command        `json:"command,omitempty"`         // AR command details
}

// Predecoder represents the log pre-decoder details
type Predecoder struct {
	Hostname    string `json:"hostname"`
	ProgramName string `json:"program_name"`
	Timestamp   string `json:"timestamp"`
}

// Agent contains information about the reporting agent
type Agent struct {
	Name string `json:"name"`
	ID   string `json:"id"`
}

// Manager represents the manager details
type Manager struct {
	Name string `json:"name"`
}

// Data represents the command execution details and Wazuh alert data
type Data struct {
	// Basic log data
	SrcUser string `json:"srcuser,omitempty"`
	DstUser string `json:"dstuser,omitempty"`
	TTY     string `json:"tty,omitempty"`
	PWD     string `json:"pwd,omitempty"`
	Command string `json:"command,omitempty"`
	
	// Wazuh-specific alert data (optional)
	SrcIP      string `json:"srcip,omitempty"`       // Source IP address
	DstIP      string `json:"dstip,omitempty"`       // Destination IP address
	SrcPort    string `json:"srcport,omitempty"`     // Source port
	DstPort    string `json:"dstport,omitempty"`     // Destination port
	Protocol   string `json:"protocol,omitempty"`    // Network protocol
	ProcessID  string `json:"processid,omitempty"`   // Process ID
	ProcessName string `json:"processname,omitempty"` // Process name
	FileName   string `json:"filename,omitempty"`    // File name
	FilePath   string `json:"filepath,omitempty"`    // File path
	FileHash   string `json:"filehash,omitempty"`    // File hash
	URL        string `json:"url,omitempty"`         // URL (for web-related alerts)
	UserAgent  string `json:"useragent,omitempty"`  // User agent
	Action     string `json:"action,omitempty"`      // Action performed
	Status     string `json:"status,omitempty"`      // Status or result
	Method     string `json:"method,omitempty"`      // HTTP method or similar
}

// Rule contains information about the detection rule
type Rule struct {
	FiredTimes  int      `json:"firedtimes"`
	Mail        bool     `json:"mail"`
	Level       int      `json:"level"`
	Description string   `json:"description"`
	Groups      []string `json:"groups"`
	Mitre       Mitre    `json:"mitre"`
	ID          string   `json:"id"`
}

// Mitre represents the MITRE ATT&CK classification
type Mitre struct {
	Technique []string `json:"technique"`
	ID        []string `json:"id"`
	Tactic    []string `json:"tactic"`
}

// Decoder contains decoding metadata
type Decoder struct {
	Parent     string `json:"parent"`
	Name       string `json:"name"`
	FTSComment string `json:"ftscomment"`
}

// Input represents log input type
type Input struct {
	Type string `json:"type"`
}

type ParsedLog struct {
	Timestamp string
	Name      string
	Category  string
	ID        string
	Source    string
	User      string
	Domain    string
	Computer  string
	Message   string
	GroupID   int
	OrgID     int // Added OrgID field
}

type AgentDetails struct {
	HostName  string `json:"hostname"`
	OS        string `json:"os"`
	GroupName string `json:"group_name"`
	GroupID   int64  `json:"group_id"`
}

// Wazuh Active Response Structures

// ActiveResponse represents Wazuh active response configuration in alerts
type ActiveResponse struct {
	Enabled bool   `json:"enabled,omitempty"`    // Whether AR is enabled for this rule
	Command string `json:"command,omitempty"`    // AR command to execute  
	Type    string `json:"type,omitempty"`       // Type of response (block, quarantine, etc.)
	Timeout int    `json:"timeout,omitempty"`    // Command timeout
	Level   int    `json:"level,omitempty"`      // Minimum level to trigger
}

// Command represents AR command details from Wazuh
type Command struct {
	Name        string   `json:"name,omitempty"`        // Command name
	Executable  string   `json:"executable,omitempty"`  // Executable path
	Arguments   []string `json:"arguments,omitempty"`   // Command arguments
	Expect      string   `json:"expect,omitempty"`      // Expected result
	TimeoutAllowed bool  `json:"timeout_allowed,omitempty"` // Whether timeout is allowed
}
