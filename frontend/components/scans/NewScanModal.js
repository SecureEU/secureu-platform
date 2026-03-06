'use client'

import React, { useState } from 'react';
import { useAuth } from '@/lib/auth';

// Icons as simple SVG components
const NetworkIcon = () => (
  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
  </svg>
);

const WebIcon = () => (
  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
  </svg>
);

const MultiIcon = () => (
  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
  </svg>
);

const ShieldIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
  </svg>
);

const SpinnerIcon = () => (
  <svg className="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
  </svg>
);

const CheckIcon = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
  </svg>
);

const NewScanModal = ({ onClose }) => {
  const { authFetch, API_URL } = useAuth();
  const [step, setStep] = useState(1);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    target: '',
    scan_type: '',
    scan_speed: 'normal',
    network_scan_type: 'normal',
    web_scan_type: 'baseline',
    port: '80'
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const scanTypes = [
    {
      id: 'network',
      name: 'Network Scan',
      description: 'Port scanning and service detection using Nmap',
      icon: NetworkIcon,
      color: 'blue'
    },
    {
      id: 'web',
      name: 'Web Application',
      description: 'OWASP ZAP vulnerability scanning',
      icon: WebIcon,
      color: 'purple'
    },
    {
      id: 'multi',
      name: 'Multi Scan',
      description: 'Combined network and web scanning',
      icon: MultiIcon,
      color: 'emerald'
    }
  ];

  const networkScanTypes = [
    { id: 'normal', name: 'Standard', description: 'Service version detection (-sV)', time: '~2-5 min' },
    { id: 'service', name: 'Service Scripts', description: 'Version + default scripts (-sV -sC)', time: '~5-10 min' },
    { id: 'vulners', name: 'Vulnerability Scan', description: 'CVE detection with vulners script', time: '~5-15 min', recommended: true },
    { id: 'full', name: 'Full Scan', description: 'All scripts + vulnerability detection', time: '~10-20 min' }
  ];

  const webScanTypes = [
    { id: 'baseline', name: 'Baseline', description: 'Quick passive scan for common issues', time: '~1-3 min' },
    { id: 'full-scan', name: 'Full Scan', description: 'Active scanning with attack simulation', time: '~10-30 min' },
    { id: 'api-scan', name: 'API Scan', description: 'REST API security testing', time: '~5-15 min' }
  ];

  const scanSpeeds = [
    { id: 'slow', name: 'Stealth', description: 'Slow and quiet, evades detection' },
    { id: 'normal', name: 'Normal', description: 'Balanced speed and accuracy' },
    { id: 'fast', name: 'Aggressive', description: 'Fast but may trigger alerts' }
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  const selectScanType = (type) => {
    setFormData(prev => ({
      ...prev,
      scan_type: type,
      network_scan_type: type === 'network' || type === 'multi' ? 'normal' : prev.network_scan_type,
      web_scan_type: type === 'web' || type === 'multi' ? 'baseline' : prev.web_scan_type
    }));
    setError('');
  };

  const resetForm = () => {
    setFormData({
      name: '',
      target: '',
      scan_type: '',
      scan_speed: 'normal',
      network_scan_type: 'normal',
      web_scan_type: 'baseline',
      port: '80'
    });
    setError('');
    setSuccess('');
    setStep(1);
  };

  const validateStep = (stepNum) => {
    if (stepNum === 1) {
      if (!formData.scan_type) {
        setError('Please select a scan type');
        return false;
      }
    }
    if (stepNum === 2) {
      if (!formData.name.trim()) {
        setError('Please enter a scan name');
        return false;
      }
      if (!formData.target.trim()) {
        setError('Please enter a target');
        return false;
      }
    }
    return true;
  };

  const nextStep = () => {
    if (validateStep(step)) {
      setStep(step + 1);
      setError('');
    }
  };

  const prevStep = () => {
    setStep(step - 1);
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateStep(2)) return;

    setIsLoading(true);
    setError('');

    const requestData = {
      name: formData.name,
      target: formData.target,
      scan_type: formData.scan_type,
      scan_speed: formData.scan_speed,
      network_scan_type: formData.network_scan_type,
      web_scan_type: formData.web_scan_type,
      port: formData.port
    };

    try {
      const response = await authFetch(`${API_URL}/scan/create`, {
        method: 'POST',
        body: JSON.stringify(requestData)
      });

      const data = await response.json();

      if (response.ok) {
        setSuccess(`Scan created successfully!`);
        setTimeout(() => {
          onClose();
          resetForm();
        }, 1500);
      } else {
        setError(data.message || 'Failed to create scan');
      }
    } catch (err) {
      setError('An unexpected error occurred. Please try again.');
      console.error('Error creating scan:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const getColorClasses = (color, isSelected) => {
    const colors = {
      blue: isSelected
        ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-500'
        : 'border-slate-200 hover:border-blue-300 hover:bg-blue-50/50',
      purple: isSelected
        ? 'border-purple-500 bg-purple-50 ring-2 ring-purple-500'
        : 'border-slate-200 hover:border-purple-300 hover:bg-purple-50/50',
      emerald: isSelected
        ? 'border-emerald-500 bg-emerald-50 ring-2 ring-emerald-500'
        : 'border-slate-200 hover:border-emerald-300 hover:bg-emerald-50/50'
    };
    return colors[color] || colors.blue;
  };

  const getIconColorClasses = (color, isSelected) => {
    const colors = {
      blue: isSelected ? 'text-blue-600' : 'text-slate-400 group-hover:text-blue-500',
      purple: isSelected ? 'text-purple-600' : 'text-slate-400 group-hover:text-purple-500',
      emerald: isSelected ? 'text-emerald-600' : 'text-slate-400 group-hover:text-emerald-500'
    };
    return colors[color] || colors.blue;
  };

  return (
    <div className="bg-white rounded-xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden flex flex-col" style={{ maxHeight: 'calc(100vh - 2rem)' }}>
      {/* Header */}
      <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">Create New Scan</h2>
            <p className="text-sm text-slate-500 mt-0.5">Step {step} of 3</p>
          </div>
          <button
            onClick={() => { resetForm(); onClose(); }}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        {/* Progress bar */}
        <div className="mt-4 h-1.5 bg-slate-200 rounded-full overflow-hidden">
          <div
            className="h-full bg-blue-500 transition-all duration-300 ease-out"
            style={{ width: `${(step / 3) * 100}%` }}
          />
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 min-h-0 overflow-y-auto p-6">
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg flex items-center gap-2">
            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 bg-green-50 border border-green-200 text-green-700 rounded-lg flex items-center gap-2">
            <CheckIcon />
            {success}
          </div>
        )}

        {/* Step 1: Select Scan Type */}
        {step === 1 && (
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-medium text-slate-900 mb-1">Select Scan Type</h3>
              <p className="text-sm text-slate-500">Choose the type of security scan you want to perform</p>
            </div>

            <div className="grid gap-3">
              {scanTypes.map((type) => {
                const Icon = type.icon;
                const isSelected = formData.scan_type === type.id;
                return (
                  <button
                    key={type.id}
                    type="button"
                    onClick={() => selectScanType(type.id)}
                    className={`group relative p-4 rounded-xl border-2 text-left transition-all duration-200 ${getColorClasses(type.color, isSelected)}`}
                  >
                    <div className="flex items-start gap-4">
                      <div className={`flex-shrink-0 transition-colors ${getIconColorClasses(type.color, isSelected)}`}>
                        <Icon />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <h4 className={`font-medium ${isSelected ? 'text-slate-900' : 'text-slate-700'}`}>
                            {type.name}
                          </h4>
                          {isSelected && (
                            <span className={`inline-flex items-center justify-center w-5 h-5 rounded-full bg-${type.color}-500 text-white`}>
                              <CheckIcon />
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-slate-500 mt-0.5">{type.description}</p>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Step 2: Configure Scan */}
        {step === 2 && (
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-medium text-slate-900 mb-1">Configure Scan</h3>
              <p className="text-sm text-slate-500">Enter target details and scan options</p>
            </div>

            {/* Basic Info */}
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Scan Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2.5 bg-white text-slate-900 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                  placeholder="e.g., Production Server Scan"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Target <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="target"
                  value={formData.target}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2.5 bg-white text-slate-900 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                  placeholder={formData.scan_type === 'web' ? 'https://example.com' : '192.168.1.1 or example.com'}
                />
                <p className="mt-1.5 text-xs text-slate-500">
                  {formData.scan_type === 'network' && 'Enter IP address, hostname, or CIDR range'}
                  {formData.scan_type === 'web' && 'Enter the full URL including protocol (http/https)'}
                  {formData.scan_type === 'multi' && 'Enter IP address or hostname'}
                </p>
              </div>

              {(formData.scan_type === 'web' || formData.scan_type === 'multi') && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">
                    Port
                  </label>
                  <input
                    type="text"
                    name="port"
                    value={formData.port}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2.5 bg-white text-slate-900 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                    placeholder="80, 443, 8080..."
                  />
                </div>
              )}
            </div>

            {/* Network Scan Type */}
            {(formData.scan_type === 'network' || formData.scan_type === 'multi') && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Network Scan Type
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {networkScanTypes.map((type) => (
                    <button
                      key={type.id}
                      type="button"
                      onClick={() => setFormData(prev => ({ ...prev, network_scan_type: type.id }))}
                      className={`relative p-3 rounded-lg border-2 text-left transition-all ${
                        formData.network_scan_type === type.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      {type.recommended && (
                        <span className="absolute -top-2 -right-2 px-2 py-0.5 bg-amber-400 text-amber-900 text-xs font-medium rounded-full">
                          Recommended
                        </span>
                      )}
                      <div className="flex items-center gap-2">
                        {type.id === 'vulners' && <ShieldIcon />}
                        <span className={`font-medium text-sm ${formData.network_scan_type === type.id ? 'text-blue-700' : 'text-slate-700'}`}>
                          {type.name}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">{type.description}</p>
                      <p className="text-xs text-slate-400 mt-1">{type.time}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Web Scan Type */}
            {(formData.scan_type === 'web' || formData.scan_type === 'multi') && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Web Scan Type
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {webScanTypes.map((type) => (
                    <button
                      key={type.id}
                      type="button"
                      onClick={() => setFormData(prev => ({ ...prev, web_scan_type: type.id }))}
                      className={`p-3 rounded-lg border-2 text-left transition-all ${
                        formData.web_scan_type === type.id
                          ? 'border-purple-500 bg-purple-50'
                          : 'border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className={`font-medium text-sm ${formData.web_scan_type === type.id ? 'text-purple-700' : 'text-slate-700'}`}>
                        {type.name}
                      </span>
                      <p className="text-xs text-slate-500 mt-1">{type.description}</p>
                      <p className="text-xs text-slate-400 mt-1">{type.time}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 3: Review & Launch */}
        {step === 3 && (
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-medium text-slate-900 mb-1">Review & Launch</h3>
              <p className="text-sm text-slate-500">Confirm your scan configuration</p>
            </div>

            {/* Summary Card */}
            <div className="bg-slate-50 rounded-xl p-5 space-y-4">
              <div className="flex items-center gap-3 pb-4 border-b border-slate-200">
                {formData.scan_type === 'network' && <NetworkIcon />}
                {formData.scan_type === 'web' && <WebIcon />}
                {formData.scan_type === 'multi' && <MultiIcon />}
                <div>
                  <h4 className="font-semibold text-slate-900">{formData.name}</h4>
                  <p className="text-sm text-slate-500">{formData.target}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-slate-500">Scan Type</span>
                  <p className="font-medium text-slate-900 capitalize">{formData.scan_type}</p>
                </div>
                {(formData.scan_type === 'network' || formData.scan_type === 'multi') && (
                  <div>
                    <span className="text-slate-500">Network Scan</span>
                    <p className="font-medium text-slate-900">
                      {networkScanTypes.find(t => t.id === formData.network_scan_type)?.name}
                    </p>
                  </div>
                )}
                {(formData.scan_type === 'web' || formData.scan_type === 'multi') && (
                  <>
                    <div>
                      <span className="text-slate-500">Web Scan</span>
                      <p className="font-medium text-slate-900">
                        {webScanTypes.find(t => t.id === formData.web_scan_type)?.name}
                      </p>
                    </div>
                    <div>
                      <span className="text-slate-500">Port</span>
                      <p className="font-medium text-slate-900">{formData.port}</p>
                    </div>
                  </>
                )}
                <div>
                  <span className="text-slate-500">Speed</span>
                  <p className="font-medium text-slate-900 capitalize">{formData.scan_speed}</p>
                </div>
              </div>

              {formData.network_scan_type === 'vulners' && (
                <div className="flex items-center gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                  <ShieldIcon />
                  <p className="text-sm text-amber-800">
                    <strong>CVE Detection enabled</strong> - This scan will check for known vulnerabilities
                  </p>
                </div>
              )}
            </div>

            {/* Scan Speed */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Scan Speed
              </label>
              <div className="grid grid-cols-3 gap-2">
                {scanSpeeds.map((speed) => (
                  <button
                    key={speed.id}
                    type="button"
                    onClick={() => setFormData(prev => ({ ...prev, scan_speed: speed.id }))}
                    className={`p-3 rounded-lg border-2 text-left transition-all ${
                      formData.scan_speed === speed.id
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <span className={`font-medium text-sm ${formData.scan_speed === speed.id ? 'text-blue-700' : 'text-slate-700'}`}>
                      {speed.name}
                    </span>
                    <p className="text-xs text-slate-500 mt-1">{speed.description}</p>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 flex items-center justify-between">
        <button
          type="button"
          onClick={step === 1 ? () => { resetForm(); onClose(); } : prevStep}
          className="px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
        >
          {step === 1 ? 'Cancel' : 'Back'}
        </button>

        {step < 3 ? (
          <button
            type="button"
            onClick={nextStep}
            disabled={step === 1 && !formData.scan_type}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Continue
          </button>
        ) : (
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isLoading}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2 transition-colors"
          >
            {isLoading ? (
              <>
                <SpinnerIcon />
                Creating...
              </>
            ) : (
              <>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Launch Scan
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
};

export default NewScanModal;
