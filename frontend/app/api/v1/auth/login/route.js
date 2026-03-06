import { NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { getDb } from '@/lib/mongodb';
import { signAccessToken } from '@/lib/jwt';

export async function POST(request) {
  try {
    const { email, password } = await request.json();

    if (!email || !password) {
      return NextResponse.json(
        { error: 'Email and password are required' },
        { status: 400 }
      );
    }

    const db = await getDb();

    const user = await db.collection('users').findOne({
      email: email.toLowerCase(),
    });

    if (!user) {
      return NextResponse.json(
        { error: 'Invalid email or password' },
        { status: 401 }
      );
    }

    const valid = await bcrypt.compare(password, user.password);
    if (!valid) {
      return NextResponse.json(
        { error: 'Invalid email or password' },
        { status: 401 }
      );
    }

    const accessToken = signAccessToken({
      sub: user._id.toString(),
      email: user.email,
      role: user.role,
    });

    const refreshToken = crypto.randomBytes(40).toString('hex');
    await db.collection('refresh_tokens').insertOne({
      token: refreshToken,
      userId: user._id,
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    });

    return NextResponse.json({
      access_token: accessToken,
      refresh_token: refreshToken,
      user: {
        id: user._id.toString(),
        name: user.name,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
