'use client'

import Layout from '@/components/Layout'
import VSPDashboard from '@/components/cti/VSPDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function VSPPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <VSPDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
