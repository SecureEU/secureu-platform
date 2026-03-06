'use client'

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import {
  Shield,
  Users,
  Globe,
  HelpCircle,
  Target,
  Menu,
  X,
  ExternalLink,
  Github,
  FileText,
  User,

  LogOut,
  LogIn,
  ChevronDown,
  ChevronRight,
  Crosshair,
  Wrench,
  EyeOff,
  Search,
  ShieldCheck,
  Brain,
  Lock,
  Eye,
  Calculator,
  Flag,
  AlertTriangle,
  Wifi,
  Server,
  Activity,
  Monitor,
  Settings,
  Network
} from 'lucide-react';

const Layout = ({ children }) => {
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [offensiveMenuOpen, setOffensiveMenuOpen] = useState(false);
  const [defensiveMenuOpen, setDefensiveMenuOpen] = useState(false);
  const [mobileOffensiveOpen, setMobileOffensiveOpen] = useState(false);
  const [mobileDefensiveOpen, setMobileDefensiveOpen] = useState(false);
  const [ctiMenuOpen, setCTIMenuOpen] = useState(false);
  const [mobileCTIOpen, setMobileCTIOpen] = useState(false);
  const [sqsMenuOpen, setSqsMenuOpen] = useState(false);
  const [mobileSqsOpen, setMobileSqsOpen] = useState(false);
  const [dtmadMenuOpen, setDtmadMenuOpen] = useState(false);
  const [mobileDtmadOpen, setMobileDtmadOpen] = useState(false);
  const { user, isAuthenticated, logout, loading: authLoading } = useAuth();
  const [workspace, setWorkspace] = useState(null);

  useEffect(() => {
    fetch('/api/v1/settings/workspace', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        'Content-Type': 'application/json',
      },
    })
      .then(res => res.ok ? res.json() : null)
      .then(data => { if (data) setWorkspace(data); })
      .catch(() => {});
  }, []);

  // Penetration Testing Tools items under Offensive Solutions
  const pentestingItems = [
    { name: 'Dashboard', route: '/offsec/pentest/dashboard', icon: Shield, key: 'pentest-dashboard', description: 'Security overview' },
    { name: 'Scans', route: '/offsec/pentest/scans', icon: Globe, key: 'pentest-scans', description: 'Run security scans' },
    { name: 'Assets', route: '/offsec/pentest/assets', icon: Users, key: 'pentest-assets', description: 'Manage assets' },
    { name: 'Exploitation', route: '/offsec/pentest/exploitation', icon: Target, key: 'pentest-exploitation', description: 'Offensive testing' },
  ];

  // SSL Checker items under Offensive Solutions
  const sslItems = [
    { name: 'Scanner', route: '/offsec/ssl', icon: Lock, key: 'ssl-scanner', description: 'SSL/TLS security analysis' },
  ];

  // Darkweb items under Offensive Solutions
  const darkwebItems = [
    { name: 'Monitor', route: '/offsec/darkweb/monitor', icon: Search, key: 'darkweb-monitor', description: 'Search leaked credentials' },
  ];

  // Defensive Solutions items
  const defensiveItems = [
    { name: 'SIEM Dashboard', route: '/defsec/siem', icon: Eye, key: 'defsec-siem', description: 'SEUXDR host-based intrusion detection' },
  ];

  // SQS items
  const sqsItems = [
    { name: 'Botnet Detection', route: '/defsec/sqs', icon: Activity, key: 'sqs-dashboard', description: 'MIRAI botnet detection & monitoring' },
  ];

  // DTM & AD items
  const dtmadItems = [
    { name: 'DTM & AD Dashboard', route: '/defsec/dtmad', icon: Monitor, key: 'dtmad-dashboard', description: 'Traffic monitoring & anomaly detection' },
  ];

  // CTI Tools items
  const ctiItems = [
    { name: 'VSP Predictor', route: '/cti/vsp', icon: Calculator, key: 'cti-vsp', description: 'CVSS vulnerability prediction' },
    { name: 'Red Flags', route: '/cti/redflags', icon: Flag, key: 'cti-redflags', description: 'Log analysis dashboard' },
  ];

  // Check if current page is under each section
  const isOffensivePage = pathname.startsWith('/offsec');
  const isDefensivePage = pathname.startsWith('/defsec') && !pathname.startsWith('/defsec/sqs') && !pathname.startsWith('/defsec/dtmad');
  const isCTIPage = pathname.startsWith('/cti');
  const isSqsPage = pathname.startsWith('/defsec/sqs');
  const isDtmadPage = pathname.startsWith('/defsec/dtmad');

  // Get current active item key
  const getActiveKey = () => {
    if (pathname.includes('/pentest/dashboard')) return 'pentest-dashboard';
    if (pathname.includes('/pentest/scans')) return 'pentest-scans';
    if (pathname.includes('/pentest/assets')) return 'pentest-assets';
    if (pathname.includes('/pentest/exploitation')) return 'pentest-exploitation';
    if (pathname === '/offsec/ssl' || pathname.startsWith('/offsec/ssl/')) return 'ssl-scanner';
    if (pathname.includes('/darkweb/monitor')) return 'darkweb-monitor';
    if (pathname === '/defsec/siem' || pathname.startsWith('/defsec/siem/')) return 'defsec-siem';
    if (pathname === '/defsec/sqs' || pathname.startsWith('/defsec/sqs/')) return 'sqs-dashboard';
    if (pathname === '/defsec/dtmad' || pathname.startsWith('/defsec/dtmad/')) return 'dtmad-dashboard';
    if (pathname.startsWith('/defsec')) return 'defsec';
    if (pathname === '/cti/vsp' || pathname.startsWith('/cti/vsp/')) return 'cti-vsp';
    if (pathname === '/cti/redflags' || pathname.startsWith('/cti/redflags/')) return 'cti-redflags';
    if (pathname.startsWith('/cti')) return 'cti';
    return '';
  };
  const activeKey = getActiveKey();

  const handleLogout = async () => {
    await logout();
    setUserMenuOpen(false);
  };

  const NavItem = ({ item, mobile = false }) => {
    const isActive = activeKey === item.key;

    if (mobile) {
      return (
        <Link
          href={item.route}
          onClick={() => setMobileMenuOpen(false)}
          className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
            isActive
              ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-600'
              : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
          }`}
        >
          <item.icon className="h-5 w-5" />
          <div>
            <span className="font-medium">{item.name}</span>
            <p className="text-xs text-gray-500">{item.description}</p>
          </div>
        </Link>
      );
    }

    return (
      <Link
        href={item.route}
        className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
          isActive
            ? 'border-blue-600 text-blue-700 bg-blue-50/50'
            : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
        }`}
      >
        <item.icon className={`h-4 w-4 transition-colors ${isActive ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
        <span>{item.name}</span>
      </Link>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-[1600px] mx-auto">
          {/* Top Header */}
          <div className="px-4 sm:px-6 lg:px-8 py-4">
            <div className="flex items-center justify-between">
              {/* Logo */}
              <Link href="/" className="flex items-center gap-3 group">
                <div className="relative">
                  <div className="w-10 h-10 rounded-xl gradient-bg flex items-center justify-center shadow-lg group-hover:shadow-xl transition-shadow overflow-hidden">
                    {workspace?.logo_url ? (
                      <img src={workspace.logo_url} alt="Logo" className="w-full h-full object-cover" />
                    ) : (
                      <Shield className="h-6 w-6 text-white" />
                    )}
                  </div>
                  <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-white" />
                </div>
                <div>
                  <h1 className="text-xl font-bold">
                    <span className="text-gradient">{workspace?.name || 'SECUR-EU'}</span>
                  </h1>
                  <p className="text-xs text-gray-500">SME Security Platform</p>
                </div>
              </Link>

              {/* Desktop Navigation - Only show when authenticated */}
              {isAuthenticated && (
                <nav className="hidden lg:flex items-center">
                  {/* Offensive Solutions Dropdown */}
                  <div
                    className="relative"
                    onMouseEnter={() => setOffensiveMenuOpen(true)}
                    onMouseLeave={() => setOffensiveMenuOpen(false)}
                  >
                    <button
                      className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
                        isOffensivePage
                          ? 'border-blue-600 text-blue-700 bg-blue-50/50'
                          : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
                      }`}
                    >
                      <Crosshair className={`h-4 w-4 transition-colors ${isOffensivePage ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                      <span>Offensive Solutions</span>
                      <ChevronDown className={`h-4 w-4 transition-transform ${offensiveMenuOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {/* Dropdown Menu */}
                    {offensiveMenuOpen && (
                      <div className="absolute left-0 top-full mt-0 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                        {/* Penetration Testing Tools Section */}
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <Wrench className="h-3 w-3" />
                            Penetration Testing Tools
                          </div>
                          <div className="space-y-1">
                            {pentestingItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-blue-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>

                        {/* Divider */}
                        <div className="my-2 border-t border-gray-200" />

                        {/* SSL Checker Section */}
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <Lock className="h-3 w-3" />
                            SSL Checker
                          </div>
                          <div className="space-y-1">
                            {sslItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-blue-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>

                        {/* Divider */}
                        <div className="my-2 border-t border-gray-200" />

                        {/* Darkweb Section */}
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <EyeOff className="h-3 w-3" />
                            Darkweb
                          </div>
                          <div className="space-y-1">
                            {darkwebItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-blue-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Defensive Solutions Dropdown */}
                  <div
                    className="relative"
                    onMouseEnter={() => setDefensiveMenuOpen(true)}
                    onMouseLeave={() => setDefensiveMenuOpen(false)}
                  >
                    <button
                      className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
                        isDefensivePage
                          ? 'border-green-600 text-green-700 bg-green-50/50'
                          : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
                      }`}
                    >
                      <ShieldCheck className={`h-4 w-4 transition-colors ${isDefensivePage ? 'text-green-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                      <span>Defensive Solutions</span>
                      <ChevronDown className={`h-4 w-4 transition-transform ${defensiveMenuOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {/* Defensive Dropdown Menu */}
                    {defensiveMenuOpen && (
                      <div className="absolute left-0 top-full mt-0 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <Eye className="h-3 w-3" />
                            Security Monitoring
                          </div>
                          <div className="space-y-1">
                            {defensiveItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-green-50 text-green-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-green-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* CTI Tools Dropdown */}
                  <div
                    className="relative"
                    onMouseEnter={() => setCTIMenuOpen(true)}
                    onMouseLeave={() => setCTIMenuOpen(false)}
                  >
                    <button
                      className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
                        isCTIPage
                          ? 'border-purple-600 text-purple-700 bg-purple-50/50'
                          : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
                      }`}
                    >
                      <Brain className={`h-4 w-4 transition-colors ${isCTIPage ? 'text-purple-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                      <span>CTI Tools</span>
                      <ChevronDown className={`h-4 w-4 transition-transform ${ctiMenuOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {/* CTI Dropdown Menu */}
                    {ctiMenuOpen && (
                      <div className="absolute left-0 top-full mt-0 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <AlertTriangle className="h-3 w-3" />
                            Threat Intelligence
                          </div>
                          <div className="space-y-1">
                            {ctiItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-purple-50 text-purple-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-purple-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* SQS Dropdown */}
                  <div
                    className="relative"
                    onMouseEnter={() => setSqsMenuOpen(true)}
                    onMouseLeave={() => setSqsMenuOpen(false)}
                  >
                    <button
                      className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
                        isSqsPage
                          ? 'border-cyan-600 text-cyan-700 bg-cyan-50/50'
                          : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
                      }`}
                    >
                      <Wifi className={`h-4 w-4 transition-colors ${isSqsPage ? 'text-cyan-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                      <span>Botnet Detection</span>
                      <ChevronDown className={`h-4 w-4 transition-transform ${sqsMenuOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {sqsMenuOpen && (
                      <div className="absolute left-0 top-full mt-0 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <Activity className="h-3 w-3" />
                            Botnet Detection
                          </div>
                          <div className="space-y-1">
                            {sqsItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-cyan-50 text-cyan-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-cyan-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* DTM & AD Dropdown */}
                  <div
                    className="relative"
                    onMouseEnter={() => setDtmadMenuOpen(true)}
                    onMouseLeave={() => setDtmadMenuOpen(false)}
                  >
                    <button
                      className={`group relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all duration-200 border-b-2 ${
                        isDtmadPage
                          ? 'border-teal-600 text-teal-700 bg-teal-50/50'
                          : 'border-transparent text-gray-600 hover:text-gray-900 hover:bg-gray-50/50'
                      }`}
                    >
                      <Server className={`h-4 w-4 transition-colors ${isDtmadPage ? 'text-teal-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                      <span>DTM & AD</span>
                      <ChevronDown className={`h-4 w-4 transition-transform ${dtmadMenuOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {dtmadMenuOpen && (
                      <div className="absolute left-0 top-full mt-0 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                        <div className="px-3 py-2">
                          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                            <Network className="h-3 w-3" />
                            Data Traffic Monitoring
                          </div>
                          <div className="space-y-1">
                            {dtmadItems.map((item) => (
                              <Link
                                key={item.key}
                                href={item.route}
                                className={`flex items-center gap-3 px-3 py-2 rounded-md transition-colors ${
                                  activeKey === item.key
                                    ? 'bg-teal-50 text-teal-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                }`}
                              >
                                <item.icon className={`h-4 w-4 ${activeKey === item.key ? 'text-teal-600' : 'text-gray-400'}`} />
                                <div>
                                  <span className="font-medium">{item.name}</span>
                                  <p className="text-xs text-gray-500">{item.description}</p>
                                </div>
                              </Link>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </nav>
              )}

              {/* Right Side */}
              <div className="flex items-center gap-3">
                {/* Documentation Link */}
                <Link
                  href="/docs"
                  className="hidden sm:flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <FileText className="h-4 w-4" />
                  <span>Docs</span>
                </Link>


                {/* User Menu or Login */}
                {!authLoading && (
                  isAuthenticated ? (
                    <div className="relative">
                      <button
                        onClick={() => setUserMenuOpen(!userMenuOpen)}
                        className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                      >
                        <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white text-sm font-medium overflow-hidden">
                          {user?.avatar_url ? (
                            <img src={user.avatar_url} alt="" className="w-full h-full object-cover" />
                          ) : (
                            user?.name?.charAt(0)?.toUpperCase() || 'U'
                          )}
                        </div>
                        <span className="hidden sm:block max-w-24 truncate">{user?.name}</span>
                        <ChevronDown className="h-4 w-4" />
                      </button>

                      {userMenuOpen && (
                        <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                          <div className="px-4 py-3 border-b border-gray-100">
                            <p className="text-sm font-medium text-gray-900 truncate">{user?.name}</p>
                            <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                          </div>
                          <Link
                            href="/profile"
                            onClick={() => setUserMenuOpen(false)}
                            className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                          >
                            <User className="h-4 w-4" />
                            Profile Settings
                          </Link>
                          {user?.role === 'admin' && (
                            <Link
                              href="/settings"
                              onClick={() => setUserMenuOpen(false)}
                              className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Settings className="h-4 w-4" />
                              Org Settings
                            </Link>
                          )}
                          <div className="border-t border-gray-100 mt-1 pt-1">
                            <button
                              onClick={handleLogout}
                              className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                            >
                              <LogOut className="h-4 w-4" />
                              Sign out
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <Link
                      href="/login"
                      className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
                    >
                      <LogIn className="h-4 w-4" />
                      <span>Sign in</span>
                    </Link>
                  )
                )}

                {/* Mobile Menu Button */}
                <button
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="lg:hidden p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                </button>
              </div>
            </div>
          </div>

          {/* Mobile Navigation */}
          {mobileMenuOpen && (
            <div className="lg:hidden border-t border-gray-200 bg-white animate-fade-in">
              <nav className="p-4 space-y-1">
                {/* Only show navigation items when authenticated */}
                {isAuthenticated && (
                  <>
                    {/* Offensive Solutions Collapsible */}
                    <button
                      onClick={() => setMobileOffensiveOpen(!mobileOffensiveOpen)}
                      className={`flex items-center justify-between w-full px-4 py-3 rounded-lg transition-all duration-200 ${
                        isOffensivePage
                          ? 'bg-blue-50 text-blue-700'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Crosshair className="h-5 w-5" />
                        <div className="text-left">
                          <span className="font-medium">Offensive Solutions</span>
                          <p className="text-xs text-gray-500">Security testing tools</p>
                        </div>
                      </div>
                      <ChevronRight className={`h-5 w-5 transition-transform ${mobileOffensiveOpen ? 'rotate-90' : ''}`} />
                    </button>

                    {/* Expanded Offensive Solutions Menu */}
                    {mobileOffensiveOpen && (
                      <div className="ml-4 pl-4 border-l-2 border-gray-200 space-y-1">
                        <div className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <Wrench className="h-3 w-3" />
                          Penetration Testing Tools
                        </div>
                        {pentestingItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}

                        {/* SSL Checker Section */}
                        <div className="flex items-center gap-2 px-4 py-2 mt-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <Lock className="h-3 w-3" />
                          SSL Checker
                        </div>
                        {sslItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}

                        {/* Darkweb Section */}
                        <div className="flex items-center gap-2 px-4 py-2 mt-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <EyeOff className="h-3 w-3" />
                          Darkweb
                        </div>
                        {darkwebItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}
                      </div>
                    )}

                    {/* Defensive Solutions Collapsible */}
                    <button
                      onClick={() => setMobileDefensiveOpen(!mobileDefensiveOpen)}
                      className={`flex items-center justify-between w-full px-4 py-3 rounded-lg transition-all duration-200 ${
                        isDefensivePage
                          ? 'bg-green-50 text-green-700'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <ShieldCheck className="h-5 w-5" />
                        <div className="text-left">
                          <span className="font-medium">Defensive Solutions</span>
                          <p className="text-xs text-gray-500">Security monitoring tools</p>
                        </div>
                      </div>
                      <ChevronRight className={`h-5 w-5 transition-transform ${mobileDefensiveOpen ? 'rotate-90' : ''}`} />
                    </button>

                    {/* Expanded Defensive Solutions Menu */}
                    {mobileDefensiveOpen && (
                      <div className="ml-4 pl-4 border-l-2 border-gray-200 space-y-1">
                        <div className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <Eye className="h-3 w-3" />
                          Security Monitoring
                        </div>
                        {defensiveItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}
                      </div>
                    )}

                    {/* CTI Tools Collapsible */}
                    <button
                      onClick={() => setMobileCTIOpen(!mobileCTIOpen)}
                      className={`flex items-center justify-between w-full px-4 py-3 rounded-lg transition-all duration-200 ${
                        isCTIPage
                          ? 'bg-purple-50 text-purple-700'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Brain className="h-5 w-5" />
                        <div className="text-left">
                          <span className="font-medium">CTI Tools</span>
                          <p className="text-xs text-gray-500">Threat intelligence</p>
                        </div>
                      </div>
                      <ChevronRight className={`h-5 w-5 transition-transform ${mobileCTIOpen ? 'rotate-90' : ''}`} />
                    </button>

                    {/* Expanded CTI Menu */}
                    {mobileCTIOpen && (
                      <div className="ml-4 pl-4 border-l-2 border-gray-200 space-y-1">
                        <div className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <AlertTriangle className="h-3 w-3" />
                          Threat Intelligence
                        </div>
                        {ctiItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}
                      </div>
                    )}

                    {/* SQS Collapsible */}
                    <button
                      onClick={() => setMobileSqsOpen(!mobileSqsOpen)}
                      className={`flex items-center justify-between w-full px-4 py-3 rounded-lg transition-all duration-200 ${
                        isSqsPage
                          ? 'bg-cyan-50 text-cyan-700'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Wifi className="h-5 w-5" />
                        <div className="text-left">
                          <span className="font-medium">Botnet Detection</span>
                          <p className="text-xs text-gray-500">MIRAI botnet detection & monitoring</p>
                        </div>
                      </div>
                      <ChevronRight className={`h-5 w-5 transition-transform ${mobileSqsOpen ? 'rotate-90' : ''}`} />
                    </button>

                    {mobileSqsOpen && (
                      <div className="ml-4 pl-4 border-l-2 border-gray-200 space-y-1">
                        <div className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <Activity className="h-3 w-3" />
                          Botnet Detection
                        </div>
                        {sqsItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}
                      </div>
                    )}

                    {/* DTM & AD Collapsible */}
                    <button
                      onClick={() => setMobileDtmadOpen(!mobileDtmadOpen)}
                      className={`flex items-center justify-between w-full px-4 py-3 rounded-lg transition-all duration-200 ${
                        isDtmadPage
                          ? 'bg-teal-50 text-teal-700'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Server className="h-5 w-5" />
                        <div className="text-left">
                          <span className="font-medium">DTM & AD</span>
                          <p className="text-xs text-gray-500">Traffic monitoring & anomaly detection</p>
                        </div>
                      </div>
                      <ChevronRight className={`h-5 w-5 transition-transform ${mobileDtmadOpen ? 'rotate-90' : ''}`} />
                    </button>

                    {mobileDtmadOpen && (
                      <div className="ml-4 pl-4 border-l-2 border-gray-200 space-y-1">
                        <div className="flex items-center gap-2 px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                          <Network className="h-3 w-3" />
                          Data Traffic Monitoring
                        </div>
                        {dtmadItems.map((item) => (
                          <NavItem key={item.key} item={item} mobile />
                        ))}
                      </div>
                    )}
                  </>
                )}

                {/* Mobile Auth Section */}
                <div className="border-t border-gray-200 mt-4 pt-4">
                  {isAuthenticated ? (
                    <>
                      <div className="px-4 py-2 mb-2">
                        <p className="text-sm font-medium text-gray-900">{user?.name}</p>
                        <p className="text-xs text-gray-500">{user?.email}</p>
                      </div>
                      <Link
                        href="/profile"
                        onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-600 hover:bg-gray-50"
                      >
                        <User className="h-5 w-5" />
                        <span className="font-medium">Profile</span>
                      </Link>
                      {user?.role === 'admin' && (
                        <Link
                          href="/settings"
                          onClick={() => setMobileMenuOpen(false)}
                          className="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-600 hover:bg-gray-50"
                        >
                          <Settings className="h-5 w-5" />
                          <span className="font-medium">Org Settings</span>
                        </Link>
                      )}
                      <button
                        onClick={() => { handleLogout(); setMobileMenuOpen(false); }}
                        className="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-red-600 hover:bg-red-50"
                      >
                        <LogOut className="h-5 w-5" />
                        <span className="font-medium">Sign out</span>
                      </button>
                    </>
                  ) : (
                    <Link
                      href="/login"
                      onClick={() => setMobileMenuOpen(false)}
                      className="flex items-center gap-3 px-4 py-3 rounded-lg text-white bg-blue-600 hover:bg-blue-700"
                    >
                      <LogIn className="h-5 w-5" />
                      <span className="font-medium">Sign in</span>
                    </Link>
                  )}
                </div>
              </nav>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden animate-fade-in">
            <div className="p-6">
              {children}
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-auto">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 py-10">
          {/* Top row: brand left, links + EU right */}
          <div className="flex flex-col lg:flex-row gap-10 lg:gap-16">
            {/* Brand */}
            <div className="lg:max-w-sm">
              <Link href="/" className="flex items-center gap-3 mb-3">
                <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center overflow-hidden">
                  {workspace?.logo_url ? (
                    <img src={workspace.logo_url} alt="Logo" className="w-full h-full object-cover" />
                  ) : (
                    <Shield className="h-4 w-4 text-white" />
                  )}
                </div>
                <span className="text-lg font-bold text-gray-900">{workspace?.name || 'SECUR-EU'}</span>
              </Link>
              <p className="text-sm text-gray-500 leading-relaxed mb-4">
                Enhancing security of European SMEs in response to cybersecurity threats.
                Open-source offensive testing, defensive monitoring, and cyber threat intelligence.
              </p>
              <div className="flex items-center gap-1">
                <a
                  href="https://www.secur-eu.eu/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2 text-gray-400 hover:text-blue-600 rounded-md transition-colors"
                  title="Project Website"
                >
                  <Globe className="h-4 w-4" />
                </a>
                <a
                  href="https://github.com/SecureEU"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2 text-gray-400 hover:text-gray-900 rounded-md transition-colors"
                  title="GitHub"
                >
                  <Github className="h-4 w-4" />
                </a>
              </div>
            </div>

            {/* Right side: Resources + EU Project */}
            <div className="flex flex-col sm:flex-row gap-10 sm:gap-16 lg:ml-auto">
              {/* Resources */}
              <div>
                <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Resources</h4>
                <ul className="space-y-2">
                  <li>
                    <Link href="/docs" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                      Documentation
                    </Link>
                  </li>
                  <li>
                    <Link href="/about" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                      About
                    </Link>
                  </li>
                  <li>
                    <Link href="/contact" className="text-sm text-gray-600 hover:text-blue-600 transition-colors">
                      Contact
                    </Link>
                  </li>
                </ul>
              </div>

              {/* EU Funding */}
              <div className="sm:max-w-[240px]">
                <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">EU Project</h4>
                <p className="text-sm text-gray-600 leading-relaxed mb-2">
                  Funded by the European Union's DIGITAL programme under the European Cybersecurity Competence Centre.
                </p>
                <p className="text-xs text-gray-400">
                  Grant Agreement No. 101128029
                </p>
              </div>
            </div>
          </div>

          {/* Bottom bar */}
          <div className="border-t border-gray-100 mt-8 pt-6 flex flex-col sm:flex-row justify-between items-center gap-3">
            <p className="text-xs text-gray-400">
              © {new Date().getFullYear()} {workspace?.name || 'SECUR-EU'} — European SME Security Platform
            </p>
            <a
              href="https://www.secur-eu.eu/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs text-gray-400 hover:text-blue-600 transition-colors"
            >
              www.secur-eu.eu
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Layout;
