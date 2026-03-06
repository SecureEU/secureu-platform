'use client'

import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts'

const COLORS = { 1: '#4338ca', 2: '#4f46e5', 3: '#0891b2', 4: '#6366f1' }
const LABELS = { 1: 'Critical', 2: 'High', 3: 'Medium', 4: 'Low' }

export default function SeverityChart({ data }) {
  if (!data?.length) return null

  const chartData = data.map(item => ({
    name: LABELS[item.key] || `Severity ${item.key}`,
    value: item.doc_count,
    color: COLORS[item.key] || '#6b7280',
  }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Alert Severity Distribution</h3>
      <ResponsiveContainer width="100%" height={250}>
        <PieChart>
          <Pie data={chartData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
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
