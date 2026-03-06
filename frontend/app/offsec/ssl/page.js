'use client'

import Layout from '@/components/Layout'
import SSLChecker from '@/components/sslchecker/SSLChecker'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function SSLCheckerPage() {
  return (
    <Layout>
      <ProtectedRoute>
        <SSLChecker />
      </ProtectedRoute>
    </Layout>
  )
}
