'use client'

import Layout from '@/components/Layout'
import DarkwebDashboard from '@/components/darkweb/DarkwebDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function DarkwebMonitorPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <DarkwebDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
