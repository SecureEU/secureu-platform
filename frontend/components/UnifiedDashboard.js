'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import {
  Shield,
  Crosshair,
  ShieldCheck,
  Brain,
  Globe,
  Lock,
  Eye,
  EyeOff,
  Search,
  Calculator,
  Flag,
  AlertTriangle,
  Activity,
  Server,
  Target,
  ChevronRight,
  TrendingUp,
  TrendingDown,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Zap,
  BarChart3,
  PieChart,
  Users,
  FileText,
  Wifi,
  WifiOff
} from 'lucide-react'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart as RePieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend
} from 'recharts'

// Generate demo data for the dashboard
const generateDemoStats = () => ({
  offensive: {
    totalScans: Math.floor(Math.random() * 50) + 20,
    activeScans: Math.floor(Math.random() * 5),
    vulnerabilities: {
      critical: Math.floor(Math.random() * 10) + 2,
      high: Math.floor(Math.random() * 25) + 10,
      medium: Math.floor(Math.random() * 40) + 20,
      low: Math.floor(Math.random() * 60) + 30
    },
    sslCertificates: {
      valid: Math.floor(Math.random() * 20) + 15,
      expiring: Math.floor(Math.random() * 5) + 1,
      expired: Math.floor(Math.random() * 3)
    },
    darkwebAlerts: Math.floor(Math.random() * 15) + 5,
    assetsMonitored: Math.floor(Math.random() * 100) + 50
  },
  defensive: {
    totalAlerts: Math.floor(Math.random() * 500) + 200,
    activeAgents: Math.floor(Math.random() * 20) + 10,
    offlineAgents: Math.floor(Math.random() * 3),
    alertsBySeverity: {
      critical: Math.floor(Math.random() * 20) + 5,
      high: Math.floor(Math.random() * 50) + 25,
      medium: Math.floor(Math.random() * 100) + 50,
      low: Math.floor(Math.random() * 150) + 75,
      info: Math.floor(Math.random() * 200) + 100
    },
    resolvedToday: Math.floor(Math.random() * 80) + 40
  },
  cti: {
    predictionsToday: Math.floor(Math.random() * 30) + 10,
    logsAnalyzed: Math.floor(Math.random() * 5000) + 2000,
    threatsDetected: Math.floor(Math.random() * 50) + 20,
    avgCVSSScore: (Math.random() * 3 + 5).toFixed(1)
  }
})

const generateActivityTimeline = () => {
  const activities = [
    { type: 'scan', icon: Globe, color: 'blue', message: 'Nmap scan completed on 192.168.1.0/24', time: '2 min ago' },
    { type: 'alert', icon: AlertTriangle, color: 'red', message: 'Critical vulnerability detected: CVE-2024-1234', time: '5 min ago' },
    { type: 'ssl', icon: Lock, color: 'yellow', message: 'SSL certificate expiring in 7 days for api.example.com', time: '12 min ago' },
    { type: 'siem', icon: Eye, color: 'green', message: 'SIEM agent web-server-01 reconnected', time: '18 min ago' },
    { type: 'darkweb', icon: EyeOff, color: 'purple', message: 'New credential leak detected for domain example.com', time: '25 min ago' },
    { type: 'cti', icon: Flag, color: 'cyan', message: 'Red Flags: Anomaly detected in system logs', time: '32 min ago' },
    { type: 'prediction', icon: Calculator, color: 'indigo', message: 'VSP: New vulnerability predicted with CVSS 8.5', time: '45 min ago' },
    { type: 'scan', icon: Target, color: 'orange', message: 'ZAP scan started on https://app.example.com', time: '1 hour ago' }
  ]
  return activities
}

const generateTrendData = () => {
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  return days.map(day => ({
    name: day,
    scans: Math.floor(Math.random() * 20) + 5,
    alerts: Math.floor(Math.random() * 100) + 30,
    threats: Math.floor(Math.random() * 15) + 3
  }))
}

const generateVulnDistribution = () => [
  { name: 'Critical', value: Math.floor(Math.random() * 10) + 5, color: '#fca5a5' },
  { name: 'High', value: Math.floor(Math.random() * 30) + 15, color: '#fdba74' },
  { name: 'Medium', value: Math.floor(Math.random() * 50) + 30, color: '#fcd34d' },
  { name: 'Low', value: Math.floor(Math.random() * 70) + 40, color: '#93c5fd' }
]

const getColorClass = (color) => {
  const colors = {
    blue: 'bg-slate-50 text-slate-400',
    red: 'bg-rose-50/70 text-rose-400',
    yellow: 'bg-amber-50/70 text-amber-400',
    green: 'bg-emerald-50/70 text-emerald-400',
    purple: 'bg-violet-50/70 text-violet-400',
    cyan: 'bg-cyan-50/70 text-cyan-400',
    indigo: 'bg-indigo-50/70 text-indigo-400',
    orange: 'bg-orange-50/70 text-orange-400'
  }
  return colors[color] || colors.blue
}

export default function UnifiedDashboard() {
  const [stats, setStats] = useState(null)
  const [activities, setActivities] = useState([])
  const [trendData, setTrendData] = useState([])
  const [vulnDistribution, setVulnDistribution] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Simulate loading data
    setTimeout(() => {
      setStats(generateDemoStats())
      setActivities(generateActivityTimeline())
      setTrendData(generateTrendData())
      setVulnDistribution(generateVulnDistribution())
      setLoading(false)
    }, 500)
  }, [])

  if (loading || !stats) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  const totalVulnerabilities =
    stats.offensive.vulnerabilities.critical +
    stats.offensive.vulnerabilities.high +
    stats.offensive.vulnerabilities.medium +
    stats.offensive.vulnerabilities.low

  const totalAlerts = stats.defensive.totalAlerts
  const criticalItems = stats.offensive.vulnerabilities.critical + stats.defensive.alertsBySeverity.critical

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Security Overview</h1>
          <p className="text-gray-600 mt-1">Real-time security posture across all platforms</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-full text-sm font-medium">
            <Activity className="h-4 w-4" />
            All Systems Operational
          </div>
          <div className="text-sm text-gray-400">
            Last updated: {new Date().toLocaleTimeString()}
          </div>
        </div>
      </div>

      {/* Critical Summary Bar */}
      {criticalItems > 0 && (
        <div className="bg-gradient-to-r from-orange-50/70 to-amber-50/70 border border-orange-100 rounded-xl p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-orange-100/60 rounded-lg">
              <AlertTriangle className="h-6 w-6 text-orange-400" />
            </div>
            <div>
              <p className="font-semibold text-gray-700">{criticalItems} Critical Issues Require Attention</p>
              <p className="text-sm text-gray-500">
                {stats.offensive.vulnerabilities.critical} vulnerabilities, {stats.defensive.alertsBySeverity.critical} security alerts
              </p>
            </div>
          </div>
          <Link
            href="/offsec/pentest/dashboard"
            className="px-4 py-2 bg-orange-400 text-white rounded-lg font-medium hover:bg-orange-500 transition-colors flex items-center gap-2"
          >
            View Details
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>
      )}

      {/* Category Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Offensive Solutions Card */}
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <div
            className="px-6 py-4"
            style={{ background: 'linear-gradient(to right, oklch(70.4% 0.191 22.216), oklch(65% 0.18 22))' }}
          >
            <div className="flex items-center justify-between text-white">
              <div className="flex items-center gap-3">
                <div className="p-1.5 bg-white/20 rounded-lg">
                  <Crosshair className="h-5 w-5" />
                </div>
                <h2 className="text-lg font-semibold">Offensive Solutions</h2>
              </div>
              <Link href="/offsec/pentest/dashboard" className="text-white/60 hover:text-white transition-colors">
                <ChevronRight className="h-5 w-5" />
              </Link>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{stats.offensive.totalScans}</p>
                <p className="text-xs text-gray-500">Total Scans</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{totalVulnerabilities}</p>
                <p className="text-xs text-gray-500">Vulnerabilities</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Critical</span>
                <span className="font-medium text-rose-400">{stats.offensive.vulnerabilities.critical}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">High</span>
                <span className="font-medium text-orange-400">{stats.offensive.vulnerabilities.high}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Active Scans</span>
                <span className="font-medium text-slate-500">{stats.offensive.activeScans}</span>
              </div>
            </div>
            <div className="pt-3 border-t border-gray-100 grid grid-cols-2 gap-2">
              <Link href="/offsec/pentest/scans" className="flex items-center gap-2 text-sm text-gray-600 hover:text-slate-900 transition-colors">
                <Globe className="h-4 w-4" /> Scans
              </Link>
              <Link href="/offsec/ssl" className="flex items-center gap-2 text-sm text-gray-600 hover:text-slate-900 transition-colors">
                <Lock className="h-4 w-4" /> SSL Check
              </Link>
              <Link href="/offsec/darkweb/monitor" className="flex items-center gap-2 text-sm text-gray-600 hover:text-slate-900 transition-colors">
                <EyeOff className="h-4 w-4" /> Darkweb
              </Link>
              <Link href="/offsec/pentest/assets" className="flex items-center gap-2 text-sm text-gray-600 hover:text-slate-900 transition-colors">
                <Server className="h-4 w-4" /> Assets
              </Link>
            </div>
          </div>
        </div>

        {/* Defensive Solutions Card */}
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <div
            className="px-6 py-4"
            style={{ background: 'linear-gradient(to right, oklch(72% 0.15 160), oklch(68% 0.14 170))' }}
          >
            <div className="flex items-center justify-between text-white">
              <div className="flex items-center gap-3">
                <div className="p-1.5 bg-white/20 rounded-lg">
                  <ShieldCheck className="h-5 w-5" />
                </div>
                <h2 className="text-lg font-semibold">Defensive Solutions</h2>
              </div>
              <Link href="/defsec/siem" className="text-white/60 hover:text-white transition-colors">
                <ChevronRight className="h-5 w-5" />
              </Link>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{stats.defensive.totalAlerts}</p>
                <p className="text-xs text-gray-500">Total Alerts</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{stats.defensive.activeAgents}</p>
                <p className="text-xs text-gray-500">Active Agents</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Critical Alerts</span>
                <span className="font-medium text-rose-400">{stats.defensive.alertsBySeverity.critical}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">High Alerts</span>
                <span className="font-medium text-orange-400">{stats.defensive.alertsBySeverity.high}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Resolved Today</span>
                <span className="font-medium text-emerald-400">{stats.defensive.resolvedToday}</span>
              </div>
            </div>
            <div className="pt-3 border-t border-gray-100">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Wifi className="h-4 w-4 text-emerald-400" />
                  <span className="text-sm text-gray-500">{stats.defensive.activeAgents} Online</span>
                </div>
                <div className="flex items-center gap-2">
                  <WifiOff className="h-4 w-4 text-gray-300" />
                  <span className="text-sm text-gray-500">{stats.defensive.offlineAgents} Offline</span>
                </div>
              </div>
              <Link href="/defsec/siem" className="mt-3 flex items-center gap-2 text-sm text-gray-500 hover:text-teal-500 transition-colors">
                <Eye className="h-4 w-4" /> SIEM Dashboard
              </Link>
            </div>
          </div>
        </div>

        {/* CTI Tools Card */}
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <div
            className="px-6 py-4"
            style={{ background: 'linear-gradient(to right, oklch(65% 0.18 280), oklch(60% 0.17 290))' }}
          >
            <div className="flex items-center justify-between text-white">
              <div className="flex items-center gap-3">
                <div className="p-1.5 bg-white/20 rounded-lg">
                  <Brain className="h-5 w-5" />
                </div>
                <h2 className="text-lg font-semibold">CTI Tools</h2>
              </div>
              <Link href="/cti/vsp" className="text-white/60 hover:text-white transition-colors">
                <ChevronRight className="h-5 w-5" />
              </Link>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{stats.cti.logsAnalyzed.toLocaleString()}</p>
                <p className="text-xs text-gray-500">Logs Analyzed</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{stats.cti.threatsDetected}</p>
                <p className="text-xs text-gray-500">Threats Detected</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Predictions Today</span>
                <span className="font-medium text-violet-400">{stats.cti.predictionsToday}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Avg CVSS Score</span>
                <span className="font-medium text-orange-400">{stats.cti.avgCVSSScore}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-500">Darkweb Alerts</span>
                <span className="font-medium text-rose-400">{stats.offensive.darkwebAlerts}</span>
              </div>
            </div>
            <div className="pt-3 border-t border-gray-100 grid grid-cols-2 gap-2">
              <Link href="/cti/vsp" className="flex items-center gap-2 text-sm text-gray-600 hover:text-purple-600 transition-colors">
                <Calculator className="h-4 w-4" /> VSP Predictor
              </Link>
              <Link href="/cti/redflags" className="flex items-center gap-2 text-sm text-gray-600 hover:text-purple-600 transition-colors">
                <Flag className="h-4 w-4" /> Red Flags
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Activity Trend Chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-slate-500" />
            Weekly Activity Trend
          </h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trendData}>
                <defs>
                  <linearGradient id="colorScans" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorAlerts" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="name" tick={{ fill: '#6b7280', fontSize: 12 }} />
                <YAxis tick={{ fill: '#6b7280', fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'white',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                  }}
                />
                <Legend />
                <Area type="monotone" dataKey="scans" stroke="#3b82f6" fillOpacity={1} fill="url(#colorScans)" name="Scans" />
                <Area type="monotone" dataKey="alerts" stroke="#10b981" fillOpacity={1} fill="url(#colorAlerts)" name="Alerts" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Vulnerability Distribution Chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <PieChart className="h-5 w-5 text-slate-500" />
            Vulnerability Distribution
          </h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <RePieChart>
                <Pie
                  data={vulnDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={5}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                >
                  {vulnDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </RePieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Recent Activity & Quick Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Activity Timeline */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <Activity className="h-5 w-5 text-slate-500" />
              Recent Activity
            </h3>
            <span className="text-xs text-gray-400 bg-gray-100 px-2 py-1 rounded-full">Live feed</span>
          </div>
          <div className="space-y-3 max-h-[350px] overflow-y-auto">
            {activities.map((activity, index) => {
              const IconComponent = activity.icon
              return (
                <div key={index} className="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors">
                  <div className={`p-2 rounded-lg ${getColorClass(activity.color)}`}>
                    <IconComponent className="h-4 w-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-900">{activity.message}</p>
                    <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                      <Clock className="h-3 w-3" /> {activity.time}
                    </p>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* Quick Stats */}
        <div className="space-y-4">
          {/* SSL Status */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
            <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <Lock className="h-4 w-4 text-slate-500" />
              SSL Certificates
            </h4>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <CheckCircle className="h-4 w-4 text-emerald-400" />
                  <span className="text-sm text-gray-500">Valid</span>
                </div>
                <span className="font-medium text-emerald-400">{stats.offensive.sslCertificates.valid}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-amber-400" />
                  <span className="text-sm text-gray-500">Expiring Soon</span>
                </div>
                <span className="font-medium text-amber-400">{stats.offensive.sslCertificates.expiring}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <XCircle className="h-4 w-4 text-rose-300" />
                  <span className="text-sm text-gray-500">Expired</span>
                </div>
                <span className="font-medium text-rose-400">{stats.offensive.sslCertificates.expired}</span>
              </div>
            </div>
          </div>

          {/* Assets Overview */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
            <h4 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
              <Server className="h-4 w-4 text-slate-500" />
              Assets Monitored
            </h4>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-3xl font-bold text-gray-800">{stats.offensive.assetsMonitored}</p>
                <p className="text-xs text-gray-400">Total Assets</p>
              </div>
              <div className="flex items-center gap-1 text-emerald-400 text-sm">
                <TrendingUp className="h-4 w-4" />
                +5 this week
              </div>
            </div>
          </div>

          {/* Performance Score */}
          <div
            className="rounded-xl p-4"
            style={{ background: 'linear-gradient(to bottom right, oklch(86.5% 0.127 207.078), oklch(80% 0.12 210))' }}
          >
            <h4 className="text-sm font-semibold mb-3 flex items-center gap-2 text-slate-700">
              <Zap className="h-4 w-4" />
              Security Score
            </h4>
            <div className="flex items-end justify-between">
              <div>
                <p className="text-4xl font-bold text-slate-800">78</p>
                <p className="text-xs text-slate-600">out of 100</p>
              </div>
              <div className="text-right">
                <div className="flex items-center gap-1 text-emerald-700 text-sm">
                  <TrendingUp className="h-4 w-4" />
                  +3 pts
                </div>
                <p className="text-xs text-slate-600">vs last week</p>
              </div>
            </div>
            <div className="mt-3 w-full bg-white/50 rounded-full h-2">
              <div className="bg-gradient-to-r from-slate-600 to-slate-700 h-2 rounded-full" style={{ width: '78%' }} />
            </div>
          </div>
        </div>
      </div>

      {/* Quick Access Grid */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Access</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3">
          {[
            { name: 'Scans', icon: Globe, href: '/offsec/pentest/scans', bgColor: 'bg-slate-100', textColor: 'text-slate-600' },
            { name: 'Assets', icon: Server, href: '/offsec/pentest/assets', bgColor: 'bg-slate-100', textColor: 'text-slate-600' },
            { name: 'Exploits', icon: Target, href: '/offsec/pentest/exploitation', bgColor: 'bg-slate-100', textColor: 'text-slate-600' },
            { name: 'SSL Check', icon: Lock, href: '/offsec/ssl', bgColor: 'bg-slate-100', textColor: 'text-slate-600' },
            { name: 'Darkweb', icon: EyeOff, href: '/offsec/darkweb/monitor', bgColor: 'bg-slate-100', textColor: 'text-slate-600' },
            { name: 'SIEM', icon: Eye, href: '/defsec/siem', bgColor: 'bg-emerald-50', textColor: 'text-emerald-600' },
            { name: 'VSP', icon: Calculator, href: '/cti/vsp', bgColor: 'bg-violet-50', textColor: 'text-violet-600' },
            { name: 'Red Flags', icon: Flag, href: '/cti/redflags', bgColor: 'bg-violet-50', textColor: 'text-violet-600' }
          ].map((item) => (
            <Link
              key={item.name}
              href={item.href}
              className="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-100 hover:border-gray-200 hover:shadow-sm hover:bg-gray-50/50 transition-all group"
            >
              <div className={`p-3 rounded-lg ${item.bgColor} ${item.textColor} group-hover:scale-105 transition-transform`}>
                <item.icon className="h-5 w-5" />
              </div>
              <span className="text-sm font-medium text-gray-600">{item.name}</span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
