const API_BASE_URL = process.env.NEXT_PUBLIC_PENTEST_API_URL || 'http://localhost:3001';

export const fetchScans = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans`);
    if (!response.ok) throw new Error('Failed to fetch scans');
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error fetching scans:', error);
    return { success: false, error: error.message };
  }
};

export const createScan = async (scanData) => {
  try {
    if (!scanData.name || !scanData.target || !scanData.scan_type) {
      throw new Error('Missing required fields');
    }

    if (scanData.scan_type === 'network' && (!scanData.scan_speed || !scanData.scan_intensity)) {
      throw new Error('Missing required network scan parameters');
    }

    if (scanData.scan_type === 'web' && (!scanData.web_scan_type || !scanData.scan_speed || !scanData.port)) {
      throw new Error('Missing required web scan parameters');
    }

    const response = await fetch(`${API_BASE_URL}/scans`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(scanData)
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to create scan');
    }

    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error creating scan:', error);
    return { success: false, error: error.message };
  }
};

export const getScanStatus = async (scanId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans/${scanId}/status`);
    if (!response.ok) throw new Error('Failed to fetch scan status');
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error fetching scan status:', error);
    return { success: false, error: error.message };
  }
};

export const getScanResults = async (scanId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans/${scanId}/results`);
    if (!response.ok) throw new Error('Failed to fetch scan results');
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error fetching scan results:', error);
    return { success: false, error: error.message };
  }
};

export const startScan = async (scanId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans/${scanId}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to start scan');
    }

    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error starting scan:', error);
    return { success: false, error: error.message };
  }
};

export const stopScan = async (scanId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans/${scanId}/stop`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });

    if (!response.ok) throw new Error('Failed to stop scan');
    const data = await response.json();
    return { success: true, data };
  } catch (error) {
    console.error('Error stopping scan:', error);
    return { success: false, error: error.message };
  }
};

export const deleteScan = async (scanId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/scans/${scanId}`, {
      method: 'DELETE'
    });

    if (!response.ok) throw new Error('Failed to delete scan');
    return { success: true };
  } catch (error) {
    console.error('Error deleting scan:', error);
    return { success: false, error: error.message };
  }
};