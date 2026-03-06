import { Form, Input, Button, Modal } from 'antd';
import React from 'react';

interface CreateOrganisationModalProps {
  isModalVisible: boolean;
  setIsModalVisible: (visible: boolean) => void;
  handleCreateOrg: (values: any) => void;
}

const CreateOrganisationModal: React.FC<CreateOrganisationModalProps> = ({
  isModalVisible,
  setIsModalVisible,
  handleCreateOrg,
}) => {
  const [form] = Form.useForm();

  // Generates a 4-character acronym from the name
  const generateCodeFromName = (name: string) => {
    const acronym = name
      .split(' ')
      .filter(Boolean)
      .map(word => word[0].toUpperCase())
      .join('')
      .slice(0, 4);
    return acronym;
  };

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const name = e.target.value;
    form.setFieldsValue({ code: generateCodeFromName(name) });
  };

  const handleSubmit = (values: any) => {
    handleCreateOrg(values);
    form.resetFields(); // Clear form on submit
    setIsModalVisible(false); // Close modal
  };

  const handleCancel = () => {
    setIsModalVisible(false);
    form.resetFields(); // Clear form on cancel
  };

  return (
    <Modal
      title="Create New Organisation"
      open={isModalVisible}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} onFinish={handleSubmit} layout="vertical">
        <Form.Item
          label="Organisation Name"
          name="name"
          rules={[
            { required: true, message: 'Please enter the organisation name!' },
            { max: 255, message: 'Name cannot exceed 255 characters' },
          ]}
        >
          <Input onChange={handleNameChange} />
        </Form.Item>

        <Form.Item
          label="Organisation Code"
          name="code"
          rules={[
            { required: true, message: 'Please enter the organisation code!' },
            { max: 4, message: 'Code must be max 4 characters.' },
          ]}
        >
          <Input />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            Create Organisation
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateOrganisationModal;
