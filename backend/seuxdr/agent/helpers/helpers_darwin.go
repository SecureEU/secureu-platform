//go:build darwin
// +build darwin

package helpers

import (
	"SEUXDR/agent/config"
	"fmt"
	"sort"
	"strings"
)

// ConvertQueryToPredicate converts a Query struct into a valid predicate string
func ConvertQueryToPredicate(query config.Query) (string, error) {
	if query.Type == "" || query.Level == "" || query.Predicate == "" {
		return "", fmt.Errorf("query is missing required attributes (type, level, or predicate)")
	}

	// Build the predicate string
	predicate := fmt.Sprintf(
		"((%s) AND (%s))",
		formatEventType(query.Type),
		query.Predicate,
	)

	return predicate, nil
}

// formatEventType formats the 'type' attribute into a valid predicate list
func formatEventType(eventTypes string) string {
	// Split types into a slice and wrap each type with quotes
	types := strings.Split(eventTypes, ",")

	evtTypes := map[string]struct{}{}

	for _, t := range types {
		switch t {
		case "log":
			evtTypes["logEvent"] = struct{}{}
		case "activity":
			evtTypes["activityCreateEvent"] = struct{}{}
			evtTypes["activityTransitionEvent"] = struct{}{}
		case "trace":
			evtTypes["traceEvent"] = struct{}{}
		}
	}

	// Extract map keys into a slice
	finalTypes := make([]string, 0, len(evtTypes))
	for key := range evtTypes {
		finalTypes = append(finalTypes, key)
	}

	// Sort the keys so that the same query in different order doesn't trigger a new predicate
	sort.Strings(finalTypes)

	// Format the sorted keys into the desired output format
	for i, t := range finalTypes {
		if i != len(finalTypes)-1 {
			finalTypes[i] = fmt.Sprintf("eventType=\"%s\" OR ", strings.TrimSpace(t))
		} else {
			finalTypes[i] = fmt.Sprintf("eventType=\"%s\"", strings.TrimSpace(t))
		}
	}

	return strings.Join(finalTypes, "")
}
