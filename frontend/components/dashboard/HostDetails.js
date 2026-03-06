'use client'

import React, { useState, useEffect } from 'react';
import { Server, Globe, Clock } from 'lucide-react';
import NmapResults from './NmapResults';
import ZapResults from './ZapResults';
import { useAuth } from '@/lib/auth';

const HostDetails = ({ scanId }) => {
  const [activeTab, setActiveTab] = useState('nmap');
  const [scanData, setScanData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { authFetch, loading: authLoading, API_URL } = useAuth();

  useEffect(() => {
    const fetchScanData = async () => {
      try {
        const response = await authFetch(`${API_URL}/scans/${scanId}`);
        if (!response.ok) throw new Error('Failed to fetch scan data');
        const data = await response.json();
        setScanData(data);
        if (data.type === 'web') {
          setActiveTab('zap');
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    // Wait for auth to initialize before fetching
    if (scanId && !authLoading) {
      fetchScanData();
    }
  }, [scanId, authLoading]);

  if (loading) return <div className="p-6">Loading scan details...</div>;
  if (error) return <div className="p-6 text-red-600">Error: {error}</div>;
  if (!scanData) return <div className="p-6">No scan data available</div>;

  const TabButton = ({ label, icon: Icon, isActive, onClick, alertCount = 0 }) => (
    <button
      onClick={onClick}
      className={`
        flex items-center gap-2 px-6 py-3 font-medium text-sm focus:outline-none transition-all duration-200
        ${isActive 
          ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50' 
          : 'text-slate-600 border-b-2 border-transparent hover:text-slate-900 hover:border-slate-300'
        }
      `}
    >
      <Icon className="h-4 w-4" />
      {label}
      {alertCount > 0 && (
        <span className="ml-2 bg-red-100 text-red-800 text-xs font-medium px-2 py-0.5 rounded-full">
          {alertCount}
        </span>
      )}
    </button>
  );

  const zapAlertCount = scanData.zdata?.length || 0;
  const formattedStartTime = scanData.start_time?.replace(/_/g, ':');
  const formattedEndTime = scanData.end_time?.replace(/_/g, ':');

  return (
    <div className="bg-white rounded-lg shadow-lg border border-slate-200">
      <div className="p-6 border-b border-slate-200 bg-slate-50">
        <div className="flex justify-between items-start">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h2 className="text-xl font-bold text-slate-900">{scanData.scan_name || scanData.name}</h2>
              <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${
                scanData.status === 'finished' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
              }`}>
                {scanData.status}
              </span>
            </div>
            <div className="flex items-center gap-4 text-sm text-slate-600">
              <span className="flex items-center gap-1">
                <Clock className="h-4 w-4" />
                Start: {formattedStartTime?.replace('T', ' ')}
              </span>
              <span className="flex items-center gap-1">
                <Clock className="h-4 w-4" />
                End: {formattedEndTime?.replace('T', ' ') || 'Running'}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="border-b border-slate-200">
        <div className="flex">
          {(scanData.type === 'network' || scanData.type === 'multi') && (
            <TabButton
              label="Network Scan"
              icon={Server}
              isActive={activeTab === 'nmap'}
              onClick={() => setActiveTab('nmap')}
            />
          )}
          {(scanData.type === 'web' || scanData.type === 'multi') && (
            <TabButton
              label="Web Application Scan"
              icon={Globe}
              isActive={activeTab === 'zap'}
              onClick={() => setActiveTab('zap')}
              alertCount={zapAlertCount}
            />
          )}
        </div>
      </div>

      <div>
        {activeTab === 'nmap' && (scanData.type === 'network' || scanData.type === 'multi') && (
          <NmapResults data={scanData} />
        )}
        {activeTab === 'zap' && (scanData.type === 'web' || scanData.type === 'multi') && (
          <ZapResults data={scanData} />
        )}
      </div>
    </div>
  );
};

export default HostDetails;