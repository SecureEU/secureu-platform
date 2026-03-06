'use client'

import React, { useState, useMemo } from 'react';
import { Server, Lock, Shield, Eye, Filter, Download, Bug, ExternalLink, ChevronDown, ChevronRight, AlertTriangle } from 'lucide-react';

// Vulnerability Badge Component
const VulnSeverityBadge = ({ cvss }) => {
  const score = parseFloat(cvss) || 0;
  let color = 'bg-gray-100 text-gray-800';
  let label = 'Info';

  if (score >= 9.0) {
    color = 'bg-red-100 text-red-800';
    label = 'Critical';
  } else if (score >= 7.0) {
    color = 'bg-orange-100 text-orange-800';
    label = 'High';
  } else if (score >= 4.0) {
    color = 'bg-yellow-100 text-yellow-800';
    label = 'Medium';
  } else if (score > 0) {
    color = 'bg-blue-100 text-blue-800';
    label = 'Low';
  }

  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${color}`}>
      {label} ({cvss})
    </span>
  );
};

// Vulnerability Card Component
const VulnerabilityCard = ({ vuln }) => {
  const getVulnUrl = (id, type) => {
    if (id.startsWith('CVE-')) {
      return `https://nvd.nist.gov/vuln/detail/${id}`;
    }
    if (type === 'exploitdb') {
      return `https://www.exploit-db.com/exploits/${id.replace('EDB-ID:', '')}`;
    }
    return `https://vulners.com/${type}/${id}`;
  };

  return (
    <div className={`p-3 rounded-lg border ${vuln.is_exploit ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white'}`}>
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-2">
          {vuln.is_exploit ? (
            <Bug className="w-4 h-4 text-red-600" />
          ) : (
            <Shield className="w-4 h-4 text-gray-500" />
          )}
          <a
            href={getVulnUrl(vuln.id, vuln.type)}
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm font-medium text-blue-600 hover:underline flex items-center gap-1"
          >
            {vuln.id}
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
        <div className="flex items-center gap-2">
          <VulnSeverityBadge cvss={vuln.cvss} />
          {vuln.is_exploit && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-600 text-white">
              EXPLOIT
            </span>
          )}
        </div>
      </div>
      <div className="mt-1 text-xs text-gray-500">
        Source: {vuln.type}
      </div>
    </div>
  );
};

// Expandable Port Row Component
const PortRow = ({ port, getStateColor, getStateBadgeColor, getServiceIcon }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const vulnCount = port.vulnerabilities?.length || 0;
  const exploitCount = port.vulnerabilities?.filter(v => v.is_exploit).length || 0;
  const cveCount = port.vulnerabilities?.filter(v => v.id?.startsWith('CVE-')).length || 0;
  const hasVulns = vulnCount > 0;

  return (
    <>
      <tr
        className={`hover:bg-gray-50 transition-colors ${hasVulns ? 'cursor-pointer' : ''} ${isExpanded ? 'bg-yellow-50' : ''}`}
        onClick={() => hasVulns && setIsExpanded(!isExpanded)}
      >
        <td className="px-6 py-4 whitespace-nowrap">
          <div className="flex items-center gap-2">
            {hasVulns && (
              isExpanded ?
                <ChevronDown className="w-4 h-4 text-gray-500" /> :
                <ChevronRight className="w-4 h-4 text-gray-500" />
            )}
            <span className="text-lg font-mono font-bold text-slate-900">
              {port['@portid']}
            </span>
          </div>
        </td>
        <td className="px-6 py-4 whitespace-nowrap">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800 uppercase">
            {port['@protocol'] || 'unknown'}
          </span>
        </td>
        <td className="px-6 py-4 whitespace-nowrap">
          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${getStateBadgeColor(port['@state'])}`}>
            <span className={`w-2 h-2 rounded-full ${getStateColor(port['@state'])}`} style={{backgroundColor: 'currentColor'}}></span>
            {port['@state']?.toUpperCase() || 'UNKNOWN'}
          </span>
        </td>
        <td className="px-6 py-4 whitespace-nowrap">
          <div className="flex items-center gap-2">
            <span className="text-lg">{getServiceIcon(port['@service'])}</span>
            <div>
              <span className="text-sm font-medium text-gray-900">
                {port['@service'] || 'unknown'}
              </span>
              {(port['@product'] || port['@version']) && (
                <div className="text-xs text-gray-500">
                  {port['@product']} {port['@version'] && `(${port['@version']})`}
                </div>
              )}
            </div>
          </div>
        </td>
        <td className="px-6 py-4 whitespace-nowrap">
          {hasVulns ? (
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full font-medium">
                {vulnCount} vulns
              </span>
              {cveCount > 0 && (
                <span className="px-2 py-1 bg-orange-100 text-orange-800 text-xs rounded-full font-medium">
                  {cveCount} CVEs
                </span>
              )}
              {exploitCount > 0 && (
                <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full font-medium">
                  {exploitCount} exploits
                </span>
              )}
            </div>
          ) : (
            <span className="text-gray-400 text-xs">No vulnerabilities</span>
          )}
        </td>
      </tr>
      {isExpanded && hasVulns && (
        <tr>
          <td colSpan="5" className="px-6 py-4 bg-gray-50 border-t border-b border-gray-200">
            <div className="space-y-3">
              <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-yellow-500" />
                Vulnerabilities for Port {port['@portid']}
              </h4>
              <div className="grid gap-2 max-h-80 overflow-y-auto">
                {port.vulnerabilities
                  .sort((a, b) => parseFloat(b.cvss) - parseFloat(a.cvss))
                  .map((vuln, idx) => (
                    <VulnerabilityCard key={idx} vuln={vuln} />
                  ))}
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
};

// Host Card Component for IP range scans
const HostCard = ({ host, isSelected, onClick }) => {
  const openPorts = host.ports?.filter(p => p['@state'] === 'open').length || 0;
  const totalVulns = host.ports?.reduce((acc, p) => acc + (p.vulnerabilities?.length || 0), 0) || 0;

  return (
    <div
      onClick={onClick}
      className={`p-4 rounded-lg border cursor-pointer transition-all ${
        isSelected
          ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200'
          : 'border-gray-200 bg-white hover:border-blue-300 hover:bg-gray-50'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className={`w-3 h-3 rounded-full ${host.status === 'up' ? 'bg-green-500' : 'bg-gray-400'}`} />
          <div>
            <div className="font-mono font-bold text-gray-900">{host.ip}</div>
            {host.hostname && (
              <div className="text-xs text-gray-500">{host.hostname}</div>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className="px-2 py-1 bg-emerald-100 text-emerald-700 text-xs rounded-full font-medium">
            {openPorts} ports
          </span>
          {totalVulns > 0 && (
            <span className="px-2 py-1 bg-yellow-100 text-yellow-700 text-xs rounded-full font-medium">
              {totalVulns} vulns
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

const NmapResults = ({ data }) => {
  const [filterState, setFilterState] = useState('all');
  const [sortBy, setSortBy] = useState('vulnerabilities');
  const [sortOrder, setSortOrder] = useState('desc');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedHostIndex, setSelectedHostIndex] = useState(0);

  // Check for hosts array (IP range scan) or fall back to ndata (single host)
  const hosts = data?.hosts || [];
  const hasMultipleHosts = hosts.length > 1;

  // Get ports from selected host or from ndata for backward compatibility
  const getPortsData = () => {
    if (hosts.length > 0) {
      return hosts[selectedHostIndex]?.ports || [];
    }
    return data?.ndata || [];
  };

  if (!data?.ndata && hosts.length === 0) {
    return (
      <div className="p-6">
        <div className="text-center py-12">
          <Server className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No Port Data Available</h3>
          <p className="mt-1 text-sm text-gray-500">
            Port scan data is not available for this scan.
          </p>
        </div>
      </div>
    );
  }

  const getStateColor = (state) => {
    switch (state?.toLowerCase()) {
      case 'open': return 'text-emerald-500';
      case 'filtered': return 'text-amber-500';
      case 'closed': return 'text-red-500';
      case 'unfiltered': return 'text-blue-500';
      default: return 'text-slate-500';
    }
  };

  const getStateBadgeColor = (state) => {
    switch (state?.toLowerCase()) {
      case 'open': return 'bg-emerald-100 text-emerald-800';
      case 'filtered': return 'bg-amber-100 text-amber-800';
      case 'closed': return 'bg-red-100 text-red-800';
      case 'unfiltered': return 'bg-blue-100 text-blue-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  };

  const getServiceIcon = (service) => {
    const serviceIcons = {
      'http': '🌐',
      'https': '🔒',
      'ssh': '🔑',
      'ftp': '📁',
      'smtp': '📧',
      'dns': '🌍',
      'mysql': '🗄️',
      'postgresql': '🗃️',
      'redis': '⚡',
      'mongodb': '📊',
      'ldap': '👥',
      'snmp': '📡',
      'telnet': '💻',
      'pop3': '📨',
      'imap': '📬',
      'ntp': '⏰',
      'samba': '🗂️',
      'vnc': '🖥️'
    };
    return serviceIcons[service?.toLowerCase()] || '⚙️';
  };

  const ports = getPortsData();

  // Statistics including vulnerabilities
  const stats = useMemo(() => {
    const openPorts = ports.filter(p => p['@state']?.toLowerCase() === 'open');
    const closedPorts = ports.filter(p => p['@state']?.toLowerCase() === 'closed');
    const filteredPorts = ports.filter(p => p['@state']?.toLowerCase() === 'filtered');

    // Vulnerability stats
    let totalVulns = 0;
    let totalCVEs = 0;
    let totalExploits = 0;
    let criticalVulns = 0;
    let highVulns = 0;

    ports.forEach(port => {
      const vulns = port.vulnerabilities || [];
      totalVulns += vulns.length;
      vulns.forEach(v => {
        if (v.id?.startsWith('CVE-')) totalCVEs++;
        if (v.is_exploit) totalExploits++;
        const cvss = parseFloat(v.cvss) || 0;
        if (cvss >= 9.0) criticalVulns++;
        else if (cvss >= 7.0) highVulns++;
      });
    });

    const services = {};
    openPorts.forEach(port => {
      const service = port['@service'] || 'unknown';
      services[service] = (services[service] || 0) + 1;
    });

    return {
      total: ports.length,
      open: openPorts.length,
      closed: closedPorts.length,
      filtered: filteredPorts.length,
      services: Object.keys(services).length,
      topServices: Object.entries(services)
        .sort(([,a], [,b]) => b - a)
        .slice(0, 5),
      totalVulns,
      totalCVEs,
      totalExploits,
      criticalVulns,
      highVulns
    };
  }, [ports]);

  // Filtered and sorted ports
  const processedPorts = useMemo(() => {
    let filtered = [...ports];

    if (filterState !== 'all') {
      filtered = filtered.filter(port =>
        port['@state']?.toLowerCase() === filterState
      );
    }

    if (searchTerm) {
      filtered = filtered.filter(port =>
        port['@portid']?.toString().includes(searchTerm) ||
        port['@service']?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        port['@protocol']?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        port.vulnerabilities?.some(v => v.id?.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    filtered.sort((a, b) => {
      let aVal, bVal;

      switch (sortBy) {
        case 'port':
          aVal = parseInt(a['@portid']) || 0;
          bVal = parseInt(b['@portid']) || 0;
          break;
        case 'vulnerabilities':
          aVal = a.vulnerabilities?.length || 0;
          bVal = b.vulnerabilities?.length || 0;
          break;
        case 'service':
          aVal = a['@service'] || '';
          bVal = b['@service'] || '';
          break;
        case 'state':
          aVal = a['@state'] || '';
          bVal = b['@state'] || '';
          break;
        default:
          return 0;
      }

      if (typeof aVal === 'string') {
        return sortOrder === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      return sortOrder === 'asc' ? aVal - bVal : bVal - aVal;
    });

    return filtered;
  }, [ports, filterState, searchTerm, sortBy, sortOrder]);

  const handleSort = (column) => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder(column === 'vulnerabilities' ? 'desc' : 'asc');
    }
  };

  const exportToCSV = () => {
    const csvContent = [
      ['Port', 'Protocol', 'State', 'Service', 'Product', 'Version', 'Vulnerabilities', 'CVEs', 'Exploits'],
      ...processedPorts.map(port => [
        port['@portid'],
        port['@protocol'],
        port['@state'],
        port['@service'] || 'unknown',
        port['@product'] || '',
        port['@version'] || '',
        port.vulnerabilities?.length || 0,
        port.vulnerabilities?.filter(v => v.id?.startsWith('CVE-')).length || 0,
        port.vulnerabilities?.filter(v => v.is_exploit).length || 0
      ])
    ].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `nmap-results-${Date.now()}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Host Selector for IP Range Scans */}
      {hasMultipleHosts && (
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
              <Server className="h-5 w-5" />
              Scanned Hosts ({hosts.length})
            </h3>
            <span className="text-sm text-gray-500">
              Click a host to view its details
            </span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
            {hosts.map((host, index) => (
              <HostCard
                key={host.ip || index}
                host={host}
                isSelected={selectedHostIndex === index}
                onClick={() => setSelectedHostIndex(index)}
              />
            ))}
          </div>
        </div>
      )}

      {/* Selected Host Info */}
      {hasMultipleHosts && hosts[selectedHostIndex] && (
        <div className="bg-blue-50 rounded-lg border border-blue-200 p-4">
          <div className="flex items-center gap-3">
            <div className="w-4 h-4 rounded-full bg-blue-500" />
            <div>
              <span className="font-semibold text-blue-900">Currently viewing: </span>
              <span className="font-mono font-bold text-blue-700">{hosts[selectedHostIndex].ip}</span>
              {hosts[selectedHostIndex].hostname && (
                <span className="text-blue-600 ml-2">({hosts[selectedHostIndex].hostname})</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Statistics Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <div className="bg-gradient-to-r from-emerald-50 to-emerald-100 rounded-lg p-4 border border-emerald-200">
          <div className="flex items-center gap-2 text-sm font-medium text-emerald-700">
            <Lock className="h-4 w-4" />
            Open Ports
          </div>
          <div className="mt-1 text-2xl font-bold text-emerald-900">
            {stats.open}
          </div>
        </div>

        <div className="bg-gradient-to-r from-blue-50 to-blue-100 rounded-lg p-4 border border-blue-200">
          <div className="flex items-center gap-2 text-sm font-medium text-blue-700">
            <Server className="h-4 w-4" />
            Services
          </div>
          <div className="mt-1 text-2xl font-bold text-blue-900">
            {stats.services}
          </div>
        </div>

        <div className="bg-gradient-to-r from-yellow-50 to-yellow-100 rounded-lg p-4 border border-yellow-200">
          <div className="flex items-center gap-2 text-sm font-medium text-yellow-700">
            <AlertTriangle className="h-4 w-4" />
            Vulnerabilities
          </div>
          <div className="mt-1 text-2xl font-bold text-yellow-900">
            {stats.totalVulns}
          </div>
        </div>

        <div className="bg-gradient-to-r from-orange-50 to-orange-100 rounded-lg p-4 border border-orange-200">
          <div className="flex items-center gap-2 text-sm font-medium text-orange-700">
            <Shield className="h-4 w-4" />
            CVEs
          </div>
          <div className="mt-1 text-2xl font-bold text-orange-900">
            {stats.totalCVEs}
          </div>
        </div>

        <div className="bg-gradient-to-r from-red-50 to-red-100 rounded-lg p-4 border border-red-200">
          <div className="flex items-center gap-2 text-sm font-medium text-red-700">
            <Bug className="h-4 w-4" />
            Exploits
          </div>
          <div className="mt-1 text-2xl font-bold text-red-900">
            {stats.totalExploits}
          </div>
        </div>

        <div className="bg-gradient-to-r from-purple-50 to-purple-100 rounded-lg p-4 border border-purple-200">
          <div className="flex items-center gap-2 text-sm font-medium text-purple-700">
            <AlertTriangle className="h-4 w-4" />
            Critical/High
          </div>
          <div className="mt-1 text-2xl font-bold text-purple-900">
            {stats.criticalVulns + stats.highVulns}
          </div>
        </div>
      </div>

      {/* Top Services */}
      {stats.topServices.length > 0 && (
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <h3 className="text-lg font-semibold text-slate-900 mb-3">Top Services</h3>
          <div className="flex flex-wrap gap-2">
            {stats.topServices.map(([service, count]) => (
              <div
                key={service}
                className="flex items-center gap-2 bg-slate-100 rounded-full px-3 py-1 text-sm"
              >
                <span className="text-lg">{getServiceIcon(service)}</span>
                <span className="font-medium text-slate-900">{service}</span>
                <span className="bg-slate-200 text-slate-700 rounded-full px-2 py-0.5 text-xs font-medium">
                  {count}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Controls */}
      <div className="bg-white rounded-lg border border-slate-200 p-4">
        <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
            <div className="relative">
              <input
                type="text"
                placeholder="Search ports, services, CVEs..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-64"
              />
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Eye className="h-4 w-4 text-gray-400" />
              </div>
            </div>

            <select
              value={filterState}
              onChange={(e) => setFilterState(e.target.value)}
              className="border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">All States ({stats.total})</option>
              <option value="open">Open ({stats.open})</option>
              <option value="closed">Closed ({stats.closed})</option>
              <option value="filtered">Filtered ({stats.filtered})</option>
            </select>
          </div>

          <button
            onClick={exportToCSV}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          >
            <Download className="h-4 w-4" />
            Export CSV
          </button>
        </div>
      </div>

      {/* Results Table */}
      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 bg-slate-50">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              <Server className="h-5 w-5" />
              Port Scan Results
              {stats.totalVulns > 0 && (
                <span className="ml-2 px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full font-medium">
                  {stats.totalVulns} vulnerabilities found
                </span>
              )}
            </h3>
            <div className="text-sm text-slate-600">
              Showing {processedPorts.length} of {stats.total} ports
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                  onClick={() => handleSort('port')}
                >
                  <div className="flex items-center gap-1">
                    Port
                    {sortBy === 'port' && (
                      <span className="text-blue-500">{sortOrder === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </div>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Protocol
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                  onClick={() => handleSort('state')}
                >
                  <div className="flex items-center gap-1">
                    State
                    {sortBy === 'state' && (
                      <span className="text-blue-500">{sortOrder === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                  onClick={() => handleSort('service')}
                >
                  <div className="flex items-center gap-1">
                    Service
                    {sortBy === 'service' && (
                      <span className="text-blue-500">{sortOrder === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-colors"
                  onClick={() => handleSort('vulnerabilities')}
                >
                  <div className="flex items-center gap-1">
                    Vulnerabilities
                    {sortBy === 'vulnerabilities' && (
                      <span className="text-blue-500">{sortOrder === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {processedPorts.length > 0 ? (
                processedPorts.map((port, index) => (
                  <PortRow
                    key={`${port['@portid']}-${port['@protocol']}-${index}`}
                    port={port}
                    getStateColor={getStateColor}
                    getStateBadgeColor={getStateBadgeColor}
                    getServiceIcon={getServiceIcon}
                  />
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center">
                    <div className="text-gray-500">
                      {searchTerm || filterState !== 'all'
                        ? 'No ports match your current filters.'
                        : 'No port data available.'
                      }
                    </div>
                    {(searchTerm || filterState !== 'all') && (
                      <button
                        onClick={() => {
                          setSearchTerm('');
                          setFilterState('all');
                        }}
                        className="mt-2 text-blue-600 hover:text-blue-800 text-sm underline"
                      >
                        Clear filters
                      </button>
                    )}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Summary Footer */}
      <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
        <div className="flex flex-wrap items-center gap-4 text-sm text-slate-600">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-emerald-500 rounded-full"></div>
            <span>Open: {stats.open}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-red-500 rounded-full"></div>
            <span>Closed: {stats.closed}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-amber-500 rounded-full"></div>
            <span>Filtered: {stats.filtered}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
            <span>Vulnerabilities: {stats.totalVulns}</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
            <span>CVEs: {stats.totalCVEs}</span>
          </div>
          <div className="ml-auto text-slate-700 font-medium">
            Total: {stats.total} ports scanned
          </div>
        </div>
      </div>
    </div>
  );
};

export default NmapResults;
