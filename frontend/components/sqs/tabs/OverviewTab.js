import StatCard from '../StatCard'
import TimelineChart from '../charts/TimelineChart'
import SeverityChart from '../charts/SeverityChart'
import MiraiStageChart from '../charts/MiraiStageChart'
import ProtocolChart from '../charts/ProtocolChart'
import TopTable from '../tables/TopTable'
import RecentAlerts from '../tables/RecentAlerts'
import TopAttackers from '../tables/TopAttackers'
import { formatBytes } from '@/utils/format'

export default function OverviewTab({ data }) {
  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
        <StatCard title="Total Alerts" value={data.summary?.alerts?.total} color="blue" />
        <StatCard title="Critical Alerts" value={data.summary?.alerts?.critical} color="red" />
        <StatCard title="High Severity" value={data.summary?.alerts?.high} color="yellow" />
        <StatCard title="DDOS Events" value={data.summary?.ddos?.total} color="purple" />
        <StatCard title="Unique Sources" value={data.summary?.alerts?.unique_sources} color="green" />
        <StatCard title="DDOS Traffic" value={formatBytes(data.summary?.ddos?.total_bytes)} subtitle="to server" color="red" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <TimelineChart data={data.timeline} />
        </div>
        <SeverityChart data={data.alertStats?.by_severity} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <MiraiStageChart data={data.alertStats?.by_mirai_stage} />
        <ProtocolChart data={data.alertStats?.by_protocol} />
        <TopAttackers attackers={data.topAttackers?.attackers} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentAlerts alerts={data.recentAlerts?.alerts} />
        <TopTable title="Top Signatures" data={data.alertStats?.by_signature?.slice(0, 10)} keyLabel="Signature" valueLabel="Count" />
      </div>
    </>
  )
}
