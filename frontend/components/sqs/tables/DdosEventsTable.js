import { formatTimestamp, formatBytes } from '@/utils/format'

const severityColors = {
  critical: 'bg-red-500',
  high: 'bg-orange-500',
  medium: 'bg-yellow-500',
  low: 'bg-blue-500',
}

export default function DdosEventsTable({ events }) {
  if (!events?.length) return null

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Recent DDOS Events</h3>
      <div className="overflow-x-auto max-h-80 overflow-y-auto">
        <table className="w-full text-sm">
          <thead className="sticky top-0 bg-white">
            <tr className="text-gray-600 border-b border-gray-200">
              <th className="text-left py-2 px-2">Time</th>
              <th className="text-left py-2 px-2">Severity</th>
              <th className="text-left py-2 px-2">Type</th>
              <th className="text-left py-2 px-2">Source</th>
              <th className="text-left py-2 px-2">Target</th>
              <th className="text-right py-2 px-2">Traffic</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event, idx) => (
              <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-2 text-xs text-gray-500">
                  {formatTimestamp(event['@timestamp'])}
                </td>
                <td className="py-2 px-2">
                  <span className={`px-2 py-0.5 rounded text-xs text-white ${severityColors[event.severity_level] || 'bg-gray-500'}`}>
                    {event.severity_level || 'N/A'}
                  </span>
                </td>
                <td className="py-2 px-2 text-xs max-w-xs truncate text-gray-700" title={event.alert_signature}>
                  {event.alert_signature?.replace('[MIRAI][DDoS] ', '')}
                </td>
                <td className="py-2 px-2 font-mono text-xs text-gray-700">{event.src_ip}</td>
                <td className="py-2 px-2 font-mono text-xs text-gray-700">{event.dest_ip}:{event.dest_port}</td>
                <td className="py-2 px-2 text-right text-xs text-gray-700">
                  {formatBytes(event.flow?.bytes_toserver)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
