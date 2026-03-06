'use client'

import React, { useState, useEffect } from 'react';
import { X, Shield, AlertTriangle, ExternalLink, Server, Bug } from 'lucide-react';
import { useAuth } from '@/lib/auth';

const SeverityBadge = ({ cvss }) => {
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
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${color}`}>
      {label} ({cvss})
    </span>
  );
};

const ExploitBadge = ({ isExploit }) => {
  if (!isExploit) return null;
  return (
    <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-600 text-white ml-2">
      EXPLOIT
    </span>
  );
};

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
      <div className="flex items-center justify-between">
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
        <div className="flex items-center">
          <SeverityBadge cvss={vuln.cvss} />
          <ExploitBadge isExploit={vuln.is_exploit} />
        </div>
      </div>
      <div className="mt-1 text-xs text-gray-500">
        Source: {vuln.type}
      </div>
    </div>
  );
};

const PortCard = ({ port }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const vulnCount = port.vulnerabilities?.length || 0;
  const exploitCount = port.vulnerabilities?.filter(v => v.is_exploit).length || 0;
  const cveCount = port.vulnerabilities?.filter(v => v.id.startsWith('CVE-')).length || 0;

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div
        className={`p-4 cursor-pointer hover:bg-gray-50 ${vulnCount > 0 ? 'bg-yellow-50' : 'bg-white'}`}
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Server className="w-5 h-5 text-gray-500" />
            <div>
              <div className="font-medium text-gray-900">
                Port {port['@portid']}/{port['@protocol']}
              </div>
              <div className="text-sm text-gray-500">
                {port['@service']} {port['@product'] && `- ${port['@product']}`} {port['@version'] && `(${port['@version']})`}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {vulnCount > 0 && (
              <>
                <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">
                  {vulnCount} vulns
                </span>
                {cveCount > 0 && (
                  <span className="px-2 py-1 bg-orange-100 text-orange-800 text-xs rounded-full">
                    {cveCount} CVEs
                  </span>
                )}
                {exploitCount > 0 && (
                  <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
                    {exploitCount} exploits
                  </span>
                )}
              </>
            )}
            <span className={`px-2 py-1 text-xs rounded-full ${port['@state'] === 'open' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
              {port['@state']}
            </span>
          </div>
        </div>
      </div>

      {isExpanded && vulnCount > 0 && (
        <div className="border-t border-gray-200 p-4 bg-gray-50">
          <h4 className="text-sm font-medium text-gray-700 mb-3">Vulnerabilities</h4>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {port.vulnerabilities
              .sort((a, b) => parseFloat(b.cvss) - parseFloat(a.cvss))
              .map((vuln, idx) => (
                <VulnerabilityCard key={idx} vuln={vuln} />
              ))}
          </div>
        </div>
      )}
    </div>
  );
};

const ScanDetail = ({ scanId, onClose }) => {
  const [scan, setScan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { authFetch, API_URL } = useAuth();

  useEffect(() => {
    const fetchScan = async () => {
      try {
        const response = await authFetch(`${API_URL}/scans/${scanId}`);
        if (!response.ok) throw new Error('Failed to fetch scan details');
        const data = await response.json();
        setScan(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    if (scanId) {
      fetchScan();
    }
  }, [scanId]);

  if (loading) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-8">
          <div className="text-gray-600">Loading scan details...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg p-8">
          <div className="text-red-600">Error: {error}</div>
          <button onClick={onClose} className="mt-4 px-4 py-2 bg-gray-200 rounded">Close</button>
        </div>
      </div>
    );
  }

  const ports = scan?.ndata || [];
  const totalVulns = ports.reduce((acc, p) => acc + (p.vulnerabilities?.length || 0), 0);
  const totalExploits = ports.reduce((acc, p) => acc + (p.vulnerabilities?.filter(v => v.is_exploit).length || 0), 0);
  const totalCVEs = ports.reduce((acc, p) => acc + (p.vulnerabilities?.filter(v => v.id?.startsWith('CVE-')).length || 0), 0);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">{scan?.scan_name || scan?.name || 'Scan Details'}</h2>
            <p className="text-sm text-gray-500">Target: {scan?.target || 'N/A'}</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Summary */}
        <div className="p-4 bg-white border-b border-gray-200">
          <div className="grid grid-cols-4 gap-4">
            <div className="text-center p-3 bg-blue-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-600">{ports.length}</div>
              <div className="text-sm text-gray-600">Open Ports</div>
            </div>
            <div className="text-center p-3 bg-yellow-50 rounded-lg">
              <div className="text-2xl font-bold text-yellow-600">{totalVulns}</div>
              <div className="text-sm text-gray-600">Vulnerabilities</div>
            </div>
            <div className="text-center p-3 bg-orange-50 rounded-lg">
              <div className="text-2xl font-bold text-orange-600">{totalCVEs}</div>
              <div className="text-sm text-gray-600">CVEs</div>
            </div>
            <div className="text-center p-3 bg-red-50 rounded-lg">
              <div className="text-2xl font-bold text-red-600">{totalExploits}</div>
              <div className="text-sm text-gray-600">Exploits</div>
            </div>
          </div>
        </div>

        {/* Ports List */}
        <div className="p-4 overflow-y-auto max-h-[60vh]">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            <AlertTriangle className="w-5 h-5 inline mr-2 text-yellow-500" />
            Network Scan Results
          </h3>

          {ports.length > 0 ? (
            <div className="space-y-3">
              {ports
                .sort((a, b) => (b.vulnerabilities?.length || 0) - (a.vulnerabilities?.length || 0))
                .map((port, idx) => (
                  <PortCard key={idx} port={port} />
                ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              No port data available for this scan.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ScanDetail;
