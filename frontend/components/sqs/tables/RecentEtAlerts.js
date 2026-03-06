import { formatTimestamp } from '@/utils/format'

const severityColors = {
  1: 'bg-red-500',
  2: 'bg-orange-500',
  3: 'bg-yellow-500',
}

export default function RecentEtAlerts({ alerts }) {
  if (!alerts?.length) return null

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Recent ET Alerts</h3>
      <div className="overflow-x-auto max-h-96 overflow-y-auto">
        <table className="w-full text-sm">
          <thead className="sticky top-0 bg-white">
            <tr className="text-gray-600 border-b border-gray-200">
              <th className="text-left py-2 px-2">Time</th>
              <th className="text-left py-2 px-2">Sev</th>
              <th className="text-left py-2 px-2">Signature</th>
              <th className="text-left py-2 px-2">Source</th>
              <th className="text-left py-2 px-2">Destination</th>
            </tr>
          </thead>
          <tbody>
            {alerts.map((alert, idx) => (
              <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-2 text-xs text-gray-500">
                  {formatTimestamp(alert['@timestamp'])}
                </td>
                <td className="py-2 px-2">
                  <span className={`inline-block w-2 h-2 rounded-full ${severityColors[alert.alert?.severity] || 'bg-gray-500'}`} />
                </td>
                <td className="py-2 px-2 text-xs max-w-xs truncate text-gray-700" title={alert.alert?.signature}>
                  {alert.alert?.signature?.replace('ET ', '')}
                </td>
                <td className="py-2 px-2 font-mono text-xs text-gray-700">
                  {alert.src_ip}:{alert.src_port}
                </td>
                <td className="py-2 px-2 font-mono text-xs text-gray-700">
                  {alert.dest_ip}:{alert.dest_port}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
