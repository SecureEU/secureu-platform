'use client'

import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'

export default function FlowStateChart({ data }) {
  if (!data?.length) return null

  const chartData = data.map(item => ({ name: item.key, count: item.doc_count }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Connection States</h3>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={chartData} layout="vertical">
          <XAxis type="number" stroke="#6b7280" fontSize={12} />
          <YAxis dataKey="name" type="category" stroke="#6b7280" fontSize={12} width={80} />
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} />
          <Bar dataKey="count" fill="#0891b2" radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
