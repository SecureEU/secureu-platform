'use client'

import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { formatShortTime } from '@/utils/format'

export default function TimelineChart({ data }) {
  if (!data) return null

  // Use the longest array as the time axis (alerts/ddos may be empty if no attack data)
  const timeBuckets = [data.alerts, data.ddos, data.http]
    .filter(arr => arr?.length)
    .sort((a, b) => b.length - a.length)[0]

  if (!timeBuckets?.length) return null

  const chartData = timeBuckets.map((item, idx) => ({
    time: formatShortTime(item.key_as_string),
    Alerts: data.alerts?.[idx]?.doc_count || 0,
    DDOS: data.ddos?.[idx]?.doc_count || 0,
    HTTP: data.http?.[idx]?.doc_count || 0,
  }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Event Timeline</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <XAxis dataKey="time" stroke="#6b7280" fontSize={12} />
          <YAxis stroke="#6b7280" fontSize={12} />
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} />
          <Legend />
          <Line type="monotone" dataKey="Alerts" stroke="#4338ca" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="DDOS" stroke="#0891b2" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="HTTP" stroke="#6366f1" strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
