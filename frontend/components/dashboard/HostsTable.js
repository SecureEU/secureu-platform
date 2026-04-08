'use client'

import React, { useState, useMemo } from 'react';
import { Search, ChevronRight, ChevronLeft } from 'lucide-react';

const HostsTable = ({ scans, onScanSelect, searchTerm, onSearchChange }) => {
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const totalPages = Math.max(1, Math.ceil(scans.length / pageSize));
  const paginatedScans = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return scans.slice(start, start + pageSize);
  }, [scans, currentPage, pageSize]);

  // Reset page when search changes
  const handleSearchChange = (value) => {
    onSearchChange(value);
    setCurrentPage(1);
  };

  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)));
  };

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

  const pageNumbers = useMemo(() => {
    return Array.from({ length: totalPages }, (_, i) => i + 1)
      .filter(page => page === 1 || page === totalPages || Math.abs(page - currentPage) <= 1)
      .reduce((acc, page, idx, arr) => {
        if (idx > 0 && page - arr[idx - 1] > 1) acc.push('...');
        acc.push(page);
        return acc;
      }, []);
  }, [totalPages, currentPage]);

  return (
    <div className="space-y-4">
      <div className="relative">
        <Search className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
        <input
          type="text"
          placeholder="Search scans by name..."
          className="w-full pl-10 pr-4 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          value={searchTerm}
          onChange={(e) => handleSearchChange(e.target.value)}
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
              {paginatedScans.length > 0 ? (
                paginatedScans.map((scan) => (
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
                ))
              ) : (
                <tr>
                  <td colSpan="7" className="px-6 py-12 text-center text-slate-500">
                    {searchTerm ? 'No scans found matching your search.' : 'No scans available.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-6 py-3 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-600">
              Showing {scans.length > 0 ? (currentPage - 1) * pageSize + 1 : 0}–{Math.min(currentPage * pageSize, scans.length)} of {scans.length}
            </span>
            <div className="flex items-center gap-2">
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
          {totalPages > 1 && (
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
              {pageNumbers.map((item, idx) =>
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
          )}
        </div>
      </div>
    </div>
  );
};

export default HostsTable;
