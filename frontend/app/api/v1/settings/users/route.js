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

export async function GET(request) {
  const payload = getTokenPayload(request);
  if (!payload || payload.role !== 'admin') {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const db = await getDb();
    const users = await db.collection('users')
      .find({}, { projection: { password: 0 } })
      .sort({ createdAt: 1 })
      .toArray();

    return NextResponse.json({
      users: users.map(u => ({
        id: u._id.toString(),
        name: u.name,
        email: u.email,
        role: u.role,
        createdAt: u.createdAt,
      })),
    });
  } catch (error) {
    console.error('GET users error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
