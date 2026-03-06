# install mockgen
go install go.uber.org/mock/mockgen@latest

# make sure GOPATH/bin is in path
export PATH=$PATH:$(go env GOPATH)/bin

# Make certs directories if they don't exist
mkdir -p manager/mocks

# Mock services
mockgen -destination=manager/mocks/mock_logging.go -package=mocks -source=manager/logging/logging.go
mockgen -destination=manager/mocks/mock_encryptionservice.go -package=mocks -source=manager/api/encryptionservice/encryption_service.go
mockgen -destination=manager/mocks/mock_mtlsservice.go -package=mocks -source=manager/mtls/mtls.go
mockgen -destination=manager/mocks/mock_agentauthservice.go -package=mocks -source=manager/api/agentauthenticationservice/authentication_service.go


# Mock repositories
mockgen -destination=manager/mocks/mock_orgRepository.go -package=mocks -source=manager/db/organisations.go
mockgen -destination=manager/mocks/mock_groupRepository.go -package=mocks -source=manager/db/groups.go
mockgen -destination=manager/mocks/mock_agentRepository.go -package=mocks -source=manager/db/agents.go
mockgen -destination=manager/mocks/mock_userRepository.go -package=mocks -source=manager/db/users.go
mockgen -destination=manager/mocks/mock_agentversion.go -package=mocks -source=manager/db/agent_versions.go
mockgen -destination=manager/mocks/mock_groupcertrepository.go -package=mocks -source=manager/db/groupcertRepository.go
mockgen -destination=manager/mocks/mock_roles.go -package=mocks -source=manager/db/roles.go
mockgen -destination=manager/mocks/mock_cas.go -package=mocks -source=manager/db/cas.go
mockgen -destination=manager/mocks/mock_servercerts.go -package=mocks -source=manager/db/server_certs.go
mockgen -destination=manager/mocks/mock_executables.go -package=mocks -source=manager/db/executables.go

