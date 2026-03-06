'use client'

import Layout from '@/components/Layout'
import ProtectedRoute from '@/components/ProtectedRoute'
import { Shield, Lock, Eye, Bell, FileSearch, Server } from 'lucide-react'
import Link from 'next/link'

const activeTools = [
  { name: 'SIEM Dashboard', icon: Eye, description: 'SEUXDR host-based intrusion detection with agent management, MITRE ATT&CK alerts, and security event monitoring', route: '/defsec/siem' },
]

const comingSoonItems = [
  { name: 'Threat Detection', icon: Bell, description: 'Real-time threat alerts and notifications', route: '/defsec/threats' },
  { name: 'Log Analysis', icon: FileSearch, description: 'Centralized log management and search', route: '/defsec/logs' },
  { name: 'Asset Protection', icon: Server, description: 'Endpoint and infrastructure security', route: '/defsec/protection' },
]

export default function DefensiveSolutionsPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Defensive Solutions</h1>
              <p className="text-sm text-slate-500 mt-1">Security monitoring, detection, and response tools</p>
            </div>
            <div className="p-3 bg-green-100 rounded-xl">
              <Shield className="h-8 w-8 text-green-600" />
            </div>
          </div>

          {/* Info Banner */}
          <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-xl p-6">
            <div className="flex items-start gap-4">
              <div className="p-2 bg-green-100 rounded-lg">
                <Lock className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <h3 className="font-semibold text-green-900">Defense-in-Depth Security</h3>
                <p className="text-sm text-green-700 mt-1">
                  Comprehensive defensive security tools to monitor, detect, and respond to threats across your infrastructure.
                </p>
              </div>
            </div>
          </div>

          {/* Active Tools */}
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Active Tools</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {activeTools.map((item) => (
                <Link
                  key={item.name}
                  href={item.route}
                  className="bg-white border border-green-200 rounded-xl p-6 hover:border-green-400 hover:shadow-md transition-all group"
                >
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-green-100 rounded-lg">
                      <item.icon className="h-6 w-6 text-green-600" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">{item.name}</h3>
                      <p className="text-sm text-slate-500 mt-1">{item.description}</p>
                      <span className="inline-block mt-3 px-3 py-1 text-xs font-medium bg-green-100 text-green-700 rounded-full">
                        Active
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>

          {/* Coming Soon Cards */}
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Planned Tools</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {comingSoonItems.map((item) => (
                <div
                  key={item.name}
                  className="bg-white border border-slate-200 rounded-xl p-6 hover:border-green-300 hover:shadow-md transition-all group"
                >
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-slate-100 rounded-lg group-hover:bg-green-100 transition-colors">
                      <item.icon className="h-6 w-6 text-slate-600 group-hover:text-green-600 transition-colors" />
                    </div>
                    <div className="flex-1">
                      <h3 className="font-semibold text-slate-900">{item.name}</h3>
                      <p className="text-sm text-slate-500 mt-1">{item.description}</p>
                      <span className="inline-block mt-3 px-3 py-1 text-xs font-medium bg-amber-100 text-amber-700 rounded-full">
                        Coming Soon
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </ProtectedRoute>
    </Layout>
  )
}
