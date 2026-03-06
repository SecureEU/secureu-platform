import { NextResponse } from 'next/server';
import { ObjectId } from 'mongodb';
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

export async function PATCH(request, { params }) {
  const payload = getTokenPayload(request);
  if (!payload || payload.role !== 'admin') {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { id } = await params;

  // Guard against self-demotion
  if (id === payload.sub) {
    return NextResponse.json(
      { error: 'Cannot change your own role' },
      { status: 400 }
    );
  }

  try {
    const { role } = await request.json();
    if (!role || !['admin', 'user'].includes(role)) {
      return NextResponse.json(
        { error: 'Role must be "admin" or "user"' },
        { status: 400 }
      );
    }

    const db = await getDb();
    const result = await db.collection('users').findOneAndUpdate(
      { _id: new ObjectId(id) },
      { $set: { role } },
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
    });
  } catch (error) {
    console.error('PATCH user error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function DELETE(request, { params }) {
  const payload = getTokenPayload(request);
  if (!payload || payload.role !== 'admin') {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const { id } = await params;

  // Guard against self-deletion
  if (id === payload.sub) {
    return NextResponse.json(
      { error: 'Cannot delete your own account' },
      { status: 400 }
    );
  }

  try {
    const db = await getDb();
    const result = await db.collection('users').deleteOne({
      _id: new ObjectId(id),
    });

    if (result.deletedCount === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    // Clean up refresh tokens for deleted user
    await db.collection('refresh_tokens').deleteMany({
      userId: new ObjectId(id),
    });

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('DELETE user error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
