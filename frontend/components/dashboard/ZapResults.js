'use client'

import React, { useState, useMemo } from 'react';
import { AlertTriangle, ChevronDown, ChevronUp, ChevronLeft, ChevronRight, Shield } from 'lucide-react';

const ZapResults = ({ data }) => {
  const [expandedAlerts, setExpandedAlerts] = useState({});
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [riskFilter, setRiskFilter] = useState('all');
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

  const filteredAlerts = useMemo(() => {
    if (riskFilter === 'all') return alerts;
    return alerts.filter(alert => {
      const risk = alert['@riskdesc']?.split(' ')[0].toLowerCase();
      return risk === riskFilter;
    });
  }, [alerts, riskFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredAlerts.length / pageSize));
  const paginatedAlerts = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredAlerts.slice(start, start + pageSize);
  }, [filteredAlerts, currentPage, pageSize]);

  const handlePageChange = (page) => {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    setExpandedAlerts({});
  };

  const handleFilterChange = (filter) => {
    setRiskFilter(filter);
    setCurrentPage(1);
    setExpandedAlerts({});
  };

  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(1);
    setExpandedAlerts({});
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
        {[
          { label: 'High Risk', key: 'high', color: 'red', value: riskStats.high },
          { label: 'Medium Risk', key: 'medium', color: 'orange', value: riskStats.medium },
          { label: 'Low Risk', key: 'low', color: 'blue', value: riskStats.low },
          { label: 'Info', key: 'informational', color: 'green', value: riskStats.informational },
        ].map(({ label, key, color, value }) => (
          <button
            key={key}
            onClick={() => handleFilterChange(riskFilter === key ? 'all' : key)}
            className={`bg-white rounded-lg shadow-sm border p-4 text-left transition-all ${
              riskFilter === key
                ? `border-${color}-400 ring-2 ring-${color}-200`
                : 'border-slate-200 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between text-sm font-medium text-slate-600">
              <span className="flex items-center gap-2">
                <Shield className={`h-4 w-4 text-${color}-500`} />
                {label}
              </span>
              {riskFilter === key && (
                <span className="text-xs text-slate-400">Active</span>
              )}
            </div>
            <div className="mt-2 text-2xl font-bold text-slate-900">{value}</div>
          </button>
        ))}
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-slate-200">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between flex-wrap gap-3">
          <h2 className="text-lg font-semibold text-slate-900">
            Vulnerability Alerts
            <span className="ml-2 text-sm font-normal text-slate-500">
              ({filteredAlerts.length}{riskFilter !== 'all' ? ` ${riskFilter}` : ''} total)
            </span>
          </h2>
          <div className="flex items-center gap-3">
            <label className="text-sm text-slate-600">Per page:</label>
            <select
              value={pageSize}
              onChange={(e) => handlePageSizeChange(Number(e.target.value))}
              className="border border-slate-300 rounded-md px-2 py-1 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {[5, 10, 20, 50].map(size => (
                <option key={size} value={size}>{size}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="divide-y divide-slate-200">
          {paginatedAlerts.map((alert, index) => {
            const globalIndex = (currentPage - 1) * pageSize + index;
            return (
            <div key={globalIndex} className="transition-colors hover:bg-slate-50">
              <button
                onClick={() => toggleAlert(globalIndex)}
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
                  {expandedAlerts[globalIndex] ? <ChevronUp className="h-5 w-5" /> : <ChevronDown className="h-5 w-5" />}
                </div>
              </button>

              {expandedAlerts[globalIndex] && (
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
          );
          })}
        </div>

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-200 flex items-center justify-between">
            <div className="text-sm text-slate-600">
              Showing {(currentPage - 1) * pageSize + 1}–{Math.min(currentPage * pageSize, filteredAlerts.length)} of {filteredAlerts.length}
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => handlePageChange(1)}
                disabled={currentPage === 1}
                className="px-2 py-1 text-sm rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                First
              </button>
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className="p-1 rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              {Array.from({ length: totalPages }, (_, i) => i + 1)
                .filter(page => page === 1 || page === totalPages || Math.abs(page - currentPage) <= 1)
                .reduce((acc, page, idx, arr) => {
                  if (idx > 0 && page - arr[idx - 1] > 1) {
                    acc.push('...');
                  }
                  acc.push(page);
                  return acc;
                }, [])
                .map((item, idx) =>
                  item === '...' ? (
                    <span key={`ellipsis-${idx}`} className="px-2 py-1 text-sm text-slate-400">...</span>
                  ) : (
                    <button
                      key={item}
                      onClick={() => handlePageChange(item)}
                      className={`px-3 py-1 text-sm rounded border ${
                        currentPage === item
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'border-slate-300 hover:bg-slate-50'
                      }`}
                    >
                      {item}
                    </button>
                  )
                )}
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className="p-1 rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
              <button
                onClick={() => handlePageChange(totalPages)}
                disabled={currentPage === totalPages}
                className="px-2 py-1 text-sm rounded border border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50"
              >
                Last
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ZapResults;