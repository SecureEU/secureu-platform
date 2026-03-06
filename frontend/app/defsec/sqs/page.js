'use client'

import Layout from '@/components/Layout'
import SQSDashboard from '@/components/sqs/SQSDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function SQSPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <SQSDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
