import StatCard from '../StatCard'
import TrafficChart from '../charts/TrafficChart'
import AttackTypeChart from '../charts/AttackTypeChart'
import TopTable from '../tables/TopTable'
import DdosEventsTable from '../tables/DdosEventsTable'
import { formatBytes, formatNumber } from '@/utils/format'

export default function DdosTab({ data }) {
  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
        <StatCard title="DDOS Events" value={data.ddosStats?.total} color="red" />
        <StatCard title="Traffic Out" value={formatBytes(data.ddosStats?.total_bytes_toserver)} color="yellow" />
        <StatCard title="Traffic In" value={formatBytes(data.ddosStats?.total_bytes_toclient)} color="blue" />
        <StatCard title="Packets Out" value={formatNumber(data.ddosStats?.total_pkts_toserver)} color="purple" />
        <StatCard title="Packets In" value={formatNumber(data.ddosStats?.total_pkts_toclient)} color="green" />
        <StatCard title="Attack Types" value={data.ddosStats?.by_signature?.length} color="red" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <TrafficChart data={data.ddosStats?.over_time} />
        <AttackTypeChart data={data.ddosStats?.by_signature} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <TopTable title="Top DDOS Sources" data={data.ddosStats?.top_src_ips} keyLabel="IP Address" valueLabel="Events" />
        <TopTable title="Top DDOS Targets" data={data.ddosStats?.top_dest_ips} keyLabel="IP Address" valueLabel="Events" />
        <TopTable title="Attack Targets" data={data.ddosStats?.by_attack_target} keyLabel="Target Type" valueLabel="Events" />
      </div>

      <DdosEventsTable events={data.ddosStats?.events || data.summary?.ddos?.events} />
    </>
  )
}
