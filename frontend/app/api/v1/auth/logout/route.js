import { NextResponse } from 'next/server';
import { getDb } from '@/lib/mongodb';

export async function POST(request) {
  try {
    const { refresh_token } = await request.json();

    if (refresh_token) {
      const db = await getDb();
      await db.collection('refresh_tokens').deleteOne({ token: refresh_token });
    }

    return NextResponse.json({ message: 'Logged out' });
  } catch (error) {
    console.error('Logout error:', error);
    return NextResponse.json({ message: 'Logged out' });
  }
}
