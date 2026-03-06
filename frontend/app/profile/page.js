'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Layout from '@/components/Layout';
import { useAuth } from '@/lib/auth';
import {
  User, Mail, Lock, Save, AlertCircle, Loader2, Check, Eye, EyeOff, Shield, Camera, Upload, X
} from 'lucide-react';

export default function ProfilePage() {
  const { user, isAuthenticated, loading: authLoading, updateProfile, changePassword } = useAuth();
  const router = useRouter();

  const [name, setName] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

  const [profileLoading, setProfileLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  // Avatar states
  const [avatarUrl, setAvatarUrl] = useState('');
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [avatarError, setAvatarError] = useState('');
  const [avatarSuccess, setAvatarSuccess] = useState(false);

  const passwordRequirements = [
    { test: (p) => p.length >= 8, text: 'At least 8 characters' },
    { test: (p) => /[A-Z]/.test(p), text: 'One uppercase letter' },
    { test: (p) => /[a-z]/.test(p), text: 'One lowercase letter' },
    { test: (p) => /[0-9]/.test(p), text: 'One number' },
  ];

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (user) {
      setName(user.name || '');
      setAvatarUrl(user.avatar_url || '');
    }
  }, [user]);

  const handleAvatarUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setAvatarError('');
    setAvatarSuccess(false);
    setAvatarLoading(true);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('userId', user?.id || 'user');

      const response = await fetch('/api/upload/avatar', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Upload failed');
      }

      // Update local state
      setAvatarUrl(data.avatarUrl);

      // Update profile with new avatar URL
      await updateProfile({ avatar_url: data.avatarUrl });

      setAvatarSuccess(true);
      setTimeout(() => setAvatarSuccess(false), 3000);
    } catch (err) {
      setAvatarError(err.message);
    } finally {
      setAvatarLoading(false);
    }
  };

  const handleRemoveAvatar = async () => {
    setAvatarError('');
    setAvatarLoading(true);

    try {
      await updateProfile({ avatar_url: '' });
      setAvatarUrl('');
      setAvatarSuccess(true);
      setTimeout(() => setAvatarSuccess(false), 3000);
    } catch (err) {
      setAvatarError(err.message);
    } finally {
      setAvatarLoading(false);
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    setProfileError('');
    setProfileSuccess(false);
    setProfileLoading(true);

    try {
      await updateProfile({ name });
      setProfileSuccess(true);
      setTimeout(() => setProfileSuccess(false), 3000);
    } catch (err) {
      setProfileError(err.message);
    } finally {
      setProfileLoading(false);
    }
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess(false);

    if (newPassword !== confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters');
      return;
    }

    setPasswordLoading(true);

    try {
      await changePassword(currentPassword, newPassword);
      setPasswordSuccess(true);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => setPasswordSuccess(false), 3000);
    } catch (err) {
      setPasswordError(err.message);
    } finally {
      setPasswordLoading(false);
    }
  };

  if (authLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Page Header */}
        <div className="flex items-center gap-4 mb-8">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg">
            <User className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Profile Settings</h1>
            <p className="text-gray-500">Manage your account information and security</p>
          </div>
        </div>

        {/* Profile Picture */}
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Camera className="h-5 w-5 text-gray-400" />
            Profile Picture
          </h2>

          {avatarError && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-xl p-3 flex items-center gap-2">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
              <p className="text-sm text-red-700">{avatarError}</p>
            </div>
          )}

          {avatarSuccess && (
            <div className="mb-4 bg-green-50 border border-green-200 rounded-xl p-3 flex items-center gap-2">
              <Check className="h-5 w-5 text-green-500 flex-shrink-0" />
              <p className="text-sm text-green-700">Profile picture updated successfully</p>
            </div>
          )}

          <div className="flex items-center gap-6">
            {/* Avatar Preview */}
            <div className="relative">
              <div className="w-24 h-24 rounded-full overflow-hidden bg-gray-100 border-4 border-white shadow-lg">
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt="Profile"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
                    <span className="text-3xl font-bold text-white">
                      {user?.name?.charAt(0)?.toUpperCase() || 'U'}
                    </span>
                  </div>
                )}
              </div>
              {avatarLoading && (
                <div className="absolute inset-0 bg-black/50 rounded-full flex items-center justify-center">
                  <Loader2 className="h-8 w-8 text-white animate-spin" />
                </div>
              )}
            </div>

            {/* Upload Controls */}
            <div className="flex-1">
              <p className="text-sm text-gray-600 mb-3">
                Upload a new profile picture. Recommended size is 200x200 pixels.
              </p>
              <div className="flex items-center gap-3">
                <label className="cursor-pointer">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={handleAvatarUpload}
                    className="hidden"
                    disabled={avatarLoading}
                  />
                  <span className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors text-sm font-medium">
                    <Upload className="h-4 w-4" />
                    Upload Photo
                  </span>
                </label>
                {avatarUrl && (
                  <button
                    type="button"
                    onClick={handleRemoveAvatar}
                    disabled={avatarLoading}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition-colors text-sm font-medium disabled:opacity-50"
                  >
                    <X className="h-4 w-4" />
                    Remove
                  </button>
                )}
              </div>
              <p className="text-xs text-gray-500 mt-2">
                Allowed formats: JPEG, PNG, GIF, WebP. Max size: 5MB
              </p>
            </div>
          </div>
        </div>

        {/* Profile Information */}
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <User className="h-5 w-5 text-gray-400" />
            Profile Information
          </h2>

          {profileError && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-xl p-3 flex items-center gap-2">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
              <p className="text-sm text-red-700">{profileError}</p>
            </div>
          )}

          {profileSuccess && (
            <div className="mb-4 bg-green-50 border border-green-200 rounded-xl p-3 flex items-center gap-2">
              <Check className="h-5 w-5 text-green-500 flex-shrink-0" />
              <p className="text-sm text-green-700">Profile updated successfully</p>
            </div>
          )}

          <form onSubmit={handleProfileUpdate} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Mail className="w-5 h-5 text-gray-400" />
                </div>
                <input
                  type="email"
                  value={user?.email || ''}
                  disabled
                  className="block w-full pl-12 pr-4 py-3 border border-gray-200 rounded-xl bg-gray-50 text-gray-500 cursor-not-allowed"
                />
              </div>
              <p className="text-xs text-gray-500 mt-1">Email cannot be changed</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Full Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <User className="w-5 h-5 text-gray-400" />
                </div>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className="block w-full pl-12 pr-4 py-3 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                  placeholder="Your name"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={profileLoading}
              className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl transition-colors disabled:opacity-50 font-medium"
            >
              {profileLoading ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                <Save className="h-5 w-5" />
              )}
              Save Changes
            </button>
          </form>
        </div>

        {/* Change Password */}
        <div className="bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Lock className="h-5 w-5 text-gray-400" />
            Change Password
          </h2>

          {passwordError && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-xl p-3 flex items-center gap-2">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
              <p className="text-sm text-red-700">{passwordError}</p>
            </div>
          )}

          {passwordSuccess && (
            <div className="mb-4 bg-green-50 border border-green-200 rounded-xl p-3 flex items-center gap-2">
              <Check className="h-5 w-5 text-green-500 flex-shrink-0" />
              <p className="text-sm text-green-700">Password changed successfully</p>
            </div>
          )}

          <form onSubmit={handlePasswordChange} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Current Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="w-5 h-5 text-gray-400" />
                </div>
                <input
                  type={showCurrentPassword ? 'text' : 'password'}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                  className="block w-full pl-12 pr-12 py-3 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                  placeholder="Enter current password"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                >
                  {showCurrentPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                New Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="w-5 h-5 text-gray-400" />
                </div>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  className="block w-full pl-12 pr-12 py-3 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                  placeholder="Create a new password"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                >
                  {showNewPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>

              {/* Password requirements */}
              {newPassword && (
                <div className="mt-3 grid grid-cols-2 gap-2">
                  {passwordRequirements.map((req, idx) => (
                    <div
                      key={idx}
                      className={`flex items-center gap-1.5 text-xs ${
                        req.test(newPassword) ? 'text-green-600' : 'text-gray-400'
                      }`}
                    >
                      <Check className={`w-3.5 h-3.5 ${req.test(newPassword) ? 'text-green-500' : 'text-gray-300'}`} />
                      <span>{req.text}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Confirm New Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="w-5 h-5 text-gray-400" />
                </div>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  className={`block w-full pl-12 pr-4 py-3 border rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900 ${
                    confirmPassword && newPassword !== confirmPassword
                      ? 'border-red-300 bg-red-50'
                      : 'border-gray-300'
                  }`}
                  placeholder="Confirm new password"
                />
              </div>
              {confirmPassword && newPassword !== confirmPassword && (
                <p className="mt-2 text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  Passwords do not match
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={passwordLoading || (confirmPassword && newPassword !== confirmPassword)}
              className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl transition-colors disabled:opacity-50 font-medium"
            >
              {passwordLoading ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                <Lock className="h-5 w-5" />
              )}
              Change Password
            </button>
          </form>
        </div>

        {/* Security Info */}
        <div className="bg-blue-50 rounded-xl p-6 border border-blue-200">
          <div className="flex items-start gap-4">
            <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <Shield className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold text-blue-900">Security Recommendations</h3>
              <ul className="mt-2 text-sm text-blue-700 space-y-1">
                <li>Use a strong, unique password</li>
                <li>Enable two-factor authentication when available</li>
                <li>Never share your credentials with others</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
