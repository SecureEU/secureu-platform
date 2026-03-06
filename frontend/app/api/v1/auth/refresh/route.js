import { NextResponse } from 'next/server';
import crypto from 'crypto';
import { ObjectId } from 'mongodb';
import { getDb } from '@/lib/mongodb';
import { signAccessToken } from '@/lib/jwt';

export async function POST(request) {
  try {
    const { refresh_token } = await request.json();

    if (!refresh_token) {
      return NextResponse.json(
        { error: 'Refresh token required' },
        { status: 400 }
      );
    }

    const db = await getDb();

    // Rotate: find and delete in one atomic op
    const record = await db.collection('refresh_tokens').findOneAndDelete({
      token: refresh_token,
    });

    if (!record) {
      return NextResponse.json(
        { error: 'Invalid refresh token' },
        { status: 401 }
      );
    }

    if (record.expiresAt < new Date()) {
      return NextResponse.json(
        { error: 'Refresh token expired' },
        { status: 401 }
      );
    }

    const user = await db.collection('users').findOne({
      _id: new ObjectId(record.userId),
    });

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 401 }
      );
    }

    const accessToken = signAccessToken({
      sub: user._id.toString(),
      email: user.email,
      role: user.role,
    });

    const newRefreshToken = crypto.randomBytes(40).toString('hex');
    await db.collection('refresh_tokens').insertOne({
      token: newRefreshToken,
      userId: user._id,
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    });

    return NextResponse.json({
      access_token: accessToken,
      refresh_token: newRefreshToken,
    });
  } catch (error) {
    console.error('Refresh error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
