const API_URL = process.env.NEXT_PUBLIC_SQS_API_URL || 'http://localhost:8000'

async function fetchJSON(path) {
  const res = await fetch(`${API_URL}${path}`)
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`SQS API error ${res.status}: ${text || res.statusText}`)
  }
  return res.json()
}

// Wrapper that returns null on failure instead of throwing
async function fetchSafe(path) {
  try {
    return await fetchJSON(path)
  } catch {
    return null
  }
}

export const fetchDashboardSummary = (hours = 24) =>
  fetchJSON(`/dashboard/summary?hours=${hours}`)

export const fetchTimeline = (hours = 24) =>
  fetchJSON(`/dashboard/timeline?hours=${hours}`)

export const fetchRecentAlerts = (size = 30) =>
  fetchJSON(`/dashboard/recent-alerts?size=${size}`)

export const fetchTopAttackers = (hours = 24, size = 10) =>
  fetchJSON(`/dashboard/top-attackers?hours=${hours}&size=${size}`)

export const fetchAlertStats = (hours = 24) =>
  fetchJSON(`/alerts/stats?hours=${hours}`)

export const fetchDdosStats = (hours = 24) =>
  fetchJSON(`/ddos/stats?hours=${hours}`)

export const fetchHttpStats = (hours = 24) =>
  fetchJSON(`/http/stats?hours=${hours}`)

export const fetchFlowStats = (hours = 24) =>
  fetchJSON(`/flows/stats?hours=${hours}`)

export const fetchEtAlertStats = (hours = 24) =>
  fetchJSON(`/et-alerts/stats?hours=${hours}`)

export const fetchRecentEtAlerts = (size = 30) =>
  fetchJSON(`/et-alerts/recent?size=${size}`)
