'use client'

import React from 'react';
import { Search, ChevronRight} from 'lucide-react';

const HostsTable = ({ scans, onScanSelect, searchTerm, onSearchChange }) => {
  const getScanTypeColor = (type) => {
    switch (type?.toLowerCase()) {
      case 'network':
        return 'bg-blue-100 text-blue-800';
      case 'web':
        return 'bg-purple-100 text-purple-800';
      case 'multi':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-slate-100 text-slate-800';
    }
  };

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'finished':
        return 'bg-green-100 text-green-800';
      case 'running':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-slate-100 text-slate-800';
    }
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    return dateTimeStr.replace('T', ' ').replace(/_/g, ':');
  };

  const generateScanKey = (scan) => {
    return `${scan.scan_name || scan.name}-${scan.start_time}`;
  };

  return (
    <div className="space-y-4">
      <div className="relative">
        <Search className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
        <input
          type="text"
          placeholder="Search scans by name..."
          className="w-full pl-10 pr-4 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-slate-200">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50">
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">Name</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">Target</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">Type</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">Status</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">Start Time</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900">End Time</th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-slate-900"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {scans.map((scan) => (
                <tr 
                  key={generateScanKey(scan)}
                  className="hover:bg-slate-50 cursor-pointer transition-colors duration-150"
                  onClick={() => onScanSelect(scan._id)}
                >
                  <td className="px-6 py-4 text-sm text-slate-900">{scan.scan_name || scan.name}</td>
                  <td className="px-6 py-4 text-sm text-slate-900">{scan.target}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${getScanTypeColor(scan.type)}`}>
                      {scan.type}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${getStatusColor(scan.status)}`}>
                      {scan.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-900">{formatDateTime(scan.start_time)}</td>
                  <td className="px-6 py-4 text-sm text-slate-900">{formatDateTime(scan.end_time)}</td>
                  <td className="px-6 py-4">
                    <button className="text-blue-500 hover:text-blue-700 transition-colors duration-150">
                      <ChevronRight className="h-5 w-5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default HostsTable;