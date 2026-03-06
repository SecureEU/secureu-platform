export default function TopAttackers({ attackers }) {
  if (!attackers?.length) return null

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Top Attackers</h3>
      <div className="space-y-3">
        {attackers.map((attacker, idx) => (
          <div key={idx} className="border border-gray-200 rounded-lg p-3">
            <div className="flex justify-between items-center mb-2">
              <span className="font-mono text-sm text-gray-700">{attacker.ip}</span>
              <span className="text-red-600 font-bold">{attacker.count.toLocaleString()} events</span>
            </div>
            <div className="text-xs text-gray-500">
              <span>Targets: {attacker.unique_targets}</span>
            </div>
            {attacker.top_signatures?.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {attacker.top_signatures.slice(0, 2).map((sig, sidx) => (
                  <span key={sidx} className="px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded text-xs truncate max-w-full">
                    {sig}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
