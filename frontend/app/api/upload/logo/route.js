import { NextResponse } from 'next/server'
import { writeFile, mkdir } from 'fs/promises'
import path from 'path'
import { verifyAccessToken } from '@/lib/jwt'

function getTokenPayload(request) {
  const auth = request.headers.get('authorization')
  if (!auth?.startsWith('Bearer ')) return null
  try {
    return verifyAccessToken(auth.slice(7))
  } catch {
    return null
  }
}

export async function POST(request) {
  const payload = getTokenPayload(request)
  if (!payload || payload.role !== 'admin') {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  try {
    const formData = await request.formData()
    const file = formData.get('file')

    if (!file) {
      return NextResponse.json(
        { error: 'No file provided' },
        { status: 400 }
      )
    }

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
    if (!allowedTypes.includes(file.type)) {
      return NextResponse.json(
        { error: 'Invalid file type. Only JPEG, PNG, GIF, WebP, and SVG are allowed.' },
        { status: 400 }
      )
    }

    // Validate file size (max 5MB)
    const maxSize = 5 * 1024 * 1024
    if (file.size > maxSize) {
      return NextResponse.json(
        { error: 'File too large. Maximum size is 5MB.' },
        { status: 400 }
      )
    }

    const bytes = await file.arrayBuffer()
    const buffer = Buffer.from(bytes)

    // Create uploads directory if it doesn't exist
    const uploadsDir = path.join(process.cwd(), 'public', 'uploads', 'logos')
    await mkdir(uploadsDir, { recursive: true })

    // Generate unique filename
    const ext = file.name.split('.').pop()
    const filename = `logo-${Date.now()}.${ext}`
    const filepath = path.join(uploadsDir, filename)

    // Write file
    await writeFile(filepath, buffer)

    // Return the public URL
    const logoUrl = `/uploads/logos/${filename}`

    return NextResponse.json({
      success: true,
      logoUrl
    })
  } catch (error) {
    console.error('Logo upload error:', error)
    return NextResponse.json(
      { error: 'Failed to upload file' },
      { status: 500 }
    )
  }
}
