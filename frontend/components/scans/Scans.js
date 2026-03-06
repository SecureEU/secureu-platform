'use client'

import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Trash2, AlertCircle, Play, RefreshCw, Eye } from 'lucide-react';
import NewScanModal from './NewScanModal';
import ScanDetail from './ScanDetail';
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
  const [rowsPerPage] = useState(10);
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
    if (!scan.zdata?.site?.[0]?.alerts) return { high: 0, medium: 0, low: 0, info: 0 };
    
    return scan.zdata.site[0].alerts.reduce((acc, alert) => {
      const riskLevel = alert.riskdesc.toLowerCase().split(' ')[0];
      acc[riskLevel] = (acc[riskLevel] || 0) + 1;
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
      {filteredScans.length > rowsPerPage && (
        <div className="flex justify-between items-center bg-white px-6 py-3 border border-gray-200 rounded-lg">
          <div className="flex-1 flex justify-between sm:hidden">
            <button
              onClick={() => setCurrentPage(page => Math.max(1, page - 1))}
              disabled={currentPage === 1}
              className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => setCurrentPage(page => Math.min(totalPages, page + 1))}
              disabled={currentPage === totalPages}
              className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
          <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">{indexOfFirstScan + 1}</span> to{' '}
                <span className="font-medium">{Math.min(indexOfLastScan, filteredScans.length)}</span> of{' '}
                <span className="font-medium">{filteredScans.length}</span> results
              </p>
            </div>
            <div>
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <button
                  onClick={() => setCurrentPage(page => Math.max(1, page - 1))}
                  disabled={currentPage === 1}
                  className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  const pageNum = i + 1;
                  return (
                    <button
                      key={pageNum}
                      onClick={() => setCurrentPage(pageNum)}
                      className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                        currentPage === pageNum
                          ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                          : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                      }`}
                    >
                      {pageNum}
                    </button>
                  );
                })}
                <button
                  onClick={() => setCurrentPage(page => Math.min(totalPages, page + 1))}
                  disabled={currentPage === totalPages}
                  className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </nav>
            </div>
          </div>
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
        <ScanDetail
          scanId={selectedScanId}
          onClose={() => setSelectedScanId(null)}
        />
      )}
    </div>
  );
};

export default Scans;