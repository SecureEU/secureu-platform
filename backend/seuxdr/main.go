package main

import (
	"bytes"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {

	// curl -v --insecure --user "admin:?nkRfjd8upF?*gomCIhcTNo8klS1Z41H" https://127.0.0.1:9200/_search/?size=10000 -H "Content-Type: application/json" -d "{"query": {"match_all": {}}}"

	// curl -v --insecure --user "admin:?nkRfjd8upF?*gomCIhcTNo8klS1Z41H" https://192.168.10.7:9288/wazuh-alerts*/_search

	// curl -u 'admin:?nkRfjd8upF?*gomCIhcTNo8klS1Z41H' -k -X POST "https://192.168.10.7:55000/security/user/authenticate"

	// curl -k -X POST "https://192.168.10.7:55000/security/user/authenticate" \
	// -u admin:?nkRfjd8upF?*gomCIhcTNo8klS1Z41H \
	// -H "Content-Type: application/json"

	// OpenSearch/Elasticsearch URL
	url := "https://127.0.0.1:9200/_search/?size=10000"

	// Authentication credentials
	username := "admin"
	password := "?nkRfjd8upF?*gomCIhcTNo8klS1Z41H"

	// JSON payload (match all documents)
	query := map[string]interface{}{
		"query": map[string]interface{}{
			"term": map[string]interface{}{
				"input.type": "log",
			},
		},
	}

	// Convert query to JSON
	jsonData, err := json.Marshal(query)
	if err != nil {
		log.Fatalf("Error marshalling JSON: %v", err)
	}

	// Create HTTP request
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		log.Fatalf("Error creating request: %v", err)
	}

	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.SetBasicAuth(username, password)

	// Create custom HTTP client (disable SSL verification)
	client := &http.Client{
		Timeout: 10 * time.Second, // Set request timeout
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true}, // Ignore SSL certificate errors
		},
	}

	// Send request
	resp, err := client.Do(req)
	if err != nil {
		log.Fatalf("Error making request: %v", err)
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Fatalf("Error reading response body: %v", err)
	}

	// Print response
	fmt.Println("Response Status:", resp.Status)
	fmt.Println("Response Body:", string(body))
	filePath := "testing.json"
	err = os.WriteFile(filePath, []byte(body), 0644)
	if err != nil {
		fmt.Println("Error writing to file:", err)
		return
	}

}
