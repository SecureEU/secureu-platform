import { NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { getDb } from '@/lib/mongodb';
import { signAccessToken } from '@/lib/jwt';

export async function POST(request) {
  try {
    const { email, password, name } = await request.json();

    if (!email || !password || !name) {
      return NextResponse.json(
        { error: 'Email, password, and name are required' },
        { status: 400 }
      );
    }

    const db = await getDb();

    const existing = await db.collection('users').findOne({
      email: email.toLowerCase(),
    });

    if (existing) {
      return NextResponse.json(
        { error: 'Email already registered' },
        { status: 409 }
      );
    }

    const hashedPassword = await bcrypt.hash(password, 12);

    const user = {
      email: email.toLowerCase(),
      password: hashedPassword,
      name,
      role: 'user',
      createdAt: new Date(),
    };

    const result = await db.collection('users').insertOne(user);

    const accessToken = signAccessToken({
      sub: result.insertedId.toString(),
      email: user.email,
      role: user.role,
    });

    const refreshToken = crypto.randomBytes(40).toString('hex');
    await db.collection('refresh_tokens').insertOne({
      token: refreshToken,
      userId: result.insertedId,
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 7 days
    });

    return NextResponse.json({
      access_token: accessToken,
      refresh_token: refreshToken,
      user: {
        id: result.insertedId.toString(),
        name: user.name,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    console.error('Register error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
