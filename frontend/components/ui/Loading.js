'use client';

import React from 'react';

const Loading = ({ size = 'md', className = '', text }) => {
  const sizes = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-2',
    lg: 'h-12 w-12 border-3',
    xl: 'h-16 w-16 border-4',
  };

  return (
    <div className={`flex flex-col items-center justify-center ${className}`}>
      <div
        className={`animate-spin rounded-full border-gray-200 border-t-blue-600 ${
          sizes[size] || sizes.md
        }`}
      />
      {text && <p className="mt-3 text-sm text-gray-500">{text}</p>}
    </div>
  );
};

export const LoadingOverlay = ({ text }) => (
  <div className="fixed inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-50">
    <Loading size="lg" text={text} />
  </div>
);

export const LoadingCard = ({ className = '' }) => (
  <div className={`card p-6 ${className}`}>
    <div className="animate-pulse space-y-4">
      <div className="h-4 bg-gray-200 rounded w-1/4" />
      <div className="h-8 bg-gray-200 rounded w-1/2" />
      <div className="space-y-2">
        <div className="h-3 bg-gray-200 rounded" />
        <div className="h-3 bg-gray-200 rounded w-5/6" />
      </div>
    </div>
  </div>
);

export const Skeleton = ({ className = '', variant = 'text' }) => {
  const variants = {
    text: 'h-4 w-full',
    title: 'h-6 w-3/4',
    avatar: 'h-10 w-10 rounded-full',
    thumbnail: 'h-20 w-20 rounded-lg',
    button: 'h-10 w-24 rounded-lg',
    card: 'h-32 w-full rounded-lg',
  };

  return (
    <div className={`skeleton ${variants[variant] || variants.text} ${className}`} />
  );
};

export default Loading;
