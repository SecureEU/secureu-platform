package config

// Config structs for the OSSEC configuration
type SEUConfig struct {
	Client    ClientConfig    `xml:"client"`
	Syscheck  SyscheckConfig  `xml:"syscheck"`
	Rootcheck RootcheckConfig `xml:"rootcheck"`
	Localfile []Localfile     `xml:"localfile"`
}

type ClientConfig struct {
	ServerIP string `xml:"server-ip"`
}

type SyscheckConfig struct {
	Frequency   int      `xml:"frequency"`
	Directories []string `xml:"directories"`
	Ignore      []string `xml:"ignore"`
	NoDiff      []string `xml:"nodiff"`
}

type RootcheckConfig struct {
	RootkitFiles   string `xml:"rootkit_files"`
	RootkitTrojans string `xml:"rootkit_trojans"`
}

type Localfile struct {
	LogFormat  string `xml:"log_format"`
	Location   string `xml:"location"`
	Query      Query  `xml:"query,omitempty"`
	Historical string `xml:"historical,omitempty"`
}

// QueryList represents the more complex query structure
type QueryList struct {
	Query QueryDetails `xml:"Query"`
}

type QueryDetails struct {
	Id     string `xml:"Id,attr"`
	Path   string `xml:"Path,attr"`
	Select Select `xml:"Select"`
}

type Select struct {
	Path string `xml:"Path,attr"`
	Text string `xml:",innerxml"`
}
