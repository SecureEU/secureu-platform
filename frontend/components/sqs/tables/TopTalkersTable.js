import { formatBytes } from '@/utils/format'

export default function TopTalkersTable({ data, title = 'Top Talkers' }) {
  if (!data?.length) return null

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">{title}</h3>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-gray-600 border-b border-gray-200">
              <th className="text-left py-2">IP Address</th>
              <th className="text-right py-2">Flows</th>
              <th className="text-right py-2">Traffic</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => (
              <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 font-mono text-xs text-gray-700">{item.key}</td>
                <td className="text-right py-2 text-gray-700">{item.doc_count?.toLocaleString()}</td>
                <td className="text-right py-2 text-gray-700">{formatBytes(item.bytes)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
