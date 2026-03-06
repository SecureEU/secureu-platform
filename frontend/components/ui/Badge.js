'use client';

import React from 'react';

const variants = {
  success: 'badge-success',
  warning: 'badge-warning',
  error: 'badge-error',
  info: 'badge-info',
  neutral: 'badge-neutral',
  // Severity variants
  critical: 'badge-critical',
  high: 'badge-high',
  medium: 'badge-medium',
  low: 'badge-low',
};

const sizes = {
  sm: 'text-xs px-2 py-0.5',
  md: 'text-xs px-2.5 py-1',
  lg: 'text-sm px-3 py-1',
};

const Badge = ({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
  dot = false,
  icon: Icon,
  ...props
}) => {
  const baseClasses = 'badge';
  const variantClasses = variants[variant] || variants.neutral;
  const sizeClasses = sizes[size] || sizes.md;

  return (
    <span
      className={`${baseClasses} ${variantClasses} ${sizeClasses} ${className}`}
      {...props}
    >
      {dot && (
        <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5" />
      )}
      {Icon && <Icon className="h-3 w-3 mr-1" />}
      {children}
    </span>
  );
};

// Convenience components for common use cases
export const SeverityBadge = ({ severity, count }) => {
  const severityMap = {
    critical: { variant: 'critical', label: 'Critical' },
    high: { variant: 'high', label: 'High' },
    medium: { variant: 'medium', label: 'Medium' },
    low: { variant: 'low', label: 'Low' },
    info: { variant: 'info', label: 'Info' },
  };

  const config = severityMap[severity?.toLowerCase()] || severityMap.info;

  return (
    <Badge variant={config.variant}>
      {count !== undefined ? count : config.label}
    </Badge>
  );
};

export const StatusBadge = ({ status }) => {
  const statusMap = {
    running: { variant: 'info', label: 'Running', dot: true },
    completed: { variant: 'success', label: 'Completed' },
    finished: { variant: 'success', label: 'Finished' },
    failed: { variant: 'error', label: 'Failed' },
    pending: { variant: 'warning', label: 'Pending' },
    created: { variant: 'neutral', label: 'Created' },
  };

  const config = statusMap[status?.toLowerCase()] || { variant: 'neutral', label: status };

  return (
    <Badge variant={config.variant} dot={config.dot}>
      {config.label}
    </Badge>
  );
};

export default Badge;
