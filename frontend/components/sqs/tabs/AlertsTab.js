import StatCard from '../StatCard'
import SeverityChart from '../charts/SeverityChart'
import MiraiStageChart from '../charts/MiraiStageChart'
import CategoryChart from '../charts/CategoryChart'
import ProtocolChart from '../charts/ProtocolChart'
import DestPortChart from '../charts/DestPortChart'
import EtCategoryChart from '../charts/EtCategoryChart'
import TopTable from '../tables/TopTable'
import RecentAlerts from '../tables/RecentAlerts'
import RecentEtAlerts from '../tables/RecentEtAlerts'

export default function AlertsTab({ data }) {
  return (
    <>
      <h2 className="text-lg font-semibold text-gray-700 mb-4">MIRAI Alerts</h2>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard title="Total Alerts" value={data.alertStats?.total} color="blue" />
        <StatCard title="Unique Sources" value={data.summary?.alerts?.unique_sources} color="green" />
        <StatCard title="Unique Targets" value={data.summary?.alerts?.unique_targets} color="yellow" />
        <StatCard title="Categories" value={data.alertStats?.by_category?.length} color="purple" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <SeverityChart data={data.alertStats?.by_severity} />
        <MiraiStageChart data={data.alertStats?.by_mirai_stage} />
        <CategoryChart data={data.alertStats?.by_category} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <ProtocolChart data={data.alertStats?.by_protocol} />
        <DestPortChart data={data.alertStats?.by_dest_port} />
        <TopTable title="Top Source IPs" data={data.alertStats?.top_src_ips} keyLabel="IP Address" valueLabel="Events" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <RecentAlerts alerts={data.recentAlerts?.alerts} />
        <TopTable title="Top Destination IPs" data={data.alertStats?.top_dest_ips} keyLabel="IP Address" valueLabel="Events" />
      </div>

      <h2 className="text-lg font-semibold text-gray-700 mb-4 pt-4 border-t border-gray-200">Emerging Threats (ET) Alerts</h2>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard title="ET Alerts" value={data.etAlertStats?.total} color="purple" />
        <StatCard title="Categories" value={data.etAlertStats?.by_category?.length} color="blue" />
        <StatCard title="Signatures" value={data.etAlertStats?.by_signature?.length} color="green" />
        <StatCard title="Protocols" value={data.etAlertStats?.by_proto?.length} color="yellow" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <EtCategoryChart data={data.etAlertStats?.by_category} />
        <TopTable title="Top ET Signatures" data={data.etAlertStats?.by_signature?.slice(0, 10)} keyLabel="Signature" valueLabel="Count" />
        <TopTable title="Top Source IPs (ET)" data={data.etAlertStats?.top_src_ips} keyLabel="IP Address" valueLabel="Events" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentEtAlerts alerts={data.recentEtAlerts?.alerts} />
        <TopTable title="Top Destination IPs (ET)" data={data.etAlertStats?.top_dest_ips} keyLabel="IP Address" valueLabel="Events" />
      </div>
    </>
  )
}
