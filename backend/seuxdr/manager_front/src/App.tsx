import './App.css'
import { Route, Switch, Link, Redirect } from "wouter";
import LogPage from './pages/logPage';
import { MenuItem } from './utils/types';
import { PieChartOutlined, DesktopOutlined } from '@ant-design/icons';
import OrgsPage from './pages/manageOrgsPage';
import AgentsPage from './pages/agentsPage';




function App() {
  // No authentication required - all menu items visible

  function getItem(label: React.ReactNode, key: string, icon?: React.ReactNode, children?: MenuItem[]): MenuItem {
    return {
      key,
      icon,
      children,
      label,
    };
  }

  const items: MenuItem[] = [
    getItem(<Link href='/orgs'>My Orgs</Link>, '/orgs', <PieChartOutlined />,),
    getItem(<Link href='/agents'>My Agents</Link>, '/agents', <DesktopOutlined />),
    // Users page hidden - authentication removed
  ];

  // Page components
  const Orgs = () => <OrgsPage sideBarItems={items}></OrgsPage>;
  const AlertsForOrg = () => <LogPage sideBarItems={items}></LogPage>;
  const Agents = () => <AgentsPage sideBarItems={items}></AgentsPage>;
  const NotFound = () => <h1>404 - Not Found</h1>;

  return (
      <Switch>
        <Route path="/orgs" component={Orgs} />
        <Route path="/agents" component={Agents} />
        <Route path="/alerts/:org_id" component={AlertsForOrg} />
        {/* Users route removed - authentication disabled */}
        <Route path="/">
          <Redirect to="/orgs" />
        </Route>
        <Route> {/* Fallback route for 404 */}
          <NotFound />
        </Route>
      </Switch>

  )
}

export default App
