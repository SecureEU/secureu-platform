'use client'

import React, { useState, useEffect, useCallback } from 'react';
import {
  Shield,
  AlertTriangle,
  Monitor,
  Activity,
  Search,
  ChevronRight,
  Building2,
  Plus,
  Power,
  PowerOff,
  Loader2,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Eye
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
  ResponsiveContainer,
  LineChart,
  Line
} from 'recharts';

const SEUXDR_PROXY = '/api/seuxdr';

// --- API helpers ---

async function seuxdrGet(endpoint) {
  const res = await fetch(`${SEUXDR_PROXY}?endpoint=${encodeURIComponent(endpoint)}`);
  if (!res.ok) throw new Error(`GET ${endpoint}: ${res.status}`);
  return res.json();
}

async function seuxdrPost(endpoint, body = {}) {
  const res = await fetch(`${SEUXDR_PROXY}?endpoint=${encodeURIComponent(endpoint)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST ${endpoint}: ${res.status}`);
  return res.json();
}

// --- Stats Card ---

const StatsCard = ({ title, value, icon: Icon, color, subtext }) => {
  const colorMap = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500', border: 'border-blue-200' },
    green: { bg: 'bg-emerald-50', icon: 'text-emerald-500', border: 'border-emerald-200' },
    red: { bg: 'bg-red-50', icon: 'text-red-500', border: 'border-red-200' },
    amber: { bg: 'bg-amber-50', icon: 'text-amber-500', border: 'border-amber-200' },
    purple: { bg: 'bg-purple-50', icon: 'text-purple-500', border: 'border-purple-200' },
  };
  const colors = colorMap[color] || colorMap.blue;

  return (
    <div className={`bg-white border ${colors.border} rounded-xl p-6`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-slate-500 text-sm">{title}</p>
          <h3 className="text-2xl font-bold text-slate-900 mt-1">{value}</h3>
          {subtext && <p className="text-xs text-slate-400 mt-1">{subtext}</p>}
        </div>
        <div className={`p-3 rounded-full ${colors.bg}`}>
          <Icon className={`w-6 h-6 ${colors.icon}`} />
        </div>
      </div>
    </div>
  );
};

// --- Alerts Table ---

const AlertsTable = ({ alerts, filters, onFilterChange }) => {
  const getSeverityColor = (level) => {
    if (level >= 12) return 'bg-red-100 text-red-800';
    if (level >= 8) return 'bg-amber-100 text-amber-800';
    if (level >= 4) return 'bg-yellow-100 text-yellow-800';
    return 'bg-green-100 text-green-800';
  };

  const getSeverityLabel = (level) => {
    if (level >= 12) return 'Critical';
    if (level >= 8) return 'High';
    if (level >= 4) return 'Medium';
    return 'Low';
  };

  return (
    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
      <div className="p-4 border-b border-slate-200 flex items-center justify-between">
        <h3 className="font-semibold text-slate-900">Security Alerts</h3>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search alerts..."
              className="pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500 focus:border-green-500"
              value={filters.search || ''}
              onChange={(e) => onFilterChange({ ...filters, search: e.target.value })}
            />
          </div>
          <select
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
            value={filters.severity || 'all'}
            onChange={(e) => onFilterChange({ ...filters, severity: e.target.value })}
          >
            <option value="all">All Severities</option>
            <option value="critical">Critical (12+)</option>
            <option value="high">High (8-11)</option>
            <option value="medium">Medium (4-7)</option>
            <option value="low">Low (0-3)</option>
          </select>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Timestamp</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Agent</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Tactic</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Description</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Severity</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Groups</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {alerts.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-slate-500">
                  No alerts found
                </td>
              </tr>
            ) : (
              alerts.map((alert, index) => (
                <tr key={alert.id || index} className="hover:bg-slate-50">
                  <td className="py-3 px-4 text-sm text-slate-600">{alert.timestamp}</td>
                  <td className="py-3 px-4 text-sm font-medium text-slate-900">{alert.agent}</td>
                  <td className="py-3 px-4 text-sm text-slate-600">
                    {Array.isArray(alert.tactic) ? alert.tactic.join(', ') : alert.tactic || '-'}
                  </td>
                  <td className="py-3 px-4 text-sm text-slate-600 max-w-xs truncate">{alert.description}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${getSeverityColor(alert.level)}`}>
                      {getSeverityLabel(alert.level)} ({alert.level})
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-slate-600">
                    {Array.isArray(alert.groups) ? alert.groups.join(', ') : alert.groups || '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

// --- Agents Table ---

const AgentsTable = ({ agents, organizations, onActivate, onDeactivate, loading, onRefresh }) => {
  const [showGenerate, setShowGenerate] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generateResult, setGenerateResult] = useState(null);
  const [genForm, setGenForm] = useState({ org_id: '', group_id: '', os: 'linux', arch: 'amd64', distro: 'deb' });

  // Get groups for selected org
  const selectedOrg = organizations.find((o) => o.id === Number(genForm.org_id));
  const groups = selectedOrg?.groups || [];

  const handleGenerate = async () => {
    if (!genForm.org_id || !genForm.group_id) return;
    setGenerating(true);
    setGenerateResult(null);
    try {
      // Step 1: Generate agent on server
      const payload = {
        org_id: Number(genForm.org_id),
        group_id: Number(genForm.group_id),
        os: genForm.os,
        arch: genForm.arch,
      };
      if (genForm.os === 'linux') {
        payload.distro = genForm.distro;
      }
      await seuxdrPost('create/agent', payload);

      // Step 2: Download the agent binary
      const downloadParams = new URLSearchParams({
        endpoint: 'download/agent',
        os: genForm.os,
        arch: genForm.arch,
        group_id: genForm.group_id,
      });
      if (genForm.os === 'linux') {
        downloadParams.append('distro', genForm.distro);
      }

      const downloadRes = await fetch(`${SEUXDR_PROXY}?${downloadParams.toString()}`);
      if (!downloadRes.ok) {
        throw new Error('Agent generated but download failed');
      }

      const blob = await downloadRes.blob();
      let fileName = 'agent';
      const disposition = downloadRes.headers.get('Content-Disposition');
      if (disposition) {
        const match = disposition.match(/filename="?([^"]+)"?/);
        if (match && match[1]) fileName = match[1];
      }

      // Trigger browser download
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(link.href);

      setGenerateResult({ success: true, message: `Agent generated and downloaded as "${fileName}"` });
      onRefresh();
    } catch (err) {
      setGenerateResult({ success: false, message: err.message || 'Failed to generate agent' });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* Generate Agent Form */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex items-center justify-between">
          <h3 className="font-semibold text-slate-900">Security Agents</h3>
          <button
            onClick={() => { setShowGenerate(!showGenerate); setGenerateResult(null); }}
            className="flex items-center gap-2 px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm"
          >
            <Plus className="w-4 h-4" />
            Generate Agent
          </button>
        </div>

        {showGenerate && (
          <div className="p-4 bg-slate-50 border-b border-slate-200 space-y-3">
            <h4 className="font-medium text-slate-900 text-sm">Generate New Agent</h4>
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
              <div>
                <label className="block text-xs text-slate-500 mb-1">Organization</label>
                <select
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
                  value={genForm.org_id}
                  onChange={(e) => setGenForm({ ...genForm, org_id: e.target.value, group_id: '' })}
                >
                  <option value="">Select...</option>
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>{org.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-slate-500 mb-1">Group</label>
                <select
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
                  value={genForm.group_id}
                  onChange={(e) => setGenForm({ ...genForm, group_id: e.target.value })}
                  disabled={!genForm.org_id || groups.length === 0}
                >
                  <option value="">{groups.length === 0 ? 'No groups — create one first' : 'Select...'}</option>
                  {groups.map((g) => (
                    <option key={g.id} value={g.id}>{g.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-slate-500 mb-1">OS</label>
                <select
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
                  value={genForm.os}
                  onChange={(e) => setGenForm({ ...genForm, os: e.target.value })}
                >
                  <option value="linux">Linux</option>
                  <option value="windows">Windows</option>
                  <option value="macos">macOS</option>
                </select>
              </div>
              <div>
                <label className="block text-xs text-slate-500 mb-1">Architecture</label>
                <select
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
                  value={genForm.arch}
                  onChange={(e) => setGenForm({ ...genForm, arch: e.target.value })}
                >
                  <option value="amd64">amd64 (x86_64)</option>
                  <option value="arm64">arm64 (Apple Silicon / ARM)</option>
                </select>
              </div>
              {genForm.os === 'linux' && (
                <div>
                  <label className="block text-xs text-slate-500 mb-1">Distro</label>
                  <select
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
                    value={genForm.distro}
                    onChange={(e) => setGenForm({ ...genForm, distro: e.target.value })}
                  >
                    <option value="deb">Debian/Ubuntu (deb)</option>
                    <option value="rpm">RHEL/CentOS (rpm)</option>
                  </select>
                </div>
              )}
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={handleGenerate}
                disabled={generating || !genForm.org_id || !genForm.group_id}
                className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm disabled:opacity-50"
              >
                {generating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                Generate & Download
              </button>
              {generateResult && (
                <span className={`text-sm ${generateResult.success ? 'text-green-600' : 'text-red-600'}`}>
                  {generateResult.message}
                </span>
              )}
            </div>
          </div>
        )}

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-slate-50">
            <tr>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Name</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">OS</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Organization</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Group</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Created</th>
              <th className="py-3 px-4 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {agents.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-500">
                  No agents found
                </td>
              </tr>
            ) : (
              agents.map((agent) => (
                <tr key={agent.id} className="hover:bg-slate-50">
                  <td className="py-3 px-4 text-sm font-medium text-slate-900">{agent.name}</td>
                  <td className="py-3 px-4 text-sm text-slate-600">{agent.os}</td>
                  <td className="py-3 px-4 text-sm text-slate-600">{agent.org_name}</td>
                  <td className="py-3 px-4 text-sm text-slate-600">{agent.group_name}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      agent.active ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-600'
                    }`}>
                      {agent.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-slate-500">{agent.created_at}</td>
                  <td className="py-3 px-4">
                    {agent.active ? (
                      <button
                        onClick={() => onDeactivate(agent.id)}
                        disabled={loading === agent.id}
                        className="flex items-center gap-1 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 rounded transition-colors disabled:opacity-50"
                      >
                        {loading === agent.id ? <Loader2 className="w-3 h-3 animate-spin" /> : <PowerOff className="w-3 h-3" />}
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => onActivate(agent.id)}
                        disabled={loading === agent.id}
                        className="flex items-center gap-1 px-3 py-1 text-xs font-medium text-green-600 hover:bg-green-50 rounded transition-colors disabled:opacity-50"
                      >
                        {loading === agent.id ? <Loader2 className="w-3 h-3 animate-spin" /> : <Power className="w-3 h-3" />}
                        Activate
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
    </div>
  );
};

// --- Organizations View ---

const OrganizationsView = ({ organizations, onRefresh }) => {
  const [showCreateOrg, setShowCreateOrg] = useState(false);
  const [showCreateGroup, setShowCreateGroup] = useState(null);
  const [orgForm, setOrgForm] = useState({ name: '', code: '' });
  const [groupForm, setGroupForm] = useState({ name: '' });
  const [creating, setCreating] = useState(false);

  const handleCreateOrg = async () => {
    if (!orgForm.name || !orgForm.code) return;
    setCreating(true);
    try {
      await seuxdrPost('create/org', orgForm);
      setOrgForm({ name: '', code: '' });
      setShowCreateOrg(false);
      onRefresh();
    } catch (err) {
      console.error('Failed to create organization:', err);
    } finally {
      setCreating(false);
    }
  };

  const handleCreateGroup = async (orgId) => {
    if (!groupForm.name) return;
    setCreating(true);
    try {
      await seuxdrPost('create/group', { name: groupForm.name, org_id: orgId });
      setGroupForm({ name: '' });
      setShowCreateGroup(null);
      onRefresh();
    } catch (err) {
      console.error('Failed to create group:', err);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-slate-900">Organizations</h3>
        <button
          onClick={() => setShowCreateOrg(!showCreateOrg)}
          className="flex items-center gap-2 px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm"
        >
          <Plus className="w-4 h-4" />
          New Organization
        </button>
      </div>

      {showCreateOrg && (
        <div className="bg-white border border-green-200 rounded-xl p-4 space-y-3">
          <h4 className="font-medium text-slate-900">Create Organization</h4>
          <div className="flex gap-3">
            <input
              type="text"
              placeholder="Organization name"
              className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
              value={orgForm.name}
              onChange={(e) => setOrgForm({ ...orgForm, name: e.target.value })}
            />
            <input
              type="text"
              placeholder="Code (e.g. ACME)"
              className="w-32 px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-green-500"
              value={orgForm.code}
              onChange={(e) => setOrgForm({ ...orgForm, code: e.target.value })}
            />
            <button
              onClick={handleCreateOrg}
              disabled={creating}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm disabled:opacity-50"
            >
              {creating ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Create'}
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {organizations.length === 0 ? (
          <div className="col-span-full bg-white border border-slate-200 rounded-xl p-8 text-center">
            <Building2 className="w-12 h-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-500">No organizations found</p>
            <p className="text-xs text-slate-400 mt-1">Create an organization to start deploying agents</p>
          </div>
        ) : (
          organizations.map((org) => (
            <div
              key={org.id}
              className="bg-white border border-slate-200 rounded-xl p-6 hover:border-green-300 hover:shadow-md transition-all"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="p-2 bg-green-100 rounded-lg">
                  <Building2 className="w-6 h-6 text-green-600" />
                </div>
                <span className="text-xs text-slate-400">ID: {org.id}</span>
              </div>
              <h4 className="font-semibold text-slate-900 mb-1">{org.name}</h4>
              <p className="text-sm text-slate-500 mb-3">Code: {org.code}</p>

              {org.groups && org.groups.length > 0 && (
                <div className="mb-3 space-y-1">
                  {org.groups.map((group) => (
                    <div key={group.id} className="flex items-center gap-2 text-xs text-slate-600 bg-slate-50 rounded px-2 py-1">
                      <ChevronRight className="w-3 h-3" />
                      {group.name}
                    </div>
                  ))}
                </div>
              )}

              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-600">{org.groups?.length || 0} Groups</span>
                <button
                  onClick={() => setShowCreateGroup(showCreateGroup === org.id ? null : org.id)}
                  className="text-green-600 hover:text-green-700 flex items-center gap-1 text-xs"
                >
                  <Plus className="w-3 h-3" /> Add Group
                </button>
              </div>

              {showCreateGroup === org.id && (
                <div className="mt-3 flex gap-2">
                  <input
                    type="text"
                    placeholder="Group name"
                    className="flex-1 px-2 py-1 border border-slate-300 rounded text-xs focus:ring-2 focus:ring-green-500"
                    value={groupForm.name}
                    onChange={(e) => setGroupForm({ name: e.target.value })}
                  />
                  <button
                    onClick={() => handleCreateGroup(org.id)}
                    disabled={creating}
                    className="px-3 py-1 bg-green-600 text-white rounded text-xs hover:bg-green-700 disabled:opacity-50"
                  >
                    {creating ? '...' : 'Add'}
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

// --- Dashboard Overview ---

const DashboardOverview = ({ stats, alertsByTactic, alertsTrend, topAgents }) => {
  const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4'];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard title="Total Alerts" value={stats.totalAlerts} icon={AlertTriangle} color="blue" subtext="From connected agents" />
        <StatsCard title="Critical Alerts" value={stats.criticalAlerts} icon={Shield} color="red" subtext="Severity >= 12" />
        <StatsCard title="Active Agents" value={stats.activeAgents} icon={Monitor} color="green" />
        <StatsCard title="Organizations" value={stats.organizations} icon={Building2} color="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h3 className="font-semibold text-slate-900 mb-4">Alerts by Attack Tactic</h3>
          {alertsByTactic.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-slate-500">No alert data available</div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={alertsByTactic}
                  cx="50%"
                  cy="50%"
                  labelLine={true}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                  nameKey="type"
                  label={({ type, percent }) => `${type}: ${(percent * 100).toFixed(0)}%`}
                >
                  {alertsByTactic.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-6">
          <h3 className="font-semibold text-slate-900 mb-4">Top 5 Agents by Alerts</h3>
          {topAgents.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-slate-500">No alert data available</div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topAgents} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fill: '#64748B' }} />
                <YAxis dataKey="agent" type="category" tick={{ fill: '#64748B' }} width={120} />
                <Tooltip />
                <Bar dataKey="count" fill="#10B981" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <h3 className="font-semibold text-slate-900 mb-4">Alerts Trend</h3>
        {alertsTrend.length === 0 ? (
          <div className="h-64 flex items-center justify-center text-slate-500">No alert data available</div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={alertsTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="time" tick={{ fill: '#64748B' }} />
              <YAxis tick={{ fill: '#64748B' }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="critical" stroke="#EF4444" strokeWidth={2} name="Critical" />
              <Line type="monotone" dataKey="high" stroke="#F59E0B" strokeWidth={2} name="High" />
              <Line type="monotone" dataKey="medium" stroke="#3B82F6" strokeWidth={2} name="Medium" />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
};

// --- Main SIEM Dashboard ---

const SIEMDashboard = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [agentLoading, setAgentLoading] = useState(null);

  // Manager status
  const [managerOnline, setManagerOnline] = useState(null);

  // Data
  const [alerts, setAlerts] = useState([]);
  const [agents, setAgents] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [filters, setFilters] = useState({ search: '', severity: 'all' });

  // Stats & chart data
  const [stats, setStats] = useState({ totalAlerts: 0, criticalAlerts: 0, activeAgents: 0, organizations: 0 });
  const [alertsByTactic, setAlertsByTactic] = useState([]);
  const [alertsTrend, setAlertsTrend] = useState([]);
  const [topAgents, setTopAgents] = useState([]);

  // --- Map SEUXDR alert format to flat format ---
  const mapAlerts = (rawAlerts) => {
    if (!rawAlerts || !Array.isArray(rawAlerts)) return [];
    return rawAlerts.map((hit) => {
      const src = hit._source || hit;
      const rule = src.rule || {};
      const mitre = rule.mitre || {};
      const agent = src.agent || {};
      return {
        id: hit._id || src.id,
        timestamp: src['@timestamp'] || src.timestamp || src.time || '',
        agent: agent.name || 'Unknown',
        tactic: mitre.tactic || [],
        technique: mitre.technique || [],
        description: rule.description || '',
        level: rule.level || 0,
        groups: rule.groups || [],
        fullLog: src.full_log || '',
      };
    });
  };

  // --- Compute charts from alerts ---
  const computeChartData = (mappedAlerts) => {
    // Alerts by tactic
    const tacticCounts = {};
    mappedAlerts.forEach((a) => {
      const tactics = Array.isArray(a.tactic) ? a.tactic : [a.tactic || 'Unknown'];
      tactics.forEach((t) => {
        if (t) tacticCounts[t] = (tacticCounts[t] || 0) + 1;
      });
    });
    setAlertsByTactic(Object.entries(tacticCounts).map(([type, value]) => ({ type, value })));

    // Top agents
    const agentCounts = {};
    mappedAlerts.forEach((a) => {
      agentCounts[a.agent] = (agentCounts[a.agent] || 0) + 1;
    });
    setTopAgents(
      Object.entries(agentCounts)
        .map(([agent, count]) => ({ agent, count }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5)
    );

    // Alerts trend (group by hour)
    const hourCounts = {};
    mappedAlerts.forEach((a) => {
      if (!a.timestamp) return;
      const date = new Date(a.timestamp);
      if (isNaN(date.getTime())) return;
      const hourKey = `${date.getHours().toString().padStart(2, '0')}:00`;
      if (!hourCounts[hourKey]) hourCounts[hourKey] = { critical: 0, high: 0, medium: 0 };
      if (a.level >= 12) hourCounts[hourKey].critical++;
      else if (a.level >= 8) hourCounts[hourKey].high++;
      else if (a.level >= 4) hourCounts[hourKey].medium++;
    });
    const trend = Object.entries(hourCounts)
      .map(([time, counts]) => ({ time, ...counts }))
      .sort((a, b) => a.time.localeCompare(b.time));
    setAlertsTrend(trend);
  };

  // --- Fetch all data ---
  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // Check manager status
      const statusData = await seuxdrGet('status');
      setManagerOnline(statusData.message === 'Ok' || statusData.status === 'ok');
    } catch {
      setManagerOnline(false);
    }

    try {
      // Fetch orgs, agents, alerts in parallel
      const [orgsData, agentsData] = await Promise.all([
        seuxdrPost('orgs', {}).catch(() => []),
        seuxdrPost('view/agents', {}).catch(() => []),
      ]);

      // Fetch alerts (may fail if OpenSearch not running)
      let alertsData = [];
      try {
        const now = new Date();
        const dayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        const raw = await seuxdrPost('view/alerts', {
          query: {
            org_id: '',
            group_id: '',
            gte: dayAgo.toISOString(),
            lte: now.toISOString(),
          },
        });
        // Response can be { data: [...], agent_map: [...] } or just an array
        alertsData = raw?.data || (Array.isArray(raw) ? raw : []);
      } catch {
        // OpenSearch may not be running — that's fine
      }

      const orgs = Array.isArray(orgsData) ? orgsData : [];
      const agentsList = Array.isArray(agentsData) ? agentsData : [];
      const mappedAlerts = mapAlerts(alertsData);

      setOrganizations(orgs);
      setAgents(agentsList);
      setAlerts(mappedAlerts);

      setStats({
        totalAlerts: mappedAlerts.length,
        criticalAlerts: mappedAlerts.filter((a) => a.level >= 12).length,
        activeAgents: agentsList.filter((a) => a.active).length,
        organizations: orgs.length,
      });

      computeChartData(mappedAlerts);
    } catch (err) {
      setError('Failed to fetch data from SEUXDR backend');
      console.error('SIEM fetch error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // --- Agent activate/deactivate ---
  const handleActivateAgent = async (agentId) => {
    setAgentLoading(agentId);
    try {
      await seuxdrPost('agent/activate', { agent_uuid: agentId });
      setAgents(agents.map((a) => (a.id === agentId ? { ...a, active: true } : a)));
      setStats((s) => ({ ...s, activeAgents: s.activeAgents + 1 }));
    } catch (err) {
      console.error('Failed to activate agent:', err);
    } finally {
      setAgentLoading(null);
    }
  };

  const handleDeactivateAgent = async (agentId) => {
    setAgentLoading(agentId);
    try {
      await seuxdrPost('agent/deactivate', { agent_uuid: agentId });
      setAgents(agents.map((a) => (a.id === agentId ? { ...a, active: false } : a)));
      setStats((s) => ({ ...s, activeAgents: Math.max(0, s.activeAgents - 1) }));
    } catch (err) {
      console.error('Failed to deactivate agent:', err);
    } finally {
      setAgentLoading(null);
    }
  };

  // --- Filter alerts ---
  const filteredAlerts = alerts.filter((alert) => {
    if (filters.search) {
      const q = filters.search.toLowerCase();
      if (
        !alert.description.toLowerCase().includes(q) &&
        !alert.agent.toLowerCase().includes(q)
      ) {
        return false;
      }
    }
    if (filters.severity !== 'all') {
      if (filters.severity === 'critical' && alert.level < 12) return false;
      if (filters.severity === 'high' && (alert.level < 8 || alert.level >= 12)) return false;
      if (filters.severity === 'medium' && (alert.level < 4 || alert.level >= 8)) return false;
      if (filters.severity === 'low' && alert.level >= 4) return false;
    }
    return true;
  });

  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: Activity },
    { id: 'alerts', label: 'Alerts', icon: AlertTriangle },
    { id: 'agents', label: 'Agents', icon: Monitor },
    { id: 'organizations', label: 'Organizations', icon: Building2 },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">SIEM Dashboard</h1>
          <p className="text-sm text-slate-500 mt-1">SEUXDR - Host-Based Intrusion Detection System</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchData}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <div className="p-3 bg-green-100 rounded-xl">
            <Eye className="h-8 w-8 text-green-600" />
          </div>
        </div>
      </div>

      {/* Manager Status Banner */}
      <div className={`rounded-xl p-4 border ${managerOnline ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
        <div className="flex items-center gap-3">
          <div className={`w-3 h-3 rounded-full ${managerOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
          {managerOnline === null ? (
            <span className="font-medium text-slate-600">Checking SEUXDR Manager...</span>
          ) : managerOnline ? (
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-green-600" />
              <span className="font-medium text-green-800">SEUXDR Manager is running</span>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <XCircle className="w-4 h-4 text-red-600" />
              <span className="font-medium text-red-800">SEUXDR Manager is not reachable</span>
            </div>
          )}
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex gap-2 border-b border-slate-200 pb-2">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors ${
              activeTab === tab.id
                ? 'bg-green-600 text-white'
                : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <AlertTriangle className="w-5 h-5" />
          {error}
        </div>
      )}

      {/* Tab Content */}
      {activeTab === 'dashboard' && (
        <DashboardOverview stats={stats} alertsByTactic={alertsByTactic} alertsTrend={alertsTrend} topAgents={topAgents} />
      )}

      {activeTab === 'alerts' && (
        <AlertsTable alerts={filteredAlerts} filters={filters} onFilterChange={setFilters} />
      )}

      {activeTab === 'agents' && (
        <AgentsTable
          agents={agents}
          organizations={organizations}
          onActivate={handleActivateAgent}
          onDeactivate={handleDeactivateAgent}
          loading={agentLoading}
          onRefresh={fetchData}
        />
      )}

      {activeTab === 'organizations' && (
        <OrganizationsView organizations={organizations} onRefresh={fetchData} />
      )}
    </div>
  );
};

export default SIEMDashboard;
