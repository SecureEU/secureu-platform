import { NextResponse } from 'next/server';
import { ObjectId } from 'mongodb';
import { getDb } from '@/lib/mongodb';
import { verifyAccessToken } from '@/lib/jwt';

function getUserIdFromRequest(request) {
  const auth = request.headers.get('authorization');
  if (!auth?.startsWith('Bearer ')) return null;
  try {
    const payload = verifyAccessToken(auth.slice(7));
    return payload.sub;
  } catch {
    return null;
  }
}

export async function GET(request) {
  const userId = getUserIdFromRequest(request);
  if (!userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const db = await getDb();
    const user = await db.collection('users').findOne(
      { _id: new ObjectId(userId) },
      { projection: { password: 0 } }
    );

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    return NextResponse.json({
      id: user._id.toString(),
      name: user.name,
      email: user.email,
      role: user.role,
      avatar_url: user.avatar_url || '',
    });
  } catch (error) {
    console.error('GET /users/me error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function PUT(request) {
  const userId = getUserIdFromRequest(request);
  if (!userId) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const body = await request.json();

    // Whitelist updatable fields
    const allowed = ['name', 'avatar_url'];
    const updates = {};
    for (const key of allowed) {
      if (body[key] !== undefined) {
        updates[key] = body[key];
      }
    }

    if (Object.keys(updates).length === 0) {
      return NextResponse.json({ error: 'No valid fields to update' }, { status: 400 });
    }

    const db = await getDb();
    const result = await db.collection('users').findOneAndUpdate(
      { _id: new ObjectId(userId) },
      { $set: updates },
      { returnDocument: 'after', projection: { password: 0 } }
    );

    if (!result) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    return NextResponse.json({
      id: result._id.toString(),
      name: result.name,
      email: result.email,
      role: result.role,
      avatar_url: result.avatar_url || '',
    });
  } catch (error) {
    console.error('PUT /users/me error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
