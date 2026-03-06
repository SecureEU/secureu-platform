'use client'

import React, { useState, useEffect, useRef, useCallback } from 'react'
import {
  AlertTriangle,
  Search,
  Filter,
  RefreshCw,
  Pause,
  Play,
  Download,
  X,
  ChevronRight,
  ChevronDown,
  Database,
  Clock,
  Server,
  FileText,
  Info,
  AlertCircle,
  Skull,
  Activity,
  BarChart3,
  PieChart,
  TrendingUp
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart as RePieChart,
  Pie,
  Cell,
  Legend
} from 'recharts'

// Use local proxy to avoid CORS issues
const API_URL = '/api/redflags'

const getSeverityStyle = (severity) => {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL': return { bg: 'bg-red-100', text: 'text-red-700', border: 'border-red-300', icon: Skull }
    case 'HIGH': return { bg: 'bg-orange-100', text: 'text-orange-700', border: 'border-orange-300', icon: AlertTriangle }
    case 'MEDIUM': return { bg: 'bg-yellow-100', text: 'text-yellow-700', border: 'border-yellow-300', icon: AlertCircle }
    case 'LOW': return { bg: 'bg-blue-100', text: 'text-blue-700', border: 'border-blue-300', icon: Info }
    case 'INFO': return { bg: 'bg-cyan-100', text: 'text-cyan-700', border: 'border-cyan-300', icon: Info }
    default: return { bg: 'bg-gray-100', text: 'text-gray-700', border: 'border-gray-300', icon: Info }
  }
}

const SEVERITY_COLORS = {
  CRITICAL: '#dc2626',
  HIGH: '#ea580c',
  MEDIUM: '#ca8a04',
  LOW: '#2563eb',
  INFO: '#0891b2'
}

const LOG_TYPE_COLORS = ['#8b5cf6', '#3b82f6', '#10b981']

export default function RedFlagsDashboard() {
  const [activeTab, setActiveTab] = useState('logs')

  // Analyzed Logs State
  const [logs, setLogs] = useState([])
  const [search, setSearch] = useState('')
  const [severity, setSeverity] = useState('')
  const [logType, setLogType] = useState('')
  const [hours, setHours] = useState('')
  const [limit, setLimit] = useState(20)
  const [autoRefresh, setAutoRefresh] = useState(false)
  const [lastUpdate, setLastUpdate] = useState('never')
  const [newLogsCount, setNewLogsCount] = useState(0)
  const [selectedLog, setSelectedLog] = useState(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [logsLoading, setLogsLoading] = useState(false)
  const logsContainerRef = useRef(null)

  // Raw Logs State
  const [rawLogs, setRawLogs] = useState([])
  const [rawN, setRawN] = useState(100)
  const [rawLogType, setRawLogType] = useState('')
  const [rawSourceHost, setRawSourceHost] = useState('')
  const [availableHosts, setAvailableHosts] = useState([])
  const [expandedLog, setExpandedLog] = useState(null)
  const [rawViewMode, setRawViewMode] = useState('table')
  const [rawLoading, setRawLoading] = useState(false)
  const [rawLastUpdated, setRawLastUpdated] = useState('never')

  // Analytics State
  const [stats, setStats] = useState({})
  const [allTimeStats, setAllTimeStats] = useState({})
  const [timeRange, setTimeRange] = useState(24)
  const [analyticsLoading, setAnalyticsLoading] = useState(false)

  // Fetch analyzed logs (incidents)
  const fetchLogs = useCallback(async () => {
    setLogsLoading(true)
    try {
      let url = `${API_URL}?endpoint=incidents&limit=${limit}&offset=0`
      if (severity) url += `&severity=${severity}`
      if (logType) url += `&log_type=${logType}`
      if (hours) url += `&hours=${hours}`

      console.log('Fetching incidents from:', url)
      const response = await fetch(url)

      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const data = await response.json()

      const incidents = Array.isArray(data) ? data : (data.incidents || [])

      if (logs.length > 0 && incidents.length > 0) {
        const currentIds = new Set(logs.map(l => l.id))
        const newCount = incidents.filter(l => !currentIds.has(l.id)).length
        setNewLogsCount(newCount)
        setTimeout(() => setNewLogsCount(0), 10000)
      }

      setLogs(incidents)
      setLastUpdate(new Date().toLocaleTimeString())
    } catch (err) {
      console.error('Error fetching incidents:', err)
    } finally {
      setLogsLoading(false)
    }
  }, [limit, severity, logType, hours, logs.length])

  // Fetch raw logs
  const fetchRawLogs = useCallback(async () => {
    setRawLoading(true)
    try {
      let url = `${API_URL}?endpoint=raw-logs/recent&n=${rawN}`
      if (rawLogType) url += `&log_type=${rawLogType}`
      if (rawSourceHost) url += `&source_host=${rawSourceHost}`

      console.log('Fetching raw logs from:', url)
      const response = await fetch(url)

      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const data = await response.json()
      const rawLogsList = Array.isArray(data) ? data : (data.logs || [])
      setRawLogs(rawLogsList)
      setRawLastUpdated(new Date().toLocaleTimeString())
      setExpandedLog(null)

      // Extract unique hosts for filter dropdown
      if (rawLogsList.length > 0) {
        const hosts = [...new Set(rawLogsList.map(l => l.source_host).filter(Boolean))]
        if (hosts.length > 0) setAvailableHosts(hosts)
      }
    } catch (err) {
      console.error('Error fetching raw logs:', err)
    } finally {
      setRawLoading(false)
    }
  }, [rawN, rawLogType, rawSourceHost])

  // Fetch statistics
  const fetchStats = useCallback(async () => {
    setAnalyticsLoading(true)
    try {
      // Fetch stats for selected time range
      console.log('Fetching statistics from:', `${API_URL}?endpoint=statistics&hours=${timeRange}`)
      const response = await fetch(`${API_URL}?endpoint=statistics&hours=${timeRange}`)

      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const data = await response.json()
      setStats(data)

      // Fetch all-time stats (1 year)
      const allTimeResponse = await fetch(`${API_URL}?endpoint=statistics&hours=8760`)

      if (allTimeResponse.ok) {
        const allTimeData = await allTimeResponse.json()
        setAllTimeStats(allTimeData)
      }
    } catch (err) {
      console.error('Error fetching statistics:', err)
    } finally {
      setAnalyticsLoading(false)
    }
  }, [timeRange])

  // Initial load
  useEffect(() => {
    fetchLogs()
  }, [])

  // Fetch stats when analytics tab is active or time range changes
  useEffect(() => {
    if (activeTab === 'analytics') {
      fetchStats()
    }
  }, [activeTab, timeRange, fetchStats])

  // Auto-refresh for logs
  useEffect(() => {
    let interval
    if (autoRefresh && activeTab === 'logs') {
      interval = setInterval(fetchLogs, 5000)
    }
    return () => clearInterval(interval)
  }, [autoRefresh, activeTab, fetchLogs])

  const filteredLogs = logs.filter(log => {
    if (search && !log.raw_log_message?.toLowerCase().includes(search.toLowerCase())) return false
    return true
  })

  const formatTime = (timestamp) => {
    if (!timestamp) return ''
    return new Date(timestamp).toLocaleString()
  }

  const getTimeRangeLabel = () => {
    if (timeRange === 24) return '24 hours'
    if (timeRange === 168) return '7 days'
    if (timeRange === 720) return '30 days'
    if (timeRange === 8760) return '1 year'
    if (timeRange < 24) return `${timeRange} hours`
    if (timeRange < 168) return `${Math.round(timeRange / 24)} days`
    return `${Math.round(timeRange / 720)} months`
  }

  const exportRawLogsJSON = () => {
    const exportData = rawLogs.map(log => ({
      Message: log.message,
      'Log type': log.log_type,
      'Source Host': log.source_host,
      'Collection time': log.timestamp
    }))
    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `raw-logs-export-${new Date().toISOString()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  // Prepare chart data from stats
  const severityChartData = stats.by_severity
    ? Object.entries(stats.by_severity).map(([name, value]) => ({
        name,
        value,
        fill: SEVERITY_COLORS[name] || '#6b7280'
      }))
    : []

  const logTypeChartData = stats.by_log_type
    ? Object.entries(stats.by_log_type).map(([name, value], i) => ({
        name: name.charAt(0).toUpperCase() + name.slice(1),
        value,
        fill: LOG_TYPE_COLORS[i % LOG_TYPE_COLORS.length]
      }))
    : []

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Red Flags Dashboard</h1>
          <p className="text-gray-600 mt-1">Real-time log analysis and incident monitoring</p>
        </div>
        <div className="flex items-center gap-2">
          <div className={`px-3 py-1 rounded-full text-sm font-medium flex items-center gap-2 ${autoRefresh ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}`}>
            <div className={`w-2 h-2 rounded-full ${autoRefresh ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`} />
            {autoRefresh ? 'Live' : 'Paused'}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-1 border-b border-gray-200">
        {[
          { id: 'raw-logs', label: 'Pre-analysis Logs', icon: FileText },
          { id: 'logs', label: 'Analyzed Logs', icon: AlertTriangle },
          { id: 'analytics', label: 'Analytics', icon: BarChart3 }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium transition-colors ${
              activeTab === tab.id
                ? 'border-cyan-500 text-cyan-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            <tab.icon className="h-4 w-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Analyzed Logs Tab */}
      {activeTab === 'logs' && (
        <div className="space-y-4">
          {/* Filters */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search logs..."
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500"
                />
              </div>
            </div>
            <select
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
            >
              {[10, 20, 30, 40, 50, 60, 70, 80, 90, 100].map(n => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
            <select
              value={severity}
              onChange={(e) => setSeverity(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
            >
              <option value="">All Severities</option>
              {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'].map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
            <select
              value={logType}
              onChange={(e) => setLogType(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
            >
              <option value="">All Types</option>
              {['system', 'web', 'application'].map(t => (
                <option key={t} value={t}>{t.charAt(0).toUpperCase() + t.slice(1)}</option>
              ))}
            </select>
            <input
              type="number"
              value={hours}
              onChange={(e) => setHours(e.target.value)}
              min="1"
              placeholder="Hours"
              className="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
            />
            <button
              onClick={fetchLogs}
              disabled={logsLoading}
              className="px-4 py-2 bg-cyan-500 text-white rounded-lg hover:bg-cyan-600 disabled:bg-gray-300 transition-colors font-medium flex items-center gap-2"
            >
              {logsLoading && <RefreshCw className="h-4 w-4 animate-spin" />}
              Apply
            </button>
            <div className="flex items-center gap-2 ml-auto">
              <button
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`px-3 py-2 rounded-lg font-medium transition-colors ${
                  autoRefresh ? 'bg-red-500 text-white hover:bg-red-600' : 'bg-green-500 text-white hover:bg-green-600'
                }`}
              >
                {autoRefresh ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
              </button>
              <span className="text-xs text-gray-500">Updated {lastUpdate}</span>
            </div>
          </div>

          {/* New Logs Alert */}
          {newLogsCount > 0 && (
            <div className="bg-cyan-50 border border-cyan-200 rounded-lg p-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-cyan-500" />
                <span className="text-cyan-700">{newLogsCount} new log{newLogsCount > 1 ? 's' : ''} received</span>
              </div>
              <button
                onClick={() => logsContainerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })}
                className="px-3 py-1 bg-cyan-500 text-white rounded text-sm hover:bg-cyan-600 transition-colors"
              >
                View
              </button>
            </div>
          )}

          {/* Logs List */}
          <div ref={logsContainerRef} className="bg-white rounded-xl border border-gray-200 p-4 max-h-[500px] overflow-y-auto">
            {logsLoading && logs.length === 0 ? (
              <div className="text-center py-12 text-gray-500">
                <RefreshCw className="h-12 w-12 mx-auto mb-3 animate-spin" />
                <p>Loading logs...</p>
              </div>
            ) : filteredLogs.length > 0 ? (
              <div className="space-y-2">
                {filteredLogs.map((log, index) => {
                  const style = getSeverityStyle(log.severity)
                  const IconComponent = style.icon
                  return (
                    <div
                      key={log.id || index}
                      onClick={() => { setSelectedLog(log); setSidebarOpen(true) }}
                      className={`p-3 rounded-lg border cursor-pointer transition-colors hover:bg-gray-50 ${style.bg} ${style.border}`}
                    >
                      <div className="flex items-start gap-3">
                        <IconComponent className={`h-5 w-5 mt-0.5 ${style.text}`} />
                        <div className="flex-1 min-w-0">
                          <p className="text-gray-900 font-medium truncate">{log.raw_log_message}</p>
                          <p className="text-sm text-gray-500 mt-1">
                            Source: {log.source_host} | Severity: {log.severity} | {formatTime(log.event_timestamp)}
                          </p>
                        </div>
                        <ChevronRight className="h-5 w-5 text-cyan-500" />
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div className="text-center py-12 text-gray-500">
                <Search className="h-12 w-12 mx-auto mb-3 opacity-50" />
                <p>No logs found</p>
              </div>
            )}
          </div>

          {/* Sidebar */}
          {sidebarOpen && selectedLog && (
            <>
              <div className="fixed inset-0 bg-black/50 z-40" onClick={() => setSidebarOpen(false)} />
              <div className="fixed right-0 top-0 h-full w-full md:w-2/3 lg:w-1/2 bg-white shadow-2xl z-50 overflow-y-auto">
                <div className="p-6 space-y-6">
                  <div className="flex items-center justify-between border-b border-gray-200 pb-4">
                    <h2 className="text-xl font-bold text-cyan-600">Incident Details</h2>
                    <button onClick={() => setSidebarOpen(false)} className="text-gray-500 hover:text-gray-700">
                      <X className="h-6 w-6" />
                    </button>
                  </div>

                  <div className="space-y-4">
                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                      <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Basic Information</h3>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-gray-500">Incident ID:</span>
                          <span className="font-mono">{selectedLog.id}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-500">Created At:</span>
                          <span>{formatTime(selectedLog.created_at)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-500">Event Timestamp:</span>
                          <span>{formatTime(selectedLog.event_timestamp)}</span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                      <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Log Information</h3>
                      <div className="space-y-2">
                        <div className="flex justify-between items-center">
                          <span className="text-gray-500">Log Type:</span>
                          <span className="px-2 py-1 bg-blue-100 text-blue-700 text-sm rounded">{selectedLog.log_type}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-500">Source Host:</span>
                          <span className="px-2 py-1 bg-purple-100 text-purple-700 text-sm rounded">{selectedLog.source_host}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-gray-500">Severity:</span>
                          <span className={`px-2 py-1 text-sm rounded ${getSeverityStyle(selectedLog.severity).bg} ${getSeverityStyle(selectedLog.severity).text}`}>
                            {selectedLog.severity}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                      <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Raw Log Message</h3>
                      <p className="bg-gray-900 text-green-400 p-3 rounded font-mono text-sm break-words">
                        {selectedLog.raw_log_message}
                      </p>
                    </div>

                    {selectedLog.analysis_result && (
                      <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                        <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Analysis Result</h3>
                        <div className="space-y-3">
                          <div className="flex justify-between items-center">
                            <span className="text-gray-500">Event Type:</span>
                            <span className="px-2 py-1 bg-green-100 text-green-700 text-sm rounded capitalize">
                              {selectedLog.analysis_result.event_type}
                            </span>
                          </div>
                          {selectedLog.analysis_result.description && (
                            <div>
                              <span className="text-gray-500 block mb-2">Description:</span>
                              <p className="bg-white p-3 rounded text-sm border">{selectedLog.analysis_result.description}</p>
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                      <h3 className="text-sm font-semibold text-gray-500 uppercase mb-3">Full JSON Data</h3>
                      <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs font-mono overflow-x-auto max-h-64 overflow-y-auto">
                        {JSON.stringify(selectedLog, null, 2)}
                      </pre>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* Raw Logs Tab */}
      {activeTab === 'raw-logs' && (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Filters Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
              <h3 className="text-lg font-bold text-cyan-600 mb-4">Filters</h3>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Number of recent logs</label>
                <select
                  value={rawN}
                  onChange={(e) => setRawN(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
                >
                  {[20, 50, 100, 200, 500, 1000].map(n => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Log Type</label>
                <select
                  value={rawLogType}
                  onChange={(e) => setRawLogType(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
                >
                  <option value="">All Types</option>
                  <option value="system">System Logs</option>
                  <option value="web">Web Logs</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Source Host</label>
                <select
                  value={rawSourceHost}
                  onChange={(e) => setRawSourceHost(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
                >
                  <option value="">All Hosts</option>
                  {availableHosts.map(host => (
                    <option key={host} value={host}>{host}</option>
                  ))}
                </select>
              </div>

              <button
                onClick={fetchRawLogs}
                disabled={rawLoading}
                className="w-full px-4 py-2 bg-cyan-500 text-white rounded-lg hover:bg-cyan-600 disabled:bg-gray-300 transition-colors font-medium flex items-center justify-center gap-2"
              >
                {rawLoading ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Filter className="h-4 w-4" />}
                {rawLoading ? 'Loading...' : 'Apply Filters'}
              </button>

              <button
                onClick={exportRawLogsJSON}
                disabled={rawLogs.length === 0}
                className="w-full px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:bg-gray-300 transition-colors font-medium flex items-center justify-center gap-2"
              >
                <Download className="h-4 w-4" />
                Export JSON
              </button>

              <div className="pt-4 border-t border-gray-200">
                <p className="text-sm text-gray-500">Logs loaded: <span className="text-cyan-600 font-bold">{rawLogs.length}</span></p>
                <p className="text-sm text-gray-500 mt-1">Last updated: <span className="text-gray-700">{rawLastUpdated}</span></p>
              </div>
            </div>
          </div>

          {/* Logs Display */}
          <div className="lg:col-span-3">
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-gray-900">Raw Log Messages</h3>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setRawViewMode('table')}
                    className={`px-3 py-1 rounded-lg transition-colors ${rawViewMode === 'table' ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700'}`}
                  >
                    <FileText className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => setRawViewMode('json')}
                    className={`px-3 py-1 rounded-lg transition-colors ${rawViewMode === 'json' ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700'}`}
                  >
                    {'{ }'}
                  </button>
                </div>
              </div>

              {rawViewMode === 'table' ? (
                <div className="max-h-[600px] overflow-y-auto">
                  {rawLoading ? (
                    <div className="text-center py-12 text-gray-500">
                      <RefreshCw className="h-12 w-12 mx-auto mb-3 animate-spin" />
                      <p>Loading logs...</p>
                    </div>
                  ) : rawLogs.length > 0 ? (
                    <div className="space-y-2">
                      {rawLogs.map((log, index) => (
                        <div
                          key={index}
                          onClick={() => setExpandedLog(expandedLog === index ? null : index)}
                          className={`border border-gray-200 rounded-lg p-3 cursor-pointer transition-colors hover:bg-gray-50 ${expandedLog === index ? 'bg-cyan-50' : ''}`}
                        >
                          <div className="flex items-start gap-3">
                            {expandedLog === index ? (
                              <ChevronDown className="h-5 w-5 text-cyan-500 mt-0.5" />
                            ) : (
                              <ChevronRight className="h-5 w-5 text-cyan-500 mt-0.5" />
                            )}
                            <div className="flex-1">
                              <p className="text-gray-900 font-medium font-mono text-sm">{log.message}</p>

                              {expandedLog === index && (
                                <div className="mt-3 space-y-2 pl-4 border-l-2 border-cyan-400">
                                  <div className="flex items-center gap-2">
                                    <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded">{log.log_type}</span>
                                    <span className="px-2 py-1 bg-purple-100 text-purple-700 text-xs rounded">{log.source_host}</span>
                                  </div>
                                  <p className="text-gray-500 text-sm flex items-center gap-1">
                                    <FileText className="h-3 w-3" /> {log.file_path}
                                  </p>
                                  <p className="text-gray-500 text-sm flex items-center gap-1">
                                    <Clock className="h-3 w-3" /> {formatTime(log.timestamp)}
                                  </p>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-12 text-gray-500">
                      <Database className="h-12 w-12 mx-auto mb-3 opacity-50" />
                      <p>No logs loaded</p>
                      <p className="text-sm mt-2">Apply filters to fetch raw logs</p>
                    </div>
                  )}
                </div>
              ) : (
                <div className="max-h-[600px] overflow-y-auto">
                  <pre className="bg-gray-900 text-green-400 p-4 rounded-lg text-sm font-mono">
                    {JSON.stringify(rawLogs.map(log => ({
                      Message: log.message,
                      'Log type': log.log_type,
                      'Source Host': log.source_host,
                      'Collection time': log.timestamp
                    })), null, 2)}
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Analytics Tab */}
      {activeTab === 'analytics' && (
        <div className="space-y-6">
          {/* Time Range Filter */}
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div className="flex items-center gap-4">
                <label className="text-gray-700 font-medium">Time Range:</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    value={timeRange}
                    onChange={(e) => setTimeRange(Number(e.target.value))}
                    min="1"
                    max="8760"
                    className="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-cyan-500"
                  />
                  <span className="text-gray-500">hours</span>
                </div>
                <span className="text-cyan-600 font-medium">({getTimeRangeLabel()})</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-gray-500 text-sm">Quick select:</span>
                {[
                  { label: '24h', value: 24 },
                  { label: '7d', value: 168 },
                  { label: '30d', value: 720 },
                  { label: '1y', value: 8760 }
                ].map(({ label, value }) => (
                  <button
                    key={value}
                    onClick={() => setTimeRange(value)}
                    className={`px-3 py-1 rounded-lg text-sm transition-colors ${
                      timeRange === value ? 'bg-cyan-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {analyticsLoading ? (
            <div className="text-center py-12 text-gray-500">
              <RefreshCw className="h-12 w-12 mx-auto mb-3 animate-spin" />
              <p>Loading analytics...</p>
            </div>
          ) : (
            <>
              {/* Stats Cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-gradient-to-br from-cyan-500 to-cyan-600 rounded-xl p-6 text-white">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-medium opacity-80 uppercase tracking-wide">Total Incidents</h3>
                    <Database className="h-6 w-6 opacity-80" />
                  </div>
                  <p className="text-4xl font-bold">{allTimeStats.total_incidents || 0}</p>
                  <p className="text-xs opacity-70 mt-2">Last year (8760 hours)</p>
                </div>

                <div className="bg-gradient-to-br from-red-500 to-red-600 rounded-xl p-6 text-white">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-medium opacity-80 uppercase tracking-wide">Incidents Last {getTimeRangeLabel()}</h3>
                    <AlertTriangle className="h-6 w-6 opacity-80" />
                  </div>
                  <p className="text-4xl font-bold">{stats.total_incidents || 0}</p>
                  <p className="text-xs opacity-70 mt-2">In selected time range</p>
                </div>

                <div className="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-xl p-6 text-white">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-sm font-medium opacity-80 uppercase tracking-wide">Top Source Host</h3>
                    <Server className="h-6 w-6 opacity-80" />
                  </div>
                  <p className="text-2xl font-bold truncate">{stats.top_source_hosts?.[0]?.host || '-'}</p>
                  <p className="text-xs opacity-70 mt-2">
                    Count: {stats.top_source_hosts?.[0]?.count || 0} | Last {getTimeRangeLabel()}
                  </p>
                </div>
              </div>

              {/* Charts */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                    <BarChart3 className="h-5 w-5 text-cyan-500" />
                    Incidents by Severity
                    <span className="text-sm text-gray-500">(Last {getTimeRangeLabel()})</span>
                  </h3>
                  <div className="h-64">
                    {severityChartData.length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={severityChartData}>
                          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                          <XAxis dataKey="name" tick={{ fill: '#6b7280', fontSize: 12 }} />
                          <YAxis tick={{ fill: '#6b7280', fontSize: 12 }} />
                          <Tooltip />
                          <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                            {severityChartData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={entry.fill} />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="h-full flex items-center justify-center text-gray-500">No data available</div>
                    )}
                  </div>
                </div>

                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                    <PieChart className="h-5 w-5 text-cyan-500" />
                    Incidents by Log Type
                    <span className="text-sm text-gray-500">(Last {getTimeRangeLabel()})</span>
                  </h3>
                  <div className="h-64">
                    {logTypeChartData.length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <RePieChart>
                          <Pie
                            data={logTypeChartData}
                            cx="50%"
                            cy="50%"
                            outerRadius={80}
                            dataKey="value"
                            label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                          >
                            {logTypeChartData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={entry.fill} />
                            ))}
                          </Pie>
                          <Tooltip />
                          <Legend />
                        </RePieChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="h-full flex items-center justify-center text-gray-500">No data available</div>
                    )}
                  </div>
                </div>
              </div>

              {/* Top Source Hosts */}
              {stats.top_source_hosts && stats.top_source_hosts.length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-lg font-semibold mb-4 flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <TrendingUp className="h-5 w-5 text-cyan-500" />
                      Top Source Hosts
                    </span>
                    <span className="text-sm text-gray-500">Last {getTimeRangeLabel()}</span>
                  </h3>
                  <div className="space-y-3">
                    {stats.top_source_hosts.slice(0, 10).map((host, index) => (
                      <div key={host.host} className="flex items-center gap-4">
                        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-cyan-100 flex items-center justify-center border border-cyan-300">
                          <span className="text-sm font-bold text-cyan-700">{index + 1}</span>
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center justify-between mb-1">
                            <span className="font-medium">{host.host}</span>
                            <span className="text-gray-500 text-sm">{host.count} incidents</span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-gradient-to-r from-cyan-500 to-blue-500 h-2 rounded-full transition-all"
                              style={{ width: `${(host.count / (stats.top_source_hosts[0]?.count || 1)) * 100}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
