import { Modal, Button } from 'antd';
import React from 'react';

interface ConfirmationModalProps {
  visible: boolean;
  title: string;
  content: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  danger?: boolean;
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  visible,
  title,
  content,
  confirmText = 'OK',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
  loading = false,
  danger = false,
}) => {
  return (
    <Modal
      title={title}
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          {cancelText}
        </Button>,
        <Button
          key="confirm"
          type="primary"
          danger={danger}
          loading={loading}
          onClick={onConfirm}
        >
          {confirmText}
        </Button>,
      ]}
      destroyOnClose
    >
      <p>{content}</p>
    </Modal>
  );
};

export default ConfirmationModal;