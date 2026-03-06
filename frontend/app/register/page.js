'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/lib/auth';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Eye, EyeOff, UserPlus, AlertCircle, Check, Shield, User, Mail, Lock, ArrowLeft, Gift, Zap, CheckCircle } from 'lucide-react';

export default function RegisterPage() {
  const { register, logout, loading: authLoading, isAuthenticated } = useAuth();
  const router = useRouter();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const passwordRequirements = [
    { test: (p) => p.length >= 8, text: 'At least 8 characters' },
    { test: (p) => /[A-Z]/.test(p), text: 'One uppercase letter' },
    { test: (p) => /[a-z]/.test(p), text: 'One lowercase letter' },
    { test: (p) => /[0-9]/.test(p), text: 'One number' },
  ];

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validate passwords match
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    // Validate password strength
    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }

    setLoading(true);

    try {
      await register(formData.email, formData.password, formData.name);
      // Logout to clear auto-login, then redirect to login page
      await logout();
      router.push('/login');
    } catch (err) {
      setError(err.message || 'Failed to create account');
      setLoading(false);
    }
  };

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">
      {/* Left Panel - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-12 flex-col justify-between relative overflow-hidden">
        {/* Background Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-96 h-96 bg-blue-500 rounded-full filter blur-3xl"></div>
          <div className="absolute bottom-0 right-0 w-96 h-96 bg-blue-500 rounded-full filter blur-3xl"></div>
        </div>

        <div className="relative">
          <Link href="/" className="inline-flex items-center text-gray-400 hover:text-white transition-colors mb-12">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to home
          </Link>

          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center shadow-lg">
              <Shield className="w-7 h-7 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white">SECUR-EU</h1>
              <p className="text-gray-400 text-sm">SME Security Platform</p>
            </div>
          </div>

          <h2 className="text-4xl font-bold leading-tight mb-6">
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-300">
              Start Protecting
            </span>
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400">
              Your Systems Today
            </span>
          </h2>

          <p className="text-gray-400 text-lg max-w-md mb-8">
            Join security teams using SECUR-EU to detect vulnerabilities, monitor threats, and secure their infrastructure.
          </p>

          {/* Benefits List */}
          <div className="space-y-4">
            {[
              { icon: Gift, text: 'Full access to security tools' },
              { icon: Zap, text: 'Instant vulnerability scanning' },
              { icon: CheckCircle, text: 'AI-powered threat analysis' },
            ].map((item, index) => (
              <div key={index} className="flex items-center gap-3 text-gray-300">
                <div className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center">
                  <item.icon className="w-4 h-4 text-blue-400" />
                </div>
                <span>{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative">
          <div className="flex items-center gap-8">
            <div className="text-center">
              <div className="text-3xl font-bold text-white">10+</div>
              <div className="text-gray-500 text-sm">Security Tools</div>
            </div>
            <div className="w-px h-12 bg-gray-700"></div>
            <div className="text-center">
              <div className="text-3xl font-bold text-white">99.9%</div>
              <div className="text-gray-500 text-sm">Uptime</div>
            </div>
            <div className="w-px h-12 bg-gray-700"></div>
            <div className="text-center">
              <div className="text-3xl font-bold text-white">24/7</div>
              <div className="text-gray-500 text-sm">Monitoring</div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Registration Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 bg-gray-50">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden flex items-center justify-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900">SECUR-EU</span>
          </div>

          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">Create your account</h2>
            <p className="text-gray-600">
              Already have an account?{' '}
              <Link href="/login" className="text-blue-600 hover:text-blue-500 font-medium">
                Sign in
              </Link>
            </p>
          </div>

          {/* Welcome banner */}
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-4 mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <Shield className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <p className="font-semibold text-blue-800">Secure your infrastructure</p>
              <p className="text-sm text-blue-700">Get instant access to all security tools</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl flex items-center gap-3">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span className="text-sm">{error}</span>
              </div>
            )}

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
                  name="name"
                  type="text"
                  autoComplete="name"
                  required
                  value={formData.name}
                  onChange={handleChange}
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
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  className="block w-full pl-12 pr-4 py-3.5 border border-gray-300 rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                  placeholder="you@example.com"
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
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  required
                  value={formData.password}
                  onChange={handleChange}
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

              {/* Password requirements */}
              {formData.password && (
                <div className="mt-3 grid grid-cols-2 gap-2">
                  {passwordRequirements.map((req, idx) => (
                    <div
                      key={idx}
                      className={`flex items-center gap-1.5 text-xs ${
                        req.test(formData.password) ? 'text-green-600' : 'text-gray-400'
                      }`}
                    >
                      <Check className={`w-3.5 h-3.5 ${req.test(formData.password) ? 'text-green-500' : 'text-gray-300'}`} />
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
                  name="confirmPassword"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  required
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`block w-full pl-12 pr-4 py-3.5 border rounded-xl shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900 ${
                    formData.confirmPassword && formData.password !== formData.confirmPassword
                      ? 'border-red-300 bg-red-50'
                      : 'border-gray-300'
                  }`}
                  placeholder="Confirm your password"
                />
              </div>
              {formData.confirmPassword && formData.password !== formData.confirmPassword && (
                <p className="mt-2 text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  Passwords do not match
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading || formData.password !== formData.confirmPassword}
              className="w-full flex justify-center items-center gap-2 py-4 px-6 border border-transparent rounded-xl shadow-lg text-base font-semibold text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all hover:shadow-xl hover:shadow-blue-200"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
              ) : (
                <>
                  <UserPlus className="w-5 h-5" />
                  Create account
                </>
              )}
            </button>
          </form>

          <div className="mt-8">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-4 bg-gray-50 text-gray-500">Already a member?</span>
              </div>
            </div>

            <div className="mt-6">
              <Link
                href="/login"
                className="w-full flex justify-center items-center py-3.5 px-6 border-2 border-gray-200 rounded-xl text-base font-medium text-gray-700 bg-white hover:bg-gray-50 hover:border-gray-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-all"
              >
                Sign in to your account
              </Link>
            </div>
          </div>

          <p className="mt-8 text-center text-xs text-gray-500">
            By creating an account, you agree to our{' '}
            <Link href="/terms-and-conditions" className="text-blue-600 hover:text-blue-500">Terms of Service</Link>
            {' '}and{' '}
            <Link href="/privacy-policy" className="text-blue-600 hover:text-blue-500">Privacy Policy</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
