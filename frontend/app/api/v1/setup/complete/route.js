import { NextResponse } from 'next/server';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { getDb } from '@/lib/mongodb';
import { signAccessToken } from '@/lib/jwt';

export async function POST(request) {
  try {
    const { companyName, email, password, name } = await request.json();

    if (!companyName || !email || !password || !name) {
      return NextResponse.json(
        { error: 'All fields are required' },
        { status: 400 }
      );
    }

    const db = await getDb();

    // One-time use: reject if workspace already exists
    const existing = await db.collection('workspace').findOne({});
    if (existing) {
      return NextResponse.json(
        { error: 'Setup has already been completed' },
        { status: 409 }
      );
    }

    // Create workspace
    await db.collection('workspace').insertOne({
      name: companyName.trim(),
      createdAt: new Date(),
    });

    // Create admin user
    const hashedPassword = await bcrypt.hash(password, 12);
    const user = {
      email: email.toLowerCase(),
      password: hashedPassword,
      name,
      role: 'admin',
      createdAt: new Date(),
    };

    const result = await db.collection('users').insertOne(user);

    // Generate tokens
    const accessToken = signAccessToken({
      sub: result.insertedId.toString(),
      email: user.email,
      role: user.role,
    });

    const refreshToken = crypto.randomBytes(40).toString('hex');
    await db.collection('refresh_tokens').insertOne({
      token: refreshToken,
      userId: result.insertedId,
      expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
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
    console.error('Setup complete error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
