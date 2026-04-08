'use client'

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Trash2, AlertCircle, Play, RefreshCw, Eye, ChevronLeft, ChevronRight } from 'lucide-react';
import NewScanModal from './NewScanModal';
import HostDetails from '../dashboard/HostDetails';
import { useAuth } from '@/lib/auth';

// Badge Components
const SeverityBadge = ({ count, level }) => {
  const colors = {
    high: 'bg-red-100 text-red-800',
    medium: 'bg-yellow-100 text-yellow-800',
    low: 'bg-blue-100 text-blue-800',
    info: 'bg-gray-100 text-gray-800'
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs ${colors[level]}`}>
      {count}
    </span>
  );
};

const PortBadge = ({ port, service }) => (
  <span className="inline-flex items-center px-2 py-1 mr-2 mb-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
    {port}/{service}
  </span>
);

const ActionButton = ({ scan, onStartScan }) => {
  const isFinished = scan.status === 'finished';
  const isRunning = scan.status === 'running';

  if (isRunning) return null;

  return (
    <button
      onClick={() => onStartScan(scan._id, scan.type)}
      className={`p-1 rounded ${isFinished ? 'text-blue-600 hover:text-blue-700' : 'text-green-600 hover:text-green-700'}`}
      title={isFinished ? "Rescan" : "Start Scan"}
    >
      {isFinished ? <RefreshCw className="w-4 h-4" /> : <Play className="w-4 h-4" />}
    </button>
  );
};

const formatScanDate = (dateString) => {
  if (!dateString) return 'N/A';
  
  const formattedString = dateString
    .replace('T', ' ')
    .replace(/_/g, ':')
    .replace(' ', 'T');
    
  try {
    const date = new Date(formattedString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true
    });
  } catch (e) {
    console.error('Date parsing error:', e);
    return 'Invalid Date';
  }
};

const Scans = () => {
  const [scans, setScans] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedScanId, setSelectedScanId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const { authFetch, loading: authLoading, API_URL } = useAuth();

  const fetchScans = async () => {
    setIsLoading(true);
    try {
      const response = await authFetch(`${API_URL}/scans`);
      if (!response.ok) throw new Error('Failed to fetch scans');
      const data = await response.json();
      setScans(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    // Wait for auth to initialize before fetching
    if (!authLoading) {
      fetchScans();
    }
  }, [authLoading]);

  const handleStartScan = async (scanId, scanType) => {
    let endpoint;

    switch(scanType) {
      case 'network':
        endpoint = 'nmap/start';
        break;
      case 'web':
        endpoint = 'zap/start';
        break;
      case 'multi':
        endpoint = 'multi/start';
        break;
      default:
        setError(`Unknown scan type: ${scanType}`);
        return;
    }

    try {
      const response = await authFetch(`${API_URL}/${endpoint}`, {
        method: 'POST',
        body: JSON.stringify({ scan_id: scanId })
      });

      if (!response.ok) throw new Error(`Failed to start ${scanType} scan`);

      fetchScans();
    } catch (err) {
      setError(`Failed to start ${scanType} scan: ${err.message}`);
    }
  };

  const getVulnerabilityCounts = (scan) => {
    const sites = scan.zdata?.site;
    if (!sites || !Array.isArray(sites)) return { high: 0, medium: 0, low: 0, info: 0 };

    return sites.flatMap(s => s.alerts || []).reduce((acc, alert) => {
      const riskLevel = alert.riskdesc?.toLowerCase().split(' ')[0];
      if (riskLevel === 'informational') acc.info++;
      else if (acc[riskLevel] !== undefined) acc[riskLevel]++;
      return acc;
    }, { high: 0, medium: 0, low: 0, info: 0 });
  };

  const getOpenPorts = (scan) => {
    // Handle multiple hosts (IP range scans)
    if (scan.ndata?.nmap?.host) {
      const host = scan.ndata.nmap.host;

      // Check if host is an array (multiple hosts from IP range scan)
      if (Array.isArray(host)) {
        // Return summary for multiple hosts
        let allPorts = [];
        host.forEach(h => {
          const ports = h.ports?.port || [];
          const portArr = Array.isArray(ports) ? ports : [ports];
          portArr.forEach(port => {
            const state = port?.state?.['@state'] || port?.state?.state;
            if (state === 'open') {
              allPorts.push({
                port: port['@portid'] || port.portid,
                service: port.service?.['@name'] || port.service?.name || 'unknown',
                host: h.address?.[0]?.['@addr'] || h.address?.['@addr'] || 'unknown'
              });
            }
          });
        });
        return allPorts;
      }

      // Single host
      const ports = host.ports?.port;
      if (!ports) return [];

      const portArr = Array.isArray(ports) ? ports : [ports];
      return portArr
        .filter(port => {
          const state = port?.state?.['@state'] || port?.state?.state;
          return state === 'open';
        })
        .map(port => ({
          port: port['@portid'] || port.portid,
          service: port.service?.['@name'] || port.service?.name || 'unknown'
        }));
    }

    return [];
  };

  const getHostCount = (scan) => {
    if (scan.ndata?.nmap?.host) {
      const host = scan.ndata.nmap.host;
      if (Array.isArray(host)) {
        return host.length;
      }
      return 1;
    }
    return 0;
  };

  const handleDeleteScan = async (scanId) => {
    try {
      const response = await authFetch(`${API_URL}/scans/${scanId}`, {
        method: 'DELETE'
      });
      if (!response.ok) throw new Error('Failed to delete scan');
      setScans(prevScans => prevScans.filter(scan => scan._id !== scanId));
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(1);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    fetchScans();
  };

  const filteredScans = scans.filter(scan =>
    (scan.scan_name?.toLowerCase().includes(searchTerm.toLowerCase()) || 
     scan.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
     scan.target?.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const indexOfLastScan = currentPage * rowsPerPage;
  const indexOfFirstScan = indexOfLastScan - rowsPerPage;
  const currentScans = filteredScans.slice(indexOfFirstScan, indexOfLastScan);

  const totalPages = Math.ceil(filteredScans.length / rowsPerPage);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading scans...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border-l-4 border-red-400 p-4">
        <div className="flex">
          <AlertCircle className="h-5 w-5 text-red-400" />
          <div className="ml-3">
            <p className="text-sm text-red-700">{error}</p>
            <button
              onClick={() => {setError(null); fetchScans();}}
              className="mt-2 text-sm text-red-600 underline hover:text-red-800"
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Controls */}
      <div className="flex justify-between items-center">
        <div className="relative">
          <input
            type="text"
            placeholder="Search scans..."
            value={searchTerm}
            onChange={handleSearchChange}
            className="bg-white text-gray-900 pl-10 pr-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
          <svg className="w-5 h-5 absolute left-3 top-2.5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md transition-colors duration-200 font-medium"
        >
          + New Scan
        </button>
      </div>

      {/* Scans Table */}
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Scan Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Target</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Vulnerabilities</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Open Ports</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Start Time</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">End Time</th>
                <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Delete</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {currentScans.length > 0 ? (
                currentScans.map((scan) => {
                  const vulnCounts = getVulnerabilityCounts(scan);
                  const openPorts = getOpenPorts(scan);

                  return (
                    <tr key={scan._id} className="hover:bg-gray-50 transition-colors duration-150">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => setSelectedScanId(scan._id)}
                            className="p-1 rounded text-gray-600 hover:text-blue-600 hover:bg-blue-50"
                            title="View Details"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <ActionButton scan={scan} onStartScan={handleStartScan} />
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {scan.scan_name || scan.name || 'Unnamed Scan'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{scan.target || 'N/A'}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          scan.type === 'network' ? 'bg-blue-100 text-blue-800' :
                          scan.type === 'web' ? 'bg-purple-100 text-purple-800' :
                          scan.type === 'multi' ? 'bg-green-100 text-green-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {scan.type || 'Unknown'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          scan.status === 'finished' ? 'bg-green-100 text-green-800' :
                          scan.status === 'running' ? 'bg-blue-100 text-blue-800' :
                          scan.status === 'failed' ? 'bg-red-100 text-red-800' :
                          'bg-yellow-100 text-yellow-800'
                        }`}>
                          {scan.status || 'Unknown'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex space-x-1">
                          <SeverityBadge count={vulnCounts.high} level="high" />
                          <SeverityBadge count={vulnCounts.medium} level="medium" />
                          <SeverityBadge count={vulnCounts.low} level="low" />
                          <SeverityBadge count={vulnCounts.info} level="info" />
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {scan.type === 'network' || scan.type === 'multi' ? (
                          <div className="flex flex-wrap gap-1 max-w-xs items-center">
                            {getHostCount(scan) > 1 && (
                              <span className="px-2 py-1 mr-2 text-xs font-medium bg-purple-100 text-purple-800 rounded-full">
                                {getHostCount(scan)} hosts
                              </span>
                            )}
                            {openPorts.length > 0 ? (
                              openPorts.slice(0, 3).map(({ port, service }, idx) => (
                                <PortBadge key={`${port}-${idx}`} port={port} service={service} />
                              ))
                            ) : (
                              <span className="text-sm text-gray-400">No open ports</span>
                            )}
                            {openPorts.length > 3 && (
                              <span className="text-xs text-gray-500">+{openPorts.length - 3} more</span>
                            )}
                          </div>
                        ) : (
                          <span className="text-sm text-gray-400">N/A</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {formatScanDate(scan.start_time)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {formatScanDate(scan.end_time)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <button
                          onClick={() => handleDeleteScan(scan._id)}
                          className="text-red-600 hover:text-red-800 p-1 rounded transition-colors duration-150"
                          title="Delete Scan"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="10" className="px-6 py-12 text-center">
                    <div className="text-gray-500">
                      {searchTerm ? 'No scans found matching your search.' : 'No scans available. Create your first scan!'}
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {filteredScans.length > 0 && (
        <div className="flex flex-col sm:flex-row justify-between items-center gap-3 bg-white px-6 py-3 border border-gray-200 rounded-lg">
          <div className="flex items-center gap-4">
            <p className="text-sm text-gray-700">
              Showing <span className="font-medium">{indexOfFirstScan + 1}</span>–<span className="font-medium">{Math.min(indexOfLastScan, filteredScans.length)}</span> of{' '}
              <span className="font-medium">{filteredScans.length}</span> scans
            </p>
            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-600">Per page:</label>
              <select
                value={rowsPerPage}
                onChange={(e) => { setRowsPerPage(Number(e.target.value)); setCurrentPage(1); }}
                className="border border-gray-300 rounded-md px-2 py-1 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
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
                onClick={() => setCurrentPage(1)}
                disabled={currentPage === 1}
                className="px-2 py-1 text-sm rounded border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                First
              </button>
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="p-1 rounded border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              {Array.from({ length: totalPages }, (_, i) => i + 1)
                .filter(page => page === 1 || page === totalPages || Math.abs(page - currentPage) <= 1)
                .reduce((acc, page, idx, arr) => {
                  if (idx > 0 && page - arr[idx - 1] > 1) acc.push('...');
                  acc.push(page);
                  return acc;
                }, [])
                .map((item, idx) =>
                  item === '...' ? (
                    <span key={`ellipsis-${idx}`} className="px-2 py-1 text-sm text-gray-400">...</span>
                  ) : (
                    <button
                      key={item}
                      onClick={() => setCurrentPage(item)}
                      className={`px-3 py-1 text-sm rounded border ${
                        currentPage === item
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      {item}
                    </button>
                  )
                )}
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="p-1 rounded border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
              <button
                onClick={() => setCurrentPage(totalPages)}
                disabled={currentPage === totalPages}
                className="px-2 py-1 text-sm rounded border border-gray-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Last
              </button>
            </div>
          )}
        </div>
      )}

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-8 h-8 bg-blue-500 rounded-md flex items-center justify-center">
                  <span className="text-white font-bold text-sm">T</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Total Scans</dt>
                  <dd className="text-lg font-medium text-gray-900">{scans.length}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-8 h-8 bg-green-500 rounded-md flex items-center justify-center">
                  <span className="text-white font-bold text-sm">N</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Network Scans</dt>
                  <dd className="text-lg font-medium text-gray-900">
                    {scans.filter(scan => scan.type === 'network').length}
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-8 h-8 bg-purple-500 rounded-md flex items-center justify-center">
                  <span className="text-white font-bold text-sm">W</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Web Scans</dt>
                  <dd className="text-lg font-medium text-gray-900">
                    {scans.filter(scan => scan.type === 'web').length}
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-8 h-8 bg-orange-500 rounded-md flex items-center justify-center">
                  <span className="text-white font-bold text-sm">M</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Multi Scans</dt>
                  <dd className="text-lg font-medium text-gray-900">
                    {scans.filter(scan => scan.type === 'multi').length}
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* New Scan Modal - Portal to body to escape containing block from animate-fade-in */}
      {isModalOpen && createPortal(
        <div className="fixed inset-0 bg-slate-900/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <NewScanModal onClose={handleModalClose} />
        </div>,
        document.body
      )}

      {/* Scan Detail Modal */}
      {selectedScanId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="w-full max-w-5xl max-h-[90vh] overflow-y-auto m-4">
            <div className="flex justify-end mb-2">
              <button
                onClick={() => setSelectedScanId(null)}
                className="p-2 bg-white rounded-full shadow hover:bg-gray-100"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <HostDetails scanId={selectedScanId} />
          </div>
        </div>
      )}
    </div>
  );
};

export default Scans;