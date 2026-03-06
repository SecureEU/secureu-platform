import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Button, message, Space } from 'antd';
import { UPDATE_USER_URI } from '@/utils/uris';
import { fetchWithAuth } from '@/utils/requests';
import { OrganisationJSON, UserJSON } from '@/utils/types';
import { emailValidator, passwordValidator, nameValidator } from '@/utils/validators';

const { Option } = Select;

interface EditUserFormProps {
  visible: boolean;
  onClose: () => void;
  user: Partial<UserJSON>;
  organisations: OrganisationJSON[];
  onSubmit: (updatedUser: UserJSON) => void;
}

const EditUserForm: React.FC<EditUserFormProps> = ({
  visible,
  onClose,
  user,
  organisations,
  onSubmit,
}) => {
  const [form] = Form.useForm();
  const [editingPassword, setEditingPassword] = useState(false);
  const role = Form.useWatch('role', form);
  const selectedOrgId = Form.useWatch('org_id', form);

  // Populate or reset the form whenever the modal opens/closes
  useEffect(() => {
    if (visible) {
      form.setFieldsValue({
        first_name: user.first_name,
        last_name:  user.last_name,
        email:      user.email,
        role:       user.role,
        org_id:     user.org_id,
        group_id:   user.group_id,
        password:   '',
      });
      setEditingPassword(false);
    } else {
      form.resetFields();
    }
  }, [visible, user, form]);

  const handleUpdateUser = async (values: UserJSON) => {
    try {
      const response = await fetchWithAuth(`${UPDATE_USER_URI}/${user.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values),
      });
      if (!response.ok) throw new Error('Failed to update user');
      const updatedUser: UserJSON = await response.json();
      onSubmit(updatedUser);
      message.success('User updated successfully');
      onClose();
    } catch (err: any) {
      message.error('Failed to update user: ' + err.message);
    }
  };

  return (
    <Modal
      title="Edit User"
      visible={visible}
      onCancel={onClose}
      onOk={() => form.submit()}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleUpdateUser}
      >
        <Form.Item
          label="First Name"
          name="first_name"
          rules={[
            { required: true, message: 'First name is required' },
            { validator: nameValidator },
          ]}
        >
          <Input />
        </Form.Item>

        <Form.Item
          label="Last Name"
          name="last_name"
          rules={[
            { required: true, message: 'Last name is required' },
            { validator: nameValidator },
          ]}
        >
          <Input />
        </Form.Item>

        <Form.Item
          label="Email"
          name="email"
          rules={[
            { required: true, message: 'Email is required', type: 'email' },
            { validator: emailValidator },
          ]}
        >
          <Input disabled />
        </Form.Item>

        <Form.Item label="Password" style={{ marginBottom: 0 }}>
          <Space align="baseline">
            <Form.Item
              name="password"
              noStyle
              rules={
                editingPassword
                  ? [{ validator: passwordValidator }]
                  : []
              }
            >
              <Input.Password
                placeholder="Enter new password"
                disabled={!editingPassword}
              />
            </Form.Item>
            <Button type="link" onClick={() => setEditingPassword((v) => !v)}>
              {editingPassword ? 'Cancel' : 'Edit Password'}
            </Button>
          </Space>
        </Form.Item>

        <Form.Item
          label="Role"
          name="role"
          rules={[{ required: true, message: 'Role is required' }]}
        >
          <Select placeholder="Select a role">
            <Option value="employee">Employee</Option>
            <Option value="manager">Manager</Option>
          </Select>
        </Form.Item>

        {(role === 'manager' || role === 'employee') && (
          <Form.Item
            label="Organisation"
            name="org_id"
            rules={[{ required: true, message: 'Organisation is required' }]}
          >
            <Select placeholder="Select an organisation">
              {organisations.map((o) => (
                <Option key={o.id} value={o.id}>{o.name}</Option>
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
            <Select
              placeholder="Select a group"
              disabled={!selectedOrgId}
            >
              {organisations
                .find((o) => o.id === selectedOrgId)
                ?.groups.map((g) => (
                  <Option key={g.id} value={g.id}>{g.name}</Option>
                ))}
            </Select>
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default EditUserForm;
