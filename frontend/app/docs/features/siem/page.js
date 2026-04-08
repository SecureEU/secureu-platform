'use client';
import { useTheme } from '@/components/docs/ThemeProvider';

export default function SIEMPage() {
  const { theme } = useTheme();
  const isDark = theme === 'dark';
  const h1 = `text-3xl font-bold mb-4 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h2 = `text-2xl font-semibold mt-8 mb-3 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const h3 = `text-xl font-semibold mt-6 mb-2 ${isDark ? 'text-white' : 'text-gray-900'}`;
  const p = `mb-4 leading-relaxed ${isDark ? 'text-gray-300' : 'text-gray-700'}`;
  const code = `${isDark ? 'bg-gray-800 text-green-400' : 'bg-gray-100 text-gray-800'} rounded px-3 py-2 block overflow-x-auto text-sm font-mono my-3 whitespace-pre`;
  const li = `mb-2 ${isDark ? 'text-gray-300' : 'text-gray-700'}`;

  return (
    <div>
      <h1 className={h1}>SIEM Dashboard (SEUXDR)</h1>
      <p className={p}>
        The SIEM module provides host-based intrusion detection and security event monitoring using SEUXDR agents and the Wazuh analysis engine. It collects logs from monitored hosts, analyzes them against thousands of detection rules, and surfaces security alerts with MITRE ATT&CK classification.
      </p>

      <h2 className={h2}>Architecture</h2>
      <p className={p}>The SIEM pipeline consists of:</p>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>SEUXDR Agents</strong> - Lightweight Go binaries deployed on monitored hosts. Collect logs from journald, syslog, and configured log files. Communicate with the manager via mTLS WebSocket.</li>
        <li className={li}><strong>SEUXDR Manager</strong> (port 8443/8081) - Go server running inside a Docker container. Receives agent logs, writes them to queue files, manages agent enrollment and certificates.</li>
        <li className={li}><strong>Wazuh Engine</strong> - Runs inside the manager container. Reads queue files via logcollector, applies 1800+ detection rules, generates alerts with MITRE ATT&CK mappings.</li>
        <li className={li}><strong>Wazuh Indexer</strong> - OpenSearch instance inside the container. Stores alerts for querying. Filebeat ships alerts from Wazuh to the indexer.</li>
        <li className={li}><strong>Active Response</strong> - Automated threat response system. Monitors alerts with rule level &gt;= 10 and can trigger commands on agents (block IP, kill process, quarantine file).</li>
      </ul>

      <h2 className={h2}>Agent Deployment</h2>
      <h3 className={h3}>Generating an Agent</h3>
      <p className={p}>Agents are generated from the SIEM dashboard. Navigate to <strong>DefSec &rarr; SIEM</strong>, create an organization and group, then generate an agent binary:</p>
      <ol className="list-decimal pl-6 mb-4">
        <li className={li}>Create an organization (e.g., &quot;Clone Systems&quot;)</li>
        <li className={li}>Create a group within the organization</li>
        <li className={li}>Click &quot;Generate Agent&quot; and select OS (Linux/Windows/macOS), architecture, and distribution</li>
        <li className={li}>Download the agent package</li>
      </ol>

      <h3 className={h3}>Installing on Linux (Debian/Ubuntu)</h3>
      <pre className={code}>{`sudo dpkg -i seuxdr_<OrgName>_<GroupID>_linux_amd64.deb`}</pre>
      <p className={p}>The agent installs as a systemd service and starts automatically. It connects to the manager via mTLS on port 8081.</p>

      <h3 className={h3}>Removing an Agent</h3>
      <pre className={code}>{`sudo dpkg --remove seuxdr`}</pre>

      <h2 className={h2}>Agent Configuration</h2>
      <p className={p}>The agent configuration is at <code>/var/seuxdr/config/agent.conf</code>. It defines which log sources to monitor:</p>
      <pre className={code}>{`<localfile>
  <log_format>journald</log_format>
  <location>journald</location>
</localfile>
<localfile>
  <log_format>syslog</log_format>
  <location>/var/log/auth.log</location>
</localfile>
<localfile>
  <log_format>syslog</log_format>
  <location>/var/log/syslog</location>
</localfile>`}</pre>
      <p className={p}>Supported log formats: <code>journald</code> and <code>syslog</code>. For Apache/Nginx logs, use <code>syslog</code> format with the correct file path.</p>

      <h2 className={h2}>Dashboard Features</h2>
      <ul className="list-disc pl-6 mb-4">
        <li className={li}><strong>Total Alerts</strong> - Count of all security events in the selected time range</li>
        <li className={li}><strong>Critical Alerts</strong> - Events with Wazuh rule level &gt;= 12</li>
        <li className={li}><strong>Active Agents</strong> - Currently connected agents</li>
        <li className={li}><strong>Alerts by Attack Tactic</strong> - Pie chart showing MITRE ATT&CK tactic distribution</li>
        <li className={li}><strong>Top Agents by Alerts</strong> - Bar chart of agents generating the most alerts</li>
        <li className={li}><strong>Alert Table</strong> - Searchable, sortable table of all alerts with severity, description, agent, and timestamp</li>
      </ul>

      <h2 className={h2}>MITRE ATT&CK Mapping</h2>
      <p className={p}>
        Alerts are automatically mapped to MITRE ATT&CK tactics and techniques. This mapping comes from Wazuh&apos;s rule definitions and the MITRE database at <code>/var/ossec/var/db/mitre.db</code>. Each rule specifies technique IDs (e.g., T1110 for Brute Force), which are resolved to tactics (e.g., Credential Access) at analysis time.
      </p>

      <h2 className={h2}>Custom Detection Rules</h2>
      <p className={p}>Add custom Wazuh rules inside the manager container:</p>
      <pre className={code}>{`docker exec -it seuxdr-manager bash
vi /var/ossec/etc/rules/local_rules.xml
systemctl restart wazuh-manager`}</pre>
      <p className={p}>Example rule for detecting vsFTPd backdoor exploitation:</p>
      <pre className={code}>{`<group name="local,exploit">
  <rule id="100100" level="15">
    <decoded_as>vsftpd</decoded_as>
    <match>:)</match>
    <description>vsFTPd 2.3.4 backdoor exploitation (CVE-2011-2523)</description>
    <mitre>
      <id>T1190</id>
    </mitre>
  </rule>
</group>`}</pre>

      <h2 className={h2}>Manager Configuration</h2>
      <p className={p}>The SEUXDR manager is configured via <code>/seuxdr/manager/manager.yaml</code> inside the container. Key settings:</p>
      <pre className={code}>{`tls_port: 8443        # Dashboard API
mtls_port: 8081       # Agent registration
domain: "192.168.1.173"

wazuh:
  url: "https://127.0.0.1:9200/_search/?size=10000"
  username: "admin"
  password: "admin"

active_response:
  enabled: true
  min_rule_level: 10
  polling_interval: 30`}</pre>
    </div>
  );
}
