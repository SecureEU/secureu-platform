//go:build linux
// +build linux

package config

// Query struct can handle both simple and complex queries
type Query struct {
	SimpleQuery string `xml:",innerxml"`
}
