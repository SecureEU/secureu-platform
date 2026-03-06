'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { Server, AlertTriangle, Monitor, Settings, Activity, RefreshCw, Loader2 } from 'lucide-react'
import { fetchDTMInstances } from '@/utils/dtmadActions'
import InstancesTab from './tabs/InstancesTab'
import DTMADAlerts from './tabs/AlertsTab'
import TrafficTab from './tabs/TrafficTab'
import AssetDiscoveryTab from './tabs/AssetDiscoveryTab'
import ADConfigTab from './tabs/ADConfigTab'

const TABS = [
  { key: 'traffic', label: 'Live Traffic', icon: Activity },
  { key: 'alerts', label: 'Alerts', icon: AlertTriangle },
  { key: 'instances', label: 'Instances', icon: Server },
  { key: 'assets', label: 'Asset Discovery', icon: Monitor },
  { key: 'adconfig', label: 'AD Config', icon: Settings },
]

const DTMADDashboard = () => {
  const [activeTab, setActiveTab] = useState('traffic')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [instances, setInstances] = useState([])

  const loadInstances = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchDTMInstances()
      setInstances(Array.isArray(result) ? result : [])
    } catch {
      // Backend not reachable — show error but don't block the UI
      setError('Cannot reach DTM backend. The Spring Boot backends (DTM on port 8087, AD on port 5001) need to be running with their full infrastructure (Kafka, PostgreSQL).')
      setInstances([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadInstances()
  }, [loadInstances])

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-teal-100 rounded-xl">
            <Server className="h-8 w-8 text-teal-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">DTM & AD</h1>
            <p className="text-sm text-slate-500 mt-1">Data Traffic Monitoring & Anomaly Detection</p>
          </div>
        </div>

        <button
          onClick={loadInstances}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200 pb-2">
        {TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-lg flex items-center gap-2 transition-colors text-sm ${
              activeTab === tab.key
                ? 'bg-teal-600 text-white'
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
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <div>
            <p className="font-medium">Connection Error</p>
            <p className="text-sm">{error}</p>
            <p className="text-xs mt-1 text-red-500">
              DTM backend: {process.env.NEXT_PUBLIC_DTM_API_URL || 'http://localhost:8087'} |
              AD backend: {process.env.NEXT_PUBLIC_AD_API_URL || 'http://localhost:5001'}
            </p>
          </div>
        </div>
      )}

      {/* Tab Content */}
      {activeTab === 'traffic' && <TrafficTab />}
      {activeTab === 'alerts' && <DTMADAlerts />}
      {activeTab === 'instances' && (
        loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
          </div>
        ) : (
          <InstancesTab instances={instances} onRefresh={loadInstances} />
        )
      )}
      {activeTab === 'assets' && <AssetDiscoveryTab />}
      {activeTab === 'adconfig' && <ADConfigTab />}
    </div>
  )
}

export default DTMADDashboard
