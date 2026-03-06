import React from 'react';
import { Modal, Form, Input, Button } from 'antd';

interface CreateGroupModalProps {
  visible: boolean;
  onClose: () => void;
  onCreate: (values: { name: string }) => void;
}

const CreateGroupModal: React.FC<CreateGroupModalProps> = ({
  visible,
  onClose,
  onCreate,
}) => {
  const [form] = Form.useForm();

  const handleFinish = (values: { name: string }) => {
    onCreate(values);
    form.resetFields();
  };

  return (
    <Modal
      title="Create New Group"
      open={visible}
      onCancel={onClose}
      footer={null}
    >
      <Form form={form} onFinish={handleFinish} layout="vertical">
        <Form.Item
          label="Group Name"
          name="name"
          rules={[{ required: true, message: 'Please enter the group name!' }]}
        >
          <Input />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            Create Group
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateGroupModal;
