import HeaderBar from "../components/headerBar";
import SideBar from "../components/sideBar";
import "../styles/common.css";
import { AgentGroupView, MenuItem } from "../utils/types";
import { useState, useEffect } from "react";
import LogTable from "../components/logTable";
import { VIEW_AGENTS_URI } from "../utils/uris";
import { useLocation } from "wouter";
import {
  fetchWithAuth,
  activateAgent,
  deactivateAgent,
} from "@/utils/requests";
import { Button, message } from "antd";
import ConfirmationModal from "../components/forms/confirmationModal";

const AgentsPage = ({ sideBarItems }: { sideBarItems: MenuItem[] }) => {
  const [agents, setAgents] = useState<AgentGroupView[]>([]);
  const [location] = useLocation();
  const [loadingAgents, setLoadingAgents] = useState<Set<string>>(new Set());
  const [activateModalVisible, setActivateModalVisible] = useState(false);
  const [deactivateModalVisible, setDeactivateModalVisible] = useState(false);
  const [selectedAgentId, setSelectedAgentId] = useState<string>("");

  // Handle agent activation
  const handleActivateAgent = (agentId: string) => {
    setSelectedAgentId(agentId);
    setActivateModalVisible(true);
  };

  const confirmActivateAgent = async () => {
    setLoadingAgents((prev) => new Set(prev).add(selectedAgentId));
    try {
      await activateAgent(selectedAgentId);
      message.success("Agent activated successfully");
      // Refresh the agents list
      fetchAgentsData();
    } catch (error: any) {
      message.error(error.message || "Failed to activate agent");
    } finally {
      setLoadingAgents((prev) => {
        const newSet = new Set(prev);
        newSet.delete(selectedAgentId);
        return newSet;
      });
      setActivateModalVisible(false);
      setSelectedAgentId("");
    }
  };

  // Handle agent deactivation
  const handleDeactivateAgent = (agentId: string) => {
    setSelectedAgentId(agentId);
    setDeactivateModalVisible(true);
  };

  const confirmDeactivateAgent = async () => {
    setLoadingAgents((prev) => new Set(prev).add(selectedAgentId));
    try {
      await deactivateAgent(selectedAgentId);
      message.success("Agent deactivated successfully");
      // Refresh the agents list
      fetchAgentsData();
    } catch (error: any) {
      message.error(error.message || "Failed to deactivate agent");
    } finally {
      setLoadingAgents((prev) => {
        const newSet = new Set(prev);
        newSet.delete(selectedAgentId);
        return newSet;
      });
      setDeactivateModalVisible(false);
      setSelectedAgentId("");
    }
  };

  const handleCancelActivate = () => {
    setActivateModalVisible(false);
    setSelectedAgentId("");
  };

  const handleCancelDeactivate = () => {
    setDeactivateModalVisible(false);
    setSelectedAgentId("");
  };

  // Fetch agents data
  const fetchAgentsData = async () => {
    try {
      const response = await fetchWithAuth(VIEW_AGENTS_URI, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error("Failed to fetch agents");
      }

      const data = await response.json();

      // Copy the 'id' field to the 'key' field for each agent
      const updatedAgents = data?.map((agent: AgentGroupView) => ({
        ...agent,
        key: agent.id, // Set the 'key' field to be the same as 'id'
      }));
      setAgents(updatedAgents);
    } catch (error) {
      console.error("Error fetching agents:", error);
      message.error("Failed to fetch agents");
    }
  };

  // Columns for the LogTable component
  const columns = [
    {
      title: "ID",
      dataIndex: "id",
      key: "id",
    },
    {
      title: "Created At",
      dataIndex: "created_at",
      key: "created_at",
    },
    {
      title: "Agent Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Operating System",
      dataIndex: "os",
      key: "os",
    },
    {
      title: "Active",
      dataIndex: "active",
      key: "active",
      render: (active: boolean) => (active ? "Yes" : "No"),
    },
    {
      title: "Organisation",
      dataIndex: "org_name",
      key: "org_name",
    },

    {
      title: "Group",
      dataIndex: "group_name",
      key: "group_name",
    },
    {
      title: "Actions",
      key: "actions",
      render: (_: any, record: AgentGroupView) => {
        const isLoading = loadingAgents.has(record.id);
        return (
          <div style={{ display: "flex", gap: "8px" }}>
            {record.active ? (
              <Button
                danger
                size="small"
                loading={isLoading}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  handleDeactivateAgent(record.id);
                }}
              >
                Deactivate
              </Button>
            ) : (
              <Button
                type="primary"
                size="small"
                loading={isLoading}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  handleActivateAgent(record.id);
                }}
              >
                Activate
              </Button>
            )}
          </div>
        );
      },
    },
  ];

  // Fetch the agents from the API
  useEffect(() => {
    fetchAgentsData();
  }, []);

  return (
    <>
      <HeaderBar />
      <SideBar location={location} items={sideBarItems}>
        <div>
          <h2>Agents</h2>
          <LogTable dataSource={agents} columns={columns} />
        </div>
      </SideBar>

      {/* Confirmation Modals */}
      <ConfirmationModal
        visible={activateModalVisible}
        title="Activate Agent"
        content="Are you sure you want to activate this agent?"
        confirmText="Activate"
        onConfirm={confirmActivateAgent}
        onCancel={handleCancelActivate}
        loading={loadingAgents.has(selectedAgentId)}
      />

      <ConfirmationModal
        visible={deactivateModalVisible}
        title="Deactivate Agent"
        content="Are you sure you want to deactivate this agent? The agent will shut down permanently."
        confirmText="Deactivate"
        onConfirm={confirmDeactivateAgent}
        onCancel={handleCancelDeactivate}
        loading={loadingAgents.has(selectedAgentId)}
        danger={true}
      />
    </>
  );
};

export default AgentsPage;
