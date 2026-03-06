'use client'

import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { formatShortTime, formatBytes } from '@/utils/format'

export default function TrafficChart({ data }) {
  if (!data?.length) return null

  const chartData = data.map(item => ({
    time: formatShortTime(item.key_as_string),
    inbound: item.bytes_toclient?.value || 0,
    outbound: item.bytes_toserver?.value || 0,
  }))

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">DDOS Traffic Volume</h3>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <XAxis dataKey="time" stroke="#6b7280" fontSize={12} />
          <YAxis stroke="#6b7280" fontSize={12} tickFormatter={formatBytes} />
          <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '8px' }} formatter={(value) => formatBytes(value)} />
          <Legend />
          <Area type="monotone" dataKey="outbound" stackId="1" stroke="#2563eb" fill="#2563eb" fillOpacity={0.7} name="To Server" />
          <Area type="monotone" dataKey="inbound" stackId="2" stroke="#6366f1" fill="#6366f1" fillOpacity={0.7} name="To Client" />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
