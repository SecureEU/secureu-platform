'use client';

import { createContext, useContext, useState, useEffect } from 'react';

const API_URL = process.env.NEXT_PUBLIC_PENTEST_API_URL || 'http://localhost:3001';
const AUTH_URL = ''; // Auth is handled by Next.js API routes

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [accessToken, setAccessToken] = useState(null);

  // Initialize auth state from localStorage
  useEffect(() => {
    const storedToken = localStorage.getItem('accessToken');
    const storedUser = localStorage.getItem('user');
    const storedRefreshToken = localStorage.getItem('refreshToken');

    if (storedToken && storedUser) {
      setAccessToken(storedToken);
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  // Register a new user
  const register = async (email, password, name) => {
    const response = await fetch(`${AUTH_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, name }),
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || data.error || 'Registration failed');
    }

    // Store tokens and user
    localStorage.setItem('accessToken', data.access_token);
    localStorage.setItem('refreshToken', data.refresh_token);
    localStorage.setItem('user', JSON.stringify(data.user));

    setAccessToken(data.access_token);
    setUser(data.user);

    return data;
  };

  // Login user
  const login = async (email, password) => {
    const response = await fetch(`${AUTH_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || data.error || 'Login failed');
    }

    // Store tokens and user
    localStorage.setItem('accessToken', data.access_token);
    localStorage.setItem('refreshToken', data.refresh_token);
    localStorage.setItem('user', JSON.stringify(data.user));

    setAccessToken(data.access_token);
    setUser(data.user);

    return data;
  };

  // Logout user
  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken');

    try {
      await fetch(`${AUTH_URL}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: refreshToken }),
      });
    } catch (error) {
      console.error('Logout error:', error);
    }

    // Clear storage
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');

    setAccessToken(null);
    setUser(null);
  };

  // Refresh access token
  const refreshAccessToken = async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      throw new Error('No refresh token');
    }

    const response = await fetch(`${AUTH_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    const data = await response.json();
    if (!response.ok) {
      // Refresh token expired, logout
      await logout();
      throw new Error('Session expired');
    }

    // Update tokens
    localStorage.setItem('accessToken', data.access_token);
    localStorage.setItem('refreshToken', data.refresh_token);
    setAccessToken(data.access_token);

    return data.access_token;
  };

  // Make authenticated API request
  const authFetch = async (url, options = {}) => {
    let token = accessToken || localStorage.getItem('accessToken');

    if (!token) {
      throw new Error('Not authenticated');
    }

    const headers = {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'ngrok-skip-browser-warning': 'true',
    };

    let response = await fetch(url, { ...options, headers });

    // If unauthorized, try to refresh token
    if (response.status === 401) {
      try {
        token = await refreshAccessToken();
        headers['Authorization'] = `Bearer ${token}`;
        response = await fetch(url, { ...options, headers });
      } catch (error) {
        throw new Error('Session expired');
      }
    }

    return response;
  };

  // Update user profile
  const updateProfile = async (data) => {
    const response = await authFetch(`${AUTH_URL}/api/v1/users/me`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || result.error || 'Update failed');
    }

    // Update stored user
    const updatedUser = { ...user, ...result };
    localStorage.setItem('user', JSON.stringify(updatedUser));
    setUser(updatedUser);

    return result;
  };

  // Change password
  const changePassword = async (currentPassword, newPassword) => {
    const response = await authFetch(`${AUTH_URL}/api/v1/users/me/password`, {
      method: 'POST',
      body: JSON.stringify({
        current_password: currentPassword,
        new_password: newPassword,
      }),
    });

    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || result.error || 'Password change failed');
    }

    return result;
  };

  // Fetch current user
  const fetchCurrentUser = async () => {
    const response = await authFetch(`${AUTH_URL}/api/v1/users/me`);
    const result = await response.json();

    if (response.ok) {
      localStorage.setItem('user', JSON.stringify(result));
      setUser(result);
    }

    return result;
  };

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    accessToken,
    register,
    login,
    logout,
    refreshAccessToken,
    authFetch,
    updateProfile,
    changePassword,
    fetchCurrentUser,
    API_URL,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
