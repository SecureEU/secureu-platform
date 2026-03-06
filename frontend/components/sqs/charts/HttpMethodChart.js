'use client'

import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts'

const COLORS = { GET: '#4338ca', POST: '#0891b2', PUT: '#6366f1', DELETE: '#2563eb', PATCH: '#4f46e5', HEAD: '#94a3b8', OPTIONS: '#a5b4fc' }

export default function HttpMethodChart({ data }) {
  if (!data?.length) return null

  const sorted = [...data].sort((a, b) => b.doc_count - a.doc_count)
  const top5 = sorted.slice(0, 5)
  const othersTotal = sorted.slice(5).reduce((sum, item) => sum + item.doc_count, 0)

  const chartData = top5.map(item => ({ name: item.key, value: item.doc_count, color: COLORS[item.key] || '#6b7280' }))
  if (othersTotal > 0) chartData.push({ name: 'Other', value: othersTotal, color: '#9ca3af' })

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">HTTP Methods</h3>
      <ResponsiveContainer width="100%" height={250}>
        <PieChart>
          <Pie data={chartData} cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={3} dataKey="value">
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} formatter={(value, name) => [value.toLocaleString(), name]} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
