'use client'

import React, { useState } from 'react';
import { AlertTriangle, ChevronDown, ChevronUp, Shield } from 'lucide-react';

const ZapResults = ({ data }) => {
  const [expandedAlerts, setExpandedAlerts] = useState({});
  const alerts = data?.zdata || [];

  const toggleAlert = (index) => {
    setExpandedAlerts(prev => ({
      ...prev,
      [index]: !prev[index]
    }));
  };

  const getRiskColor = (riskdesc) => {
    if (!riskdesc) return 'bg-gray-100 text-gray-800';
    const risk = riskdesc.split(' ')[0].toLowerCase();
    switch (risk) {
      case 'high': return 'bg-red-100 text-red-800';
      case 'medium': return 'bg-orange-100 text-orange-800';
      case 'low': return 'bg-blue-100 text-blue-800';
      case 'informational': return 'bg-green-100 text-green-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  };

  const getRiskIconColor = (riskdesc) => {
    if (!riskdesc) return 'text-slate-400';
    const risk = riskdesc.split(' ')[0].toLowerCase();
    switch (risk) {
      case 'high': return 'text-red-500';
      case 'medium': return 'text-orange-500';
      case 'low': return 'text-blue-500';
      case 'informational': return 'text-green-500';
      default: return 'text-slate-400';
    }
  };

  const getRiskStats = () => {
    const stats = { high: 0, medium: 0, low: 0, informational: 0 };
    alerts?.forEach(alert => {
      const risk = alert['@riskdesc']?.split(' ')[0].toLowerCase();
      if (stats[risk] !== undefined) stats[risk]++;
    });
    return stats;
  };

  if (!alerts || alerts.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 bg-slate-50 rounded-lg">
        <p className="text-slate-600">No vulnerabilities found</p>
      </div>
    );
  }

  const riskStats = getRiskStats();

  return (
    <div className="space-y-6 p-6">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
          <div className="flex items-center justify-between text-sm font-medium text-slate-600">
            <span className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-red-500" />
              High Risk
            </span>
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{riskStats.high}</div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
          <div className="flex items-center justify-between text-sm font-medium text-slate-600">
            <span className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-orange-500" />
              Medium Risk
            </span>
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{riskStats.medium}</div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
          <div className="flex items-center justify-between text-sm font-medium text-slate-600">
            <span className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-blue-500" />
              Low Risk
            </span>
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{riskStats.low}</div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
          <div className="flex items-center justify-between text-sm font-medium text-slate-600">
            <span className="flex items-center gap-2">
              <Shield className="h-4 w-4 text-green-500" />
              Info
            </span>
          </div>
          <div className="mt-2 text-2xl font-bold text-slate-900">{riskStats.informational}</div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-slate-200">
        <div className="px-6 py-4 border-b border-slate-200">
          <h2 className="text-lg font-semibold text-slate-900">Vulnerability Alerts</h2>
        </div>

        <div className="divide-y divide-slate-200">
          {alerts.map((alert, index) => (
            <div key={index} className="transition-colors hover:bg-slate-50">
              <button
                onClick={() => toggleAlert(index)}
                className="w-full px-6 py-4 flex items-center justify-between text-left"
              >
                <div className="flex items-center gap-3">
                  <AlertTriangle className={`h-5 w-5 ${getRiskIconColor(alert['@riskdesc'])}`} />
                  <div>
                    <h3 className="font-medium text-slate-900">{alert['@name']}</h3>
                    <p className="text-sm text-slate-600">
                      {alert.urls?.length || 0} affected URL{alert.urls?.length !== 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`px-2.5 py-1 text-xs font-medium rounded-full ${getRiskColor(alert['@riskdesc'])}`}>
                    {alert['@riskdesc']}
                  </span>
                  {alert['@cweid'] && (
                    <span className="px-2.5 py-1 text-xs font-medium bg-slate-100 text-slate-800 rounded-full">
                      CWE-{alert['@cweid']}
                    </span>
                  )}
                  {expandedAlerts[index] ? <ChevronUp className="h-5 w-5" /> : <ChevronDown className="h-5 w-5" />}
                </div>
              </button>
              
              {expandedAlerts[index] && (
                <div className="px-6 pb-4 space-y-4">
                  {alert['@description'] && (
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <h4 className="text-sm font-medium text-slate-900 mb-2">Description</h4>
                      <div 
                        className="text-sm text-slate-600 prose max-w-none"
                        dangerouslySetInnerHTML={{ __html: alert['@description'] }}
                      />
                    </div>
                  )}

                  {alert['@solution'] && (
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <h4 className="text-sm font-medium text-slate-900 mb-2">Recommended Solution</h4>
                      <div 
                        className="text-sm text-slate-600 prose max-w-none"
                        dangerouslySetInnerHTML={{ __html: alert['@solution'] }}
                      />
                    </div>
                  )}

                  {alert['@otherinfo'] && (
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <h4 className="text-sm font-medium text-slate-900 mb-2">Additional Information</h4>
                      <div 
                        className="text-sm text-slate-600 prose max-w-none"
                        dangerouslySetInnerHTML={{ __html: alert['@otherinfo'] }}
                      />
                    </div>
                  )}

                  {alert.urls && alert.urls.length > 0 && (
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <h4 className="text-sm font-medium text-slate-900 mb-2">Affected URLs</h4>
                      <ul className="space-y-1">
                        {alert.urls.map((url, urlIndex) => (
                          <li key={urlIndex} className="text-sm text-slate-600 flex items-center gap-2">
                            <span className="text-blue-500">•</span>
                            {url}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ZapResults;