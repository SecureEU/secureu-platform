package helpers

// Source contains the main alert details
type AlertResponse struct {
	Agent     ResponseAgent `json:"agent"`
	Rule      Rule          `json:"rule"`
	FullLog   string        `json:"full_log"`
	Timestamp string        `json:"@timestamp"`
}

// Agent contains information about the reporting agent
type ResponseAgent struct {
	Name      string `json:"name"`
	OS        string `json:"os"`
	GroupName string `json:"group_name"`
	ID        string `json:"id"`
}

type AlertMetadata struct {
	OS        string
	GroupName string
	HostName  string
}
