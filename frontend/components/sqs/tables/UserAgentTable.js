export default function UserAgentTable({ data }) {
  if (!data?.length) return null

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Top User Agents</h3>
      <div className="overflow-x-auto max-h-64 overflow-y-auto">
        <table className="w-full text-sm">
          <thead className="sticky top-0 bg-white">
            <tr className="text-gray-600 border-b border-gray-200">
              <th className="text-left py-2">User Agent</th>
              <th className="text-right py-2">Count</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => (
              <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 text-xs truncate max-w-xs text-gray-700" title={item.key}>
                  {item.key || '(empty)'}
                </td>
                <td className="text-right py-2 text-gray-700">{item.doc_count?.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
