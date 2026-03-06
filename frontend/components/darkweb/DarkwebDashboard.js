'use client'

import React, { useState } from 'react';
import {
  Search,
  Eye,
  AlertTriangle,
  CheckCircle,
  Download,
  RotateCcw,
  X,
  Globe,
  FileText
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const DarkwebDashboard = () => {
  const [searchValue, setSearchValue] = useState('');
  const [exactMatch, setExactMatch] = useState(false);
  const [data, setData] = useState([]);
  const [error, setError] = useState(null);
  const [showTable, setShowTable] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedRow, setSelectedRow] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);

  const fetchData = async (keyword) => {
    try {
      const apiUrl = process.env.NEXT_PUBLIC_DARKWEB_API_URL || 'http://localhost:8001';
      console.log('Fetching from:', `${apiUrl}/search?keyword=${encodeURIComponent(keyword)}`);
      const response = await fetch(
        `${apiUrl}/search?keyword=${encodeURIComponent(keyword)}`,
        {
          method: 'GET',
          mode: 'cors',
          headers: {
            'ngrok-skip-browser-warning': '1'
          }
        }
      );
      if (!response.ok) {
        // Try to get error details from response
        let errorDetail = '';
        try {
          const errorBody = await response.text();
          console.error('Server response:', errorBody);
          errorDetail = errorBody.substring(0, 200);
        } catch (e) {
          // ignore
        }
        throw new Error(`HTTP error! status: ${response.status}${errorDetail ? ` - ${errorDetail}` : ''}`);
      }
      const result = await response.json();
      setData(result);
      setError(null);
    } catch (err) {
      console.error('Error fetching data:', err);
      setData([]);
      setError(err.message || 'Engines are down. Try again later.');
    }
  };

  const handleSearch = async () => {
    if (searchValue.trim()) {
      setLoading(true);
      const formattedSearchTerm = exactMatch ? `"${searchValue.trim()}"` : searchValue.trim();
      await fetchData(formattedSearchTerm);
      setShowTable(true);
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSearchValue('');
    setExactMatch(false);
    setShowTable(false);
    setLoading(false);
    setSelectedRow(null);
    setModalVisible(false);
    setData([]);
    setError(null);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const exportToCSV = () => {
    if (data.length === 0) return;

    const headers = Object.keys(data[0]).filter(k => k !== 'full_page_text');
    const csvContent = [
      headers.join(','),
      ...data.map(row =>
        headers.map(header => {
          let value = row[header];
          if (Array.isArray(value)) value = value.join('; ');
          if (typeof value === 'string' && (value.includes(',') || value.includes('"'))) {
            value = `"${value.replace(/"/g, '""')}"`;
          }
          return value || '';
        }).join(',')
      )
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'darkweb_results.csv';
    link.click();
    URL.revokeObjectURL(url);
  };

  const showContextModal = (record) => {
    setSelectedRow(record);
    setModalVisible(true);
  };

  const getFullPageTextForLink = (link) => {
    const firstRow = data.find(row => row.found_link === link);
    return firstRow?.full_page_text || '';
  };

  const showAllContextsModal = (link) => {
    const fullPageText = getFullPageTextForLink(link);
    setSelectedRow({
      word: 'All Keywords',
      found_link: link,
      full_page_text: fullPageText
    });
    setModalVisible(true);
  };

  const highlightKeyword = (text, searchTerm, wordInRow) => {
    if (!text) return text;
    const keywords = [wordInRow, searchTerm].filter(Boolean);
    const regex = new RegExp(`(${keywords.join('|')})`, 'gi');
    const parts = text.split(regex);

    return parts.map((part, index) => {
      const partLower = part.toLowerCase();
      if (partLower === searchTerm?.toLowerCase()) {
        return <span key={index} className="bg-red-100 text-red-700 font-semibold">{part}</span>;
      } else if (partLower === wordInRow?.toLowerCase()) {
        return <span key={index} className="bg-yellow-200 font-semibold">{part}</span>;
      }
      return part;
    });
  };

  // Build keyword counts from data
  const keywordCounts = data.reduce((acc, row) => {
    if (row.word) {
      acc[row.word] = (acc[row.word] || 0) + row.times;
    }
    return acc;
  }, {});

  const chartData = Object.entries(keywordCounts)
    .map(([word, times]) => ({ word, times }))
    .sort((a, b) => b.times - a.times)
    .slice(0, 10);

  const uniqueLinks = [...new Set(data.map(item => item.found_link))].length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dark Web Monitoring</h1>
          <p className="text-sm text-slate-500 mt-1">Search for leaked credentials and exposed data</p>
        </div>
        <div className="flex items-center gap-2">
          <Globe className="h-8 w-8 text-slate-400" />
        </div>
      </div>

      {/* Search Card */}
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
        <div className="space-y-4">
          <div className="flex gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
              <input
                type="text"
                placeholder="Enter domain or IP address..."
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
                onKeyPress={handleKeyPress}
                disabled={loading}
                className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-colors disabled:bg-slate-50"
              />
            </div>
            <button
              onClick={handleSearch}
              disabled={loading || !searchValue.trim()}
              className="px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:bg-slate-300 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Scanning...
                </>
              ) : (
                <>
                  <Search className="h-4 w-4" />
                  Scan
                </>
              )}
            </button>
          </div>

          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={exactMatch}
                onChange={(e) => setExactMatch(e.target.checked)}
                className="w-4 h-4 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm text-slate-600">Exact matching</span>
            </label>
          </div>

          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700">
              <AlertTriangle className="h-5 w-5" />
              <span>{error}</span>
            </div>
          )}
        </div>
      </div>

      {/* No Results Message */}
      {showTable && data.length === 0 && !loading && !error && (
        <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
          <div className="flex items-center gap-3 p-4 bg-green-50 border border-green-200 rounded-lg text-green-700">
            <CheckCircle className="h-6 w-6" />
            <span className="font-medium">The scan completed. No leaked credentials found.</span>
          </div>
        </div>
      )}

      {/* Results */}
      {showTable && data.length > 0 && !loading && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Search Summary */}
            <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6">
              <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">Search Summary</h3>
              <div className="flex items-start gap-3 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                <AlertTriangle className="h-5 w-5 text-amber-600 mt-0.5" />
                <div className="text-sm text-amber-800">
                  <p>
                    The search term <span className="font-semibold">{exactMatch ? `"${searchValue}"` : searchValue}</span> was found in{' '}
                    <span className="font-bold">{uniqueLinks}</span> unique links ({data.length} total occurrences).
                  </p>
                </div>
              </div>
            </div>

            {/* Keyword Frequency Chart */}
            <div className="lg:col-span-2 bg-white rounded-lg shadow-sm border border-slate-200 p-6">
              <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">Keyword Frequency</h3>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <XAxis dataKey="word" tick={{ fontSize: 12 }} />
                    <YAxis />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: 'white',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px'
                      }}
                    />
                    <Bar dataKey="times" radius={[4, 4, 0, 0]}>
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={index === 0 ? '#3b82f6' : '#64748b'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Results Table */}
          <div className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-slate-900">Results ({data.length})</h3>
              <div className="flex gap-2">
                <button
                  onClick={handleReset}
                  className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition-colors flex items-center gap-2"
                >
                  <RotateCcw className="h-4 w-4" />
                  Reset
                </button>
                <button
                  onClick={exportToCSV}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
                >
                  <Download className="h-4 w-4" />
                  Export CSV
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Keyword</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Search Term</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Counts</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Found Link</th>
                    <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Raw Content</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {data.slice(0, 50).map((row, index) => (
                    <tr key={index} className="hover:bg-slate-50">
                      <td className="px-6 py-4 text-sm text-slate-900 font-medium">{row.word}</td>
                      <td className="px-6 py-4 text-sm text-slate-600">{row.searched_term}</td>
                      <td className="px-6 py-4 text-sm text-slate-600">
                        <div className="flex items-center gap-2">
                          <span>{row.times}</span>
                          {row.contexts && row.contexts.length > 0 && (
                            <button
                              onClick={() => showContextModal(row)}
                              className="p-1 text-blue-600 hover:bg-blue-50 rounded"
                            >
                              <Eye className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <a
                          href={row.found_link}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:underline truncate block max-w-xs"
                        >
                          {row.found_link}
                        </a>
                      </td>
                      <td className="px-6 py-4 text-sm">
                        {getFullPageTextForLink(row.found_link) ? (
                          <button
                            onClick={() => showAllContextsModal(row.found_link)}
                            className="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded hover:bg-blue-700 transition-colors flex items-center gap-1"
                          >
                            <Eye className="h-3 w-3" />
                            View
                          </button>
                        ) : (
                          <span className="text-slate-400">No content</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {data.length > 50 && (
              <div className="px-6 py-3 bg-slate-50 border-t border-slate-200 text-sm text-slate-500">
                Showing 50 of {data.length} results
              </div>
            )}
          </div>
        </>
      )}

      {/* Modal */}
      {modalVisible && selectedRow && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-slate-900">
                  {selectedRow.word === 'All Keywords' ? 'Full Page Content' : `Context for "${selectedRow.word}"`}
                </h3>
                <p className="text-sm text-slate-500 truncate max-w-lg">{selectedRow.found_link}</p>
              </div>
              <button
                onClick={() => setModalVisible(false)}
                className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto max-h-[70vh]">
              {selectedRow.word === 'All Keywords' && selectedRow.full_page_text ? (
                <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                  <pre className="text-sm text-slate-700 whitespace-pre-wrap font-mono leading-relaxed">
                    {highlightKeyword(selectedRow.full_page_text, searchValue, '')}
                  </pre>
                </div>
              ) : selectedRow.contexts?.length ? (
                <div className="space-y-4">
                  {selectedRow.contexts.map((context, idx) => (
                    <div key={idx} className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                      <p className="text-xs font-semibold text-slate-500 mb-2">Context #{idx + 1}</p>
                      <p className="text-sm text-slate-700 whitespace-pre-wrap">
                        {highlightKeyword(context, searchValue, selectedRow.word)}
                      </p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-500">No context available.</p>
              )}
            </div>

            <div className="px-6 py-4 border-t border-slate-200 bg-slate-50">
              <button
                onClick={() => setModalVisible(false)}
                className="px-4 py-2 text-sm font-medium text-slate-600 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DarkwebDashboard;
