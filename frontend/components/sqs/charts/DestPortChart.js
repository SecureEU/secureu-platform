'use client'

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

const PORT_NAMES = { 21: 'FTP', 22: 'SSH', 23: 'Telnet', 25: 'SMTP', 53: 'DNS', 80: 'HTTP', 443: 'HTTPS', 3389: 'RDP', 8080: 'HTTP-Alt' }

export default function DestPortChart({ data }) {
  if (!data?.length) return null

  const chartData = data.slice(0, 10).map(item => ({
    port: `${item.key} (${PORT_NAMES[item.key] || '?'})`,
    count: item.doc_count,
  }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Top Targeted Ports</h3>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={chartData}>
          <XAxis dataKey="port" stroke="#6b7280" fontSize={10} angle={-45} textAnchor="end" height={80} />
          <YAxis stroke="#6b7280" fontSize={12} />
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} />
          <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
