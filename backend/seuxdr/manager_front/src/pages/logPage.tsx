import type { TableColumnsType } from 'antd';
import LogTable from '../components/logTable'
import { useState, useEffect } from 'react';
import { fetchAlerts } from '../utils/requests'; // Import the fetch function
import convertToUTCAndFriendlyFormat from '../utils/utils';
import DemoPie from '../components/pieChart';
import '../styles/common.css'
import {PieData, ExtractedAlert, ChartData, StackedBarData, OpenSearchQuery, MenuItem} from '../utils/types';
import FlexList from '../components/flexList';
import StackedBarChart, { createChartOptions } from '../components/demoStack';
import { DatePicker, message } from "antd";
import  dayjs,{ Dayjs } from "dayjs";
import { OpenSearchData,AgentDetails, AgentValue, Alert } from '../utils/opensearch_types';
import HeaderBar from '../components/headerBar';
import SideBar from "../components/sideBar";
import { useParams, useLocation } from 'wouter';
import { v4 as uuidv4 } from 'uuid';


const { RangePicker } = DatePicker;
  
const timestampPatterns = [
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})/, // ISO 8601
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?([+-]\d{2}:\d{2}|Z)/,
  /^\w{3} \d{2} \d{2}:\d{2}:\d{2}/,                        // syslog
  /^\w{3}, \d{2} \w{3} \d{4} \d{2}:\d{2}:\d{2} [+-]\d{4}/, // RFC 2822
  /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}/,
  /^\d{10}/,
  /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}(?: [AP]M)?/,
  /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}/,
  /^\[\d{2}\/\w{3}\/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]/,
  /^\d{8}\d{6}/,
  /^\w{3,9}, \w{3,9} \d{1,2}, \d{4} \d{2}:\d{2}:\d{2}/,
  /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/
];

const LogPage = ({sideBarItems} : {sideBarItems : MenuItem[]}) => {

  const { org_id } = useParams();

  function MapRawAlertToResponseAlert(alert: Alert, metadata:any) {
      return {
        key:  uuidv4(),
        timestamp: convertToUTCAndFriendlyFormat( alert._source["@timestamp"] ?? null),
        agent: metadata.HostName ?? "Unknown",
        group_name: metadata.GroupName ?? "",
        os: metadata.OS ?? "",
        attacker_tactic:  alert._source.rule?.mitre?.technique ?? "N/A",
        description: alert._source.rule?.description ?? "No description",
        severity: alert._source.rule?.level ?? 0
    };
  }

  function mapAgentsByKey(agents: AgentDetails[] | undefined | null): Map<string, AgentValue> {
    const agentMap = new Map<string, AgentValue>();
    if (!Array.isArray(agents)) {
      console.warn("mapAgentsByKey: Provided agents is not an array", agents);
      return agentMap; // Return empty map
    }
    for (const agent of agents) {
      const key = `${agent.hostname}|${agent.group_id}`;
      agentMap.set(key, { os: agent.os, group_name: agent.group_name });
    }
  return agentMap;
}
  

  function processData(rawAlerts: OpenSearchData) {

    const agentMap = mapAgentsByKey(rawAlerts.agent_map)
    console.log("AGENT MAP: ", agentMap)
    
    const processedAlerts: ExtractedAlert[] = []

    rawAlerts?.data?.forEach(alert => {
      const fullLog: string = alert._source.full_log;
      // console.log(fullLog)
      let hostname: string = "";
      let groupID: number = 0;
  
      if (fullLog.includes("WinEvtLog")) {
          const windowsRegex = /^(.+?)\s+WinEvtLog:\s+([^:]+):\s+([^\(]+)\((\d+)\):\s+([^:]+):\s+([^:]+):\s+([^:]+):\s+([^:]+):\s+([^\[]+)(?:\s+\[[^\]]*\])?\s*:\s*\[group_id=(\d+)\]\s*\[org_id=(\d+)\]$/;
          const match = fullLog.match(windowsRegex);
  
          if (match) {
              hostname = match[8];
              groupID = parseInt(match[10], 10);
          }
      } else {
          try {
              const {  hostname: parsedHostname, groupID: gID } = parseSyslog(fullLog);
              hostname = parsedHostname;
              groupID = parseInt(gID, 10);
          } catch (err) {
              message.error("failed to process data: " + err)
              return;
          }
      }
  
      const key = `${hostname}|${groupID}`;
      const val = agentMap.get(key);
  
      if (!val) {
          return;
      }
  
      const metadata = { OS: val.os, GroupName: val.group_name, HostName: hostname };
      const alertResponse = MapRawAlertToResponseAlert(alert, metadata);
      processedAlerts.push(alertResponse);
  });
  console.log(processedAlerts)
  return processedAlerts
  }

function parseSyslog(syslog: string): { timestamp: string, hostname: string, groupID: string } {
  let timestamp = "";
  for (const pattern of timestampPatterns) {
      const match = syslog.match(new RegExp(pattern));
      if (match) {
          timestamp = match[0];
          break;
      }
  }
  
  if (!timestamp) throw new Error("Timestamp not found");
  
  const remainingLog = syslog.replace(timestamp, '').trim();
  const parts = remainingLog.split(/\s+/);
  if (parts.length < 2) throw new Error("Hostname not found");
  
  const hostname = parts[0];
  const groupIDMatch = syslog.match(/\[group_id=(\d+)\]/);
  if (!groupIDMatch) throw new Error("group_id not found");
  return { timestamp, hostname, groupID: groupIDMatch[1] };
}

  
  // State declarations
  // const [alerts, setAlerts] = useState<Alert[]>([]);
  const [barExtractedAlerts, setBarExtractedAlerts] = useState<ExtractedAlert[]>([]);
  const [extractedAlerts, setExtractedAlerts] = useState<ExtractedAlert[]>([]);
  const [agentFilters, setAgentFilters] = useState<ChartData[]>([]);
  const [groupFilters, setGroupFilters] = useState<ChartData[]>([]);
  const [attackFilters, setAttackFilters] = useState<ChartData[]>([]);
  const [alertsByTechnique, setAlertsByTechnique] = useState<PieData[]>([]);
  const [barDatasetLabels, setBarDatasetLabels] = useState<string[]>([]);
  const [barLabels, setBarLabelsData] = useState<string[]>([]);
  const [groupedAlerts, setGroupedAlerts] = useState<StackedBarData[]>([]);
  const [location, setLocation] = useLocation();
  
  
  // Create agent filters from extracted alerts
  const getAgentFilters = (extractedAlerts: ExtractedAlert[]) => {
    const uniqueAgents = [...new Set(extractedAlerts.map(alert => alert.agent).filter(Boolean))];

    if (uniqueAgents.length === 0) {
      // Optionally set a fallback or log
      setAgentFilters([]);
      console.warn("No agents found in extracted alerts.");
      return;
    }

    const filters = uniqueAgents.map(agent => ({
      text: agent,
      value: agent,
    }));

    setAgentFilters(filters);
  };


  const getGroupFilters = (extractedAlerts: ExtractedAlert[]) => {
    const uniqueGroups = [...new Set(extractedAlerts.map(alert => alert.group_name))];
    const filters = uniqueGroups.map(group => ({
      text: group,
      value: group,
    }));
    setGroupFilters(filters);
  };
  
  // Create attack filters from extracted alerts
  const getAttackFilters = (extractedAlerts: ExtractedAlert[]) => {
    const attackerTactics = extractedAlerts.flatMap(alert => {
      const tactic = alert.attacker_tactic ?? "Unknown";
      return Array.isArray(tactic) ? tactic : [tactic]; // Ensure always an array
    });
    
    const uniqueAtt = [...new Set(attackerTactics)]; // Remove duplicates
    const filters = uniqueAtt.map(attackerTactic => ({
      text: attackerTactic,
      value: attackerTactic,
    }));
    
    setAttackFilters(filters);
  };
  
  // Count alerts by technique for pie chart
  const countAlertsByTechniqueAsPieData = (extractedAlerts: ExtractedAlert[]) => {
    const tacticCounts: Record<string, number> = {};
    
    extractedAlerts.forEach(alert => {
      if (!alert.attacker_tactic) return; // Skip if null or undefined
      
      const tactics = Array.isArray(alert.attacker_tactic)
        ? alert.attacker_tactic
        : [alert.attacker_tactic]; // Ensure it's an array
      
      tactics.forEach(tactic => {
        if (tactic && tactic !== "N/A") { // Ignore "N/A"
          tacticCounts[tactic] = (tacticCounts[tactic] || 0) + 1;
        }
      });
    });
    
    // Convert the object to an array of PieData
    const alertsByTech = Object.entries(tacticCounts).map(([type, value]) => ({ type, value }));
    setAlertsByTechnique(alertsByTech);
  };
  
  // Count alerts above severity 12
  const countAlertsAboveSeverity12 = (alerts: ExtractedAlert[]): number => {
    let count = 0;
    alerts.forEach((alert) => {
      if (alert.severity > 12) {
        count++;
      }
    });
    return count;
  };
  
  // Function to get top 5 agents with the most alerts
  function getTopAgents(alerts: ExtractedAlert[]): string[] {
    const agentCounts: Record<string, number> = {};
  
    // Count the alerts for each agent
    alerts.forEach(alert => {
      if (alert.agent) {
        agentCounts[alert.agent] = (agentCounts[alert.agent] || 0) + 1;
      }
    });
  
    // Sort the agents by alert count in descending order
    const topAgents = Object.entries(agentCounts)
      .sort((a, b) => b[1] - a[1]) // Sort by count, highest first
      .slice(0, 5) // Get the top 5
      .map(entry => entry[0]); // Extract the agent names
    
    return topAgents;
  }

  function getPast24HourIntervals(): string[] {
    const intervals: string[] = [];
    const now = new Date();
    
    // Start from the current time and go back in 30-minute steps
    for (let i = 0; i < 48; i++) {
      const date = new Date(now.getTime() - i * 30 * 60 * 1000);
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = date.getMinutes() < 30 ? "00" : "30"

      const dt = date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });
      
      let interval = `${hours}:${minutes}`
      if (interval === "00:00") {
        interval = `00:00 \n(${dt})`;
      }
      
      intervals.unshift(interval); // Add in ascending order
     
    }
  
    return intervals;
  }
  
  
 // Group alerts by interval for top agents
function groupAlertsByIntervalForTopAgents(alerts: ExtractedAlert[]) {
  // Get the top 5 agents
  const topAgents = getTopAgents(alerts);

  const past24HIntervals = getPast24HourIntervals()

  // Get the current date and time
  const now = new Date();

  // Calculate the time 24 hours ago
  const twentyFourHoursAgo = new Date(now);
  twentyFourHoursAgo.setHours(now.getHours() - 24, now.getMinutes(), now.getSeconds(), now.getMilliseconds());

  console.log("UNFILTERED ALERTS: ", alerts)
  // Filter the alerts to include only those from the top 5 agents
  const filteredAlerts = alerts
    .filter(alert => topAgents.includes(alert.agent)) // Filter for top agents
    .filter(alert => {
      // console.log("ALERT: ",alert)
      if (!alert.timestamp) return false; // Skip alerts without a timestamp
      const alertDate = new Date(alert.timestamp);
      // console.log("ALERT: ",alert)
      return alertDate >= twentyFourHoursAgo && alertDate <= now; // Filter alerts within the past 24 hours
  });



  // Now group these filtered alerts by interval
  const groupedAlertsObj: Record<string, { date: string; interval: string; count: number; agent: string }> = {};

  filteredAlerts.forEach(alert => {
    if (!alert.timestamp) return;

    const dateObj = new Date(alert.timestamp);
    const date = dateObj.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });

    const hours = dateObj.getHours();
    const minutes = dateObj.getMinutes();
    const roundedMinutes = minutes < 30 ? "00" : "30";
    const interval = `${String(hours).padStart(2, '0')}:${roundedMinutes}`;

    const key = `${date}-${interval}-${alert.agent}`;

    if (!groupedAlertsObj[key]) {
      groupedAlertsObj[key] = { date, interval, count: 0, agent: alert.agent };
    }
    groupedAlertsObj[key].count++;
  });

  setBarDatasetLabels(topAgents)
  setBarLabelsData(past24HIntervals)
  setGroupedAlerts(Object.values(groupedAlertsObj));
}

  // Fetch alerts from API
  const getAlerts = async (query: OpenSearchQuery) => {
    try {
      const fetchedAlerts = await fetchAlerts(query);
      setExtractedAlerts(processData(fetchedAlerts))
    } catch (error) {
      console.error("Error fetching alerts:", error);
    }
  };

  const getBarAlerts = async () => {
    try {
      const query = {"query" : {
        "org_id" : org_id ?? "",
        "gte": "now-24h",
        "lte": "now"
        }
      };

      const fetchedAlerts = await fetchAlerts(query);
      // Process the fetched alerts
      const extrAlerts = processData(fetchedAlerts)
      setBarExtractedAlerts(extrAlerts)
    } catch (error) {
      console.error("Error fetching alerts:", error);
    }
  };

  const [selectedRange, setSelectedRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  
    const handleRangeChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
      setSelectedRange(dates);
    };
  
    useEffect(() => {
      let query: OpenSearchQuery
      if (selectedRange && selectedRange[0] && selectedRange[1]) {
        query = {
          "query" : {
            "org_id" : org_id ?? "",
            "gte": selectedRange[0].toISOString(),
            "lte": selectedRange[1].toISOString()
          }
        }
    
      } else {
        query = {"query" : {
          "org_id" : org_id ?? "",
          "gte": "now-24h",
          "lte": "now"
          }
        }
      }
      getAlerts(query)
    }, [selectedRange]);
  
  // Fetch Bar alerts on component mount only
  useEffect(() => {
    getBarAlerts();
  }, []); // Empty dependency array means this runs once on mount

  useEffect(() => {
    if (barExtractedAlerts.length > 0) {
    groupAlertsByIntervalForTopAgents(barExtractedAlerts);
    }
  }, [barExtractedAlerts]); // Empty dependency array means this runs once on mount
  

  // Process alerts when they change
  useEffect(() => {
    if (extractedAlerts.length > 0) {
      getAgentFilters(extractedAlerts);
      getAttackFilters(extractedAlerts);
      getGroupFilters(extractedAlerts);
      countAlertsByTechniqueAsPieData(extractedAlerts);
    }
  }, [extractedAlerts]); // Only run when extractedAlerts changes
  
  const alertsCount = extractedAlerts.length;
  const alertsAboveSeverity12 = countAlertsAboveSeverity12(extractedAlerts);
  
  const columns: TableColumnsType<ExtractedAlert> = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
    },
    {
      title: 'Agent',
      dataIndex: 'agent',
      key: 'agent',
      filters: agentFilters,
      filterMode: 'tree',
      filterSearch: true,
      onFilter: (value, record) => record.agent.startsWith(value as string),
      width: '30%',
    },
    {
      title: 'Attacker Tactic',
      dataIndex: 'attacker_tactic',
      key: 'attacker_tactic',
      filters: attackFilters,
      filterMode: 'tree',
      filterSearch: true,
      onFilter: (value, record) => {
        const tactic = Array.isArray(record.attacker_tactic)
          ? record.attacker_tactic.join(", ") // Convert array to a string
          : record.attacker_tactic ?? ""; // Ensure it's a string
      
        return tactic.startsWith(value as string);
      },
      width: '30%',
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      key: 'severity',
      defaultSortOrder: 'descend',
      sorter: (a, b) => a.severity - b.severity,
    },
    {
      title: 'Group',
      dataIndex: 'group_name',
      key: 'group_name',
      filters: groupFilters,
      filterMode: 'tree',
      filterSearch: true,
      onFilter: (value, record) => record.group_name.startsWith(value as string),
    },
  ];
 
  
  const flexListContent: ChartData[] = [];
  flexListContent.push(alertsCount && alertsCount !== 0 ? { text:"Total Alerts: ", value: String(alertsCount)} : {text: "", value: ""});
  flexListContent.push(alertsCount && alertsCount !== 0 ? { text:"Total Alerts With Severity >= 12: ", value: String(alertsAboveSeverity12)} : {text: "", value: ""});

  function getIntervalsFromLatestMidnight(): number {
    // Get the current date and time
    const now = new Date();
  
    // Get the latest midnight (00:00) of the current day
    const latestMidnight = new Date(now);
    latestMidnight.setHours(0, 0, 0, 0);
  
    // Calculate the difference between the latest midnight and 24 hours ago
    const twentyFourHoursAgo = new Date(now);
    twentyFourHoursAgo.setHours(now.getHours() - 24, now.getMinutes(), now.getSeconds(), now.getMilliseconds());
  
    // Calculate the difference in milliseconds
    const differenceInMs = latestMidnight.getTime() - twentyFourHoursAgo.getTime();
  
    // Convert milliseconds to 30-minute intervals (30 minutes = 1800000 milliseconds)
    const intervals = differenceInMs / 1800000;
  
    // Return the number of 30-minute intervals
    return intervals;
  }
  
  const x = getIntervalsFromLatestMidnight();

  const options = createChartOptions("Top 5 Agents (# of alerts in past 24h)", 'rgb(0, 0, 0)',x-1,x-1);




  const defaultRange: [Dayjs, Dayjs] = [
    dayjs().subtract(24, "hour"), // 24 hours ago
    dayjs(), // Now
  ];
  
  

  return (
    <>
  <HeaderBar />
  <SideBar location={location} items={sideBarItems}>
    <button onClick={() => setLocation("/orgs")}>Back</button>
    <div className="card">
      <FlexList listItems={flexListContent} />
    </div>
    <div className="card">
      <div className="charts-container">
        <div className="stacked-bar-container">
          <StackedBarChart
            options={options}
            barLabels={barLabels}
            barDatasetLabels={barDatasetLabels}
            groupedAlerts={groupedAlerts}
          />
        </div>
        <div className="pie-chart-container">
          <DemoPie data={alertsByTechnique} title={"Attacker Tactics:"} />
        </div>
      </div>

      <div className="alerts-container">
        <p className="alerts-title">Organization Alerts:</p>
        <RangePicker showTime format="YYYY-MM-DD HH:mm:ss" onChange={handleRangeChange} defaultValue={defaultRange} />
      </div>
      <LogTable dataSource={extractedAlerts} columns={columns} />
    </div>
  </SideBar>
</>

  );
};
 
export default LogPage;