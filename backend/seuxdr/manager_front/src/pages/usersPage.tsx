import  { useEffect, useState } from 'react';
import HeaderBar from '../components/headerBar';
import '../styles/common.css';
import SideBar from '@/components/sideBar';
import { useLocation } from 'wouter';
import { MenuItem } from '@/utils/types';
import { Table, Button, Input, Space, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnType } from 'antd/es/table';
import type { FilterDropdownProps } from 'antd/es/table/interface';

import { fetchUsers, fetchOrganisations } from '@/utils/requests';
import CreateUserForm from '@/components/forms/createUserForm';
import EditUserForm from '@/components/forms/editUserForm';
import { OrganisationJSON, UserJSON } from '@/utils/types';

interface EnrichedUser extends UserJSON {
  org_name: string;
  group_name: string;
}

const getColumnSearchProps = (
  dataIndex: keyof EnrichedUser
): ColumnType<EnrichedUser> => ({
  filterDropdown: ({
    setSelectedKeys,
    selectedKeys,
    confirm,
    clearFilters,
  }: FilterDropdownProps) => (
    <div style={{ padding: 8 }}>
      <Input
        placeholder={`Search ${String(dataIndex)}`}
        value={selectedKeys[0] as string}
        onChange={e =>
          setSelectedKeys(e.target.value ? [e.target.value] : [])
        }
        onPressEnter={() => confirm()}
        style={{ width: 188, marginBottom: 8, display: 'block' }}
      />
      <Space>
        <Button
          type="primary"
          onClick={() => confirm()}
          icon={<SearchOutlined />}
          size="small"
          style={{ width: 90 }}
        >
          Search
        </Button>
        <Button
          onClick={() => {
            clearFilters?.();
            confirm();
          }}
          size="small"
          style={{ width: 90 }}
        >
          Reset
        </Button>
      </Space>
    </div>
  ),
  filterIcon: (filtered: boolean) => (
    <SearchOutlined style={{ color: filtered ? '#1890ff' : undefined }} />
  ),
  onFilter: (value, record) =>
    record[dataIndex] != null &&
    record[dataIndex]!
      .toString()
      .toLowerCase()
      .includes((value as string).toLowerCase()),
});

export default function UsersPage({ sideBarItems }: { sideBarItems: MenuItem[] }) {
  const [location] = useLocation();
  const [users, setUsers] = useState<UserJSON[]>([]);
  const [organisations, setOrganisations] = useState<OrganisationJSON[]>([]);
  const [enrichedUsers, setEnrichedUsers] = useState<EnrichedUser[]>([]);
  const [formVisible, setFormVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<EnrichedUser | null>(null);

  // load orgs
  useEffect(() => {
    fetchOrganisations()
      .then(setOrganisations)
      .catch(err => message.error('Failed to load organisations: ' + err));
  }, []);

  // load users
  useEffect(() => {
    fetchUsers()
      .then(setUsers)
      .catch(() => message.error('Failed to load users'));
  }, []);

  // enrich
  useEffect(() => {
    if (!organisations.length || !users.length) return;
    const enriched = users.map(u => {
      const org = organisations.find(o => o.id === u.org_id);
      const grp = org?.groups.find(g => g.id === u.group_id);
      return {
        ...u,
        org_name: org?.name ?? '—',
        group_name: grp?.name ?? '—',
      };
    });
    setEnrichedUsers(enriched);
  }, [organisations, users]);

  const handleCreateOrEdit = (newUser: UserJSON) => {
    setUsers(prev => [...prev, newUser]);
    setFormVisible(false);
    setEditingUser(null);
  };

  const getUniqueFilterOptions = (key: keyof EnrichedUser) =>
    Array.from(new Set(enrichedUsers.map(u => u[key])))
      .filter(v => typeof v === 'string')
      .map(v => ({ text: v as string, value: v as string }));

  const columns: ColumnType<EnrichedUser>[] = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Created At', dataIndex: 'created_at', key: 'created_at' },
    {
      title: 'First Name',
      dataIndex: 'first_name',
      key: 'first_name',
      ...getColumnSearchProps('first_name'),
    },
    {
      title: 'Last Name',
      dataIndex: 'last_name',
      key: 'last_name',
      ...getColumnSearchProps('last_name'),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      ...getColumnSearchProps('email'),
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      filters: getUniqueFilterOptions('role'),
      onFilter: (value, record) => record.role === value,
    },
    {
      title: 'Org',
      dataIndex: 'org_name',
      key: 'org_name',
      filters: getUniqueFilterOptions('org_name'),
      onFilter: (value, record) => record.org_name === value,
    },
    {
      title: 'Group',
      dataIndex: 'group_name',
      key: 'group_name',
      filters: getUniqueFilterOptions('group_name'),
      onFilter: (value, record) => record.group_name === value,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: EnrichedUser) => (
        <Button
          type="link"
          onClick={() => {
            setEditingUser(record);
            setFormVisible(true);
          }}
        >
          Edit
        </Button>
      ),
    },
  ];

  return (
    <>
      <HeaderBar />
      <SideBar location={location} items={sideBarItems}>
        <div style={{ padding: '1rem' }}>
          <Button
            type="primary"
            style={{ marginBottom: '1rem' }}
            onClick={() => {
              setEditingUser(null);
              setFormVisible(true);
            }}
          >
            + Add User
          </Button>
          <Table<EnrichedUser>
            dataSource={enrichedUsers}
            columns={columns}
            rowKey="id"
            pagination={false}
          />
        </div>
      </SideBar>

      {editingUser ? (
        <EditUserForm
          visible={formVisible}
          onClose={() => {
            setFormVisible(false);
            setEditingUser(null);
          }}
          user={editingUser}
          organisations={organisations}
          onSubmit={u =>
            setUsers(prev =>
              prev.map(p => (p.id === u.id ? u : p as any))
            )
          }
        />
      ) : (
        <CreateUserForm
          visible={formVisible}
          onClose={() => setFormVisible(false)}
          onSubmit={handleCreateOrEdit}
          organisations={organisations}
        />
      )}
    </>
  );
}
