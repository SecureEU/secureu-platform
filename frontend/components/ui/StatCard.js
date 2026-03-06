'use client';

import React from 'react';

const StatCard = ({
  title,
  value,
  icon: Icon,
  trend,
  trendValue,
  description,
  color = 'blue',
  className = '',
}) => {
  const colorClasses = {
    blue: 'text-blue-600 bg-blue-100',
    red: 'text-red-600 bg-red-100',
    green: 'text-green-600 bg-green-100',
    yellow: 'text-yellow-600 bg-yellow-100',
    purple: 'text-purple-600 bg-purple-100',
    indigo: 'text-indigo-600 bg-indigo-100',
    gray: 'text-gray-600 bg-gray-100',
    gradient: '',
  };

  const trendColors = {
    up: 'text-green-600',
    down: 'text-red-600',
    neutral: 'text-gray-500',
  };

  return (
    <div className={`stat-card ${className}`}>
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-600">{title}</span>
        {Icon && (
          <div
            className={`p-2 rounded-lg ${
              color === 'gradient'
                ? 'gradient-bg text-white'
                : colorClasses[color] || colorClasses.blue
            }`}
          >
            <Icon className="h-5 w-5" />
          </div>
        )}
      </div>

      <div className="mt-3">
        <div className="text-3xl font-bold text-gray-900">{value}</div>

        {(trend || description) && (
          <div className="mt-2 flex items-center gap-2">
            {trend && trendValue && (
              <span className={`text-sm font-medium ${trendColors[trend] || trendColors.neutral}`}>
                {trend === 'up' && '+'}{trendValue}
              </span>
            )}
            {description && (
              <span className="text-sm text-gray-500">{description}</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
