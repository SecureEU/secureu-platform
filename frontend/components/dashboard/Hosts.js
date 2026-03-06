'use client'

import React, { useState, useEffect } from 'react';
import HostsTable from './HostsTable';
import HostDetails from './HostDetails';
import { useAuth } from '@/lib/auth';

const Hosts = () => {
  const [scans, setScans] = useState([]);
  const [selectedScanId, setSelectedScanId] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { authFetch, loading: authLoading, API_URL } = useAuth();

  useEffect(() => {
    const fetchScans = async () => {
      try {
        const response = await authFetch(`${API_URL}/scans`);
        if (!response.ok) throw new Error('Failed to fetch scans');
        const data = await response.json();
        setScans(data || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    // Wait for auth to initialize before fetching
    if (!authLoading) {
      fetchScans();
    }
  }, [authLoading]);

  const filteredScans = scans.filter(scan => 
    scan.scan_name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    scan.name?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleBackToList = () => {
    setSelectedScanId(null);
  };

  if (loading) return <div className="p-4">Loading scans...</div>;
  if (error) return <div className="p-4 text-red-600">Error: {error}</div>;

  return (
    <div className="space-y-6">
      {!selectedScanId ? (
        <HostsTable 
          scans={filteredScans}
          onScanSelect={setSelectedScanId}
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
        />
      ) : (
        <div>
          <button
            onClick={handleBackToList}
            className="mb-4 flex items-center gap-2 text-blue-600 hover:text-blue-800 transition-colors duration-150"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
            </svg>
            Back to Scan List
          </button>
          <HostDetails scanId={selectedScanId} />
        </div>
      )}
    </div>
  );
};

export default Hosts;