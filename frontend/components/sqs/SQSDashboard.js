'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { Activity, AlertTriangle, Shield, Wifi, Globe, Network, RefreshCw, Clock } from 'lucide-react'
import {
  fetchDashboardSummary,
  fetchTimeline,
  fetchRecentAlerts,
  fetchTopAttackers,
  fetchAlertStats,
  fetchDdosStats,
  fetchHttpStats,
  fetchFlowStats,
  fetchEtAlertStats,
  fetchRecentEtAlerts,
} from '@/utils/sqsActions'
import OverviewTab from './tabs/OverviewTab'
import AlertsTab from './tabs/AlertsTab'
import DdosTab from './tabs/DdosTab'
import HttpTab from './tabs/HttpTab'
import NetworkTab from './tabs/NetworkTab'

const TIME_RANGES = [
  { label: '1h', value: 1 },
  { label: '6h', value: 6 },
  { label: '24h', value: 24 },
  { label: '7d', value: 168 },
  { label: '30d', value: 720 },
]

const REFRESH_OPTIONS = [
  { label: 'Off', value: 0 },
  { label: '10s', value: 10 },
  { label: '30s', value: 30 },
  { label: '1m', value: 60 },
  { label: '5m', value: 300 },
]

const TABS = [
  { key: 'overview', label: 'Overview', icon: Activity },
  { key: 'alerts', label: 'Alerts', icon: AlertTriangle },
  { key: 'ddos', label: 'DDoS', icon: Shield },
  { key: 'http', label: 'HTTP', icon: Globe },
  { key: 'network', label: 'Network', icon: Network },
]

const SQSDashboard = () => {
  const [hours, setHours] = useState(24)
  const [refreshInterval, setRefreshInterval] = useState(60)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)
  const [activeTab, setActiveTab] = useState('overview')
  const [data, setData] = useState({
    summary: null,
    timeline: null,
    recentAlerts: null,
    topAttackers: null,
    alertStats: null,
    ddosStats: null,
    httpStats: null,
    flowStats: null,
    etAlertStats: null,
    recentEtAlerts: null,
  })

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // Check health first
      const healthRes = await fetch(`${process.env.NEXT_PUBLIC_SQS_API_URL || 'http://localhost:8000'}/health`).catch(() => null)
      if (!healthRes || !healthRes.ok) {
        throw new Error('Cannot reach SQS backend')
      }

      // Fetch all data — use allSettled so partial failures don't block everything
      const results = await Promise.allSettled([
        fetchDashboardSummary(hours),
        fetchTimeline(hours),
        fetchRecentAlerts(30),
        fetchTopAttackers(hours, 10),
        fetchAlertStats(hours),
        fetchDdosStats(hours),
        fetchHttpStats(hours),
        fetchFlowStats(hours),
        fetchEtAlertStats(hours),
        fetchRecentEtAlerts(30),
      ])

      const vals = results.map(r => r.status === 'fulfilled' ? r.value : null)
      const [summary, timeline, recentAlerts, topAttackers, alertStats, ddosStats, httpStats, flowStats, etAlertStats, recentEtAlerts] = vals

      setData({ summary, timeline, recentAlerts, topAttackers, alertStats, ddosStats, httpStats, flowStats, etAlertStats, recentEtAlerts })
      setLastUpdated(new Date())

      // Warn if some endpoints failed (e.g. OpenSearch down)
      const failures = results.filter(r => r.status === 'rejected')
      if (failures.length === results.length) {
        setError('Backend is reachable but all data queries failed — OpenSearch may not be running')
      } else if (failures.length > 0) {
        setError(`${failures.length} of ${results.length} data queries failed — some data may be missing`)
      }
    } catch (err) {
      setError(err.message || 'Failed to load data from SQS backend')
    } finally {
      setLoading(false)
    }
  }, [hours])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    if (refreshInterval === 0) return
    const interval = setInterval(loadData, refreshInterval * 1000)
    return () => clearInterval(interval)
  }, [loadData, refreshInterval])

  const ActiveTabComponent = TABS.find(t => t.key === activeTab)?.icon

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-cyan-100 rounded-xl">
            <Wifi className="h-8 w-8 text-cyan-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Botnet Detection</h1>
            <p className="text-sm text-slate-500 mt-1">MIRAI Botnet Detection & Network Monitoring</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Time Range */}
          <div className="flex items-center gap-1 bg-white border border-slate-200 rounded-lg p-1">
            {TIME_RANGES.map(tr => (
              <button
                key={tr.value}
                onClick={() => setHours(tr.value)}
                className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
                  hours === tr.value ? 'bg-cyan-600 text-white' : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                {tr.label}
              </button>
            ))}
          </div>

          {/* Refresh */}
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-slate-400" />
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(Number(e.target.value))}
              className="bg-white border border-slate-200 text-slate-700 px-2 py-1.5 rounded-lg text-sm"
            >
              {REFRESH_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <button
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-2">
        <div className="flex gap-2">
          {TABS.map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors text-sm ${
                activeTab === tab.key
                  ? 'bg-cyan-600 text-white'
                  : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.label}
            </button>
          ))}
        </div>
        {lastUpdated && (
          <span className="text-xs text-slate-500">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </span>
        )}
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <div>
            <p className="font-medium">Connection Error</p>
            <p className="text-sm">{error}</p>
            <p className="text-xs mt-1 text-red-500">Make sure the SQS backend is running on {process.env.NEXT_PUBLIC_SQS_API_URL || 'http://localhost:8000'}</p>
          </div>
        </div>
      )}

      {/* Tab Content */}
      {activeTab === 'overview' && <OverviewTab data={data} />}
      {activeTab === 'alerts' && <AlertsTab data={data} />}
      {activeTab === 'ddos' && <DdosTab data={data} />}
      {activeTab === 'http' && <HttpTab data={data} />}
      {activeTab === 'network' && <NetworkTab data={data} />}
    </div>
  )
}

export default SQSDashboard
