'use client'

import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar } from 'recharts';
import { Shield, Network, Wifi, Clock, Server, AlertTriangle } from 'lucide-react';

const PENTEST_API_URL = process.env.NEXT_PUBLIC_PENTEST_API_URL || 'http://localhost:3001';

const StatCard = ({ title, value, icon: Icon, color }) => (
  <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-4">
    <div className="flex items-center justify-between">
      <span className="text-sm font-medium text-slate-600">{title}</span>
      <Icon className={`h-5 w-5 ${color}`} />
    </div>
    <div className={`text-2xl font-bold mt-2 ${color}`}>{value}</div>
  </div>
);

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white p-3 border border-slate-200 rounded-lg shadow-sm">
        <p className="font-medium text-slate-900">{label}</p>
        <p className="text-sm text-slate-600">Count: {payload[0].value}</p>
        {payload[0].payload.details && (
          <p className="text-sm text-slate-500">{payload[0].payload.details}</p>
        )}
      </div>
    );
  }
  return null;
};

const Overview = () => {
  const [scanData, setScanData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await fetch(`${PENTEST_API_URL}/overview`, {
          headers: { 'ngrok-skip-browser-warning': 'true' }
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        setScanData(data && data.length > 0 ? data[0] : null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <div className="p-4">Loading...</div>;
  if (error) return <div className="p-4 text-red-600">Error: {error}</div>;
  if (!scanData) return <div className="p-4">No data available</div>;

  const vulnerabilitySeverityData = (scanData.graphs?.vulnerabilities || []).map(item => ({
    name: item.riskdesc,
    value: item.count,
    color: {
      High: '#ef4444',
      Medium: '#f97316',
      Low: '#3b82f6'
    }[item.riskdesc] || '#64748b'
  }));

  const portData = (scanData.graphs?.open_ports || []).map(item => ({
    port: item.port,
    count: item.count
  }));

  const serviceData = (scanData.graphs?.services || []).map(item => ({
    name: item.service,
    count: item.count,
    details: `Service: ${item.service}`
  }));

  const formattedVulnerabilityTypes = (scanData.graphs?.vulnerability_types || [])
    .map(item => ({
      subject: item.type
        .replace(/['"]/g, '')
        .replace(/[-]/g, ' ')
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
        .slice(0, 15),
      count: item.count
    }))
    .slice(0, 8);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <StatCard 
          title="Total Hosts" 
          value={scanData.total_hosts || 0}
          icon={Network}
          color="text-slate-600"
        />
        <StatCard 
          title="Hosts Up" 
          value={scanData.up_hosts || 0}
          icon={Shield}
          color="text-emerald-600"
        />
        <StatCard 
          title="Hosts Down" 
          value={scanData.down_hosts || 0}
          icon={Wifi}
          color="text-red-600"
        />
        <StatCard 
          title="Open Ports" 
          value={scanData.total_open_ports || 0}
          icon={Server}
          color="text-blue-600"
        />
        <StatCard 
          title="Total Vulnerabilities" 
          value={scanData.total_vulnerabilities || 0}
          icon={AlertTriangle}
          color="text-amber-600"
        />
        <StatCard 
          title="Scan Time" 
          value={`${Math.abs(Math.round((scanData.total_scan_time || 0) / 1e9))}s`}
          icon={Clock}
          color="text-purple-600"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Open Ports Distribution</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={portData}>
                <XAxis dataKey="port" />
                <YAxis />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Vulnerability Severity Distribution</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={vulnerabilitySeverityData}
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                  nameKey="name"
                >
                  {vulnerabilitySeverityData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Services Distribution</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={serviceData}>
                <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" height={80} />
                <YAxis />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="count" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Vulnerability Types Radar</h3>
          <div className="h-96">
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart data={formattedVulnerabilityTypes}>
                <PolarGrid gridType="polygon" />
                <PolarAngleAxis 
                  dataKey="subject"
                  tick={{ fill: '#64748b', fontSize: 12 }}
                />
                <PolarRadiusAxis 
                  angle={30}
                  domain={[0, 'auto']}
                />
                <Radar
                  name="Vulnerabilities"
                  dataKey="count"
                  stroke="#ef4444"
                  fill="#ef4444"
                  fillOpacity={0.6}
                />
                <Tooltip />
                <Legend />
              </RadarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Overview;