'use client'

import Layout from '@/components/Layout'
import SIEMDashboard from '@/components/siem/SIEMDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function SIEMPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <SIEMDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
