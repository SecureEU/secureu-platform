'use client'

import Layout from '@/components/Layout'
import ProtectedRoute from '@/components/ProtectedRoute'
import { Brain, Database, Share2, FileText, Globe, Radar } from 'lucide-react'
import Link from 'next/link'

const ctiItems = [
  { name: 'Threat Intelligence', icon: Brain, description: 'Aggregated threat data and indicators', route: '/cti/intelligence' },
  { name: 'IOC Database', icon: Database, description: 'Indicators of Compromise management', route: '/cti/ioc' },
  { name: 'STIX/TAXII', icon: Share2, description: 'Threat intelligence sharing and exchange', route: '/cti/stix' },
  { name: 'Threat Reports', icon: FileText, description: 'Detailed threat analysis reports', route: '/cti/reports' },
  { name: 'Feed Management', icon: Globe, description: 'External threat feed integration', route: '/cti/feeds' },
  { name: 'Threat Hunting', icon: Radar, description: 'Proactive threat discovery tools', route: '/cti/hunting' },
]

export default function CTIToolsPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">CTI Tools</h1>
              <p className="text-sm text-slate-500 mt-1">Cyber Threat Intelligence collection and analysis</p>
            </div>
            <div className="p-3 bg-purple-100 rounded-xl">
              <Brain className="h-8 w-8 text-purple-600" />
            </div>
          </div>

          {/* Info Banner */}
          <div className="bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-200 rounded-xl p-6">
            <div className="flex items-start gap-4">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Radar className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <h3 className="font-semibold text-purple-900">Intelligence-Driven Security</h3>
                <p className="text-sm text-purple-700 mt-1">
                  Leverage threat intelligence to stay ahead of adversaries with comprehensive IOC management, threat feeds, and analysis tools.
                </p>
              </div>
            </div>
          </div>

          {/* CTI Tools Cards */}
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-4">Available Tools</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {ctiItems.map((item) => (
                <div
                  key={item.name}
                  className="bg-white border border-slate-200 rounded-xl p-6 hover:border-purple-300 hover:shadow-md transition-all group"
                >
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-slate-100 rounded-lg group-hover:bg-purple-100 transition-colors">
                      <item.icon className="h-6 w-6 text-slate-600 group-hover:text-purple-600 transition-colors" />
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
