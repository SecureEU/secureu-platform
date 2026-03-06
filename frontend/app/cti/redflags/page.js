'use client'

import Layout from '@/components/Layout'
import RedFlagsDashboard from '@/components/cti/RedFlagsDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function RedFlagsPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <RedFlagsDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
