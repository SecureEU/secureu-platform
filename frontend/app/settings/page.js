'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Layout from '@/components/Layout';
import { useAuth } from '@/lib/auth';
import {
  Settings, Building2, Users, Save, Edit2, X, Trash2,
  AlertCircle, Check, Loader2, Shield, User, Crown,
  Upload, Image as ImageIcon
} from 'lucide-react';

const roleColors = {
  admin: 'text-purple-700 bg-purple-100',
  user: 'text-blue-700 bg-blue-100',
};

export default function SettingsPage() {
  const { user, isAuthenticated, loading: authLoading, authFetch } = useAuth();
  const router = useRouter();

  const [workspace, setWorkspace] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Workspace editing
  const [editingName, setEditingName] = useState(false);
  const [editedName, setEditedName] = useState('');
  const [savingName, setSavingName] = useState(false);

  // Logo upload
  const [uploadingLogo, setUploadingLogo] = useState(false);
  const [removingLogo, setRemovingLogo] = useState(false);

  // User management
  const [changingRole, setChangingRole] = useState(null);
  const [deletingUser, setDeletingUser] = useState(null);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (!authLoading && user?.role !== 'admin') {
      router.push('/');
    }
  }, [authLoading, user, router]);

  useEffect(() => {
    if (isAuthenticated && user?.role === 'admin') {
      fetchData();
    }
  }, [isAuthenticated, user]);

  const fetchData = async () => {
    try {
      const [wsRes, usersRes] = await Promise.all([
        authFetch('/api/v1/settings/workspace'),
        authFetch('/api/v1/settings/users'),
      ]);

      if (wsRes.ok) {
        const data = await wsRes.json();
        setWorkspace(data);
        setEditedName(data.name);
      }

      if (usersRes.ok) {
        const data = await usersRes.json();
        setUsers(data.users);
      }
    } catch (err) {
      setError('Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveName = async () => {
    if (!editedName.trim() || editedName.trim() === workspace?.name) {
      setEditingName(false);
      return;
    }

    setSavingName(true);
    setError('');

    try {
      const res = await authFetch('/api/v1/settings/workspace', {
        method: 'PUT',
        body: JSON.stringify({ name: editedName.trim() }),
      });

      if (res.ok) {
        const data = await res.json();
        setWorkspace(data);
        setEditingName(false);
        setSuccess('Company name updated');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const data = await res.json();
        setError(data.error || 'Failed to update name');
      }
    } catch {
      setError('Failed to update name');
    } finally {
      setSavingName(false);
    }
  };

  const handleLogoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingLogo(true);
    setError('');

    try {
      const formData = new FormData();
      formData.append('file', file);

      const token = localStorage.getItem('accessToken');
      const uploadRes = await fetch('/api/upload/logo', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
      });

      if (!uploadRes.ok) {
        const data = await uploadRes.json();
        setError(data.error || 'Failed to upload logo');
        return;
      }

      const { logoUrl } = await uploadRes.json();

      // Save logo_url to workspace
      const res = await authFetch('/api/v1/settings/workspace', {
        method: 'PUT',
        body: JSON.stringify({ logo_url: logoUrl }),
      });

      if (res.ok) {
        const data = await res.json();
        setWorkspace(data);
        setSuccess('Logo uploaded');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const data = await res.json();
        setError(data.error || 'Failed to save logo');
      }
    } catch {
      setError('Failed to upload logo');
    } finally {
      setUploadingLogo(false);
      // Reset file input
      e.target.value = '';
    }
  };

  const handleRemoveLogo = async () => {
    setRemovingLogo(true);
    setError('');

    try {
      const res = await authFetch('/api/v1/settings/workspace', {
        method: 'PUT',
        body: JSON.stringify({ logo_url: null }),
      });

      if (res.ok) {
        const data = await res.json();
        setWorkspace(data);
        setSuccess('Logo removed');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const data = await res.json();
        setError(data.error || 'Failed to remove logo');
      }
    } catch {
      setError('Failed to remove logo');
    } finally {
      setRemovingLogo(false);
    }
  };

  const handleChangeRole = async (userId, newRole) => {
    setChangingRole(userId);
    setError('');

    try {
      const res = await authFetch(`/api/v1/settings/users/${userId}`, {
        method: 'PATCH',
        body: JSON.stringify({ role: newRole }),
      });

      if (res.ok) {
        setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u));
        setSuccess('Role updated');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const data = await res.json();
        setError(data.error || 'Failed to change role');
      }
    } catch {
      setError('Failed to change role');
    } finally {
      setChangingRole(null);
    }
  };

  const handleDeleteUser = async (userId, userName) => {
    if (!confirm(`Remove ${userName} from the workspace? This cannot be undone.`)) return;

    setDeletingUser(userId);
    setError('');

    try {
      const res = await authFetch(`/api/v1/settings/users/${userId}`, {
        method: 'DELETE',
      });

      if (res.ok) {
        setUsers(prev => prev.filter(u => u.id !== userId));
        setSuccess('User removed');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const data = await res.json();
        setError(data.error || 'Failed to remove user');
      }
    } catch {
      setError('Failed to remove user');
    } finally {
      setDeletingUser(null);
    }
  };

  if (authLoading || loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
        </div>
      </Layout>
    );
  }

  if (user?.role !== 'admin') return null;

  return (
    <Layout>
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Page Header */}
        <div className="flex items-center gap-4 mb-8">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg">
            <Settings className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Org Settings</h1>
            <p className="text-gray-500">Manage your workspace and users</p>
          </div>
        </div>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
            <span className="text-sm text-red-700">{error}</span>
            <button onClick={() => setError('')} className="ml-auto text-red-500 hover:text-red-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {success && (
          <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-center gap-3">
            <Check className="w-5 h-5 text-green-500 flex-shrink-0" />
            <span className="text-sm text-green-700">{success}</span>
          </div>
        )}

        {/* Company Info */}
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Building2 className="h-5 w-5 text-gray-400" />
            Company Information
          </h2>

          {/* Logo Upload */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Company Logo
            </label>
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg overflow-hidden">
                {workspace?.logo_url ? (
                  <img
                    src={workspace.logo_url}
                    alt="Company logo"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Shield className="w-8 h-8 text-white" />
                )}
              </div>
              <div className="flex flex-col gap-2">
                <label className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-blue-700 bg-blue-50 rounded-lg hover:bg-blue-100 cursor-pointer transition-colors">
                  {uploadingLogo ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Upload className="w-4 h-4" />
                  )}
                  {uploadingLogo ? 'Uploading...' : 'Upload Logo'}
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp,image/svg+xml"
                    onChange={handleLogoUpload}
                    disabled={uploadingLogo}
                    className="hidden"
                  />
                </label>
                {workspace?.logo_url && (
                  <button
                    onClick={handleRemoveLogo}
                    disabled={removingLogo}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                  >
                    {removingLogo ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Trash2 className="w-4 h-4" />
                    )}
                    Remove Logo
                  </button>
                )}
              </div>
            </div>
            <p className="text-xs text-gray-500 mt-2">JPEG, PNG, GIF, WebP, or SVG. Max 5MB.</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Company Name
            </label>
            {editingName ? (
              <div className="flex gap-2">
                <input
                  type="text"
                  value={editedName}
                  onChange={(e) => setEditedName(e.target.value)}
                  className="flex-1 px-4 py-2.5 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-gray-900"
                  autoFocus
                />
                <button
                  onClick={() => { setEditingName(false); setEditedName(workspace?.name || ''); }}
                  className="px-3 py-2 text-gray-600 hover:text-gray-900"
                >
                  <X className="w-5 h-5" />
                </button>
                <button
                  onClick={handleSaveName}
                  disabled={savingName || !editedName.trim()}
                  className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1.5"
                >
                  {savingName ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                  Save
                </button>
              </div>
            ) : (
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                <span className="text-sm text-gray-900">{workspace?.name}</span>
                <button
                  onClick={() => setEditingName(true)}
                  className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
                >
                  <Edit2 className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* User Management */}
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Users className="h-5 w-5 text-gray-400" />
            User Management
          </h2>

          <div className="space-y-2">
            {users.map((u) => {
              const isCurrentUser = u.id === user?.id;

              return (
                <div
                  key={u.id}
                  className="p-3 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center">
                        <span className="text-sm text-gray-600 font-medium">
                          {u.name?.charAt(0)?.toUpperCase() || '?'}
                        </span>
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="text-sm font-medium text-gray-900">{u.name}</p>
                          {isCurrentUser && (
                            <span className="px-1.5 py-0.5 bg-blue-100 text-blue-700 text-xs rounded">
                              You
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-gray-500">{u.email}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {isCurrentUser ? (
                        <span className={`px-2.5 py-1 text-xs font-medium rounded-lg capitalize ${roleColors[u.role] || 'text-gray-700 bg-gray-100'}`}>
                          {u.role}
                        </span>
                      ) : (
                        <>
                          <select
                            value={u.role}
                            onChange={(e) => handleChangeRole(u.id, e.target.value)}
                            disabled={changingRole === u.id}
                            className="text-xs border border-gray-300 rounded-lg px-2 py-1.5 text-gray-700 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          >
                            <option value="admin">Admin</option>
                            <option value="user">User</option>
                          </select>
                          <button
                            onClick={() => handleDeleteUser(u.id, u.name)}
                            disabled={deletingUser === u.id}
                            className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                            title="Remove user"
                          >
                            {deletingUser === u.id ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <Trash2 className="w-4 h-4" />
                            )}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}

            {users.length === 0 && (
              <p className="text-sm text-gray-500 text-center py-4">No users found.</p>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
