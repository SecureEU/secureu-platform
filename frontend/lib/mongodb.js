import { MongoClient } from 'mongodb';

const uri = process.env.MONGODB_URI;
const dbName = process.env.MONGODB_DB;

if (!uri) throw new Error('MONGODB_URI is not set');
if (!dbName) throw new Error('MONGODB_DB is not set');

const options = {};

let clientPromise;

if (process.env.NODE_ENV === 'development') {
  // Cache on globalThis to survive hot-reload
  if (!globalThis._mongoClientPromise) {
    const client = new MongoClient(uri, options);
    globalThis._mongoClientPromise = client.connect();
  }
  clientPromise = globalThis._mongoClientPromise;
} else {
  const client = new MongoClient(uri, options);
  clientPromise = client.connect();
}

let indexesCreated = false;

async function ensureIndexes(db) {
  if (indexesCreated) return;
  await Promise.all([
    db.collection('users').createIndex({ email: 1 }, { unique: true }),
    db.collection('refresh_tokens').createIndex({ token: 1 }, { unique: true }),
    db.collection('refresh_tokens').createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 }),
  ]);
  indexesCreated = true;
}

export async function getDb() {
  const client = await clientPromise;
  const db = client.db(dbName);
  await ensureIndexes(db);
  return db;
}

