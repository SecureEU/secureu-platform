'use client'

import React, { useState, useEffect } from 'react';
import {
  Lock,
  Shield,
  AlertTriangle,
  Server,
  Search,
  Globe,
  ChevronRight,
  BarChart2,
  Calendar,
  X,
  Send,
  Loader2,
  BrainCircuit
} from 'lucide-react';
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';

const API_URL = process.env.NEXT_PUBLIC_SSL_API_URL || '';
const HAS_BACKEND = !!process.env.NEXT_PUBLIC_SSL_API_URL;

// AI Explanation Modal Component
const AskAIExplanation = ({ host, isOpen, onClose }) => {
  const [question, setQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [conversation, setConversation] = useState([]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;

    const newConversation = [
      ...conversation,
      { role: 'user', content: question }
    ];
    setConversation(newConversation);
    setIsLoading(true);

    try {
      await new Promise(resolve => setTimeout(resolve, 1500));
      const aiResponse = generateAIResponse(host, question);
      setConversation([
        ...newConversation,
        { role: 'assistant', content: aiResponse }
      ]);
      setQuestion('');
    } catch (error) {
      console.error('Failed to get AI response:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const generateAIResponse = (host, question) => {
    const lowerQuestion = question.toLowerCase();

    if (lowerQuestion.includes('ssl') || lowerQuestion.includes('tls') || lowerQuestion.includes('version')) {
      return `The host ${host.host} is using ${host.sslVersion}. ${
        host.sslVersion?.includes('TLS 1.2') || host.sslVersion?.includes('TLS 1.3')
          ? 'This is a modern and secure protocol version that is recommended for secure communications.'
          : 'This version may have some security vulnerabilities and upgrading to TLS 1.2 or TLS 1.3 is recommended.'
      }`;
    }

    if (lowerQuestion.includes('certificate') || lowerQuestion.includes('trusted')) {
      return `The certificate for ${host.host} is ${
        host.trusted ? 'trusted and properly validated. This means it has been issued by a recognized Certificate Authority and browsers will trust this connection.'
        : 'not trusted. This could mean the certificate is self-signed, expired, or not issued by a recognized Certificate Authority. Users visiting this site may see security warnings.'
      }`;
    }

    if (lowerQuestion.includes('cipher') || lowerQuestion.includes('encryption')) {
      return `This host is using ${host.cipher} cipher. ${
        host.rc4Supported
          ? 'Unfortunately, RC4 cipher is supported which is considered weak and vulnerable to attacks. It is recommended to disable RC4 support.'
          : 'The configuration appears to be using modern ciphers which is good for security.'
      }`;
    }

    if (lowerQuestion.includes('vulnerable') || lowerQuestion.includes('secure') || lowerQuestion.includes('risk')) {
      if (!host.trusted || host.rc4Supported) {
        return `There are some security concerns with this host. ${
          !host.trusted ? 'The certificate is not trusted which may cause browser warnings. ' : ''
        }${
          host.rc4Supported ? 'The server supports RC4 ciphers which are vulnerable to cryptographic attacks. ' : ''
        }I recommend addressing these issues to improve the security posture.`;
      } else {
        return `Based on the scan results, ${host.host} appears to have a good security configuration. The certificate is trusted and no obvious vulnerabilities were detected in this basic scan.`;
      }
    }

    return `The scan for ${host.host} was performed on ${host.scanDate}. The server is running ${host.serverInfo} with ${host.sslVersion}. ${
      host.trusted ? 'The certificate is trusted by browsers.' : 'The certificate is not trusted by browsers, which may cause security warnings.'
    } ${
      host.rc4Supported ? 'The server supports RC4 ciphers which are considered insecure.' : 'The server uses secure cipher configurations.'
    }`;
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-lg w-full max-w-2xl flex flex-col h-[80vh]">
        <div className="border-b p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BrainCircuit className="w-5 h-5 text-blue-600" />
            <h2 className="text-lg font-semibold text-slate-900">SSL Security Assistant</h2>
          </div>
          <button onClick={onClose} className="text-slate-500 hover:text-slate-700">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {conversation.length === 0 && (
            <div className="bg-blue-50 p-4 rounded-lg">
              <p className="text-blue-800">
                Welcome! I'm your SSL security assistant. Ask me anything about the scan results for {host.host}.
              </p>
              <div className="mt-3 text-sm text-blue-600">
                <p className="font-medium">Example questions:</p>
                <ul className="list-disc pl-5 space-y-1 mt-1">
                  <li>Explain the SSL version being used</li>
                  <li>Is this certificate trusted?</li>
                  <li>What vulnerabilities were found?</li>
                  <li>Should I be concerned about the security of this site?</li>
                </ul>
              </div>
            </div>
          )}

          {conversation.map((message, index) => (
            <div
              key={index}
              className={`${
                message.role === 'user'
                  ? 'bg-blue-100 ml-12'
                  : 'bg-slate-100 mr-12'
              } p-3 rounded-lg`}
            >
              <p className={message.role === 'user' ? 'text-blue-800' : 'text-slate-800'}>
                {message.content}
              </p>
            </div>
          ))}

          {isLoading && (
            <div className="flex items-center justify-center py-4">
              <Loader2 className="w-5 h-5 text-blue-600 animate-spin" />
              <span className="ml-2 text-blue-600">Analyzing scan data...</span>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="border-t p-4">
          <div className="flex items-center gap-2">
            <input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              className="flex-1 border border-slate-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              placeholder="Ask about the SSL scan results..."
              disabled={isLoading}
            />
            <button
              type="submit"
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center gap-2 disabled:opacity-50"
              disabled={isLoading || !question.trim()}
            >
              {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              Send
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// Scanner Component
const Scanner = ({ onScan, loading }) => {
  const [domain, setDomain] = useState('');
  const [port, setPort] = useState('443');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (domain.trim()) {
      onScan(domain, port);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-xl p-6">
      <h3 className="text-lg font-semibold text-slate-900 mb-4">Scan New Domain</h3>
      <div className="flex gap-4">
        <div className="flex-1">
          <label className="block text-sm font-medium text-slate-700 mb-2">Domain Name</label>
          <input
            type="text"
            value={domain}
            onChange={(e) => setDomain(e.target.value)}
            className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="example.com"
            disabled={loading}
          />
        </div>
        <div className="w-32">
          <label className="block text-sm font-medium text-slate-700 mb-2">Port</label>
          <input
            type="text"
            value={port}
            onChange={(e) => setPort(e.target.value)}
            className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            disabled={loading}
          />
        </div>
        <div className="flex items-end">
          <button
            type="submit"
            disabled={loading || !domain.trim()}
            className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Search className="w-4 h-4" />
            )}
            Scan
          </button>
        </div>
      </div>
    </form>
  );
};

// Host Details Component
const HostDetails = ({ host, onBack }) => {
  const [isAIModalOpen, setIsAIModalOpen] = useState(false);
  const [details, setDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(true);

  useEffect(() => {
    const fetchDetails = async () => {
      if (!HAS_BACKEND || !host.id) {
        setDetailsLoading(false);
        return;
      }
      try {
        const response = await fetch(`${API_URL}/api/host`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'ngrok-skip-browser-warning': '1',
          },
          credentials: 'include',
          body: JSON.stringify({ id: String(host.id) })
        });
        if (response.ok) {
          const data = await response.json();
          setDetails(data);
        }
      } catch (err) {
        console.error('Failed to fetch host details:', err);
      } finally {
        setDetailsLoading(false);
      }
    };
    fetchDetails();
  }, [host.id]);

  const scan = details?.scan || host;
  const cert = details?.certificate || {};
  const proto = details?.protocols || {};
  const hdrs = details?.headers || {};
  const ai = details?.ai || {};
  const subjects = details?.subjects || [];

  // Parse protocols_json if available
  let parsedProtocols = null;
  try {
    if (proto.protocols_json) {
      parsedProtocols = typeof proto.protocols_json === 'string'
        ? JSON.parse(proto.protocols_json)
        : proto.protocols_json;
    }
  } catch { /* ignore parse errors */ }

  const vulnChecks = [
    { key: 'rc4', label: 'RC4 Cipher', severity: 'High' },
    { key: 'heartbleed', label: 'Heartbleed', severity: 'Critical' },
    { key: 'poodle', label: 'POODLE', severity: 'High' },
    { key: 'beast', label: 'BEAST', severity: 'Medium' },
    { key: 'crime', label: 'CRIME', severity: 'High' },
    { key: 'freak', label: 'FREAK', severity: 'High' },
    { key: 'logjam', label: 'Logjam', severity: 'High' },
    { key: 'sweet32', label: 'Sweet32', severity: 'Medium' },
    { key: 'insecure_ciphers', label: 'Insecure Ciphers', severity: 'Medium' },
    { key: 'weak_ciphers', label: 'Weak Ciphers', severity: 'Low' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <button
          onClick={onBack}
          className="text-blue-600 hover:text-blue-800 flex items-center gap-1"
        >
          <ChevronRight className="w-4 h-4 rotate-180" />
          Back to Host List
        </button>
        {details && (
          <button
            onClick={() => setIsAIModalOpen(true)}
            className="bg-purple-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-purple-700 transition-colors"
          >
            <BrainCircuit className="w-5 h-5" />
            Ask AI
          </button>
        )}
      </div>

      {detailsLoading ? (
        <div className="text-center py-12">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-slate-600">Loading host details...</p>
        </div>
      ) : (
        <>
          {/* Scan & Certificate Info */}
          <div className="bg-white border border-slate-200 rounded-xl p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-6">
              {scan.host}
              {subjects.length > 0 && (
                <span className="text-sm font-normal text-slate-500 ml-2">
                  ({subjects.map(s => s.value).join(', ')})
                </span>
              )}
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div>
                <h3 className="font-medium text-slate-900 mb-4">Server & Certificate</h3>
                <div className="space-y-3">
                  <div>
                    <p className="text-sm text-slate-500">Server</p>
                    <p className="font-medium text-slate-900">{scan.server || 'Unknown'}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">SSL/TLS Version</p>
                    <p className="font-medium text-slate-900">{cert.ssl_version || 'Unknown'}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Cipher Suite</p>
                    <p className="font-medium text-slate-900">{cert.cipher_suite || 'Unknown'}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Key</p>
                    <p className="font-medium text-slate-900">{cert.key_type || 'Unknown'} {cert.key_size ? `(${cert.key_size} bit)` : ''}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Issuer</p>
                    <p className="font-medium text-slate-900">{cert.issuers || 'Unknown'}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Port</p>
                    <p className="font-medium text-slate-900">{scan.port || '443'}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Scan Date</p>
                    <p className="font-medium text-slate-900">{scan.date || 'Unknown'}</p>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="font-medium text-slate-900 mb-4">Certificate Status</h3>
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Shield className={`w-5 h-5 ${!scan.vulnerable ? "text-green-600" : "text-red-600"}`} />
                    <span className={!scan.vulnerable ? "text-green-600" : "text-red-600"}>
                      {!scan.vulnerable ? "Not Vulnerable" : "Vulnerable"}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <AlertTriangle className={`w-5 h-5 ${scan.has_issues ? "text-amber-600" : "text-green-600"}`} />
                    <span className={scan.has_issues ? "text-amber-600" : "text-green-600"}>
                      {scan.has_issues ? "Issues Detected" : "No Issues"}
                    </span>
                  </div>
                  {cert.expire_date && (
                    <div>
                      <p className="text-sm text-slate-500">Certificate Expires</p>
                      <p className={`font-medium ${cert.has_expired ? 'text-red-600' : 'text-slate-900'}`}>
                        {cert.expire_date} ({cert.expires_in} days {cert.has_expired ? '- EXPIRED' : 'remaining'})
                      </p>
                    </div>
                  )}
                  {cert.serial_number && (
                    <div>
                      <p className="text-sm text-slate-500">Serial Number</p>
                      <p className="font-medium text-slate-900 text-xs break-all">{cert.serial_number}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Vulnerability Checks */}
          {proto.success !== undefined && (
            <div className="bg-white border border-slate-200 rounded-xl p-6">
              <h3 className="font-medium text-slate-900 mb-4">Vulnerability Assessment</h3>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
                {vulnChecks.map(({ key, label, severity }) => {
                  const affected = proto[key];
                  if (affected === undefined) return null;
                  return (
                    <div key={key} className={`p-3 rounded-lg border ${
                      affected ? 'border-red-200 bg-red-50' : 'border-green-200 bg-green-50'
                    }`}>
                      <p className={`text-sm font-medium ${affected ? 'text-red-700' : 'text-green-700'}`}>{label}</p>
                      <p className={`text-xs ${affected ? 'text-red-500' : 'text-green-500'}`}>
                        {affected ? `Affected (${severity})` : 'Safe'}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Security Headers */}
          {hdrs.success !== undefined && (
            <div className="bg-white border border-slate-200 rounded-xl p-6">
              <h3 className="font-medium text-slate-900 mb-4">Security Headers</h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                {[
                  { key: 'strict_transport_security', label: 'Strict-Transport-Security' },
                  { key: 'content_security_policy', label: 'Content-Security-Policy' },
                  { key: 'x_frame_options', label: 'X-Frame-Options' },
                  { key: 'x_content_type_options', label: 'X-Content-Type-Options' },
                  { key: 'x_xss_protection', label: 'X-XSS-Protection' },
                  { key: 'referrer_policy', label: 'Referrer-Policy' },
                  { key: 'permissions_policy', label: 'Permissions-Policy' },
                  { key: 'cross_origin_opener_policy', label: 'COOP' },
                  { key: 'cross_origin_embedder_policy', label: 'COEP' },
                  { key: 'cross_origin_resource_policy', label: 'CORP' },
                ].map(({ key, label }) => {
                  const present = hdrs[key];
                  if (present === undefined) return null;
                  return (
                    <div key={key} className={`px-3 py-2 rounded text-sm ${
                      present ? 'bg-green-50 text-green-700' : 'bg-slate-50 text-slate-500'
                    }`}>
                      {present ? '✓' : '✗'} {label}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Protocols & Ciphers */}
          {parsedProtocols && Object.keys(parsedProtocols).length > 0 && (
            <div className="bg-white border border-slate-200 rounded-xl p-6">
              <h3 className="font-medium text-slate-900 mb-4">Supported Protocols and Ciphers</h3>
              {Object.entries(parsedProtocols).map(([protocol, ciphers]) => (
                <div key={protocol} className="mb-4">
                  <h4 className="font-medium text-slate-700 mb-2">{protocol}</h4>
                  <div className="bg-slate-50 p-3 rounded-lg space-y-1">
                    {Array.isArray(ciphers) && ciphers.map((cipher, index) => (
                      <div key={index} className="flex items-center justify-between text-sm">
                        <span className="text-slate-700">{cipher.name || cipher}</span>
                        {cipher.security && (
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                            cipher.security === 'recommended' ? 'bg-green-100 text-green-700' :
                            cipher.security === 'secure' ? 'bg-blue-100 text-blue-700' :
                            cipher.security === 'weak' ? 'bg-amber-100 text-amber-700' :
                            'bg-slate-100 text-slate-600'
                          }`}>
                            {cipher.security}
                          </span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* AI Analysis */}
          {ai.analysis && ai.success === 1 && !ai.analysis.includes('No AI analysis') && (
            <div className="bg-white border border-slate-200 rounded-xl p-6">
              <h3 className="font-medium text-slate-900 mb-4 flex items-center gap-2">
                <BrainCircuit className="w-5 h-5 text-purple-600" />
                AI Analysis
              </h3>
              <p className="text-slate-700 whitespace-pre-wrap">{ai.analysis}</p>
            </div>
          )}
        </>
      )}

      <AskAIExplanation
        host={scan}
        isOpen={isAIModalOpen}
        onClose={() => setIsAIModalOpen(false)}
      />
    </div>
  );
};

// Host List Component
const HostList = ({ hosts, onSelectHost }) => (
  <div className="bg-white border border-slate-200 rounded-xl p-6">
    <h2 className="text-lg font-semibold text-slate-900 mb-6">Scanned Hosts</h2>
    {hosts.length === 0 ? (
      <div className="text-center py-8 text-slate-500">
        No hosts scanned yet. Use the scanner above to scan a domain.
      </div>
    ) : (
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-200">
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Host</th>
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Port</th>
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Server</th>
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Scan Date</th>
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Status</th>
              <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Action</th>
            </tr>
          </thead>
          <tbody>
            {hosts.map((host, index) => (
              <tr key={host.id || index} className="border-b border-slate-100 hover:bg-slate-50">
                <td className="py-3 px-4 font-medium text-slate-900">{host.host}</td>
                <td className="py-3 px-4 text-slate-600">{host.port}</td>
                <td className="py-3 px-4 text-slate-600">{host.server || 'Unknown'}</td>
                <td className="py-3 px-4 text-slate-500">{host.date}</td>
                <td className="py-3 px-4">
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    !host.vulnerable ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {!host.vulnerable ? 'Secure' : 'Vulnerable'}
                  </span>
                  {host.has_issues ? (
                    <span className="ml-1 px-2 py-1 rounded text-xs font-medium bg-amber-100 text-amber-800">
                      Issues
                    </span>
                  ) : null}
                </td>
                <td className="py-3 px-4">
                  <button
                    onClick={() => onSelectHost(host)}
                    className="text-blue-600 hover:text-blue-800 flex items-center gap-1 text-sm"
                  >
                    View Details
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

// Analytics Dashboard Component
const AnalyticsDashboard = ({ hosts }) => {
  if (!hosts || hosts.length === 0) {
    return (
      <div className="bg-white border border-slate-200 rounded-xl p-8 text-center">
        <Globe className="w-12 h-12 text-slate-300 mx-auto mb-4" />
        <p className="text-slate-500">No scan data available. Please scan some hosts first.</p>
      </div>
    );
  }

  const totalScans = hosts.length;
  const vulnerableHosts = hosts.filter(host => host.vulnerable).length;
  const secureHosts = hosts.filter(host => !host.vulnerable).length;

  // SSL Version Distribution
  const sslVersions = {};
  hosts.forEach(host => {
    const version = host.ssl_version || host.sslVersion || 'Unknown';
    sslVersions[version] = (sslVersions[version] || 0) + 1;
  });
  const sslVersionData = Object.keys(sslVersions).map(version => ({
    name: version,
    value: sslVersions[version]
  }));

  // Server Type Distribution
  const serverTypes = {};
  hosts.forEach(host => {
    const server = host.server || host.serverInfo || 'Unknown';
    serverTypes[server] = (serverTypes[server] || 0) + 1;
  });
  const serverTypeData = Object.keys(serverTypes).map(server => ({
    name: server,
    value: serverTypes[server]
  }));

  const securityStatusData = [
    { name: 'Secure', value: secureHosts },
    { name: 'Vulnerable', value: vulnerableHosts }
  ];

  const CHART_COLORS = {
    primary: '#3B82F6',
    secondary: '#10B981',
    accent: '#8B5CF6',
    warning: '#F59E0B',
    danger: '#EF4444',
    info: '#06B6D4',
    secure: '#10B981',
    vulnerable: '#EF4444',
  };

  const SSL_VERSION_COLORS = ['#3B82F6', '#06B6D4', '#8B5CF6', '#F59E0B', '#64748B'];
  const SERVER_TYPE_COLORS = ['#10B981', '#8B5CF6', '#06B6D4', '#F59E0B', '#64748B'];
  const SECURITY_COLORS = ['#10B981', '#EF4444'];

  const stats = [
    { label: 'Total Scans', value: totalScans, icon: Globe, color: 'blue' },
    { label: 'Vulnerable Hosts', value: vulnerableHosts, icon: AlertTriangle, color: 'red' },
    { label: 'Secured Hosts', value: secureHosts, icon: Shield, color: 'green' },
    { label: 'Latest Scan', value: hosts.length > 0 ? (hosts[0].date || hosts[0].scanDate || 'N/A').split(' ')[0] : 'N/A', icon: Calendar, color: 'purple' }
  ];

  const colorMap = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500' },
    green: { bg: 'bg-emerald-50', icon: 'text-emerald-500' },
    red: { bg: 'bg-red-50', icon: 'text-red-500' },
    purple: { bg: 'bg-purple-50', icon: 'text-purple-500' }
  };

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, index) => (
          <div key={index} className="bg-white border border-slate-200 rounded-xl p-6">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-slate-500 text-sm">{stat.label}</p>
                <h3 className="text-2xl font-bold text-slate-900 mt-1">{stat.value}</h3>
              </div>
              <div className={`p-3 rounded-full ${colorMap[stat.color].bg}`}>
                <stat.icon className={`w-6 h-6 ${colorMap[stat.color].icon}`} />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Security Status Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-6">Security Status</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={securityStatusData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="name" tick={{ fill: '#64748B' }} />
              <YAxis tick={{ fill: '#64748B' }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="value" name="Hosts" barSize={60} radius={[4, 4, 0, 0]}>
                {securityStatusData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.name === 'Secure' ? CHART_COLORS.secure : CHART_COLORS.vulnerable} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-6">Security Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={securityStatusData}
                cx="50%"
                cy="50%"
                labelLine={true}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
                nameKey="name"
                label={({name, percent}) => `${name}: ${(percent * 100).toFixed(0)}%`}
              >
                {securityStatusData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={SECURITY_COLORS[index % SECURITY_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* SSL Version and Server Type Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-6">SSL/TLS Version Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={sslVersionData}
                cx="50%"
                cy="50%"
                labelLine={true}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
                nameKey="name"
                label={({name, percent}) => `${name}: ${(percent * 100).toFixed(0)}%`}
              >
                {sslVersionData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={SSL_VERSION_COLORS[index % SSL_VERSION_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-slate-900 mb-6">Server Type Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={serverTypeData}
                cx="50%"
                cy="50%"
                labelLine={true}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
                nameKey="name"
                label={({name, percent}) => `${name}: ${(percent * 100).toFixed(0)}%`}
              >
                {serverTypeData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={SERVER_TYPE_COLORS[index % SERVER_TYPE_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Vulnerabilities Table */}
      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <h2 className="text-lg font-semibold text-slate-900 mb-6">Top SSL Vulnerabilities</h2>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50">
                <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Vulnerability</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Severity</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Affected Hosts</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-slate-700">Description</th>
              </tr>
            </thead>
            <tbody>
              <tr className="border-b border-slate-100">
                <td className="py-3 px-4 font-medium text-slate-800">RC4 Supported</td>
                <td className="py-3 px-4">
                  <span className="bg-red-100 text-red-700 px-2 py-1 rounded text-xs font-medium">High</span>
                </td>
                <td className="py-3 px-4 text-slate-600">{hosts.filter(h => h.rc4_supported || h.rc4Supported).length}</td>
                <td className="py-3 px-4 text-slate-600">RC4 cipher is vulnerable to attacks</td>
              </tr>
              <tr className="border-b border-slate-100">
                <td className="py-3 px-4 font-medium text-slate-800">Untrusted Certificate</td>
                <td className="py-3 px-4">
                  <span className="bg-red-100 text-red-700 px-2 py-1 rounded text-xs font-medium">High</span>
                </td>
                <td className="py-3 px-4 text-slate-600">{vulnerableHosts}</td>
                <td className="py-3 px-4 text-slate-600">Certificate chain is not trusted</td>
              </tr>
              <tr className="border-b border-slate-100">
                <td className="py-3 px-4 font-medium text-slate-800">SSLv3 Supported</td>
                <td className="py-3 px-4">
                  <span className="bg-amber-100 text-amber-700 px-2 py-1 rounded text-xs font-medium">Medium</span>
                </td>
                <td className="py-3 px-4 text-slate-600">{hosts.filter(h => (h.ssl_version || h.sslVersion || '').includes('SSLv3')).length}</td>
                <td className="py-3 px-4 text-slate-600">Vulnerable to POODLE attack</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// Main SSL Checker Component
const SSLChecker = () => {
  const [page, setPage] = useState('scanner');
  const [selectedHost, setSelectedHost] = useState(null);
  const [loading, setLoading] = useState(false);
  const [hostHistory, setHostHistory] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    if (!HAS_BACKEND) return;
    try {
      const response = await fetch(`${API_URL}/api/scans`, {
        headers: { 'ngrok-skip-browser-warning': '1' },
        credentials: 'include'
      });
      if (!response.ok) {
        throw new Error('Failed to fetch history');
      }
      const data = await response.json();
      setHostHistory(Array.isArray(data) ? data : []);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch history:', err);
      setHostHistory([]);
    }
  };

  const handleScan = async (domain, port) => {
    if (!HAS_BACKEND) {
      setError('SSL checker backend is not configured. Please set NEXT_PUBLIC_SSL_API_URL in the .env file.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_URL}/api/scan`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'ngrok-skip-browser-warning': '1',
        },
        credentials: 'include',
        body: JSON.stringify({ hostname: domain, port: parseInt(port) })
      });

      if (!response.ok) {
        throw new Error('Scan failed');
      }

      await fetchHistory();
      setPage('hosts');
    } catch (error) {
      console.error('Scan failed:', error);
      setError('Failed to perform scan. Please check if the backend is running and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">SSL Security Scanner</h1>
          <p className="text-sm text-slate-500 mt-1">Analyze SSL/TLS configuration and security</p>
        </div>
        <div className="p-3 bg-blue-100 rounded-xl">
          <Lock className="h-8 w-8 text-blue-600" />
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => { setPage('analytics'); setSelectedHost(null); }}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors ${
            page === 'analytics'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
          }`}
        >
          <BarChart2 className="w-4 h-4" />
          Analytics
        </button>
        <button
          onClick={() => { setPage('scanner'); setSelectedHost(null); }}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors ${
            page === 'scanner'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
          }`}
        >
          <Search className="w-4 h-4" />
          Scanner
        </button>
        <button
          onClick={() => { setPage('hosts'); setSelectedHost(null); }}
          className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors ${
            page === 'hosts'
              ? 'bg-blue-600 text-white'
              : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
          }`}
        >
          <Server className="w-4 h-4" />
          Host Details
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <AlertTriangle className="w-5 h-5" />
          {error}
        </div>
      )}

      {/* Content */}
      {loading ? (
        <div className="text-center py-12">
          <Loader2 className="w-12 h-12 text-blue-600 animate-spin mx-auto mb-4" />
          <p className="text-slate-600">Scanning... Please wait</p>
        </div>
      ) : (
        <>
          {page === 'scanner' && (
            <Scanner onScan={handleScan} loading={loading} />
          )}
          {page === 'analytics' && (
            <AnalyticsDashboard hosts={hostHistory} />
          )}
          {page === 'hosts' && (
            selectedHost ? (
              <HostDetails host={selectedHost} onBack={() => setSelectedHost(null)} />
            ) : (
              <HostList hosts={hostHistory} onSelectHost={setSelectedHost} />
            )
          )}
        </>
      )}
    </div>
  );
};

export default SSLChecker;
