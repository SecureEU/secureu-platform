import  { useEffect, useState } from 'react';
import { Tabs, Row, Col, Button, Table, Form, message, Spin } from 'antd';
import HeaderBar from '../components/headerBar';
import SideBar from "../components/sideBar";
import { MenuItem } from '../utils/types';
import {Link, useLocation} from 'wouter'
import { CREATE_ORG_URI, CREATE_GROUP_URI } from '../utils/uris';
import DeployAgentForm from '../components/forms/deployAgentForm';
import CreateOrganisationModal from '../components/forms/createOrganisationForm';
import CreateGroupModal from '../components/forms/createGroupForm';
import { fetchOrganisations, fetchWithAuth } from '../utils/requests';
import { OrganisationJSON, GroupJSON, GroupDataType, Panes } from '../utils/types';



const OrgsPage = ({ sideBarItems }: { sideBarItems: MenuItem[] }) => {
  const [organisations, setOrganisations] = useState<OrganisationJSON[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
  const [isGroupModalVisible, setIsGroupModalVisible] = useState<boolean>(false);
  const [form] = Form.useForm(); // Form instance for the modal form
  const [groupForm] = Form.useForm(); // Form instance for the group modal form
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [activeTabKey, setActiveTabKey] = useState<string | undefined>(undefined); // State for active tab
  const [items, setItems] = useState<Panes[]>([]);
  const [location, ] = useLocation();
  const [formVisible, setFormVisible] = useState(false);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);


  useEffect(() => {
    const loadOrgs = async () => {
      try {
        const data = await fetchOrganisations(); // or pass user ID dynamically
        setOrganisations(data);
      } catch (err) {
        setError('Failed to load organisations: ' + err);
      } finally {
        setLoading(false);
      }
    };

    loadOrgs();
  }, []);


  useEffect(() => {

     // Set the first organization as the active tab (or handle it based on your use case)     
      const newItems: Panes[] = organisations.map((org) => ({
      label: org.name,
      children: (
        <div>
          <Row justify="space-between" align="middle" style={{ marginBottom: '20px' }}>
            <Col>
              <h2>Org Name: {org.name}</h2>
            </Col>
            <Col>
            <Link href={`/agents`}>
              <Button className="bg-white text-black px-4 py-2 rounded  hover:bg-blue-500 hover:text-white transition duration-300 button-space">
              View agents
              </Button>
            </Link>
            <Link href={`/alerts/${org.id}`}>
              <Button className="bg-white text-black px-4 py-2 rounded  hover:bg-blue-500 hover:text-white transition duration-300 button-space">
              View alerts
              </Button>
            </Link>
            <Button
              type="primary"
              shape="circle"
              icon={"+"} // Ensure you import PlusOutlined from @ant-design/icons
              size="large"
              onClick={() => {
                setSelectedOrgId(org.id);
                setIsGroupModalVisible(true);
              }}
            />
            </Col>
          </Row>
    
          <Row gutter={16}>
            <Col span={24}>
              <Table
                dataSource={org.groups}
                columns={columns}
                rowKey="id"
                pagination={false}
              />
            </Col>
          </Row>
        </div>
      ),
      key: org.id.toString(),
      }));
      // set new items
      setItems(newItems)
      if (organisations.length >0) {
        if ((activeTabKey === "") || (activeTabKey === undefined))  {
          setActiveTabKey(organisations[0].id.toString());
          setSelectedOrgId(organisations[0].id)
        } 
      }
  }, [organisations]); // This runs whenever `organisations` changes

  useEffect(() => {

  }, [items]); // This runs whenever `items` changes
  
  // Handle Create Organisation form submission
  const handleCreateOrg = async (values: { name: string; code: string }) => {
    try {
      // setLoading(true)
      const response = await fetchWithAuth(CREATE_ORG_URI, {
        method: 'POST',
        body: JSON.stringify({ name: values.name, code: values.code }),
      });
      

      if (!response.ok) {
        throw new Error('Failed to create organisation');
      }

      const newOrg: OrganisationJSON = await response.json();
      // Update the organisations state with the new organisation
      const newOrgs = [...organisations, newOrg]
    
      setOrganisations(newOrgs);

      // Close the modal and reset form
      setIsModalVisible(false);
      form.resetFields();

      // Show success message
      message.success('Organisation created successfully');
      
     
      // Set the newly created organisation as the active tab
      setActiveTabKey(newOrg?.id?.toString());
      setSelectedOrgId(newOrg?.id)
      // setLoading(false)

    } catch (err) {
      message.error('Failed to create organisation: ' + err);
    }
  };

  // Handle Create Group form submission
  const handleCreateGroup = async (values: { name: string }) => {
    if (!selectedOrgId) return;

    try {
      const response = await fetchWithAuth(CREATE_GROUP_URI, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          org_id: selectedOrgId,  // Append org_id to the request
          name: values.name,
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to create group');
      }

      const newGroup: GroupJSON = await response.json();

      const updatedOrgs = organisations.map((org) => {
        if (org.id === newGroup.org_id) {
          return { ...org, groups: [...org.groups, newGroup] }; // Add new group to the org
        }
        return org;
      });

      // Update the organisations state with the new group
      setOrganisations(updatedOrgs);

      // Close the group modal and reset form
      setIsGroupModalVisible(false);
      groupForm.resetFields();

      // Show success message
      message.success('Group created successfully');

    } catch (err) {
      message.error('Failed to create group: ' + err);
    }
  };

  // Columns for Group Table
  const columns = [
    {
      title: 'Group ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'Group Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Created At',
      dataIndex: 'created_at',
      key: 'created_at',
    },
    {
      title: 'Actions',
      key: 'action',
      render: (_: any, record: GroupDataType) => (
        <Button
          type="link"
          onClick={() => {
            setSelectedGroupId(record.id);
            setFormVisible(true);
          }}
        >
          Deploy Agent
        </Button>
      ),
    },
  ];

  const handleTabChange = (key: string) => {
    setActiveTabKey(key);

    const orgId = parseInt(key, 10);
    if (!isNaN(orgId)) {
      setSelectedOrgId(orgId);
    } else {
      setSelectedOrgId(null); // fallback if key isn't a valid number
    }
  };



  return (
    <>
      <HeaderBar />
      <SideBar  location={location} items={sideBarItems}>
          <Row justify="space-between" align="middle" style={{ marginBottom: '20px' }}>
          <Col>
            <h1>Manage Orgs</h1>
          </Col>

          <Col>
            <Button
              type="primary"
              shape="circle"
              icon="+"
              size="large"
              onClick={() => setIsModalVisible(true)}
            />
          </Col>
        </Row>

        {/* Check for loading state */}
        {loading ? (
          <Spin size="large" />
        ) : error ? (
          <div>{error}</div>
        ) : (
          <Tabs 
          hideAdd
          activeKey={activeTabKey} 
          onChange={handleTabChange}
          type='editable-card'
          items={items}
          // closable={true}
          />

        )}

        {/* Modal for creating a new organisation */}
        <CreateOrganisationModal
        isModalVisible={isModalVisible}
        setIsModalVisible={setIsModalVisible}
        handleCreateOrg={handleCreateOrg}
      />

        {/* Modal for creating a new group */}
        <CreateGroupModal
        visible={isGroupModalVisible}
        onClose={() => setIsGroupModalVisible(false)}
        onCreate={handleCreateGroup}
      />

        {selectedGroupId !== null && (
          <DeployAgentForm
            visible={formVisible}
            groupId={selectedGroupId}
            orgId={selectedOrgId}
            onClose={() => {
              setFormVisible(false);
              setSelectedGroupId(null);
            }}
          />
        )}
      </SideBar>
    </>
  );
};

export default OrgsPage;
