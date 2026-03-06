'use client'

import { useState, useEffect } from 'react'
import { useAuth } from '@/lib/auth'
import Layout from '@/components/Layout'
import LandingPage from '../components/LandingPage'
import UnifiedDashboard from '@/components/UnifiedDashboard'
import ProtectedRoute from '@/components/ProtectedRoute'

export default function HomePage() {
  const { isAuthenticated, loading } = useAuth()
  const [needsSetup, setNeedsSetup] = useState(false)
  const [workspace, setWorkspace] = useState(null)

  useEffect(() => {
    fetch('/api/v1/setup/status')
      .then(res => res.json())
      .then(data => {
        if (data.needsSetup) setNeedsSetup(true)
      })
      .catch(() => {})

    // Fetch workspace for logo/name (public info for landing page)
    fetch('/api/v1/settings/workspace')
      .then(res => res.ok ? res.json() : null)
      .then(data => { if (data) setWorkspace(data) })
      .catch(() => {})
  }, [])

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  // Show landing page for unauthenticated users
  if (!isAuthenticated) {
    return <LandingPage needsSetup={needsSetup} workspace={workspace} />
  }

  // Show unified dashboard for authenticated users
  return (
    <Layout>
      <ProtectedRoute>
        <UnifiedDashboard />
      </ProtectedRoute>
    </Layout>
  )
}
