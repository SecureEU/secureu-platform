import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, message, Button } from 'antd';
import { CREATE_USER_URI } from '@/utils/uris';
import { fetchWithAuth } from '@/utils/requests';
import { OrganisationJSON } from '@/utils/types';
import { UserJSON } from '@/utils/types';
import { nameValidator, emailValidator } from '@/utils/validators';

interface CreateUserFormProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (user: UserJSON) => void;
  initialValues?: Partial<UserJSON>;
  organisations: OrganisationJSON[];
}

const { Option } = Select;

const CreateUserForm: React.FC<CreateUserFormProps> = ({
  visible,
  onClose,
  organisations,
  onSubmit
}) => {
  const [form] = Form.useForm();
  const role = Form.useWatch('role', form);
  const selectedOrgId = Form.useWatch('org_id', form);

  const [userInfoModalVisible, setUserInfoModalVisible] = useState(false);
  const [createdUser, setCreatedUser] = useState<UserJSON | null>(null);

  useEffect(() => {
    if (organisations.length === 1) {
      form.setFieldsValue({ org_id: organisations[0].id });
    }
  }, [organisations, form]);

  const handleCreateUser = async (values: UserJSON) => {
    try {
      const response = await fetchWithAuth(CREATE_USER_URI, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values),
      });

      if (!response.ok) throw new Error('Failed to create user');

      const newUser: UserJSON = await response.json();

      form.resetFields();
      onClose();

      setCreatedUser(newUser);
      setUserInfoModalVisible(true);

      onSubmit(newUser);
      
    } catch (err) {
      message.error('Failed to create user: ' + err);
    }
  };

  return (
    <>
      <Modal
        title="Create User"
        open={visible}
        onCancel={() => {
          form.resetFields();
          onClose();
        }}
        onOk={() => form.submit()}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateUser}
        >
          <Form.Item
            label="First Name"
            name="first_name"
            rules={[{ required: true, message: 'First name is required' },{validator: nameValidator},]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="Last Name"
            name="last_name"
            rules={[{ required: true, message: 'Last name is required' },{validator: nameValidator},]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="Email"
            name="email"
            rules={[{ required: true, message: 'Email is required', type: 'email' }, { validator: emailValidator },]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="Role"
            name="role"
            rules={[{ required: true, message: 'Role is required' }]}
          >
            <Select placeholder="Select a role">
              <Option value="employee">Employee (Belongs to a single group)</Option>
              <Option value="manager">Manager (Manages a single organisation & its groups)</Option>
              <Option value="admin">Admin (Manages all organisations & groups in account)</Option>
            </Select>
          </Form.Item>

          {(role === 'manager' || role === 'employee') && (
            <Form.Item
              label="Organisation"
              name="org_id"
              rules={[{ required: true, message: 'Organisation is required' }]}
            >
              <Select placeholder="Select an organisation">
                {organisations.map((org) => (
                  <Option key={org.id} value={org.id}>
                    {org.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          )}

          {role === 'employee' && (
            <Form.Item
              label="Group"
              name="group_id"
              rules={[{ required: true, message: 'Group is required' }]}
            >
              <Select placeholder="Select a group" disabled={!selectedOrgId}>
                {organisations
                  .find((org) => org.id === selectedOrgId)
                  ?.groups.map((group) => (
                    <Option key={group.id} value={group.id}>
                      {group.name}
                    </Option>
                  ))}
              </Select>
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="User Created Successfully"
        open={userInfoModalVisible}
        onOk={() => setUserInfoModalVisible(false)}
        onCancel={() => setUserInfoModalVisible(false)}
        width={500}
        destroyOnClose
      >
        {createdUser && (
          <div>
            <p><strong>Name:</strong> {createdUser.first_name} {createdUser.last_name}</p>
            <p><strong>Email:</strong> {createdUser.email}</p>
            <p><strong>Role:</strong> {createdUser.role}</p>
            {createdUser.org_id && <p><strong>Org ID:</strong> {createdUser.org_id}</p>}
            {createdUser.group_id && <p><strong>Group ID:</strong> {createdUser.group_id}</p>}
            <p>
              <strong>Password:</strong>{' '}
              <code>{createdUser.password}</code>{' '}
              <Button
                size="small"
                onClick={() => {
                  navigator.clipboard.writeText(createdUser.password || '');
                  message.success('Password copied to clipboard');
                }}
              >
                Copy
              </Button>
            </p>
            <p style={{ marginTop: 12, fontStyle: 'italic', color: 'red' }}>
              ⚠️ Please copy and save this password. It will not be shown again.
            </p>
          </div>
        )}
      </Modal>
    </>
  );
};

export default CreateUserForm;
