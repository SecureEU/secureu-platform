// Pure bucketing logic for the HTTP Methods chart, kept separate so it can be
// unit-tested without rendering.
//
// Suricata stores the first token of a request line as http_method. Anything that
// is not really HTTP - port scans, RAT beacons (e.g. the Gh0st magic header),
// exploit payloads, RDP/WebLogic handshakes - therefore lands in this field as raw
// bytes. Rendering those verbatim broke the legend (multi-line binary soup, and
// control bytes drawn as glyph boxes), so they are collapsed into one slice.
// Keeping them as a single bucket is also more useful: the number of malformed
// requests is itself the security signal, not each individual byte sequence.

export const MALFORMED_LABEL = 'Malformed / non-HTTP'
export const OTHER_LABEL = 'Other'

// RFC 9110 methods plus the WebDAV set - all genuinely valid HTTP methods, so
// they must not be mislabelled as malformed even though scanners probe them.
export const STANDARD_METHODS = new Set([
  'GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'TRACE', 'CONNECT',
  'PROPFIND', 'PROPPATCH', 'MKCOL', 'COPY', 'MOVE', 'LOCK', 'UNLOCK',
])

const COLORS = {
  GET: '#4338ca', POST: '#0891b2', PUT: '#6366f1', DELETE: '#2563eb', PATCH: '#4f46e5',
  HEAD: '#94a3b8', OPTIONS: '#a5b4fc', TRACE: '#818cf8', CONNECT: '#7c3aed',
}

export function sanitizeLabel(raw) {
  const cleaned = String(raw ?? '')
    .replace(/\\x[0-9a-fA-F]{2}/g, '') // literal "\xNN" text as stored by Suricata
    .replace(/[^\x20-\x7E]/g, '') // real control / non-printable bytes
    .trim()
  if (!cleaned) return '(binary)'
  return cleaned.length > 24 ? `${cleaned.slice(0, 24)}…` : cleaned
}

export function buildMethodChartData(data, topN = 5) {
  if (!data?.length) return []

  const sorted = [...data].sort((a, b) => b.doc_count - a.doc_count)

  const standard = []
  let malformedTotal = 0
  const malformedSamples = []

  for (const item of sorted) {
    const key = String(item.key ?? '').toUpperCase()
    if (STANDARD_METHODS.has(key)) {
      standard.push({ key, doc_count: item.doc_count })
    } else {
      malformedTotal += item.doc_count
      if (malformedSamples.length < 3) malformedSamples.push(sanitizeLabel(item.key))
    }
  }

  const chartData = standard.slice(0, topN).map(item => ({
    name: item.key,
    value: item.doc_count,
    color: COLORS[item.key] || '#6b7280',
  }))

  const standardRest = standard.slice(topN).reduce((sum, item) => sum + item.doc_count, 0)
  if (standardRest > 0) chartData.push({ name: OTHER_LABEL, value: standardRest, color: '#9ca3af' })

  // Red so it reads as a finding rather than just another method.
  if (malformedTotal > 0) {
    chartData.push({ name: MALFORMED_LABEL, value: malformedTotal, color: '#dc2626', samples: malformedSamples })
  }

  return chartData
}
