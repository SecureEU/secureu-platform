'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Shield, Building2, User, Mail, Lock, Eye, EyeOff, ArrowRight, ArrowLeft, Check, AlertCircle, Loader2 } from 'lucide-react';

export default function SetupPage() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [companyName, setCompanyName] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Check if setup is needed
  useEffect(() => {
    fetch('/api/v1/setup/status')
      .then(res => res.json())
      .then(data => {
        if (!data.needsSetup) {
          router.replace('/');
        } else {
          setLoading(false);
        }
      })
      .catch(() => setLoading(false));
  }, [router]);

  const passwordRequirements = [
    { test: (p) => p.length >= 8, text: 'At least 8 characters' },
    { test: (p) => /[A-Z]/.test(p), text: 'One uppercase letter' },
    { test: (p) => /[a-z]/.test(p), text: 'One lowercase letter' },
    { test: (p) => /[0-9]/.test(p), text: 'One number' },
  ];

  const allPasswordReqsMet = passwordRequirements.every(r => r.test(password));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (!allPasswordReqsMet) {
      setError('Password does not meet requirements');
      return;
    }

    setSubmitting(true);

    try {
      const res = await fetch('/api/v1/setup/complete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ companyName, email, password, name }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'Setup failed');
      }

      // Store tokens and user
      localStorage.setItem('accessToken', data.access_token);
      localStorage.setItem('refreshToken', data.refresh_token);
      localStorage.setItem('user', JSON.stringify(data.user));

      // Full reload so AuthProvider picks up the new tokens
      window.location.href = '/';
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-4">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-blue-500/10 rounded-full filter blur-3xl"></div>
        <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full filter blur-3xl"></div>
      </div>

      <div className="relative w-full max-w-lg">
        {/* Logo */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg">
            <Shield className="w-7 h-7 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">SECUR-EU</h1>
            <p className="text-gray-400 text-sm">Initial Setup</p>
          </div>
        </div>

        {/* Steps indicator */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium ${
            step === 1 ? 'bg-blue-500/20 text-blue-400' : 'bg-green-500/20 text-green-400'
          }`}>
            {step > 1 ? <Check className="w-4 h-4" /> : <Building2 className="w-4 h-4" />}
            Company
          </div>
          <div className="w-8 h-px bg-gray-700"></div>
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium ${
            step === 2 ? 'bg-blue-500/20 text-blue-400' : 'text-gray-500'
          }`}>
            <User className="w-4 h-4" />
            Admin Account
          </div>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl flex items-center gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span className="text-sm">{error}</span>
            </div>
          )}

          {step === 1 && (
            <div>
              <h2 className="text-xl font-bold text-gray-900 mb-2">Welcome to SECUR-EU</h2>
              <p className="text-gray-500 mb-6">Let&apos;s set up your workspace. What&apos;s your company name?</p>

              <div>
                <label htmlFor="companyName" className="block text-sm font-medium text-gray-700 mb-2">
                  Company name
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <Building2 className="w-5 h-5 text-gray-400" />
                  </div>
                  <input
                    id="companyName"
                    type="text"
                    value={companyName}
                    onChange={(e) => { setCompanyName(e.target.value); setError(''); }}
                    className="block w-full pl-12 pr-4 py-3.5 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                    placeholder="Acme Corp"
                    autoFocus
                  />
                </div>
              </div>

              <button
                onClick={() => {
                  if (!companyName.trim()) {
                    setError('Company name is required');
                    return;
                  }
                  setError('');
                  setStep(2);
                }}
                className="w-full mt-6 flex justify-center items-center gap-2 py-3.5 px-6 rounded-xl text-base font-semibold text-white bg-blue-600 hover:bg-blue-700 transition-all"
              >
                Continue
                <ArrowRight className="w-5 h-5" />
              </button>
            </div>
          )}

          {step === 2 && (
            <form onSubmit={handleSubmit}>
              <div className="flex items-center gap-2 mb-2">
                <button
                  type="button"
                  onClick={() => { setStep(1); setError(''); }}
                  className="p-1 text-gray-400 hover:text-gray-600 rounded-lg transition-colors"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <h2 className="text-xl font-bold text-gray-900">Create admin account</h2>
              </div>
              <p className="text-gray-500 mb-6">This will be the first administrator for <span className="font-medium text-gray-700">{companyName}</span>.</p>

              <div className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                    Full name
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <User className="w-5 h-5 text-gray-400" />
                    </div>
                    <input
                      id="name"
                      type="text"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      required
                      className="block w-full pl-12 pr-4 py-3.5 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                      placeholder="John Doe"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                    Email address
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Mail className="w-5 h-5 text-gray-400" />
                    </div>
                    <input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      className="block w-full pl-12 pr-4 py-3.5 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                      placeholder="admin@company.com"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                    Password
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Lock className="w-5 h-5 text-gray-400" />
                    </div>
                    <input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      className="block w-full pl-12 pr-12 py-3.5 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                      placeholder="Create a password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>

                  {password && (
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      {passwordRequirements.map((req, idx) => (
                        <div
                          key={idx}
                          className={`flex items-center gap-1.5 text-xs ${
                            req.test(password) ? 'text-green-600' : 'text-gray-400'
                          }`}
                        >
                          <Check className={`w-3.5 h-3.5 ${req.test(password) ? 'text-green-500' : 'text-gray-300'}`} />
                          <span>{req.text}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div>
                  <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
                    Confirm password
                  </label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <Lock className="w-5 h-5 text-gray-400" />
                    </div>
                    <input
                      id="confirmPassword"
                      type={showPassword ? 'text' : 'password'}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      className={`block w-full pl-12 pr-4 py-3.5 border rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900 ${
                        confirmPassword && password !== confirmPassword
                          ? 'border-red-300 bg-red-50'
                          : 'border-gray-300'
                      }`}
                      placeholder="Confirm your password"
                    />
                  </div>
                  {confirmPassword && password !== confirmPassword && (
                    <p className="mt-2 text-xs text-red-600 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      Passwords do not match
                    </p>
                  )}
                </div>
              </div>

              <button
                type="submit"
                disabled={submitting || !name || !email || !password || password !== confirmPassword || !allPasswordReqsMet}
                className="w-full mt-6 flex justify-center items-center gap-2 py-3.5 px-6 rounded-xl text-base font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              >
                {submitting ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <>
                    Complete Setup
                    <Check className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>
          )}
        </div>

        <p className="text-center text-gray-500 text-sm mt-6">
          SECUR-EU — SME Security Platform
        </p>
      </div>
    </div>
  );
}
