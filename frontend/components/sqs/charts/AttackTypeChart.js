'use client'

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'

const COLORS = ['#4338ca', '#0891b2', '#6366f1', '#2563eb', '#4f46e5', '#94a3b8', '#a5b4fc', '#64748b']

export default function AttackTypeChart({ data }) {
  if (!data?.length) return null

  const chartData = data.map(item => ({
    name: item.key.replace('[MIRAI][DDoS] ', '').replace(' Attack', ''),
    count: item.doc_count,
  }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">DDOS Attack Types</h3>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={chartData} layout="vertical">
          <XAxis type="number" stroke="#6b7280" fontSize={12} />
          <YAxis dataKey="name" type="category" stroke="#6b7280" fontSize={11} width={120} />
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} />
          <Bar dataKey="count" radius={[0, 4, 4, 0]}>
            {chartData.map((_, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
