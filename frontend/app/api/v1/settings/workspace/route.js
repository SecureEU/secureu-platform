import { NextResponse } from 'next/server';
import { getDb } from '@/lib/mongodb';
import { verifyAccessToken } from '@/lib/jwt';

function getTokenPayload(request) {
  const auth = request.headers.get('authorization');
  if (!auth?.startsWith('Bearer ')) return null;
  try {
    return verifyAccessToken(auth.slice(7));
  } catch {
    return null;
  }
}

export async function GET() {
  // Public endpoint — name and logo are needed for header/landing page
  try {
    const db = await getDb();
    const workspace = await db.collection('workspace').findOne({});
    if (!workspace) {
      return NextResponse.json({ error: 'Workspace not found' }, { status: 404 });
    }
    return NextResponse.json({
      name: workspace.name,
      logo_url: workspace.logo_url || null,
      createdAt: workspace.createdAt,
    });
  } catch (error) {
    console.error('GET workspace error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function PUT(request) {
  const payload = getTokenPayload(request);
  if (!payload || payload.role !== 'admin') {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const body = await request.json();
    const { name, logo_url } = body;

    // At least one field must be provided
    if (name === undefined && logo_url === undefined) {
      return NextResponse.json({ error: 'No fields to update' }, { status: 400 });
    }

    const updateFields = {};
    if (name !== undefined) {
      if (!name?.trim()) {
        return NextResponse.json({ error: 'Name is required' }, { status: 400 });
      }
      updateFields.name = name.trim();
    }
    if (logo_url !== undefined) {
      // logo_url can be null (to remove) or a string
      updateFields.logo_url = logo_url;
    }

    const db = await getDb();
    const result = await db.collection('workspace').findOneAndUpdate(
      {},
      { $set: updateFields },
      { returnDocument: 'after' }
    );

    if (!result) {
      return NextResponse.json({ error: 'Workspace not found' }, { status: 404 });
    }

    return NextResponse.json({
      name: result.name,
      logo_url: result.logo_url || null,
      createdAt: result.createdAt,
    });
  } catch (error) {
    console.error('PUT workspace error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
