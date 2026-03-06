'use client'

import React, { useState, useEffect } from 'react'
import { Settings, Loader2, AlertCircle, Save, Play, FileText } from 'lucide-react'
import { fetchADAllConfig, saveADConfig, fetchADSimulations, executeADSimulation } from '@/utils/dtmadActions'

export default function ADConfigTab() {
  const [config, setConfig] = useState({})
  const [simulations, setSimulations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(null)
  const [simRunning, setSimRunning] = useState(null)
  const [simResult, setSimResult] = useState(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [configData, simData] = await Promise.all([
        fetchADAllConfig().catch(() => ({})),
        fetchADSimulations().catch(() => []),
      ])
      // Backend returns Map<String, ConfigModel> e.g. { "ad.algo.x": { id, code, value, ... } }
      setConfig(configData || {})
      setSimulations(Array.isArray(simData) ? simData : [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async (code, newValue) => {
    setSaving(code)
    try {
      const model = config[code]
      await saveADConfig({ ...model, value: newValue })
      setConfig(prev => ({
        ...prev,
        [code]: { ...prev[code], value: newValue },
      }))
    } catch (err) {
      console.error('Save failed:', err)
    } finally {
      setSaving(null)
    }
  }

  const handleRunSimulation = async (filename) => {
    setSimRunning(filename)
    setSimResult(null)
    try {
      const result = await executeADSimulation(filename)
      setSimResult({ file: filename, data: result })
    } catch (err) {
      setSimResult({ file: filename, error: err.message })
    } finally {
      setSimRunning(null)
    }
  }

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
        <p className="text-slate-500">Failed to load AD configuration</p>
        <p className="text-xs text-slate-400 mt-1">{error}</p>
      </div>
    )
  }

  // Group config by prefix — each entry is { code, value, id, ... }
  const groups = {}
  Object.entries(config).forEach(([code, model]) => {
    const prefix = code.split('.').slice(0, 2).join('.')
    if (!groups[prefix]) groups[prefix] = []
    groups[prefix].push({ code, value: model.value ?? '' })
  })

  return (
    <div className="space-y-6">
      {/* Algorithm Configuration */}
      <div>
        <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <Settings className="w-5 h-5" />
          Anomaly Detection Configuration
        </h3>

        {Object.keys(groups).length === 0 ? (
          <div className="text-center py-8 text-slate-500">
            <Settings className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            No configuration available
          </div>
        ) : (
          <div className="space-y-4">
            {Object.entries(groups).map(([prefix, items]) => (
              <div key={prefix} className="bg-white border border-slate-200 rounded-xl overflow-hidden">
                <div className="px-4 py-3 bg-slate-50 border-b border-slate-200">
                  <h4 className="text-sm font-semibold text-slate-700">{prefix}</h4>
                </div>
                <div className="divide-y divide-slate-100">
                  {items.map(({ code, value }) => (
                    <div key={code} className="flex items-center justify-between px-4 py-3">
                      <div className="flex-1 min-w-0 mr-4">
                        <p className="text-sm font-mono text-slate-700 truncate">{code}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          defaultValue={value}
                          className="w-48 px-3 py-1.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                          onBlur={(e) => {
                            if (e.target.value !== value) {
                              handleSave(code, e.target.value)
                            }
                          }}
                        />
                        {saving === code && <Loader2 className="w-4 h-4 animate-spin text-teal-500" />}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Simulations */}
      <div>
        <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <Play className="w-5 h-5" />
          Algorithm Simulations
        </h3>

        {simulations.length === 0 ? (
          <div className="text-center py-8 text-slate-500">
            <FileText className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            No simulations available
          </div>
        ) : (
          <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <div className="divide-y divide-slate-100">
              {simulations.map((sim, idx) => {
                const filename = typeof sim === 'string' ? sim : sim.name || sim.filename
                return (
                  <div key={idx} className="flex items-center justify-between px-4 py-3">
                    <div className="flex items-center gap-2">
                      <FileText className="w-4 h-4 text-slate-400" />
                      <span className="text-sm text-slate-700">{filename}</span>
                    </div>
                    <button
                      onClick={() => handleRunSimulation(filename)}
                      disabled={simRunning === filename}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm bg-teal-600 text-white rounded-lg hover:bg-teal-700 disabled:opacity-50"
                    >
                      {simRunning === filename ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Play className="w-3.5 h-3.5" />}
                      Run
                    </button>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {simResult && (
          <div className={`mt-4 p-4 rounded-lg border ${simResult.error ? 'bg-red-50 border-red-200' : 'bg-emerald-50 border-emerald-200'}`}>
            <p className="text-sm font-medium">{simResult.file}</p>
            <pre className="text-xs mt-2 whitespace-pre-wrap">
              {simResult.error || JSON.stringify(simResult.data, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  )
}
