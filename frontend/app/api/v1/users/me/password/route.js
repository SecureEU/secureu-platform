import { NextResponse } from 'next/server';
import { ObjectId } from 'mongodb';
import bcrypt from 'bcryptjs';
import { getDb } from '@/lib/mongodb';
import { verifyAccessToken } from '@/lib/jwt';

export async function POST(request) {
  // Verify token
  const auth = request.headers.get('authorization');
  if (!auth?.startsWith('Bearer ')) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  let userId;
  try {
    const payload = verifyAccessToken(auth.slice(7));
    userId = payload.sub;
  } catch {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {
    const { current_password, new_password } = await request.json();

    if (!current_password || !new_password) {
      return NextResponse.json(
        { error: 'Current password and new password are required' },
        { status: 400 }
      );
    }

    if (new_password.length < 6) {
      return NextResponse.json(
        { error: 'New password must be at least 6 characters' },
        { status: 400 }
      );
    }

    const db = await getDb();
    const user = await db.collection('users').findOne({ _id: new ObjectId(userId) });

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const valid = await bcrypt.compare(current_password, user.password);
    if (!valid) {
      return NextResponse.json({ error: 'Current password is incorrect' }, { status: 401 });
    }

    const hashedPassword = await bcrypt.hash(new_password, 12);
    await db.collection('users').updateOne(
      { _id: new ObjectId(userId) },
      { $set: { password: hashedPassword } }
    );

    return NextResponse.json({ message: 'Password changed successfully' });
  } catch (error) {
    console.error('Change password error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
