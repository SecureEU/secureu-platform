import { NextResponse } from 'next/server'

const REDFLAGS_API = process.env.NEXT_PUBLIC_REDFLAGS_API_URL || 'https://api.redflags.iee.ihu.gr'

export async function GET(request) {
  try {
    const { searchParams } = new URL(request.url)
    const endpoint = searchParams.get('endpoint') || 'incidents'

    // Build the target URL with all query params except 'endpoint'
    const params = new URLSearchParams()
    for (const [key, value] of searchParams.entries()) {
      if (key !== 'endpoint') {
        params.append(key, value)
      }
    }

    const targetUrl = `${REDFLAGS_API}/${endpoint}${params.toString() ? '?' + params.toString() : ''}`
    console.log('Proxying to:', targetUrl)

    const response = await fetch(targetUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    })

    if (!response.ok) {
      return NextResponse.json(
        { error: `Backend returned ${response.status}` },
        { status: response.status }
      )
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error('Proxy error:', error)
    return NextResponse.json(
      { error: 'Failed to fetch from backend', details: error.message },
      { status: 500 }
    )
  }
}
