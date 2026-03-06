import StatCard from '../StatCard'
import NetworkTrafficChart from '../charts/NetworkTrafficChart'
import AppProtoChart from '../charts/AppProtoChart'
import FlowStateChart from '../charts/FlowStateChart'
import ProtocolChart from '../charts/ProtocolChart'
import TopTable from '../tables/TopTable'
import TopTalkersTable from '../tables/TopTalkersTable'
import { formatBytes, formatNumber } from '@/utils/format'

export default function NetworkTab({ data }) {
  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
        <StatCard title="Total Flows" value={data.flowStats?.total} color="blue" />
        <StatCard title="Outbound" value={formatBytes(data.flowStats?.total_bytes_toserver)} color="purple" />
        <StatCard title="Inbound" value={formatBytes(data.flowStats?.total_bytes_toclient)} color="green" />
        <StatCard title="Packets Out" value={formatNumber(data.flowStats?.total_pkts_toserver)} color="yellow" />
        <StatCard title="Packets In" value={formatNumber(data.flowStats?.total_pkts_toclient)} color="blue" />
        <StatCard title="Protocols" value={data.flowStats?.by_app_proto?.length} color="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <NetworkTrafficChart data={data.flowStats?.over_time} />
        <AppProtoChart data={data.flowStats?.by_app_proto} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <FlowStateChart data={data.flowStats?.by_state} />
        <ProtocolChart data={data.flowStats?.by_proto} />
        <TopTable title="Top Ports" data={data.flowStats?.by_dest_port} keyLabel="Port" valueLabel="Flows" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TopTalkersTable data={data.flowStats?.top_src_ips} title="Top Sources (by traffic)" />
        <TopTalkersTable data={data.flowStats?.top_dest_ips} title="Top Destinations (by traffic)" />
      </div>
    </>
  )
}
