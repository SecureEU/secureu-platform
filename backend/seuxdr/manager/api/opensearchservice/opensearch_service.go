package opensearchservice

import (
	conf "SEUXDR/manager/config"
	"SEUXDR/manager/db"
	"SEUXDR/manager/db/scopes"
	"SEUXDR/manager/helpers"
	"SEUXDR/manager/logging"
	"bytes"
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"gorm.io/gorm"
)

type OpenSearchService interface {
	Search(qry helpers.LogQuery) (helpers.OpenSearchData, error)
	Stats(qry helpers.LogQuery) (helpers.AlertStats, error)
	SearchWithoutOrgFilter(timestampRange helpers.TimestampRange) (helpers.OpenSearchData, error)
	getByQuery(qry helpers.LogQuery) error
	getAgentMap(orgID, groupID int64) ([]helpers.AgentDetails, error)
}

type openSearchService struct {
	orgRepo   db.OrganisationsRepository
	groupRepo db.GroupRepository
	agentRepo db.AgentRepository
	RawAlerts helpers.Response
	Alerts    helpers.OpenSearchData
	client    *http.Client
	config    conf.Configuration
	logger    logging.EULogger
}

func NewOpenSearchServiceFactory(dbConn *gorm.DB, logger logging.EULogger) OpenSearchService {
	// Create custom HTTP client (disable SSL verification)
	client := &http.Client{
		Timeout: 10 * time.Second, // Set request timeout
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true}, // Ignore SSL certificate errors
		},
	}
	orgRepo := db.NewOrganisationsRepository(dbConn)
	groupRepo := db.NewGroupRepository(dbConn)
	agentRepo := db.NewAgentRepository(dbConn)

	cfg := conf.GetConfigFunc()()

	return NewOpenSearhcService(orgRepo, groupRepo, agentRepo, client, cfg, logger)

}

func NewOpenSearhcService(orgRepo db.OrganisationsRepository, groupRepo db.GroupRepository, agentRepo db.AgentRepository, client *http.Client, config conf.Configuration, logger logging.EULogger) OpenSearchService {
	return &openSearchService{orgRepo: orgRepo, groupRepo: groupRepo, agentRepo: agentRepo, client: client, config: config, logger: logger}
}

func (searchSvc *openSearchService) Search(qry helpers.LogQuery) (helpers.OpenSearchData, error) {
	var (
		agentMap []helpers.AgentDetails
		err      error
	)

	orgID, err := strconv.Atoi(qry.Query.OrgID)
	if err != nil {
		return searchSvc.Alerts, err
	}

	groupID, err := strconv.Atoi(qry.Query.GroupID)
	if err != nil {
		groupID = 0
	}

	org, err := searchSvc.orgRepo.Get(scopes.ByID(int64(orgID)))
	if err != nil {
		return searchSvc.Alerts, err
	}

	if org.IsEmpty() {
		return searchSvc.Alerts, fmt.Errorf("invalid org id %s", qry.Query.OrgID)
	}

	if err = searchSvc.getByQuery(qry); err != nil {
		return searchSvc.Alerts, err
	}

	if agentMap, err = searchSvc.getAgentMap(org.ID, int64(groupID)); err != nil {
		return searchSvc.Alerts, err
	}

	searchSvc.Alerts.AgentMap = agentMap
	searchSvc.Alerts.Response = searchSvc.RawAlerts.Hits.Hits

	return searchSvc.Alerts, nil

}

// SearchWithoutOrgFilter searches for alerts without filtering by organization or group
// This is used by active response service to process alerts across all tenants
func (searchSvc *openSearchService) SearchWithoutOrgFilter(timestampRange helpers.TimestampRange) (helpers.OpenSearchData, error) {
	var alerts helpers.OpenSearchData

	// Build query without org/group filters - include both logs and alerts
	query := map[string]interface{}{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"should": []map[string]interface{}{
					{
						"term": map[string]interface{}{
							"input.type": "log",
						},
					},
					{
						"term": map[string]interface{}{
							"input.type": "alert",
						},
					},
					{
						"exists": map[string]interface{}{
							"field": "rule.level", // Wazuh alerts always have rule.level
						},
					},
				},
				"minimum_should_match": 1,
				"filter": []map[string]interface{}{
					{
						"range": map[string]interface{}{
							"timestamp": map[string]interface{}{
								"gte":    timestampRange.GTE,
								"lte":    timestampRange.LTE,
								"format": "strict_date_optional_time",
							},
						},
					},
				},
			},
		},
		// _doc breaks ties: sorting on @timestamp alone makes search_after
		// re-read every row sharing a timestamp, which is why this endpoint
		// returned duplicates and the dashboard had to de-duplicate by _id.
		"sort": []map[string]interface{}{
			{
				"@timestamp": map[string]string{
					"order": "desc",
				},
			},
			{
				"_doc": map[string]string{
					"order": "asc",
				},
			},
		},
	}

	// Execute query and collect all results
	var allHits []helpers.Alert
	var lastSort []interface{} // Stores last sort values

	for {
		// Set search_after if it's not the first request
		if len(lastSort) > 0 {
			query["search_after"] = lastSort
		}

		// Execute the query
		hits, err := searchSvc.fetchNextBatch(query)
		if err != nil {
			break
		}

		// Process results
		if len(hits) == 0 {
			break // No more data
		}

		// Update last sort values
		lastSort = hits[len(hits)-1].Sort

		allHits = append(allHits, hits...)
	}

	// Set response without agent mapping (not needed for active response)
	alerts.Response = allHits
	// AgentMap is left empty since active response doesn't need agent details

	return alerts, nil
}

func (searchSvc *openSearchService) getByQuery(qry helpers.LogQuery) error {
	// OpenSearch/Elasticsearch URL

	url := searchSvc.config.WAZUH.URL
	username := searchSvc.config.WAZUH.USERNAME
	password := searchSvc.config.WAZUH.PASSWORD

	orgMatch := fmt.Sprintf("org_id=%s", qry.Query.OrgID)

	// JSON payload (match all documents)
	// Define the query payload with sorting and timestamp range filter
	// Construct the base must clause
	// Wazuh alert documents don't have input.type; filter only by full_log content and timestamp
	mustClauses := []map[string]interface{}{
		{
			"match_phrase": map[string]interface{}{
				"full_log": orgMatch,
			},
		},
	}

	// If group_id is present, add it to the must clause
	if len(qry.Query.GroupID) > 0 {
		mustClauses = append(mustClauses, map[string]interface{}{
			"match_phrase": map[string]interface{}{
				"full_log": fmt.Sprintf("group_id=%s", qry.Query.GroupID),
			},
		})
	}

	// Now build the full query
	query := map[string]interface{}{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"must": mustClauses,
				"filter": []map[string]interface{}{
					{
						"range": map[string]interface{}{
							"timestamp": map[string]interface{}{
								"gte":    qry.Query.GTE,
								"lte":    qry.Query.LTE,
								"format": "strict_date_optional_time",
							},
						},
					},
				},
			},
		},
		// _doc breaks ties: sorting on @timestamp alone makes search_after
		// re-read every row sharing a timestamp, which is why this endpoint
		// returned duplicates and the dashboard had to de-duplicate by _id.
		"sort": []map[string]interface{}{
			{
				"@timestamp": map[string]string{
					"order": "desc",
				},
			},
			{
				"_doc": map[string]string{
					"order": "asc",
				},
			},
		},
	}

	// A caller that asked for a bounded page gets exactly that - no paging loop.
	// The size has to go in the body, so drop the ?size=10000 the configured URL
	// carries - OpenSearch rejects a request that sets size in both places.
	if qry.Query.Size > 0 {
		query["size"] = qry.Query.Size
		url = searchSvc.searchBaseURL()
	}

	// Convert query to JSON
	jsonData, err := json.Marshal(query)
	if err != nil {
		return fmt.Errorf("Error marshalling JSON: %v", err)
	}

	// Create HTTP request
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("Error creating request: %v", err)
	}

	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.SetBasicAuth(username, password)

	// Send request
	resp, err := searchSvc.client.Do(req)
	if err != nil {
		return fmt.Errorf("Failed to fetch alerts: %v", err)
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("Error reading response body: %v", err)
	}

	err = json.Unmarshal(body, &searchSvc.RawAlerts)
	if err != nil {
		return fmt.Errorf("Error reading response body: %v", err)
	}

	// // get list of hits from subsequent requests into alerts list
	// var alerts []helpers.Alert

	// Bounded request: the single response above is the whole answer.
	if qry.Query.Size > 0 {
		return nil
	}

	var lastSort []interface{} // Stores last sort values
	for {
		// Set search_after if it's not the first request
		if len(lastSort) > 0 {
			query["search_after"] = lastSort
		}

		// Execute the query
		hits, err := searchSvc.fetchNextBatch(query)
		if err != nil {
			break
		}

		// Process results
		if len(hits) == 0 {
			break // No more data
		}

		// Update last sort values
		lastSort = hits[len(hits)-1].Sort

		searchSvc.RawAlerts.Hits.Hits = append(searchSvc.RawAlerts.Hits.Hits, hits...)
	}

	return nil
}

// Fetch the next batch of results using the scroll ID
func (searchSvc *openSearchService) fetchNextBatch(query interface{}) ([]helpers.Alert, error) {
	var (
		result helpers.Response
		hits   []helpers.Alert
	)

	url := searchSvc.config.WAZUH.URL
	username := searchSvc.config.WAZUH.USERNAME
	password := searchSvc.config.WAZUH.PASSWORD
	// Convert query to JSON
	jsonData, err := json.Marshal(query)
	if err != nil {
		return hits, fmt.Errorf("Error marshalling JSON: %v", err)
	}

	// Create HTTP request
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return hits, fmt.Errorf("Error creating request: %v", err)
	}

	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.SetBasicAuth(username, password)

	// Send request
	resp, err := searchSvc.client.Do(req)
	if err != nil {
		return hits, fmt.Errorf("Error making request: %v", err)
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return hits, fmt.Errorf("Error reading response body: %v", err)
	}

	err = json.Unmarshal(body, &result)
	if err != nil {
		return hits, fmt.Errorf("Error reading response body: %v", err)
	}

	hits = result.Hits.Hits

	return hits, nil
}

func (searchSvc *openSearchService) getAgentMap(orgID int64, groupID int64) ([]helpers.AgentDetails, error) {

	// Create a map with the struct as the key
	data := []helpers.AgentDetails{}

	if orgID <= 0 {
		return data, errors.New("invalid org id")
	}
	org, err := searchSvc.orgRepo.Get(scopes.ByID(orgID))
	if err != nil {
		return data, err
	}

	groups, err := searchSvc.groupRepo.Find(scopes.ByOrgID(org.ID))
	if err != nil {
		return data, err
	}

	agentScopes := []func(*gorm.DB) *gorm.DB{}
	if groupID > 0 {
		agentScopes = append(agentScopes, scopes.ByGroupID(groupID))
	} else {
		var groupIDs []int64
		for _, group := range groups {
			groupIDs = append(groupIDs, group.ID)
		}
		agentScopes = append(agentScopes, scopes.ByGroupIDs(groupIDs))
	}

	agents, err := searchSvc.agentRepo.Find([]string{"Group"}, agentScopes...)
	if err != nil {
		return data, err
	}

	// prepare a map for finding the agent OS in O(1)
	for _, agent := range agents {
		cleanedName := strings.TrimSuffix(agent.Name, ".local")
		data = append(data, helpers.AgentDetails{HostName: strings.TrimSpace(cleanedName), GroupID: *agent.GroupID, OS: agent.OS, GroupName: agent.Group.Name})
	}

	return data, nil
}

// searchBaseURL is the configured search endpoint without its query string.
// The configured value carries "?size=10000" for the unbounded alert listing;
// callers that set size in the body (a bounded page, or size 0 for an
// aggregation) must not also send it in the URL.
func (searchSvc *openSearchService) searchBaseURL() string {
	u := searchSvc.config.WAZUH.URL
	if i := strings.Index(u, "?"); i >= 0 {
		u = u[:i]
	}
	return strings.TrimSuffix(u, "/")
}

// Stats answers the dashboard's questions - how many alerts, how many critical,
// the split by MITRE tactic and by agent, and the hourly trend - with a single
// aggregation query.
//
// The dashboards used to fetch every alert in the range and count them in the
// browser. On a live deployment that is tens of thousands of documents and tens
// of megabytes per page load, to render six numbers and two charts. OpenSearch
// does the counting here and returns a few kilobytes.
func (searchSvc *openSearchService) Stats(qry helpers.LogQuery) (helpers.AlertStats, error) {
	var stats helpers.AlertStats

	orgMatch := fmt.Sprintf("org_id=%s", qry.Query.OrgID)
	mustClauses := []map[string]interface{}{
		{"match_phrase": map[string]interface{}{"full_log": orgMatch}},
	}
	if len(qry.Query.GroupID) > 0 {
		mustClauses = append(mustClauses, map[string]interface{}{
			"match_phrase": map[string]interface{}{"full_log": fmt.Sprintf("group_id=%s", qry.Query.GroupID)},
		})
	}

	// Only a fixed set of bucket widths is accepted - the value goes straight
	// into the OpenSearch query and an unknown one just fails the request.
	interval := "1h"
	switch qry.Query.Interval {
	case "1h", "3h", "6h", "12h", "1d", "7d":
		interval = qry.Query.Interval
	}

	query := map[string]interface{}{
		// size 0: we want the counts, never the documents.
		"size": 0,
		// Without this hits.total caps at 10000 and the dashboard shows 10000
		// alerts no matter how many there really are.
		"track_total_hits": true,
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"must": mustClauses,
				"filter": []map[string]interface{}{
					{"range": map[string]interface{}{
						"timestamp": map[string]interface{}{
							"gte":    qry.Query.GTE,
							"lte":    qry.Query.LTE,
							"format": "strict_date_optional_time",
						},
					}},
				},
			},
		},
		"aggs": map[string]interface{}{
			"by_tactic": map[string]interface{}{
				"terms": map[string]interface{}{"field": "rule.mitre.tactic", "size": 20},
			},
			"by_agent": map[string]interface{}{
				"terms": map[string]interface{}{"field": "agent.name", "size": 10},
			},
			"trend": map[string]interface{}{
				"date_histogram": map[string]interface{}{
					"field":          "timestamp",
					"fixed_interval": interval,
					"min_doc_count":  0,
				},
				// Severity bands per bucket - the trend chart plots one line each.
				"aggs": map[string]interface{}{
					"severity": map[string]interface{}{
						"range": map[string]interface{}{
							"field": "rule.level",
							"ranges": []map[string]interface{}{
								{"key": "low", "to": 4},
								{"key": "medium", "from": 4, "to": 8},
								{"key": "high", "from": 8, "to": 12},
								{"key": "critical", "from": 12},
							},
						},
					},
				},
			},
			// Wazuh rule levels >= 12 are what the dashboard calls critical.
			"critical": map[string]interface{}{
				"filter": map[string]interface{}{
					"range": map[string]interface{}{"rule.level": map[string]interface{}{"gte": 12}},
				},
			},
		},
	}

	jsonData, err := json.Marshal(query)
	if err != nil {
		return stats, fmt.Errorf("error marshalling stats query: %v", err)
	}

	req, err := http.NewRequest("POST", searchSvc.searchBaseURL(), bytes.NewBuffer(jsonData))
	if err != nil {
		return stats, fmt.Errorf("error creating stats request: %v", err)
	}
	req.Header.Set("Content-Type", "application/json")
	req.SetBasicAuth(searchSvc.config.WAZUH.USERNAME, searchSvc.config.WAZUH.PASSWORD)

	resp, err := searchSvc.client.Do(req)
	if err != nil {
		return stats, fmt.Errorf("failed to fetch alert stats: %v", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return stats, fmt.Errorf("error reading stats response: %v", err)
	}

	var parsed struct {
		Hits struct {
			Total struct {
				Value int `json:"value"`
			} `json:"total"`
		} `json:"hits"`
		Aggregations struct {
			ByTactic struct {
				Buckets []struct {
					Key      string `json:"key"`
					DocCount int    `json:"doc_count"`
				} `json:"buckets"`
			} `json:"by_tactic"`
			ByAgent struct {
				Buckets []struct {
					Key      string `json:"key"`
					DocCount int    `json:"doc_count"`
				} `json:"buckets"`
			} `json:"by_agent"`
			Trend struct {
				Buckets []struct {
					KeyAsString string `json:"key_as_string"`
					DocCount    int    `json:"doc_count"`
					Severity    struct {
						Buckets []struct {
							Key      string `json:"key"`
							DocCount int    `json:"doc_count"`
						} `json:"buckets"`
					} `json:"severity"`
				} `json:"buckets"`
			} `json:"trend"`
			Critical struct {
				DocCount int `json:"doc_count"`
			} `json:"critical"`
		} `json:"aggregations"`
	}
	if err := json.Unmarshal(body, &parsed); err != nil {
		return stats, fmt.Errorf("error parsing stats response: %v", err)
	}

	stats.Total = parsed.Hits.Total.Value
	stats.Critical = parsed.Aggregations.Critical.DocCount
	for _, b := range parsed.Aggregations.ByTactic.Buckets {
		stats.ByTactic = append(stats.ByTactic, helpers.AlertBucket{Key: b.Key, Count: b.DocCount})
	}
	for _, b := range parsed.Aggregations.ByAgent.Buckets {
		stats.ByAgent = append(stats.ByAgent, helpers.AlertBucket{Key: b.Key, Count: b.DocCount})
	}
	for _, b := range parsed.Aggregations.Trend.Buckets {
		bucket := helpers.TrendBucket{Key: b.KeyAsString, Count: b.DocCount}
		for _, sev := range b.Severity.Buckets {
			switch sev.Key {
			case "critical":
				bucket.Critical = sev.DocCount
			case "high":
				bucket.High = sev.DocCount
			case "medium":
				bucket.Medium = sev.DocCount
			case "low":
				bucket.Low = sev.DocCount
			}
		}
		stats.Trend = append(stats.Trend, bucket)
	}

	return stats, nil
}
