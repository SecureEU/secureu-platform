import { NextResponse } from 'next/server'
import https from 'https'

const SEUXDR_API = process.env.NEXT_PUBLIC_SEUXDR_API_URL || 'https://localhost:8443'

// Helper to make HTTPS requests that accept self-signed certs (text/JSON)
function fetchInsecure(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url)
    const reqOptions = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port,
      path: parsedUrl.pathname + parsedUrl.search,
      method: options.method || 'GET',
      headers: options.headers || {},
      rejectUnauthorized: false,
    }

    const req = https.request(reqOptions, (res) => {
      let data = ''
      res.on('data', (chunk) => { data += chunk })
      res.on('end', () => {
        resolve({ status: res.statusCode, data, headers: res.headers, ok: res.statusCode >= 200 && res.statusCode < 300 })
      })
    })

    req.on('error', reject)

    if (options.body) {
      req.write(options.body)
    }

    req.end()
  })
}

// Helper for binary downloads (returns Buffer)
function fetchInsecureBinary(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url)
    const reqOptions = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port,
      path: parsedUrl.pathname + parsedUrl.search,
      method: options.method || 'GET',
      headers: options.headers || {},
      rejectUnauthorized: false,
    }

    const req = https.request(reqOptions, (res) => {
      const chunks = []
      res.on('data', (chunk) => chunks.push(chunk))
      res.on('end', () => {
        resolve({
          status: res.statusCode,
          data: Buffer.concat(chunks),
          headers: res.headers,
          ok: res.statusCode >= 200 && res.statusCode < 300,
        })
      })
    })

    req.on('error', reject)
    req.end()
  })
}

export async function GET(request) {
  try {
    const { searchParams } = new URL(request.url)
    const endpoint = searchParams.get('endpoint') || 'status'

    const params = new URLSearchParams()
    for (const [key, value] of searchParams.entries()) {
      if (key !== 'endpoint') {
        params.append(key, value)
      }
    }

    const targetUrl = `${SEUXDR_API}/api/${endpoint}${params.toString() ? '?' + params.toString() : ''}`

    // Binary download for agent downloads
    if (endpoint === 'download/agent') {
      const response = await fetchInsecureBinary(targetUrl)

      if (!response.ok) {
        return NextResponse.json(
          { error: `Backend returned ${response.status}` },
          { status: response.status }
        )
      }

      const headers = new Headers()
      const contentType = response.headers['content-type'] || 'application/octet-stream'
      headers.set('Content-Type', contentType)
      headers.set('Content-Length', String(response.data.length))
      if (response.headers['content-disposition']) {
        headers.set('Content-Disposition', response.headers['content-disposition'])
      }
      if (response.headers['x-agent-version']) {
        headers.set('X-Agent-Version', response.headers['x-agent-version'])
      }

      return new NextResponse(response.data, { status: 200, headers })
    }

    // JSON endpoints
    const response = await fetchInsecure(targetUrl, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    })

    if (!response.ok) {
      return NextResponse.json(
        { error: `Backend returned ${response.status}` },
        { status: response.status }
      )
    }

    const data = JSON.parse(response.data)
    return NextResponse.json(data)
  } catch (error) {
    console.error('SEUXDR proxy error:', error)
    return NextResponse.json(
      { error: 'Failed to fetch from SEUXDR backend', details: error.message },
      { status: 500 }
    )
  }
}

export async function POST(request) {
  try {
    const { searchParams } = new URL(request.url)
    const endpoint = searchParams.get('endpoint')

    if (!endpoint) {
      return NextResponse.json({ error: 'Missing endpoint parameter' }, { status: 400 })
    }

    const body = await request.json().catch(() => null)
    const targetUrl = `${SEUXDR_API}/api/${endpoint}`

    const response = await fetchInsecure(targetUrl, {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
      body: body ? JSON.stringify(body) : undefined,
    })

    if (!response.ok) {
      return NextResponse.json(
        { error: `Backend returned ${response.status}` },
        { status: response.status }
      )
    }

    const data = JSON.parse(response.data)
    return NextResponse.json(data)
  } catch (error) {
    console.error('SEUXDR proxy error:', error)
    return NextResponse.json(
      { error: 'Failed to fetch from SEUXDR backend', details: error.message },
      { status: 500 }
    )
  }
}
