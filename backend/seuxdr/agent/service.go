package main

import (
	"SEUXDR/agent/agentd"

	"github.com/kardianos/service"
)

type program struct {
	agent agentd.Agent
}

// Start is called by the Service package to start the service.
// Start should not block, so it starts agent in a goroutine.
func (p *program) Start(s service.Service) error {
	go p.run()
	return nil
}

// run contains the main logic of the service.
// Here we start the agent and continue running until Stop is called.
func (p *program) run() {
	go p.agent.Start()
}

// Stop is called by the Service package to stop the service.
func (p *program) Stop(s service.Service) error {
	p.agent.Stop() // Assuming Stop is a method on agent that handles graceful shutdown
	return nil
}
