'use client'

import React, { useState, useEffect, useRef } from 'react'
import { Activity, Loader2, AlertCircle, RefreshCw } from 'lucide-react'
import { fetchSuricataDecoderStats, fetchSuricataPerInstanceStats } from '@/utils/dtmadActions'

function formatBytes(bytes) {
  const n = Number(bytes)
  if (n >= 1e9) return (n / 1e9).toFixed(2) + ' GB'
  if (n >= 1e6) return (n / 1e6).toFixed(1) + ' MB'
  if (n >= 1e3) return (n / 1e3).toFixed(1) + ' KB'
  return n + ' B'
}

function formatCount(val) {
  const n = Number(val)
  if (n >= 1e6) return (n / 1e6).toFixed(2) + 'M'
  if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K'
  return String(n)
}

function StatCard({ label, value, sub, color = 'teal' }) {
  const colors = {
    teal: 'bg-teal-50 border-teal-200 text-teal-700',
    blue: 'bg-blue-50 border-blue-200 text-blue-700',
    purple: 'bg-purple-50 border-purple-200 text-purple-700',
    amber: 'bg-amber-50 border-amber-200 text-amber-700',
    emerald: 'bg-emerald-50 border-emerald-200 text-emerald-700',
    slate: 'bg-slate-50 border-slate-200 text-slate-700',
  }
  return (
    <div className={`rounded-xl border p-4 ${colors[color] || colors.teal}`}>
      <p className="text-xs font-medium uppercase tracking-wide opacity-70">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
      {sub && <p className="text-xs mt-1 opacity-60">{sub}</p>}
    </div>
  )
}

export default function TrafficTab() {
  const [stats, setStats] = useState(null)
  const [perInstance, setPerInstance] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const intervalRef = useRef(null)

  const loadStats = async () => {
    try {
      const [decoder, instances] = await Promise.all([
        fetchSuricataDecoderStats(),
        fetchSuricataPerInstanceStats().catch(() => null),
      ])
      setStats(decoder)
      setPerInstance(instances)
      setError(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadStats()
  }, [])

  useEffect(() => {
    if (autoRefresh) {
      intervalRef.current = setInterval(loadStats, 10000)
    }
    return () => clearInterval(intervalRef.current)
  }, [autoRefresh])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="w-12 h-12 text-amber-400 mx-auto mb-4" />
        <p className="text-slate-500">Failed to load traffic statistics</p>
        <p className="text-xs text-slate-400 mt-1">{error}</p>
      </div>
    )
  }

  if (!stats || Object.keys(stats).length === 0) {
    return (
      <div className="text-center py-12">
        <Activity className="w-12 h-12 text-slate-300 mx-auto mb-4" />
        <p className="text-slate-500">No traffic data yet</p>
        <p className="text-xs text-slate-400 mt-1">Waiting for Suricata to capture packets...</p>
      </div>
    )
  }

  const totalPkts = Number(stats.pkts || 0)
  const tcpPkts = Number(stats.tcp || 0)
  const udpPkts = Number(stats.udp || 0)
  const ipv4Pkts = Number(stats.ipv4 || 0)
  const ipv6Pkts = Number(stats.ipv6 || 0)
  const tcpPct = totalPkts > 0 ? ((tcpPkts / totalPkts) * 100).toFixed(1) : '0'
  const udpPct = totalPkts > 0 ? ((udpPkts / totalPkts) * 100).toFixed(1) : '0'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-slate-900 flex items-center gap-2">
          <Activity className="w-5 h-5 text-teal-600" />
          Live Traffic Statistics
        </h3>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded border-slate-300 text-teal-600 focus:ring-teal-500"
            />
            Auto-refresh (10s)
          </label>
          <button
            onClick={loadStats}
            className="flex items-center gap-1 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-slate-50"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Refresh
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <StatCard label="Total Packets" value={formatCount(stats.pkts)} sub={`${stats.ethernet || stats.pkts} frames`} color="teal" />
        <StatCard label="Total Bytes" value={formatBytes(stats.bytes)} sub={`avg ${stats.avg_pkt_size} B/pkt`} color="blue" />
        <StatCard label="TCP" value={formatCount(stats.tcp)} sub={`${tcpPct}% of traffic`} color="purple" />
        <StatCard label="UDP" value={formatCount(stats.udp)} sub={`${udpPct}% of traffic`} color="amber" />
        <StatCard label="IPv4" value={formatCount(stats.ipv4)} color="emerald" />
        <StatCard label="IPv6" value={formatCount(stats.ipv6)} color="slate" />
      </div>

      {/* Protocol Breakdown Bar */}
      <div className="bg-white border border-slate-200 rounded-xl p-4">
        <h4 className="text-sm font-semibold text-slate-700 mb-3">Protocol Distribution</h4>
        <div className="w-full h-6 bg-slate-100 rounded-full overflow-hidden flex">
          {tcpPkts > 0 && (
            <div
              className="bg-purple-400 h-full flex items-center justify-center text-[10px] text-white font-medium"
              style={{ width: `${tcpPct}%` }}
            >
              {Number(tcpPct) > 8 ? `TCP ${tcpPct}%` : ''}
            </div>
          )}
          {udpPkts > 0 && (
            <div
              className="bg-amber-400 h-full flex items-center justify-center text-[10px] text-white font-medium"
              style={{ width: `${udpPct}%` }}
            >
              {Number(udpPct) > 8 ? `UDP ${udpPct}%` : ''}
            </div>
          )}
          {totalPkts > tcpPkts + udpPkts && (
            <div
              className="bg-slate-300 h-full flex items-center justify-center text-[10px] text-slate-600 font-medium"
              style={{ width: `${(100 - Number(tcpPct) - Number(udpPct)).toFixed(1)}%` }}
            >
              Other
            </div>
          )}
        </div>
        <div className="flex gap-4 mt-2 text-xs text-slate-500">
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded bg-purple-400 inline-block" /> TCP</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded bg-amber-400 inline-block" /> UDP</span>
          <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded bg-slate-300 inline-block" /> Other</span>
        </div>
      </div>

      {/* Per-Instance Table */}
      {perInstance && Object.keys(perInstance).length > 0 && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="px-4 py-3 bg-slate-50 border-b border-slate-200">
            <h4 className="text-sm font-semibold text-slate-700">Per-Instance Breakdown</h4>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50">
                <tr>
                  <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">Instance</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">Packets</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">Bytes</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">TCP</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">UDP</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">IPv4</th>
                  <th className="py-2 px-4 text-right text-xs font-medium text-slate-500 uppercase">IPv6</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {Object.entries(perInstance).map(([name, s]) => (
                  <tr key={name} className="hover:bg-slate-50">
                    <td className="py-2 px-4 text-sm font-medium text-slate-900">{name}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatCount(s.pkts)}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatBytes(s.bytes)}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatCount(s.tcp)}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatCount(s.udp)}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatCount(s.ipv4)}</td>
                    <td className="py-2 px-4 text-sm text-right text-slate-600 font-mono">{formatCount(s.ipv6)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
