# Active Response System

The Active Response System enables SEUXDR to automatically respond to security threats in real-time by executing commands on connected agents based on alert conditions.

## Architecture

The system consists of several key components:

### 1. ConnectionManager
- Tracks active WebSocket connections from agents
- Manages bidirectional communication channels
- Automatically cleans up stale connections

### 2. ActiveResponseService
- Monitors OpenSearch for new security alerts
- Evaluates response rules against incoming alerts
- Dispatches commands to appropriate agents
- Tracks command execution status

### 3. Enhanced WebSocket Communication
- Supports bidirectional message flow (logs + commands)
- Handles different message types (log, command, command_result, heartbeat)
- Maintains backward compatibility with existing log processing

### 4. Response Rules Engine
- Configurable rules for different threat scenarios
- Rule-based filtering by alert level, rule IDs, etc.
- Built-in cooldown mechanisms to prevent spam

## Configuration

Add the following section to your `manager.yaml`:

```yaml
active_response:
  enabled: true                    # Enable/disable active response
  polling_interval: 30             # Alert polling interval in seconds
  min_rule_level: 10              # Minimum Wazuh rule level to process
  max_alerts_batch: 100           # Maximum alerts to process per batch
  cooldown_period: 300            # Cooldown between same commands (seconds)
  command_timeout: 30             # Command execution timeout (seconds)
  cleanup_interval: 60            # Cleanup interval for expired commands (seconds)
```

## Supported Commands

### 1. Block IP Address (`block_ip`)
Blocks suspicious IP addresses using firewall rules.

**Parameters:**
- `ip_address`: IP address to block
- `duration`: Block duration in seconds
- `method`: Blocking method (`iptables`, `firewalld`, `windows_firewall`)

### 2. Kill Process (`kill_process`)
Terminates suspicious processes.

**Parameters:**
- `process_user`: User running the process
- `command`: Command/process name to terminate

### 3. Quarantine File (`quarantine_file`)
Moves suspicious files to quarantine.

**Parameters:**
- `file_path`: Path to file to quarantine
- `quarantine_dir`: Directory to move file to

### 4. Disable User (`disable_user`)
Disables user accounts.

**Parameters:**
- `username`: Username to disable

### 5. Custom Script (`custom_script`)
Executes custom scripts on agents.

**Parameters:**
- `script_path`: Path to script to execute
- `arguments`: Script arguments

## Default Response Rules

The system includes two default rules:

### Block Brute Force Attacks
- **Trigger:** Rule level ≥ 10
- **Action:** Block source IP for 1 hour
- **Cooldown:** 5 minutes

### Kill Suspicious Processes
- **Trigger:** Rule level ≥ 12
- **Action:** Terminate flagged processes
- **Cooldown:** 1 minute
- **Status:** Disabled by default (high impact)

## Message Flow

1. **Agent → Manager:** Log entries via WebSocket
2. **Manager → OpenSearch:** Log storage and indexing
3. **Wazuh → OpenSearch:** Alert generation and analysis
4. **Manager:** Polls OpenSearch for new alerts
5. **Manager:** Evaluates alerts against response rules
6. **Manager → Agent:** Active response commands via WebSocket
7. **Agent → Manager:** Command execution results

## Security Considerations

- All commands are encrypted using existing agent encryption keys
- Commands require valid agent authentication
- Role-based permissions control command execution
- All actions are logged with full audit trail
- Cooldown periods prevent command flooding
- Connection timeouts prevent resource exhaustion

## Agent Integration

Agents must be updated to:
1. Handle bidirectional WebSocket communication
2. Process incoming command messages
3. Execute platform-specific response actions
4. Return command execution results

## Monitoring

The system provides comprehensive logging:
- Connection registration/unregistration
- Alert processing statistics
- Command execution tracking
- Error conditions and failures
- Performance metrics

## Performance

- Asynchronous alert processing (30-second intervals)
- Non-blocking command dispatch
- Automatic cleanup of expired commands
- Connection pooling for multiple agents
- Efficient alert deduplication

## Troubleshooting

### Active Response Not Working
1. Check `active_response.enabled` in configuration
2. Verify OpenSearch connectivity
3. Ensure agents are connected via WebSocket
4. Check manager logs for errors

### Commands Not Executing
1. Verify agent is connected and authenticated
2. Check command timeout settings
3. Review agent logs for execution errors
4. Validate command parameters

### Performance Issues
1. Adjust polling interval
2. Reduce max_alerts_batch size
3. Increase cleanup interval
4. Monitor connection count

## Future Enhancements

- Web-based rule management interface
- Custom rule scripting engine
- Integration with external threat intelligence
- Advanced command templating
- Real-time command status dashboard
- Multi-tenant rule isolation