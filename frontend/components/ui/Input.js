'use client';

import React, { forwardRef } from 'react';

const Input = forwardRef(
  (
    {
      label,
      error,
      helperText,
      className = '',
      inputClassName = '',
      icon: Icon,
      iconPosition = 'left',
      ...props
    },
    ref
  ) => {
    const hasError = Boolean(error);
    const inputClasses = `input ${hasError ? 'input-error' : ''} ${
      Icon ? (iconPosition === 'left' ? 'pl-10' : 'pr-10') : ''
    } ${inputClassName}`;

    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            {label}
          </label>
        )}
        <div className="relative">
          {Icon && iconPosition === 'left' && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Icon className="h-5 w-5 text-gray-400" />
            </div>
          )}
          <input ref={ref} className={inputClasses} {...props} />
          {Icon && iconPosition === 'right' && (
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
              <Icon className="h-5 w-5 text-gray-400" />
            </div>
          )}
        </div>
        {(error || helperText) && (
          <p className={`mt-1.5 text-sm ${hasError ? 'text-red-600' : 'text-gray-500'}`}>
            {error || helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export const Textarea = forwardRef(
  ({ label, error, helperText, className = '', rows = 4, ...props }, ref) => {
    const hasError = Boolean(error);

    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          rows={rows}
          className={`input resize-none ${hasError ? 'input-error' : ''}`}
          {...props}
        />
        {(error || helperText) && (
          <p className={`mt-1.5 text-sm ${hasError ? 'text-red-600' : 'text-gray-500'}`}>
            {error || helperText}
          </p>
        )}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';

export const Select = forwardRef(
  ({ label, error, helperText, className = '', options = [], placeholder, ...props }, ref) => {
    const hasError = Boolean(error);

    return (
      <div className={className}>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            {label}
          </label>
        )}
        <select
          ref={ref}
          className={`input ${hasError ? 'input-error' : ''}`}
          {...props}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {(error || helperText) && (
          <p className={`mt-1.5 text-sm ${hasError ? 'text-red-600' : 'text-gray-500'}`}>
            {error || helperText}
          </p>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';

export default Input;
