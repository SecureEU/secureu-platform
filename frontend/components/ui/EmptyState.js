'use client';

import React from 'react';
import { Inbox } from 'lucide-react';
import Button from './Button';

const EmptyState = ({
  icon: Icon = Inbox,
  title = 'No data available',
  description,
  action,
  actionLabel,
  onAction,
  className = '',
}) => {
  return (
    <div className={`empty-state ${className}`}>
      <div className="empty-state-icon">
        <Icon className="w-full h-full" />
      </div>
      <h3 className="empty-state-title">{title}</h3>
      {description && <p className="empty-state-description">{description}</p>}
      {(action || (actionLabel && onAction)) && (
        <div className="mt-6">
          {action || (
            <Button onClick={onAction} variant="primary">
              {actionLabel}
            </Button>
          )}
        </div>
      )}
    </div>
  );
};

export default EmptyState;
