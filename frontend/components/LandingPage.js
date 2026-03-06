'use client';

import Link from 'next/link';
import {
  Shield, Target, Globe, Search, Lock, Zap, CheckCircle,
  ArrowRight, BarChart3, Users, Code, AlertTriangle,
  Layers, Activity, TrendingUp, BookOpen, Bot, Award,
  Network, Server, Bug, Eye, EyeOff, Calculator, Flag,
  ShieldCheck, Brain, Crosshair, FileText
} from 'lucide-react';

export default function LandingPage({ needsSetup = false, workspace = null }) {
  const features = [
    {
      icon: Crosshair,
      title: 'Offensive Security',
      description: 'Complete penetration testing suite with Nmap scanning, ZAP web security testing, Metasploit exploitation, and SSL analysis.',
      color: 'red'
    },
    {
      icon: ShieldCheck,
      title: 'Defensive Security',
      description: 'Real-time SIEM dashboard for security event monitoring, log analysis, and incident detection across your infrastructure.',
      color: 'green'
    },
    {
      icon: Brain,
      title: 'Cyber Threat Intelligence',
      description: 'AI-powered vulnerability prediction (VSP), log analysis with Red Flags, and threat intelligence gathering.',
      color: 'purple'
    },
    {
      icon: EyeOff,
      title: 'Darkweb Monitoring',
      description: 'Monitor the dark web for leaked credentials, data breaches, and mentions of your organization.',
      color: 'gray'
    }
  ];

  const stats = [
    { value: '8+', label: 'Integrated Tools' },
    { value: '3', label: 'Security Domains' },
    { value: '24/7', label: 'Monitoring' },
    { value: 'Real-time', label: 'Threat Detection' }
  ];

  const securityCapabilities = [
    { name: 'Port Scanning', icon: Network, description: 'TCP/UDP port discovery and service detection with Nmap' },
    { name: 'Web App Testing', icon: Code, description: 'OWASP Top 10 vulnerability scanning with ZAP' },
    { name: 'SSL/TLS Analysis', icon: Lock, description: 'Certificate validation and encryption assessment' },
    { name: 'Darkweb Monitoring', icon: EyeOff, description: 'Leaked credential and data breach detection' },
    { name: 'SIEM Dashboard', icon: Eye, description: 'Real-time security event monitoring and alerting' },
    { name: 'VSP Predictor', icon: Calculator, description: 'AI-powered CVSS vulnerability scoring' },
    { name: 'Red Flags Analysis', icon: Flag, description: 'Automated log analysis and incident detection' },
    { name: 'Exploit Validation', icon: Target, description: 'Metasploit integration for penetration testing' },
    { name: 'Threat Intelligence', icon: Brain, description: 'CTI feeds and threat landscape analysis' }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <Link href="/" className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg overflow-hidden">
                {workspace?.logo_url ? (
                  <img src={workspace.logo_url} alt="Logo" className="w-full h-full object-cover" />
                ) : (
                  <Shield className="w-6 h-6 text-white" />
                )}
              </div>
              <div>
                <h1 className="text-xl font-bold text-gray-900">{workspace?.name || 'SECUR-EU'}</h1>
                <p className="text-xs text-gray-500">SME Security Platform</p>
              </div>
            </Link>
            <nav className="hidden md:flex items-center gap-6">
              <Link href="/docs" className="text-gray-600 hover:text-gray-900 text-sm font-medium">
                Documentation
              </Link>
              <Link href="/about" className="text-gray-600 hover:text-gray-900 text-sm font-medium">
                About
              </Link>
              <Link href="/login" className="text-gray-600 hover:text-gray-900 text-sm font-medium">
                Sign In
              </Link>
              {needsSetup ? (
                <Link
                  href="/setup"
                  className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
                >
                  Set Up Workspace
                </Link>
              ) : (
                <Link
                  href="/register"
                  className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
                >
                  Get Started
                </Link>
              )}
            </nav>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-blue-50 via-white to-indigo-50"></div>
        <div className="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-blue-100/30 to-transparent"></div>

        <div className="relative max-w-7xl mx-auto px-6 py-24 lg:py-32">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <div className="inline-flex items-center px-4 py-2 bg-blue-100 text-blue-700 rounded-full text-sm font-medium mb-6">
                <Shield className="w-4 h-4 mr-2" />
                European SME Security
              </div>

              <h1 className="text-5xl lg:text-6xl font-bold text-gray-900 leading-tight mb-6">
                Protect Your
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-500"> Infrastructure</span>
              </h1>

              <p className="text-xl text-gray-600 mb-8 leading-relaxed">
                The complete security operations platform combining offensive testing, defensive monitoring,
                and cyber threat intelligence. Protect your infrastructure with confidence.
              </p>

              <div className="flex flex-wrap items-center gap-3">
                {needsSetup ? (
                  <Link
                    href="/setup"
                    className="inline-flex items-center px-5 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-all shadow-sm hover:shadow-md"
                  >
                    <Shield className="w-4 h-4 mr-2" />
                    Set Up Workspace
                  </Link>
                ) : (
                  <Link
                    href="/register"
                    className="inline-flex items-center px-5 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-all shadow-sm hover:shadow-md"
                  >
                    Start Free
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </Link>
                )}
                <Link
                  href="/login"
                  className="inline-flex items-center px-5 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-all"
                >
                  Sign In
                </Link>
                <Link
                  href="/docs"
                  className="inline-flex items-center px-5 py-2.5 text-sm font-medium text-indigo-700 bg-indigo-50 rounded-lg hover:bg-indigo-100 transition-all"
                >
                  <BookOpen className="w-4 h-4 mr-1.5" />
                  Documentation
                </Link>
              </div>

              {!needsSetup && (
                <div className="mt-8 flex items-center gap-6 text-sm text-gray-500">
                  <div className="flex items-center gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500" />
                    Free to start
                  </div>
                  <div className="flex items-center gap-2">
                    <CheckCircle className="w-5 h-5 text-green-500" />
                    No credit card required
                  </div>
                </div>
              )}
            </div>

            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-3xl blur-3xl opacity-20"></div>
              <div className="relative bg-white rounded-3xl shadow-2xl p-8 border border-gray-100">
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-3 h-3 rounded-full bg-red-500"></div>
                  <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                  <div className="w-3 h-3 rounded-full bg-green-500"></div>
                  <span className="ml-4 text-sm text-gray-500 font-mono">security-scan.log</span>
                </div>
                <div className="font-mono text-sm space-y-2">
                  <p className="text-gray-500"># Running SECUR-EU Security Suite...</p>
                  <p className="text-blue-600">[NMAP] Scanning 192.168.1.0/24</p>
                  <p className="text-green-600">[PASS] 24 hosts discovered</p>
                  <p className="text-blue-600">[ZAP] Web application scan</p>
                  <p className="text-yellow-600">[WARN] 3 medium vulnerabilities</p>
                  <p className="text-cyan-600">[SSL] Certificates validated</p>
                  <p className="text-purple-600">[SIEM] 0 critical events</p>
                  <p className="text-pink-600">[RED FLAGS] Logs analyzed</p>
                  <p className="text-gray-500">---</p>
                  <p className="text-green-600 font-bold">[DONE] Security Score: 87/100</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-gradient-to-b from-blue-100 to-blue-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <div key={index} className="text-center">
                <div className="text-4xl lg:text-5xl font-bold text-gray-900 mb-2">{stat.value}</div>
                <div className="text-gray-700">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-24">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Complete Security Operations Platform
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              Three integrated security domains: Offensive, Defensive, and Cyber Threat Intelligence.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => {
              const Icon = feature.icon;
              const colorClasses = {
                red: 'bg-red-100 text-red-600',
                orange: 'bg-orange-100 text-orange-600',
                blue: 'bg-blue-100 text-blue-600',
                purple: 'bg-purple-100 text-purple-600',
                green: 'bg-green-100 text-green-600',
                gray: 'bg-gray-100 text-gray-600'
              };

              return (
                <div
                  key={index}
                  className="bg-white rounded-2xl p-8 shadow-lg border border-gray-100 hover:shadow-xl hover:border-gray-200 transition-all group"
                >
                  <div className={`w-14 h-14 rounded-xl ${colorClasses[feature.color]} flex items-center justify-center mb-6 group-hover:scale-110 transition-transform`}>
                    <Icon className="w-7 h-7" />
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-3">{feature.title}</h3>
                  <p className="text-gray-600 leading-relaxed">{feature.description}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Security Capabilities Section */}
      <section className="py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Full Security Capabilities
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              From network scanning to threat intelligence - all the tools you need in one platform.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            {securityCapabilities.map((capability, index) => {
              const Icon = capability.icon;
              return (
                <div
                  key={index}
                  className="bg-white rounded-xl p-6 flex items-start gap-4 shadow-sm border border-gray-100 hover:shadow-md transition-shadow"
                >
                  <div className="w-12 h-12 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
                    <Icon className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">{capability.name}</h3>
                    <p className="text-sm text-gray-600">{capability.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-24 bg-gradient-to-b from-white to-blue-100">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              How It Works
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              Get started in minutes with our simple three-step process.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              {
                step: '01',
                title: 'Add Your Assets',
                description: 'Register your hosts, networks, and web applications. Configure monitoring for darkweb exposure.',
                icon: Server
              },
              {
                step: '02',
                title: 'Assess & Test',
                description: 'Run offensive scans with Nmap, ZAP, and SSL checker. Analyze threats with VSP and Red Flags.',
                icon: Activity
              },
              {
                step: '03',
                title: 'Monitor & Respond',
                description: 'Track events in SIEM dashboard. Get real-time alerts and AI-powered threat intelligence.',
                icon: TrendingUp
              }
            ].map((item, index) => {
              const Icon = item.icon;
              return (
                <div key={index} className="relative">
                  <div className="text-8xl font-bold text-gray-100 absolute -top-6 -left-2">{item.step}</div>
                  <div className="relative bg-white rounded-2xl p-8 shadow-lg border border-gray-100">
                    <div className="w-14 h-14 rounded-xl bg-blue-600 text-white flex items-center justify-center mb-6">
                      <Icon className="w-7 h-7" />
                    </div>
                    <h3 className="text-xl font-bold text-gray-900 mb-3">{item.title}</h3>
                    <p className="text-gray-600">{item.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Integrations Section */}
      <section className="py-24 bg-gradient-to-b from-blue-100 to-blue-50">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Integrated Tools & Services
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              Industry-leading tools for offensive security, defensive monitoring, and threat intelligence.
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {[
              { name: 'Nmap', description: 'Network Scanner', icon: Network, color: 'from-emerald-500 to-teal-600' },
              { name: 'OWASP ZAP', description: 'Web Security', icon: Shield, color: 'from-orange-500 to-amber-600' },
              { name: 'Metasploit', description: 'Penetration Testing', icon: Target, color: 'from-red-500 to-rose-600' },
              { name: 'SSL Checker', description: 'Certificate Analysis', icon: Lock, color: 'from-cyan-500 to-blue-600' },
              { name: 'Darkweb Monitor', description: 'Breach Detection', icon: EyeOff, color: 'from-gray-600 to-gray-800' },
              { name: 'SIEM', description: 'Event Monitoring', icon: Eye, color: 'from-green-500 to-emerald-600' },
              { name: 'VSP Predictor', description: 'CVSS Prediction', icon: Calculator, color: 'from-purple-500 to-violet-600' },
              { name: 'Red Flags', description: 'Log Analysis', icon: Flag, color: 'from-rose-500 to-pink-600' }
            ].map((tool, index) => (
              <div key={index} className="group relative bg-white rounded-2xl p-6 text-center transition-all duration-300 border border-gray-100 hover:border-gray-200 hover:shadow-lg hover:scale-105">
                <div className={`w-14 h-14 mx-auto mb-4 rounded-xl bg-gradient-to-br ${tool.color} flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-300`}>
                  <tool.icon className="w-7 h-7 text-white" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">{tool.name}</h3>
                <p className="text-sm text-gray-500 group-hover:text-gray-700 transition-colors">{tool.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-4xl font-bold text-gray-900 mb-6">
            Ready to Secure Your Infrastructure?
          </h2>
          <p className="text-xl text-gray-600 mb-8">
            Join European SMEs who trust SECUR-EU for offensive testing, defensive monitoring, and threat intelligence.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              href="/register"
              className="inline-flex items-center justify-center px-8 py-4 bg-blue-600 text-white font-semibold rounded-xl hover:bg-blue-700 transition-all shadow-lg shadow-blue-200"
            >
              Get Started Free
              <ArrowRight className="w-5 h-5 ml-2" />
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center justify-center px-8 py-4 bg-gray-100 text-gray-700 font-semibold rounded-xl hover:bg-gray-200 transition-all"
            >
              Sign In
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gradient-to-b from-blue-100 to-blue-50 py-12">
        <div className="max-w-7xl mx-auto px-6">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center overflow-hidden">
                {workspace?.logo_url ? (
                  <img src={workspace.logo_url} alt="Logo" className="w-full h-full object-cover" />
                ) : (
                  <Shield className="w-6 h-6 text-white" />
                )}
              </div>
              <span className="text-lg font-bold text-gray-900">{workspace?.name || 'SECUR-EU'}</span>
            </div>
            <nav className="flex items-center gap-6 text-sm">
              <Link href="/docs" className="text-gray-700 hover:text-gray-900">Documentation</Link>
              <Link href="/about" className="text-gray-700 hover:text-gray-900">About</Link>
              <Link href="/contact" className="text-gray-700 hover:text-gray-900">Contact</Link>
              <Link href="/privacy-policy" className="text-gray-700 hover:text-gray-900">Privacy</Link>
              <Link href="/terms-and-conditions" className="text-gray-700 hover:text-gray-900">Terms</Link>
            </nav>
          </div>
          <div className="border-t border-blue-200 mt-8 pt-8 flex flex-col sm:flex-row justify-between items-center gap-4 text-sm text-gray-700">
            <p>© {new Date().getFullYear()} {workspace?.name || 'SECUR-EU'} — European SME Security Platform</p>
            <p>Funded by the European Union</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
