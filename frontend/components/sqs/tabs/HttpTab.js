import StatCard from '../StatCard'
import HttpMethodChart from '../charts/HttpMethodChart'
import HostnameTable from '../tables/HostnameTable'
import TopTable from '../tables/TopTable'
import UserAgentTable from '../tables/UserAgentTable'

export default function HttpTab({ data }) {
  return (
    <>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <StatCard title="HTTP Requests" value={data.httpStats?.total} color="blue" />
        <StatCard title="Unique Sources" value={data.httpStats?.top_src_ips?.length} color="green" />
        <StatCard title="Unique Hosts" value={data.httpStats?.by_hostname?.length} color="yellow" />
        <StatCard title="User Agents" value={data.httpStats?.by_user_agent?.length} color="purple" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
        <HttpMethodChart data={data.httpStats?.by_method} />
        <HostnameTable data={data.httpStats?.by_hostname?.slice(0, 10)} />
        <TopTable title="Top Source IPs" data={data.httpStats?.top_src_ips} keyLabel="IP Address" valueLabel="Requests" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <UserAgentTable data={data.httpStats?.by_user_agent?.slice(0, 15)} />
        <TopTable title="Top Destinations" data={data.httpStats?.top_dest_ips} keyLabel="IP Address" valueLabel="Requests" />
      </div>
    </>
  )
}
