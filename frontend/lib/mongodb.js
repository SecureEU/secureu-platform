import { MongoClient } from 'mongodb';

// Everything here is deferred to the first getDb() call. Reading the config, and
// opening the connection, used to happen while this module was being evaluated -
// which Next.js does for every route during `next build`. A missing variable
// therefore failed the build, reported against whichever route imported this
// first, and a build on a machine with no database would try to connect to one.
const options = {};

let clientPromise;

function config() {
  const uri = process.env.MONGODB_URI;
  const dbName = process.env.MONGODB_DB;
  if (!uri) throw new Error('MONGODB_URI is not set');
  if (!dbName) throw new Error('MONGODB_DB is not set');
  return { uri, dbName };
}

function connect(uri) {
  if (process.env.NODE_ENV === 'development') {
    // Cache on globalThis to survive hot-reload
    if (!globalThis._mongoClientPromise) {
      globalThis._mongoClientPromise = new MongoClient(uri, options).connect();
    }
    return globalThis._mongoClientPromise;
  }
  if (!clientPromise) {
    clientPromise = new MongoClient(uri, options).connect();
  }
  return clientPromise;
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
  const { uri, dbName } = config();
  const client = await connect(uri);
  const db = client.db(dbName);
  await ensureIndexes(db);
  return db;
}
