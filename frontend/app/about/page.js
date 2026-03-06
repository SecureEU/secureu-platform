import Layout from '../../components/Layout'
import { Shield, Target, Users, Globe, Award, Bot } from 'lucide-react'

export default function AboutPage() {
  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-blue-600 rounded-xl">
              <Shield className="h-8 w-8 text-white" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900">About SECUR-EU</h1>
          </div>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto">
            Advanced cybersecurity platform designed to enhance security of European SMEs
            with AI-driven threat forecasting and intelligent response capabilities.
          </p>
        </div>

        {/* Mission Statement */}
        <div className="bg-gradient-to-r from-blue-50 to-blue-100 rounded-xl p-8 mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Our Mission</h2>
          <p className="text-gray-700 text-lg leading-relaxed">
            To empower European SMEs with next-generation cybersecurity solutions that proactively
            defend against evolving threats through advanced security platforms,
            artificial intelligence, and comprehensive threat intelligence.
          </p>
        </div>

        {/* Key Features */}
        <div className="mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-8 text-center">Platform Capabilities</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-blue-100 rounded-lg w-fit mb-4">
                <Shield className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Security Dashboard</h3>
              <p className="text-gray-600">
                Comprehensive overview of your security posture with real-time threat monitoring 
                and risk assessment.
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-blue-100 rounded-lg w-fit mb-4">
                <Globe className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Security Scans</h3>
              <p className="text-gray-600">
                Automated vulnerability scanning and penetration testing to identify 
                security weaknesses before attackers do.
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-green-100 rounded-lg w-fit mb-4">
                <Users className="h-6 w-6 text-green-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Asset Management</h3>
              <p className="text-gray-600">
                Complete visibility and control over your IT infrastructure with 
                real-time asset monitoring and management.
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-orange-100 rounded-lg w-fit mb-4">
                <Target className="h-6 w-6 text-orange-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Active Exploitation</h3>
              <p className="text-gray-600">
                Controlled offensive security testing to validate defenses and 
                improve incident response capabilities.
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-purple-100 rounded-lg w-fit mb-4">
                <Bot className="h-6 w-6 text-purple-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">AI Assistant</h3>
              <p className="text-gray-600">
                Intelligent AI-powered support for SOC analysts with threat analysis 
                and automated response recommendations.
              </p>
            </div>

            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
              <div className="p-2 bg-yellow-100 rounded-lg w-fit mb-4">
                <Award className="h-6 w-6 text-yellow-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Compliance Manager</h3>
              <p className="text-gray-600">
                Automated compliance validation for PCI DSS, OWASP, NIS2, and other 
                industry standards and regulations.
              </p>
            </div>
          </div>
        </div>

        {/* Technology */}
        <div className="bg-gray-50 rounded-xl p-8 mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Advanced Technology Stack</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-3">AI & Machine Learning</h3>
              <ul className="space-y-2 text-gray-600">
                <li>• Predictive threat intelligence</li>
                <li>• Behavioral anomaly detection</li>
                <li>• Automated incident classification</li>
                <li>• Natural language processing for threat analysis</li>
              </ul>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-3">Security Infrastructure</h3>
              <ul className="space-y-2 text-gray-600">
                <li>• Real-time threat monitoring</li>
                <li>• Distributed security sensors</li>
                <li>• Encrypted data transmission</li>
                <li>• Multi-layered defense architecture</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Contact CTA */}
        <div className="text-center bg-blue-600 text-white rounded-xl p-8">
          <h2 className="text-2xl font-bold mb-4">Ready to Enhance Your Security?</h2>
          <p className="text-blue-100 mb-6 text-lg">
            Join European SMEs in strengthening their cybersecurity posture with SECUR-EU.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <a
              href="/contact"
              className="bg-white text-blue-600 px-6 py-3 rounded-lg font-semibold hover:bg-gray-100 transition-colors"
            >
              Contact Us
            </a>
            <a
              href="/"
              className="border border-white text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
            >
              View Dashboard
            </a>
          </div>
        </div>
      </div>
    </Layout>
  )
}