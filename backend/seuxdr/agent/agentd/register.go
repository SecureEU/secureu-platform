package agentd

import (
	"SEUXDR/agent/comms"
	"SEUXDR/agent/helpers"
	"encoding/json"
	"io"
	"runtime"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

func (agent *agent) Register() error {
	var (
		responsePayload comms.RegistrationResponse
		err             error
	)

	osVersion, err := helpers.GetOSVersion()
	if err != nil {
		agent.logger.LogWithContext(logrus.ErrorLevel, "failed to get OS details for registration. Terminating...", logrus.Fields{"error": err.Error()})
		return err
	}
	osGeneric := runtime.GOOS

	osArch := runtime.GOARCH

	var distro string
	if osGeneric == "linux" {
		distro, err = helpers.GetPackageType()
		if err != nil {
			agent.logger.LogWithContext(logrus.ErrorLevel, "failed to get OS distro details for registration. Terminating...", logrus.Fields{"error": err.Error()})
			return err
		}
	}

	regPayload := comms.RegistrationPayload{
		LicenseKey: agent.Auth.Info.LicenseKey,
		ApiKey:     agent.Auth.Info.ApiKey,
		Name:       agent.Auth.Info.Name,
		Version:    agent.Auth.Info.Version,
		Metadata: comms.AgentMetadata{
			OSVersion:    osVersion,
			OS:           osGeneric,
			Architecture: osArch,
			Distro:       distro,
		},
	}

	if responsePayload, err = agent.communicationService.RegisterAgent(regPayload); err != nil {
		return err
	}
	agent.Auth.Info.AgentID = responsePayload.AgentID
	agent.Auth.Info.GroupID = responsePayload.GroupID
	agent.Auth.Info.OrgID = responsePayload.OrgID
	agent.Auth.Info.AgentUUID = responsePayload.AgentUUID
	agent.Auth.Info.AgentKey = responsePayload.EncryptionKey
	if err := agent.Auth.StoreEncryptedData(agentInfoPath); err != nil {
		return err
	}

	return nil
}

func (agent *agent) getRegistrationCredentials(filePath string) (helpers.Keys, error) {
	var keys helpers.Keys

	// Open the JSON file
	file, err := agent.EmbeddedFiles.Open(filePath)
	if err != nil {
		return keys, errors.Errorf("error opening file: %v", err)
	}
	defer file.Close()

	// Read the file content into a byte slice
	byteValue, err := io.ReadAll(file)
	if err != nil {
		return keys, errors.Errorf("Error reading file: %v", err)
	}

	// Unmarshal the byte slice into the Config struct

	if err := json.Unmarshal(byteValue, &keys); err != nil {
		return keys, errors.Errorf("Error parsing JSON: %v", err)
	}

	return keys, nil

}
