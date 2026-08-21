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





	// OpenSearch/Elasticsearch URL
	url := "https://127.0.0.1:9200/_search/?size=10000"

	// Authentication credentials come from the environment; never hardcode them.
	// They are written to /seuxdr/manager/.env by startup.sh after the Wazuh
	// install (INDEXER_USERNAME / INDEXER_PASSWORD).
	username := os.Getenv("INDEXER_USERNAME")
	password := os.Getenv("INDEXER_PASSWORD")
	if username == "" || password == "" {
		fmt.Println("INDEXER_USERNAME and INDEXER_PASSWORD must be set")
		os.Exit(1)
	}

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
