'use client'

import React, { useState } from 'react'
import { Server, Power, PowerOff, Activity, Loader2, ChevronDown, ChevronRight, Play, Square, Shield, Wifi, Plus, Trash2, X } from 'lucide-react'
import { toggleDTMInstance, deleteDTMInstance, saveInstance, fetchTsharkProcesses, startTsharkProcess, stopTsharkProcess } from '@/utils/dtmadActions'

export default function InstancesTab({ instances, onRefresh }) {
  const [expandedInstance, setExpandedInstance] = useState(null)
  const [processes, setProcesses] = useState({})
  const [actionLoading, setActionLoading] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', key: '', description: '', url: '', hasTshark: false, hasSuricata: false, isMaster: false })

  const handleToggle = async (id) => {
    setActionLoading(`toggle-${id}`)
    try {
      await toggleDTMInstance(id)
      onRefresh()
    } catch (err) {
      console.error('Toggle failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handleSaveInstance = async () => {
    if (!form.name || !form.url) return
    setActionLoading('save-instance')
    try {
      await saveInstance(form)
      setShowForm(false)
      setForm({ name: '', key: '', description: '', url: '', hasTshark: false, hasSuricata: false, isMaster: false })
      onRefresh()
    } catch (err) {
      console.error('Save instance failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this instance?')) return
    setActionLoading(`delete-${id}`)
    try {
      await deleteDTMInstance(id)
      onRefresh()
    } catch (err) {
      console.error('Delete failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handleExpand = async (instance) => {
    const id = instance.id
    if (expandedInstance === id) {
      setExpandedInstance(null)
      return
    }
    setExpandedInstance(id)
    if (!processes[id]) {
      try {
        const procs = await fetchTsharkProcesses(id)
        setProcesses(prev => ({ ...prev, [id]: Array.isArray(procs) ? procs : [] }))
      } catch {
        setProcesses(prev => ({ ...prev, [id]: [] }))
      }
    }
  }

  const handleStartProcess = async (processId, instanceId) => {
    setActionLoading(`start-${processId}`)
    try {
      await startTsharkProcess(processId, instanceId)
      const procs = await fetchTsharkProcesses(instanceId)
      setProcesses(prev => ({ ...prev, [instanceId]: procs }))
    } catch (err) {
      console.error('Start failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handleStopProcess = async (processId, instanceId) => {
    setActionLoading(`stop-${processId}`)
    try {
      await stopTsharkProcess(processId, instanceId)
      const procs = await fetchTsharkProcesses(instanceId)
      setProcesses(prev => ({ ...prev, [instanceId]: procs }))
    } catch (err) {
      console.error('Stop failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  if (!instances?.length && !showForm) {
    return (
      <div className="text-center py-12">
        <Server className="w-12 h-12 text-slate-300 mx-auto mb-4" />
        <p className="text-slate-500">No instances found</p>
        <p className="text-xs text-slate-400 mt-1">Make sure the DTM backend is running</p>
        <button onClick={() => setShowForm(true)} className="mt-4 px-4 py-2 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700 inline-flex items-center gap-2">
          <Plus className="w-4 h-4" /> Add Instance
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-slate-900">DTM Instances ({instances?.length || 0})</h3>
        <button onClick={() => setShowForm(!showForm)} className="px-3 py-1.5 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 inline-flex items-center gap-1.5">
          {showForm ? <X className="w-3.5 h-3.5" /> : <Plus className="w-3.5 h-3.5" />}
          {showForm ? 'Cancel' : 'Add Instance'}
        </button>
      </div>

      {showForm && (
        <div className="bg-white border border-teal-200 rounded-xl p-4 mb-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="Name *" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
            <input value={form.key} onChange={e => setForm(f => ({ ...f, key: e.target.value }))} placeholder="Key (e.g. local-001)" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
            <input value={form.url} onChange={e => setForm(f => ({ ...f, url: e.target.value }))} placeholder="URL * (e.g. http://localhost:8087)" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
            <input value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} placeholder="Description" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
          </div>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={form.hasTshark} onChange={e => setForm(f => ({ ...f, hasTshark: e.target.checked }))} className="rounded" /> Tshark
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={form.hasSuricata} onChange={e => setForm(f => ({ ...f, hasSuricata: e.target.checked }))} className="rounded" /> Suricata
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={form.isMaster} onChange={e => setForm(f => ({ ...f, isMaster: e.target.checked }))} className="rounded" /> Master
            </label>
            <button onClick={handleSaveInstance} disabled={actionLoading === 'save-instance' || !form.name || !form.url} className="ml-auto px-4 py-2 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700 disabled:opacity-50 inline-flex items-center gap-2">
              {actionLoading === 'save-instance' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Save
            </button>
          </div>
        </div>
      )}

      <div className="space-y-3">
        {instances.map((instance) => (
          <div key={instance.id} className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <div
              className="flex items-center justify-between p-4 cursor-pointer hover:bg-slate-50"
              onClick={() => handleExpand(instance)}
            >
              <div className="flex items-center gap-3">
                {expandedInstance === instance.id ? (
                  <ChevronDown className="w-4 h-4 text-slate-400" />
                ) : (
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                )}
                {/* Up/down indicator */}
                <div className={`w-3 h-3 rounded-full ${instance.up ? 'bg-emerald-500' : instance.enabled ? 'bg-amber-400' : 'bg-slate-300'}`} />
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-slate-900">{instance.name || `Instance ${instance.id}`}</p>
                    {instance.isMaster && (
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-teal-100 text-teal-700 uppercase">Master</span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500">{instance.description || instance.url || 'No description'}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-[10px] font-mono text-slate-400">{instance.key || ''}</span>
                    <span className="text-[10px] text-slate-400">{instance.url || ''}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {/* Capability badges */}
                {instance.hasSuricata && (
                  <span className="flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-purple-100 text-purple-700">
                    <Shield className="w-3 h-3" /> Suricata
                  </span>
                )}
                {instance.hasTshark && (
                  <span className="flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-blue-100 text-blue-700">
                    <Wifi className="w-3 h-3" /> Tshark
                  </span>
                )}

                {/* Status */}
                <span className={`px-2 py-1 rounded text-xs font-medium ${
                  instance.up ? 'bg-emerald-100 text-emerald-800'
                    : instance.enabled ? 'bg-amber-100 text-amber-800'
                    : 'bg-slate-100 text-slate-600'
                }`}>
                  {instance.up ? 'Online' : instance.enabled ? 'Enabled' : 'Disabled'}
                </span>

                <button
                  onClick={(e) => { e.stopPropagation(); handleToggle(instance.id); }}
                  disabled={actionLoading === `toggle-${instance.id}`}
                  className={`p-2 rounded-lg transition-colors ${
                    instance.enabled
                      ? 'text-red-600 hover:bg-red-50'
                      : 'text-emerald-600 hover:bg-emerald-50'
                  }`}
                >
                  {actionLoading === `toggle-${instance.id}` ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : instance.enabled ? (
                    <PowerOff className="w-4 h-4" />
                  ) : (
                    <Power className="w-4 h-4" />
                  )}
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); handleDelete(instance.id); }}
                  disabled={actionLoading === `delete-${instance.id}`}
                  className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                >
                  {actionLoading === `delete-${instance.id}` ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Trash2 className="w-4 h-4" />
                  )}
                </button>
              </div>
            </div>

            {/* Expanded: Show processes */}
            {expandedInstance === instance.id && (
              <div className="border-t border-slate-200 bg-slate-50 p-4">
                <h4 className="text-sm font-semibold text-slate-700 mb-3 flex items-center gap-2">
                  <Activity className="w-4 h-4" />
                  Tshark Processes
                </h4>
                {processes[instance.id]?.length > 0 ? (
                  <div className="space-y-2">
                    {processes[instance.id].map((proc) => (
                      <div key={proc.id || proc.pid} className="flex items-center justify-between bg-white rounded-lg p-3 border border-slate-200">
                        <div>
                          <p className="text-sm font-medium text-slate-800">{proc.name || proc.description || `Process ${proc.id || proc.pid}`}</p>
                          <p className="text-xs text-slate-500">
                            {proc.interface && `Interface: ${proc.interface}`}
                            {proc.filter && ` | Filter: ${proc.filter}`}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded text-xs ${
                            proc.active || proc.running ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'
                          }`}>
                            {proc.active || proc.running ? 'Running' : 'Stopped'}
                          </span>
                          {proc.active || proc.running ? (
                            <button
                              onClick={() => handleStopProcess(proc.id || proc.pid, instance.id)}
                              disabled={actionLoading === `stop-${proc.id || proc.pid}`}
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded"
                            >
                              {actionLoading === `stop-${proc.id || proc.pid}` ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Square className="w-3.5 h-3.5" />}
                            </button>
                          ) : (
                            <button
                              onClick={() => handleStartProcess(proc.id || proc.pid, instance.id)}
                              disabled={actionLoading === `start-${proc.id || proc.pid}`}
                              className="p-1.5 text-emerald-600 hover:bg-emerald-50 rounded"
                            >
                              {actionLoading === `start-${proc.id || proc.pid}` ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Play className="w-3.5 h-3.5" />}
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">No processes configured</p>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
