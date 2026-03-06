export default function StatCard({ title, value, subtitle, color = 'blue' }) {
  const colors = {
    blue: 'border-blue-500 bg-blue-50',
    red: 'border-red-500 bg-red-50',
    yellow: 'border-amber-500 bg-amber-50',
    green: 'border-emerald-500 bg-emerald-50',
    purple: 'border-purple-500 bg-purple-50',
    cyan: 'border-cyan-500 bg-cyan-50',
  }

  return (
    <div className={`rounded-lg border-l-4 ${colors[color] || colors.blue} p-4 bg-white shadow-sm`}>
      <p className="text-sm text-gray-600">{title}</p>
      <p className="text-2xl font-bold mt-1 text-gray-800">
        {typeof value === 'number' ? value.toLocaleString() : value ?? '-'}
      </p>
      {subtitle && <p className="text-xs text-gray-500 mt-1">{subtitle}</p>}
    </div>
  )
}
