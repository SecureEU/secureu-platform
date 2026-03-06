import Layout from '../../components/Layout'
import { Shield, Lock, Eye, FileText } from 'lucide-react'

export default function PrivacyPolicyPage() {
  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-blue-600 rounded-xl">
              <Lock className="h-8 w-8 text-white" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900">Privacy Policy</h1>
          </div>
          <p className="text-lg text-gray-600">
            Last updated: {new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>

        {/* Content */}
        <div className="prose max-w-none">
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-6 mb-8">
            <div className="flex items-start gap-3">
              <Shield className="h-6 w-6 text-blue-600 mt-1" />
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Our Commitment to Privacy</h3>
                <p className="text-gray-700">
                  SECUR-EU is committed to protecting your privacy and ensuring the security of your personal information.
                  This policy outlines how we collect, use, and safeguard your data.
                </p>
              </div>
            </div>
          </div>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">1. Information We Collect</h2>
            
            <div className="space-y-6">
              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Personal Information</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• Name, email address, and contact information</li>
                  <li>• Company/organization details</li>
                  <li>• Account credentials and authentication data</li>
                  <li>• Professional role and security clearance information (where applicable)</li>
                </ul>
              </div>

              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Technical Information</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• IP addresses and network configuration data</li>
                  <li>• System logs and security event data</li>
                  <li>• Device and browser information</li>
                  <li>• Usage patterns and platform interactions</li>
                </ul>
              </div>

              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Security Data</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• Vulnerability scan results and security assessments</li>
                  <li>• Threat intelligence and incident response data</li>
                  <li>• Asset inventory and configuration information</li>
                  <li>• Compliance and audit trail data</li>
                </ul>
              </div>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">2. How We Use Your Information</h2>
            <div className="bg-gray-50 rounded-lg p-6">
              <ul className="space-y-3 text-gray-700">
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-green-100 rounded">
                    <Shield className="h-4 w-4 text-green-600" />
                  </div>
                  <span>Provide and improve our cybersecurity services</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-blue-100 rounded">
                    <Eye className="h-4 w-4 text-blue-600" />
                  </div>
                  <span>Monitor and analyze security threats and vulnerabilities</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-purple-100 rounded">
                    <FileText className="h-4 w-4 text-purple-600" />
                  </div>
                  <span>Generate compliance reports and security assessments</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-blue-100 rounded">
                    <Lock className="h-4 w-4 text-blue-600" />
                  </div>
                  <span>Communicate security alerts and incident notifications</span>
                </li>
              </ul>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">3. Data Security & Protection</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="bg-red-50 border border-red-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Encryption & Storage</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• End-to-end encryption for all data transmission</li>
                  <li>• AES-256 encryption for data at rest</li>
                  <li>• Secure cloud infrastructure with SOC 2 compliance</li>
                  <li>• Regular security audits and penetration testing</li>
                </ul>
              </div>
              
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Access Controls</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• Multi-factor authentication requirements</li>
                  <li>• Role-based access control (RBAC)</li>
                  <li>• Regular access reviews and deprovisioning</li>
                  <li>• Zero-trust security architecture</li>
                </ul>
              </div>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">4. Data Sharing & Third Parties</h2>
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                We do not sell, trade, or otherwise transfer your personal information to third parties without your consent, except in the following circumstances:
              </p>
              <ul className="space-y-2 text-gray-600">
                <li>• When required by law or legal process</li>
                <li>• To protect our rights, property, or safety</li>
                <li>• With trusted service providers who assist in our operations (under strict confidentiality agreements)</li>
                <li>• For threat intelligence sharing with authorized security organizations (anonymized data only)</li>
              </ul>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">5. Your Rights & Choices</h2>
            <div className="space-y-4">
              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">Data Subject Rights</h3>
                <ul className="space-y-2 text-gray-600">
                  <li>• Right to access your personal data</li>
                  <li>• Right to rectify inaccurate information</li>
                  <li>• Right to erasure (subject to legal and security requirements)</li>
                  <li>• Right to data portability</li>
                  <li>• Right to object to processing</li>
                </ul>
              </div>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">6. Contact Information</h2>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                If you have questions about this Privacy Policy or wish to exercise your rights, please contact us:
              </p>
              <div className="space-y-2 text-gray-600">
                <p><strong>Email:</strong> privacy@secur-eu.eu</p>
                <p><strong>Phone:</strong> +1 (555) 123-4567</p>
                <p><strong>Address:</strong> SECUR-EU Privacy Office, 123 Security Boulevard, Cyber City, CC 12345</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </Layout>
  )
}