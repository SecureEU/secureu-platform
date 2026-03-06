'use client'

import React, { useState, useEffect } from 'react'
import { AlertTriangle, ChevronLeft, ChevronRight, ChevronDown, ChevronUp } from 'lucide-react'
import { fetchDTMAlerts } from '@/utils/dtmadActions'

const severityMap = {
  '1': { label: 'Critical', style: 'bg-red-100 text-red-800' },
  '2': { label: 'Major', style: 'bg-amber-100 text-amber-800' },
  '3': { label: 'Minor', style: 'bg-blue-100 text-blue-800' },
  Critical: { label: 'Critical', style: 'bg-red-100 text-red-800' },
  Major: { label: 'Major', style: 'bg-amber-100 text-amber-800' },
  Minor: { label: 'Minor', style: 'bg-blue-100 text-blue-800' },
}

const actionStyles = {
  blocked: 'bg-red-100 text-red-700',
  allowed: 'bg-emerald-100 text-emerald-700',
}

export default function DTMADAlerts() {
  const [alerts, setAlerts] = useState([])
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [expanded, setExpanded] = useState(null)
  const pageSize = 10

  useEffect(() => {
    loadAlerts()
  }, [page])

  const loadAlerts = async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchDTMAlerts(page, pageSize)
      if (result?.content) {
        setAlerts(result.content)
        setTotal(result.totalElements || 0)
      } else if (Array.isArray(result)) {
        setAlerts(result)
        setTotal(result.length)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize))

  if (error) {
    return (
      <div className="text-center py-12">
        <AlertTriangle className="w-12 h-12 text-amber-400 mx-auto mb-4" />
        <p className="text-slate-500">Failed to load alerts</p>
        <p className="text-xs text-slate-400 mt-1">{error}</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-slate-900">DTM Alerts ({total})</h3>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50">
              <tr>
                <th className="py-3 px-3 w-8"></th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Time</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Signature</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Severity</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Source</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Destination</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Proto</th>
                <th className="py-3 px-3 text-left text-xs font-medium text-slate-500 uppercase">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500">Loading...</td>
                </tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500">No alerts found</td>
                </tr>
              ) : (
                alerts.map((alert, idx) => {
                  const sev = severityMap[alert.severity] || {}
                  const isExpanded = expanded === alert.id
                  return (
                    <React.Fragment key={alert.id || idx}>
                      <tr
                        className="hover:bg-slate-50 cursor-pointer"
                        onClick={() => setExpanded(isExpanded ? null : alert.id)}
                      >
                        <td className="py-2 px-3 text-slate-400">
                          {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                        </td>
                        <td className="py-2 px-3 text-xs text-slate-500 whitespace-nowrap">{alert.timestamp || alert.createdDate || '-'}</td>
                        <td className="py-2 px-3 text-sm text-slate-800 max-w-xs truncate font-medium">{alert.signature || '-'}</td>
                        <td className="py-2 px-3">
                          <span className={`px-2 py-0.5 rounded text-xs font-medium ${sev.style || 'bg-slate-100 text-slate-600'}`}>
                            {sev.label || alert.severity || '?'}
                          </span>
                        </td>
                        <td className="py-2 px-3 text-xs font-mono text-slate-600 whitespace-nowrap">
                          {alert.srcIp || '-'}{alert.srcPort ? `:${alert.srcPort}` : ''}
                        </td>
                        <td className="py-2 px-3 text-xs font-mono text-slate-600 whitespace-nowrap">
                          {alert.destIp || '-'}{alert.destPort ? `:${alert.destPort}` : ''}
                        </td>
                        <td className="py-2 px-3 text-xs text-slate-600">{alert.protocol || '-'}</td>
                        <td className="py-2 px-3">
                          {alert.action && (
                            <span className={`px-2 py-0.5 rounded text-xs font-medium ${actionStyles[alert.action] || 'bg-slate-100 text-slate-600'}`}>
                              {alert.action}
                            </span>
                          )}
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr className="bg-slate-50">
                          <td colSpan={8} className="px-6 py-3">
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                              <div>
                                <span className="text-slate-400 uppercase font-medium">Category</span>
                                <p className="text-slate-700 mt-0.5">{alert.category || '-'}</p>
                              </div>
                              <div>
                                <span className="text-slate-400 uppercase font-medium">Host</span>
                                <p className="text-slate-700 mt-0.5 font-mono">{alert.host || '-'}</p>
                              </div>
                              <div>
                                <span className="text-slate-400 uppercase font-medium">Tool</span>
                                <p className="text-slate-700 mt-0.5">{alert.sphinxTool || '-'}</p>
                              </div>
                              <div>
                                <span className="text-slate-400 uppercase font-medium">Count</span>
                                <p className="text-slate-700 mt-0.5">{alert.count || 1}</p>
                              </div>
                              {alert.details && (
                                <div className="col-span-2 md:col-span-4">
                                  <span className="text-slate-400 uppercase font-medium">Details</span>
                                  <p className="text-slate-700 mt-0.5">{alert.details}</p>
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50">
          <p className="text-sm text-slate-600">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="flex items-center gap-1 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-white disabled:opacity-50"
            >
              <ChevronLeft className="w-4 h-4" /> Previous
            </button>
            <button
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="flex items-center gap-1 px-3 py-1.5 text-sm border border-slate-300 rounded-lg hover:bg-white disabled:opacity-50"
            >
              Next <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
