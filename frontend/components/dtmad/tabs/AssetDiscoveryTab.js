'use client'

import React, { useState, useEffect } from 'react'
import { Monitor, AlertCircle, Loader2, Wifi, Server, Plus, Trash2, ArrowUpCircle, X } from 'lucide-react'
import { fetchAssetCatalogue, fetchAssetDiscoveryAlerts, saveAsset, deleteAsset } from '@/utils/dtmadActions'

const typeLabels = {
  1: { label: 'Known', style: 'bg-emerald-100 text-emerald-700' },
  2: { label: 'Discovered', style: 'bg-amber-100 text-amber-700' },
}

export default function AssetDiscoveryTab() {
  const [assets, setAssets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [actionLoading, setActionLoading] = useState(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [addForm, setAddForm] = useState({ name: '', description: '', ip: '', physicalAddress: '' })
  const [promoteId, setPromoteId] = useState(null)
  const [promoteForm, setPromoteForm] = useState({ name: '', description: '' })

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [catalogue, discovered] = await Promise.all([
        fetchAssetCatalogue().catch(() => []),
        fetchAssetDiscoveryAlerts().catch(() => []),
      ])
      const knownAssets = Array.isArray(catalogue) ? catalogue : []
      // Discovery alerts come as typeId=2 devices; dedupe by MAC against catalogue
      const knownMacs = new Set(knownAssets.map(a => a.physicalAddress?.toLowerCase()))
      const discoveredAssets = (Array.isArray(discovered) ? discovered : [])
        .filter(d => !knownMacs.has(d.physicalAddress?.toLowerCase()))
        .map(d => ({ ...d, typeId: 2 }))
      setAssets([...knownAssets, ...discoveredAssets])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this asset?')) return
    setActionLoading(`delete-${id}`)
    try {
      await deleteAsset(id)
      await loadData()
    } catch (err) {
      console.error('Delete asset failed:', err)
    } finally {
      setActionLoading(null)
    }
  }

  const handlePromote = async (asset) => {
    if (promoteId === asset.id) {
      // Submit promotion
      setActionLoading(`promote-${asset.id}`)
      try {
        await saveAsset({ ...asset, name: promoteForm.name, description: promoteForm.description, typeId: 1 })
        setPromoteId(null)
        setPromoteForm({ name: '', description: '' })
        await loadData()
      } catch (err) {
        console.error('Promote asset failed:', err)
      } finally {
        setActionLoading(null)
      }
    } else {
      setPromoteId(asset.id)
      setPromoteForm({ name: asset.name || '', description: asset.description || '' })
    }
  }

  const handleAddKnown = async () => {
    if (!addForm.name || !addForm.physicalAddress) return
    setActionLoading('add-asset')
    try {
      await saveAsset({ ...addForm, typeId: 1 })
      setShowAddForm(false)
      setAddForm({ name: '', description: '', ip: '', physicalAddress: '' })
      await loadData()
    } catch (err) {
      console.error('Add asset failed:', err)
    } finally {
      setActionLoading(null)
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
        <p className="text-slate-500">Failed to load asset data</p>
        <p className="text-xs text-slate-400 mt-1">{error}</p>
      </div>
    )
  }

  const knownAssets = assets.filter(a => a.typeId === 1)
  const discoveredAssets = assets.filter(a => a.typeId !== 1)

  return (
    <div className="space-y-6">
      {/* Summary */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <p className="text-xs font-medium text-slate-500 uppercase">Total Assets</p>
          <p className="text-2xl font-bold text-slate-900 mt-1">{assets.length}</p>
        </div>
        <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4">
          <p className="text-xs font-medium text-emerald-600 uppercase">Known</p>
          <p className="text-2xl font-bold text-emerald-700 mt-1">{knownAssets.length}</p>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
          <p className="text-xs font-medium text-amber-600 uppercase">Auto-Discovered</p>
          <p className="text-2xl font-bold text-amber-700 mt-1">{discoveredAssets.length}</p>
        </div>
      </div>

      {/* Known Assets */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-slate-900 flex items-center gap-2">
            <Server className="w-4 h-4 text-emerald-600" />
            Known Assets ({knownAssets.length})
          </h3>
          <button onClick={() => setShowAddForm(!showAddForm)} className="px-3 py-1.5 bg-teal-600 text-white text-xs rounded-lg hover:bg-teal-700 inline-flex items-center gap-1.5">
            {showAddForm ? <X className="w-3.5 h-3.5" /> : <Plus className="w-3.5 h-3.5" />}
            {showAddForm ? 'Cancel' : 'Add Known Asset'}
          </button>
        </div>
        {showAddForm && (
          <div className="bg-white border border-teal-200 rounded-xl p-4 mb-3 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <input value={addForm.name} onChange={e => setAddForm(f => ({ ...f, name: e.target.value }))} placeholder="Name *" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
              <input value={addForm.physicalAddress} onChange={e => setAddForm(f => ({ ...f, physicalAddress: e.target.value }))} placeholder="MAC Address *" className="px-3 py-2 border border-slate-200 rounded-lg text-sm font-mono" />
              <input value={addForm.ip} onChange={e => setAddForm(f => ({ ...f, ip: e.target.value }))} placeholder="IP Address" className="px-3 py-2 border border-slate-200 rounded-lg text-sm font-mono" />
              <input value={addForm.description} onChange={e => setAddForm(f => ({ ...f, description: e.target.value }))} placeholder="Description" className="px-3 py-2 border border-slate-200 rounded-lg text-sm" />
            </div>
            <button onClick={handleAddKnown} disabled={actionLoading === 'add-asset' || !addForm.name || !addForm.physicalAddress} className="px-4 py-2 bg-teal-600 text-white text-sm rounded-lg hover:bg-teal-700 disabled:opacity-50 inline-flex items-center gap-2">
              {actionLoading === 'add-asset' ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Add Asset
            </button>
          </div>
        )}
        <AssetTable assets={knownAssets} onDelete={handleDelete} actionLoading={actionLoading} />
      </div>

      {/* Auto-Discovered Devices */}
      <div>
        <h3 className="font-semibold text-slate-900 mb-3 flex items-center gap-2">
          <Wifi className="w-4 h-4 text-amber-600" />
          Auto-Discovered Devices ({discoveredAssets.length})
        </h3>
        <AssetTable assets={discoveredAssets} onDelete={handleDelete} onPromote={handlePromote} promoteId={promoteId} promoteForm={promoteForm} setPromoteForm={setPromoteForm} setPromoteId={setPromoteId} actionLoading={actionLoading} />
      </div>
    </div>
  )
}

function AssetTable({ assets, onDelete, onPromote, promoteId, promoteForm, setPromoteForm, setPromoteId, actionLoading }) {
  if (assets.length === 0) {
    return (
      <div className="bg-white border border-slate-200 rounded-xl p-8 text-center">
        <Monitor className="w-8 h-8 text-slate-300 mx-auto mb-2" />
        <p className="text-sm text-slate-500">No assets in this category</p>
      </div>
    )
  }

  return (
    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
      <div className="overflow-x-auto max-h-96 overflow-y-auto">
        <table className="w-full">
          <thead className="sticky top-0 bg-slate-50">
            <tr>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">Name</th>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">IP</th>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">MAC Address</th>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">Type</th>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">Last Seen</th>
              <th className="py-2 px-4 text-left text-xs font-medium text-slate-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {assets.map((asset, idx) => {
              const typeInfo = typeLabels[asset.typeId] || { label: `Type ${asset.typeId}`, style: 'bg-slate-100 text-slate-600' }
              const isPromoting = promoteId === asset.id
              return (
                <React.Fragment key={asset.id || idx}>
                  <tr className="hover:bg-slate-50">
                    <td className="py-2 px-4 text-sm font-medium text-slate-900">{asset.name || '-'}</td>
                    <td className="py-2 px-4 text-xs font-mono text-slate-600">{asset.ip || '-'}</td>
                    <td className="py-2 px-4 text-xs font-mono text-slate-500">{asset.physicalAddress || '-'}</td>
                    <td className="py-2 px-4">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-medium ${typeInfo.style}`}>
                        {typeInfo.label}
                      </span>
                    </td>
                    <td className="py-2 px-4 text-xs text-slate-500">{asset.lastTouchDate || '-'}</td>
                    <td className="py-2 px-4">
                      <div className="flex items-center gap-1">
                        {onPromote && asset.typeId === 2 && (
                          <button onClick={() => onPromote(asset)} disabled={actionLoading === `promote-${asset.id}`} className="p-1 text-teal-600 hover:bg-teal-50 rounded" title="Promote to known">
                            {actionLoading === `promote-${asset.id}` ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <ArrowUpCircle className="w-3.5 h-3.5" />}
                          </button>
                        )}
                        {onDelete && (
                          <button onClick={() => onDelete(asset.id)} disabled={actionLoading === `delete-${asset.id}`} className="p-1 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded" title="Delete">
                            {actionLoading === `delete-${asset.id}` ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Trash2 className="w-3.5 h-3.5" />}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                  {isPromoting && (
                    <tr className="bg-teal-50">
                      <td colSpan={6} className="py-2 px-4">
                        <div className="flex items-center gap-2">
                          <input value={promoteForm.name} onChange={e => setPromoteForm(f => ({ ...f, name: e.target.value }))} placeholder="Name for this asset *" className="px-2 py-1 border border-slate-200 rounded text-sm flex-1" autoFocus />
                          <input value={promoteForm.description} onChange={e => setPromoteForm(f => ({ ...f, description: e.target.value }))} placeholder="Description" className="px-2 py-1 border border-slate-200 rounded text-sm flex-1" />
                          <button onClick={() => onPromote(asset)} disabled={!promoteForm.name} className="px-3 py-1 bg-teal-600 text-white text-xs rounded hover:bg-teal-700 disabled:opacity-50">Save</button>
                          <button onClick={() => setPromoteId(null)} className="px-3 py-1 bg-slate-200 text-slate-700 text-xs rounded hover:bg-slate-300">Cancel</button>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
