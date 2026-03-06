'use client';

import React from 'react';
import { AlertCircle, CheckCircle, AlertTriangle, Info, X } from 'lucide-react';

const variants = {
  success: {
    className: 'alert-success',
    icon: CheckCircle,
  },
  warning: {
    className: 'alert-warning',
    icon: AlertTriangle,
  },
  error: {
    className: 'alert-error',
    icon: AlertCircle,
  },
  info: {
    className: 'alert-info',
    icon: Info,
  },
};

const Alert = ({
  children,
  title,
  variant = 'info',
  className = '',
  dismissible = false,
  onDismiss,
  icon: CustomIcon,
  ...props
}) => {
  const config = variants[variant] || variants.info;
  const Icon = CustomIcon || config.icon;

  return (
    <div className={`alert ${config.className} ${className}`} role="alert" {...props}>
      <Icon className="h-5 w-5 flex-shrink-0" />
      <div className="flex-1">
        {title && <h4 className="font-medium mb-1">{title}</h4>}
        <div className="text-sm">{children}</div>
      </div>
      {dismissible && onDismiss && (
        <button
          onClick={onDismiss}
          className="flex-shrink-0 p-1 rounded hover:bg-black/10 transition-colors"
          aria-label="Dismiss"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
};

export default Alert;
