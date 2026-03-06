import Layout from '../../components/Layout'
import { FileText, AlertTriangle, Shield, Scale } from 'lucide-react'

export default function TermsAndConditionsPage() {
  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-gray-600 rounded-xl">
              <FileText className="h-8 w-8 text-white" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900">Terms and Conditions</h1>
          </div>
          <p className="text-lg text-gray-600">
            Last updated: {new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>

        {/* Important Notice */}
        <div className="bg-orange-50 border border-orange-200 rounded-xl p-6 mb-8">
          <div className="flex items-start gap-3">
            <AlertTriangle className="h-6 w-6 text-orange-600 mt-1" />
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Important Notice</h3>
              <p className="text-gray-700">
                Please read these Terms and Conditions carefully before using the SECUR-EU platform.
                By accessing or using our services, you agree to be bound by these terms.
              </p>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="prose max-w-none">
          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">1. Acceptance of Terms</h2>
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                By accessing and using the SECUR-EU cybersecurity platform ("Service"), you accept and agree to be bound by the terms and provision of this agreement.
              </p>
              <p className="text-gray-700">
                If you do not agree to abide by the above, please do not use this service. These terms apply to all users of the service, including without limitation users who are browsers, vendors, customers, merchants, and contributors of content.
              </p>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">2. Service Description</h2>
            <div className="space-y-4">
              <p className="text-gray-700">
                SECUR-EU provides a comprehensive cybersecurity platform that includes:
              </p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h3 className="font-semibold text-gray-900 mb-2">Security Services</h3>
                  <ul className="space-y-1 text-gray-600 text-sm">
                    <li>• Vulnerability scanning and assessment</li>
                    <li>• Threat monitoring and detection</li>
                    <li>• Incident response capabilities</li>
                    <li>• Compliance management tools</li>
                  </ul>
                </div>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <h3 className="font-semibold text-gray-900 mb-2">Platform Features</h3>
                  <ul className="space-y-1 text-gray-600 text-sm">
                    <li>• Asset management and tracking</li>
                    <li>• AI-powered threat intelligence</li>
                    <li>• Automated security orchestration</li>
                    <li>• Real-time dashboards and reporting</li>
                  </ul>
                </div>
              </div>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">3. User Responsibilities</h2>
            <div className="bg-gray-50 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">You agree to:</h3>
              <ul className="space-y-3 text-gray-700">
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-green-100 rounded">
                    <Shield className="h-4 w-4 text-green-600" />
                  </div>
                  <span>Use the service only for lawful purposes and in accordance with these terms</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-blue-100 rounded">
                    <FileText className="h-4 w-4 text-blue-600" />
                  </div>
                  <span>Maintain the confidentiality of your account credentials</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-purple-100 rounded">
                    <AlertTriangle className="h-4 w-4 text-purple-600" />
                  </div>
                  <span>Not attempt to gain unauthorized access to any part of the service</span>
                </li>
                <li className="flex items-start gap-3">
                  <div className="p-1 bg-blue-100 rounded">
                    <Scale className="h-4 w-4 text-blue-600" />
                  </div>
                  <span>Comply with all applicable laws, regulations, and industry standards</span>
                </li>
              </ul>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">4. Prohibited Activities</h2>
            <div className="bg-red-50 border border-red-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">Users are expressly prohibited from:</p>
              <ul className="space-y-2 text-gray-600">
                <li>• Using the service for any illegal or unauthorized purpose</li>
                <li>• Attempting to compromise the security of the platform or other users' data</li>
                <li>• Reverse engineering, decompiling, or disassembling the software</li>
                <li>• Sharing access credentials with unauthorized individuals</li>
                <li>• Using the service to scan or test systems without proper authorization</li>
                <li>• Violating any laws in your jurisdiction regarding data protection and privacy</li>
              </ul>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">5. Intellectual Property Rights</h2>
            <div className="space-y-6">
              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">SECUR-EU Property</h3>
                <p className="text-gray-700">
                  The service and its original content, features, and functionality are and will remain the exclusive property of SECUR-EU and its licensors. The service is protected by copyright, trademark, and other laws.
                </p>
              </div>

              <div className="bg-white border border-gray-200 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-3">User Data</h3>
                <p className="text-gray-700">
                  You retain ownership of any data, information, or content that you provide to the service. By using the service, you grant SECUR-EU a limited license to use this data solely for the purpose of providing the security services.
                </p>
              </div>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">6. Service Availability</h2>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                While we strive to maintain high availability, we do not guarantee that the service will be:
              </p>
              <ul className="space-y-2 text-gray-600">
                <li>• Available 100% of the time without interruption</li>
                <li>• Free from errors, bugs, or other technical issues</li>
                <li>• Compatible with all hardware and software configurations</li>
                <li>• Immune to cyber attacks or other security threats</li>
              </ul>
              <p className="text-gray-700 mt-4">
                We reserve the right to modify, suspend, or discontinue the service at any time with reasonable notice.
              </p>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">7. Limitation of Liability</h2>
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                <strong>IMPORTANT:</strong> In no event shall SECUR-EU, its directors, employees, partners, agents, suppliers, or affiliates be liable for any indirect, incidental, special, consequential, or punitive damages, including without limitation:
              </p>
              <ul className="space-y-2 text-gray-600">
                <li>• Loss of profits, data, or other intangible losses</li>
                <li>• Damages resulting from cyber attacks or security breaches</li>
                <li>• Unauthorized access to or alteration of your transmissions or data</li>
                <li>• Statements or conduct of any third party on the service</li>
              </ul>
            </div>
          </section>

          <section className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">8. Contact Information</h2>
            <div className="bg-white border border-gray-200 rounded-lg p-6">
              <p className="text-gray-700 mb-4">
                If you have any questions about these Terms and Conditions, please contact us:
              </p>
              <div className="space-y-2 text-gray-600">
                <p><strong>Email:</strong> legal@secur-eu.eu</p>
                <p><strong>Phone:</strong> +1 (555) 123-4567</p>
                <p><strong>Address:</strong> SECUR-EU Legal Department, 123 Security Boulevard, Cyber City, CC 12345</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </Layout>
  )
}