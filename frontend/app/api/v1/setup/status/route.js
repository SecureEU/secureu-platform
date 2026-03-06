import { NextResponse } from 'next/server';
import { getDb } from '@/lib/mongodb';

export async function GET() {
  try {
    const db = await getDb();
    const workspace = await db.collection('workspace').findOne({});
    return NextResponse.json({ needsSetup: !workspace });
  } catch (error) {
    console.error('Setup status error:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
