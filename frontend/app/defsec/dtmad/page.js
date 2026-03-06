'use client'

import Layout from '@/components/Layout'
import DTMADDashboard from '@/components/dtmad/DTMADDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function DTMADPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <DTMADDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
