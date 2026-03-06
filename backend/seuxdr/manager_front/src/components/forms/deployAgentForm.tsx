// components/DeployAgentForm.tsx
import React, { useState } from 'react';
import { Modal, Form, Select, Button, Spin, message } from 'antd';
import { GENERATE_AGENT_URI, DOWNLOAD_AGENT_URI } from '../../utils/uris';
import { fetchWithAuth } from '@/utils/requests';

const { Option } = Select;

interface DeployAgentPayload {
  org_id: number | null;   // Organization ID (e.g., UUID or unique string)
  group_id: number | null; // Group ID
  os: string;       // Operating System (e.g., 'linux', 'windows', 'darwin')
  arch: string;     // Architecture (e.g., 'amd64', 'arm64')
  distro: string;
}


interface DeployAgentFormProps {
  visible: boolean;
  onClose: () => void;
  groupId: number | null;
  orgId: number | null;
}

const DeployAgentForm: React.FC<DeployAgentFormProps> = ({ visible, onClose, groupId, orgId }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [osType, setOsType] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    if (!groupId) return;

    const { os, arch, distro } = values;
    const payload: DeployAgentPayload = {
      org_id: orgId, // You can make this dynamic later
      group_id: groupId,
      os,
      arch,
      distro
    };

    if (os === 'linux') {
      payload.distro = distro;
    }

    setLoading(true);

    try {
      console.log("PAYLOAD ",payload)
      const createResp = await fetchWithAuth(GENERATE_AGENT_URI, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!createResp.ok) {
        throw new Error('Failed to generate agent.');
      }

        // Fetch and trigger download
    const queryParams = new URLSearchParams({
        os,
        arch,
        group_id: groupId.toString(),
    });
    if (os === 'linux') {
        queryParams.append("distro", distro);
    }
    
    const downloadResp = await fetchWithAuth(`${DOWNLOAD_AGENT_URI}?${queryParams.toString()}`);
    if (!downloadResp.ok) {
        throw new Error('Failed to download agent.');
    }
    
    const blob = await downloadResp.blob();
    let fileName = "agent";
    const contentDisposition = downloadResp.headers.get("Content-Disposition");
    console.log(downloadResp.headers.get("Content-Type"))
    console.log(downloadResp.headers.get("Content-Length"))
    console.log(downloadResp.headers.get("Content-Disposition"))
    
    
    
    
    if (contentDisposition) {
        const match = contentDisposition.match(/filename="?([^"]+)"?/);
        if (match && match[1]) {
        fileName = match[1];
        }
    }
    
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  

      message.success('Agent deployed and downloaded successfully.');
      form.resetFields();
      onClose();
    } catch (err: any) {
      console.error(err);
      message.error(err.message || 'Something went wrong.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={visible}
      title={`Deploy Agent - Group ${groupId}`}
      onCancel={onClose}
      footer={null}
    >
      <Spin spinning={loading}>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item name="os" label="Operating System" rules={[{ required: true }]}>
            <Select onChange={(value) => setOsType(value)}>
              <Option value="linux">Linux</Option>
              <Option value="windows">Windows</Option>
              <Option value="macos">macOS</Option>
            </Select>
          </Form.Item>

          {osType === 'linux' && (
            <Form.Item name="distro" label="Linux Distro" rules={[{ required: true }]}>
              <Select>
                <Option value="deb">Debian (.deb)</Option>
                <Option value="rpm">Red Hat (.rpm)</Option>
              </Select>
            </Form.Item>
          )}

          <Form.Item name="arch" label="Architecture" rules={[{ required: true }]}>
            <Select>
              <Option value="amd64">amd64</Option>
              {osType !== 'windows' && <Option value="arm64">arm64</Option>}
            </Select>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Download
            </Button>
          </Form.Item>
        </Form>
      </Spin>
    </Modal>
  );
};

export default DeployAgentForm;
