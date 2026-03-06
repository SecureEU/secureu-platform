//go:build darwin
// +build darwin

package config

// Query struct supports attributes and complex content
type Query struct {
	Type      string `xml:"type,attr"`  // Capture the 'type' attribute
	Level     string `xml:"level,attr"` // Capture the 'level' attribute
	Predicate string `xml:",chardata"`  // Capture the inner XML content (complex condition)
}
