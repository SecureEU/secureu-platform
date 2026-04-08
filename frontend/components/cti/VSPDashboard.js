'use client'

import React, { useState, useEffect, useCallback } from 'react'
import {
  Shield,
  AlertTriangle,
  ChevronDown,
  Download,
  Trash2,
  Save,
  RefreshCw,
  Info,
  ChevronLeft,
  ChevronRight,
  Calculator
} from 'lucide-react'

// CVSS Calculator utility functions
const cvssWeights = {
  AV: { NETWORK: 0.85, ADJACENT: 0.62, LOCAL: 0.55, PHYSICAL: 0.2 },
  AC: { LOW: 0.77, HIGH: 0.44 },
  PR: {
    NONE: { UNCHANGED: 0.85, CHANGED: 0.85 },
    LOW: { UNCHANGED: 0.62, CHANGED: 0.68 },
    HIGH: { UNCHANGED: 0.27, CHANGED: 0.5 }
  },
  UI: { NONE: 0.85, REQUIRED: 0.62 },
  C: { NONE: 0, LOW: 0.22, HIGH: 0.56 },
  I: { NONE: 0, LOW: 0.22, HIGH: 0.56 },
  A: { NONE: 0, LOW: 0.22, HIGH: 0.56 }
}

const calculateCVSS = (metrics) => {
  const { AV, AC, PR, UI, S, C, I, A } = metrics

  const avWeight = cvssWeights.AV[AV]
  const acWeight = cvssWeights.AC[AC]
  const prWeight = cvssWeights.PR[PR][S]
  const uiWeight = cvssWeights.UI[UI]

  const exploitability = 8.22 * avWeight * acWeight * prWeight * uiWeight

  const cWeight = cvssWeights.C[C]
  const iWeight = cvssWeights.I[I]
  const aWeight = cvssWeights.A[A]

  const iscBase = 1 - ((1 - cWeight) * (1 - iWeight) * (1 - aWeight))
  let impact

  if (S === 'UNCHANGED') {
    impact = 6.42 * iscBase
  } else {
    impact = 7.52 * (iscBase - 0.029) - 3.25 * Math.pow(iscBase - 0.02, 15)
  }

  let baseScore
  if (impact <= 0) {
    baseScore = 0
  } else if (S === 'UNCHANGED') {
    baseScore = Math.min(impact + exploitability, 10)
  } else {
    baseScore = Math.min(1.08 * (impact + exploitability), 10)
  }

  baseScore = Math.ceil(baseScore * 10) / 10

  let severity
  if (baseScore === 0) severity = 'NONE'
  else if (baseScore < 4) severity = 'LOW'
  else if (baseScore < 7) severity = 'MEDIUM'
  else if (baseScore < 9) severity = 'HIGH'
  else severity = 'CRITICAL'

  return {
    cvssScore: baseScore.toFixed(1),
    exploitabilityScore: Math.min(exploitability, 3.89).toFixed(2),
    impactScore: Math.max(0, impact).toFixed(2),
    severity
  }
}

const generateCVSSVector = (metrics) => {
  return `CVSS:3.1/AV:${metrics.AV[0]}/AC:${metrics.AC[0]}/PR:${metrics.PR[0]}/UI:${metrics.UI[0]}/S:${metrics.S[0]}/C:${metrics.C[0]}/I:${metrics.I[0]}/A:${metrics.A[0]}`
}

const getSeverityColor = (severity) => {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL': return 'bg-red-500 text-white'
    case 'HIGH': return 'bg-orange-500 text-white'
    case 'MEDIUM': return 'bg-yellow-500 text-black'
    case 'LOW': return 'bg-green-500 text-white'
    default: return 'bg-gray-500 text-white'
  }
}

const getSeverityBorder = (severity) => {
  switch (severity?.toUpperCase()) {
    case 'CRITICAL': return 'border-red-500'
    case 'HIGH': return 'border-orange-500'
    case 'MEDIUM': return 'border-yellow-500'
    case 'LOW': return 'border-green-500'
    default: return 'border-gray-500'
  }
}

const VSP_API_URL = process.env.NEXT_PUBLIC_VSP_API_URL || 'http://localhost:5002'
const PENTEST_API_URL = process.env.NEXT_PUBLIC_PENTEST_API_URL || 'http://localhost:3001'

export default function VSPDashboard() {
  const [description, setDescription] = useState('')
  const [predicting, setPredicting] = useState(false)
  const [results, setResults] = useState(null)
  const [history, setHistory] = useState([])
  const [currentPage, setCurrentPage] = useState(1)
  const [loadingHistory, setLoadingHistory] = useState(true)
  const itemsPerPage = 5

  const fetchHistory = useCallback(async () => {
    try {
      const res = await fetch(`${PENTEST_API_URL}/vsp/predictions`, {
        headers: { 'ngrok-skip-browser-warning': 'true' }
      })
      if (res.ok) {
        const data = await res.json()
        setHistory(Array.isArray(data) ? data : [])
      }
    } catch (err) {
      console.error('Failed to load VSP history:', err)
    } finally {
      setLoadingHistory(false)
    }
  }, [])

  useEffect(() => {
    fetchHistory()
  }, [fetchHistory])

  // CVSS Metrics State
  const [metrics, setMetrics] = useState({
    AV: 'NETWORK',
    AC: 'LOW',
    PR: 'NONE',
    UI: 'NONE',
    S: 'UNCHANGED',
    C: 'HIGH',
    I: 'HIGH',
    A: 'HIGH'
  })

  const metricOptions = {
    AV: ['NETWORK', 'ADJACENT', 'LOCAL', 'PHYSICAL'],
    AC: ['LOW', 'HIGH'],
    PR: ['NONE', 'LOW', 'HIGH'],
    UI: ['NONE', 'REQUIRED'],
    S: ['UNCHANGED', 'CHANGED'],
    C: ['NONE', 'LOW', 'HIGH'],
    I: ['NONE', 'LOW', 'HIGH'],
    A: ['NONE', 'LOW', 'HIGH']
  }

  const metricLabels = {
    AV: 'Attack Vector',
    AC: 'Attack Complexity',
    PR: 'Privileges Required',
    UI: 'User Interaction',
    S: 'Scope',
    C: 'Confidentiality',
    I: 'Integrity',
    A: 'Availability'
  }

  const handlePredict = useCallback(async () => {
    if (!description.trim()) return

    setPredicting(true)
    try {
      const res = await fetch(`${VSP_API_URL}/predict`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ description })
      })
      if (!res.ok) throw new Error(`API error: ${res.status}`)
      const data = await res.json()

      const pv = data.predicted_vector
      const newMetrics = {
        AV: pv.AV, AC: pv.AC, PR: pv.PR, UI: pv.UI,
        S: pv.S, C: pv.C, I: pv.I, A: pv.A
      }
      setMetrics(newMetrics)
      setResults({
        description,
        ...newMetrics,
        cvssScore: data.cvss_score.toFixed(1),
        exploitabilityScore: data.exploitability_score.toFixed(2),
        impactScore: data.impact_score.toFixed(2),
        severity: data.base_severity,
        vector: data.cvss_vector
      })
    } catch (err) {
      console.warn('VSP API unreachable, falling back to local calculation:', err.message)
      const scores = calculateCVSS(metrics)
      setResults({
        description,
        ...metrics,
        ...scores,
        vector: generateCVSSVector(metrics)
      })
    } finally {
      setPredicting(false)
    }
  }, [description, metrics])

  const handleMetricChange = async (metric, value) => {
    const newMetrics = { ...metrics, [metric]: value }
    setMetrics(newMetrics)

    if (results) {
      try {
        const res = await fetch(`${VSP_API_URL}/recalculate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newMetrics)
        })
        if (!res.ok) throw new Error(`API error: ${res.status}`)
        const data = await res.json()
        setResults({
          ...results,
          ...newMetrics,
          cvssScore: data['CVSS Score'].toFixed(1),
          exploitabilityScore: data['Exploitability Score'].toFixed(2),
          impactScore: data['Impact Score'].toFixed(2),
          severity: data.Severity,
          vector: data['CVSS Vector']
        })
      } catch (err) {
        console.warn('VSP recalculate API unreachable, falling back to local:', err.message)
        const scores = calculateCVSS(newMetrics)
        setResults({
          ...results,
          ...newMetrics,
          ...scores,
          vector: generateCVSSVector(newMetrics)
        })
      }
    }
  }

  const handleSave = async () => {
    if (!results) return

    const entry = {
      description: results.description,
      vector: results.vector,
      cvss_score: results.cvssScore,
      exploitability_score: results.exploitabilityScore,
      impact_score: results.impactScore,
      severity: results.severity,
      metrics: { AV: metrics.AV, AC: metrics.AC, PR: metrics.PR, UI: metrics.UI, S: metrics.S, C: metrics.C, I: metrics.I, A: metrics.A },
    }

    try {
      const res = await fetch(`${PENTEST_API_URL}/vsp/predictions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
        body: JSON.stringify(entry),
      })
      if (!res.ok) throw new Error('Failed to save')
      await fetchHistory()
    } catch (err) {
      console.error('Failed to save prediction:', err)
    }

    setResults(null)
    setDescription('')
    setMetrics({
      AV: 'NETWORK',
      AC: 'LOW',
      PR: 'NONE',
      UI: 'NONE',
      S: 'UNCHANGED',
      C: 'HIGH',
      I: 'HIGH',
      A: 'HIGH'
    })
  }

  const handleClearHistory = async () => {
    try {
      await fetch(`${PENTEST_API_URL}/vsp/predictions`, {
        method: 'DELETE',
        headers: { 'ngrok-skip-browser-warning': 'true' },
      })
      setHistory([])
      setCurrentPage(1)
    } catch (err) {
      console.error('Failed to clear history:', err)
    }
  }

  const handleClearLast = async () => {
    if (history.length === 0) return
    const lastId = history[0]._id
    try {
      await fetch(`${PENTEST_API_URL}/vsp/predictions/${lastId}`, {
        method: 'DELETE',
        headers: { 'ngrok-skip-browser-warning': 'true' },
      })
      await fetchHistory()
    } catch (err) {
      console.error('Failed to delete prediction:', err)
    }
  }

  const handleDownload = () => {
    const blob = new Blob([JSON.stringify(history, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `vsp-predictions-${new Date().toISOString().split('T')[0]}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  // Pagination
  const totalPages = Math.ceil(history.length / itemsPerPage)
  const paginatedHistory = history.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage)

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">CVE Vulnerability Score Prediction</h1>
          <p className="text-gray-600 mt-1">Predict CVSS scores from vulnerability descriptions using ML models</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm font-medium flex items-center gap-2">
            <Calculator className="h-4 w-4" />
            CVSS 3.1
          </div>
        </div>
      </div>

      {/* Prediction Input */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Enter Vulnerability Description</h2>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Enter vulnerability description... (e.g., 'A remote code execution vulnerability exists in Apache Log4j that allows attackers to execute arbitrary code by sending specially crafted requests...')"
          className="w-full h-32 p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500 resize-none"
        />
        <div className="mt-4 flex justify-center">
          <button
            onClick={handlePredict}
            disabled={!description.trim() || predicting}
            className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            {predicting ? (
              <>
                <RefreshCw className="h-4 w-4 animate-spin" />
                Predicting...
              </>
            ) : (
              <>
                <Shield className="h-4 w-4" />
                Predict
              </>
            )}
          </button>
        </div>
      </div>

      {/* Results */}
      {results && (
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="text-center mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">Prediction Results (Draft) for:</h2>
            <p className="text-gray-600 italic max-w-2xl mx-auto">"{results.description}"</p>
            <div className="mt-4 flex justify-center">
              <div className={`px-6 py-3 rounded-lg font-bold text-lg ${getSeverityColor(results.severity)}`}>
                {results.cvssScore} - {results.severity}
              </div>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            {/* Base Metrics Table */}
            <div>
              <h3 className="text-md font-semibold text-gray-900 mb-3 flex items-center gap-2">
                <Info className="h-4 w-4 text-purple-500" />
                Base Metrics (Editable)
              </h3>
              <div className="border border-gray-200 rounded-lg overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="bg-gray-50">
                      <th className="px-4 py-2 text-left text-sm font-medium text-gray-700">Metric</th>
                      <th className="px-4 py-2 text-left text-sm font-medium text-gray-700">Value</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {['AV', 'AC', 'PR', 'UI', 'S', 'C', 'I', 'A'].map((metric) => (
                      <tr key={metric} className="hover:bg-gray-50">
                        <td className="px-4 py-2 text-sm text-gray-700">{metricLabels[metric]} ({metric})</td>
                        <td className="px-4 py-2">
                          <select
                            value={metrics[metric]}
                            onChange={(e) => handleMetricChange(metric, e.target.value)}
                            className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-purple-500 focus:border-purple-500"
                          >
                            {metricOptions[metric].map((opt) => (
                              <option key={opt} value={opt}>{opt}</option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Scores Table */}
            <div>
              <h3 className="text-md font-semibold text-gray-900 mb-3 flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-orange-500" />
                Calculated Scores
              </h3>
              <div className="border border-gray-200 rounded-lg overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="bg-gray-50">
                      <th className="px-4 py-2 text-left text-sm font-medium text-gray-700">Score</th>
                      <th className="px-4 py-2 text-left text-sm font-medium text-gray-700">Value</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-700">Base Score</td>
                      <td className="px-4 py-2">
                        <span className={`px-2 py-1 rounded font-medium ${getSeverityColor(results.severity)}`}>
                          {results.cvssScore}
                        </span>
                      </td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-700">Severity</td>
                      <td className="px-4 py-2">
                        <span className={`px-2 py-1 rounded font-medium ${getSeverityColor(results.severity)}`}>
                          {results.severity}
                        </span>
                      </td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-700">Exploitability Score</td>
                      <td className="px-4 py-2 text-sm font-mono">{results.exploitabilityScore}</td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-700">Impact Score</td>
                      <td className="px-4 py-2 text-sm font-mono">{results.impactScore}</td>
                    </tr>
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-sm text-gray-700">CVSS Vector</td>
                      <td className="px-4 py-2 text-xs font-mono text-purple-600">{results.vector}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="mt-6 flex justify-center">
            <button
              onClick={handleSave}
              className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2"
            >
              <Save className="h-4 w-4" />
              Save to Previous Predictions
            </button>
          </div>
        </div>
      )}

      {/* History */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Previous Predictions</h2>
          <div className="flex items-center gap-2">
            <button
              onClick={handleClearLast}
              disabled={history.length === 0}
              className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-1"
            >
              <Trash2 className="h-3 w-3" />
              Clear Last
            </button>
            <button
              onClick={handleClearHistory}
              disabled={history.length === 0}
              className="px-3 py-1.5 text-sm border border-red-300 text-red-600 rounded-lg hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-1"
            >
              <Trash2 className="h-3 w-3" />
              Clear All
            </button>
            <button
              onClick={handleDownload}
              disabled={history.length === 0}
              className="px-3 py-1.5 text-sm bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-1"
            >
              <Download className="h-3 w-3" />
              Download JSON
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Description</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">CVSS Vector</th>
                <th className="px-4 py-3 text-center text-sm font-medium text-gray-700">Base Score</th>
                <th className="px-4 py-3 text-center text-sm font-medium text-gray-700">Exploitability</th>
                <th className="px-4 py-3 text-center text-sm font-medium text-gray-700">Impact</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedHistory.length > 0 ? (
                paginatedHistory.map((entry) => (
                  <tr key={entry._id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-700 max-w-xs truncate">{entry.description}</td>
                    <td className="px-4 py-3 text-xs font-mono text-purple-600">{entry.vector}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`px-2 py-1 rounded text-sm font-medium ${getSeverityColor(entry.severity)}`}>
                        {entry.cvss_score}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center text-sm font-mono">{entry.exploitability_score}</td>
                    <td className="px-4 py-3 text-center text-sm font-mono">{entry.impact_score}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                    No predictions saved yet. Make a prediction and save it to see history.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-center gap-4">
            <button
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="p-1 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <span className="text-sm text-gray-600">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="p-1 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
