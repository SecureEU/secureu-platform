import Layout from '../../components/Layout'
import { Mail, Phone, MapPin, Shield, Clock, Users } from 'lucide-react'

export default function ContactPage() {
  return (
    <Layout>
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">Contact SECUR-EU</h1>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto">
            Get in touch with our cybersecurity experts. We're here to help strengthen 
            your organization's security posture.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Contact Form */}
          <div className="bg-white border border-gray-200 rounded-xl p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Send us a Message</h2>
            <form className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="firstName" className="block text-sm font-medium text-gray-700 mb-2">
                    First Name *
                  </label>
                  <input
                    type="text"
                    id="firstName"
                    name="firstName"
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors"
                    placeholder="Enter your first name"
                  />
                </div>
                <div>
                  <label htmlFor="lastName" className="block text-sm font-medium text-gray-700 mb-2">
                    Last Name *
                  </label>
                  <input
                    type="text"
                    id="lastName"
                    name="lastName"
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors"
                    placeholder="Enter your last name"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                  Email Address *
                </label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors"
                  placeholder="your.email@company.com"
                />
              </div>

              <div>
                <label htmlFor="company" className="block text-sm font-medium text-gray-700 mb-2">
                  Company/Organization
                </label>
                <input
                  type="text"
                  id="company"
                  name="company"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors"
                  placeholder="Your company name"
                />
              </div>

              <div>
                <label htmlFor="subject" className="block text-sm font-medium text-gray-700 mb-2">
                  Subject *
                </label>
                <select
                  id="subject"
                  name="subject"
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors"
                >
                  <option value="">Select a subject</option>
                  <option value="general">General Inquiry</option>
                  <option value="demo">Request Demo</option>
                  <option value="support">Technical Support</option>
                  <option value="sales">Sales & Pricing</option>
                  <option value="partnership">Partnership Opportunities</option>
                  <option value="compliance">Compliance Questions</option>
                </select>
              </div>

              <div>
                <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-2">
                  Message *
                </label>
                <textarea
                  id="message"
                  name="message"
                  rows={6}
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors resize-vertical"
                  placeholder="Please describe your inquiry or how we can help you..."
                />
              </div>

              <button
                type="submit"
                className="w-full bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
              >
                Send Message
              </button>
            </form>
          </div>

          {/* Contact Information */}
          <div className="space-y-8">
            {/* Contact Details */}
            <div className="bg-white border border-gray-200 rounded-xl p-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Get in Touch</h2>
              <div className="space-y-6">
                <div className="flex items-start gap-4">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <Mail className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Email</h3>
                    <p className="text-gray-600">contact@secur-eu.eu</p>
                    <p className="text-gray-600">support@secur-eu.eu</p>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <Phone className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Phone</h3>
                    <p className="text-gray-600">+1 (555) 123-4567</p>
                    <p className="text-gray-600">+1 (555) 123-4568 (Support)</p>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="p-2 bg-green-100 rounded-lg">
                    <MapPin className="h-5 w-5 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Address</h3>
                    <p className="text-gray-600">
                      SECUR-EU Headquarters<br />
                      123 Security Boulevard<br />
                      Cyber City, CC 12345
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="p-2 bg-purple-100 rounded-lg">
                    <Clock className="h-5 w-5 text-purple-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Business Hours</h3>
                    <p className="text-gray-600">
                      Monday - Friday: 9:00 AM - 6:00 PM<br />
                      24/7 Emergency Support Available
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Support Options */}
            <div className="bg-gradient-to-r from-blue-50 to-blue-100 rounded-xl p-8">
              <h2 className="text-xl font-bold text-gray-900 mb-4">Need Immediate Help?</h2>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <Shield className="h-5 w-5 text-blue-600" />
                  <span className="text-gray-700">24/7 Security Incident Response</span>
                </div>
                <div className="flex items-center gap-3">
                  <Users className="h-5 w-5 text-blue-600" />
                  <span className="text-gray-700">Dedicated Account Management</span>
                </div>
                <div className="mt-6">
                  <a
                    href="tel:+15551234567"
                    className="inline-flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
                  >
                    <Phone className="h-4 w-4" />
                    Emergency Hotline
                  </a>
                </div>
              </div>
            </div>

            {/* Office Locations */}
            <div className="bg-white border border-gray-200 rounded-xl p-8">
              <h2 className="text-xl font-bold text-gray-900 mb-4">Global Presence</h2>
              <div className="space-y-4 text-sm">
                <div>
                  <h3 className="font-semibold text-gray-900">North America</h3>
                  <p className="text-gray-600">New York, San Francisco, Toronto</p>
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900">Europe</h3>
                  <p className="text-gray-600">London, Frankfurt, Amsterdam</p>
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900">Asia Pacific</h3>
                  <p className="text-gray-600">Singapore, Tokyo, Sydney</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}