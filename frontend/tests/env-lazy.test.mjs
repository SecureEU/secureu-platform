// Next.js imports every route at build time to collect page data. Anything that
// throws while a module is being evaluated therefore kills the whole build, and
// it is reported against whichever route happened to import it first:
//
//   [Error: Failed to collect page data for /api/upload/logo]
//
// So configuration is checked when it is used, not when the module loads. These
// tests pin that: importing must be safe without env, using must still fail
// loudly with a message naming the missing variable.
//
// The lib files are ESM but named .js, and the package has no "type": "module"
// (Next.js transpiles them itself), so plain node cannot import them directly.
// Each is copied to a sibling .mjs and imported from there - same source text,
// same module resolution.
import test from 'node:test';
import assert from 'node:assert/strict';
import { copyFileSync, rmSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));

delete process.env.JWT_SECRET;
delete process.env.MONGODB_URI;
delete process.env.MONGODB_DB;

function loadLib(name) {
  const copy = path.join(here, `.tmp-${name}.mjs`);
  copyFileSync(path.join(here, '..', 'lib', `${name}.js`), copy);
  return import(copy).finally(() => rmSync(copy, { force: true }));
}

test('lib/jwt.js imports without JWT_SECRET set', async () => {
  const jwt = await loadLib('jwt');
  assert.equal(typeof jwt.signAccessToken, 'function');
});

test('signing without JWT_SECRET still fails, and names the variable', async () => {
  const jwt = await loadLib('jwt');
  assert.throws(() => jwt.signAccessToken({ sub: '1' }), /JWT_SECRET/);
});

test('lib/mongodb.js imports without MONGODB_URI/MONGODB_DB set', async () => {
  const mongo = await loadLib('mongodb');
  assert.equal(typeof mongo.getDb, 'function');
});

test('getDb without MONGODB_URI still fails, and names the variable', async () => {
  const mongo = await loadLib('mongodb');
  await assert.rejects(() => mongo.getDb(), /MONGODB_URI/);
});

test('getDb without MONGODB_DB still fails, and names that variable', async () => {
  process.env.MONGODB_URI = 'mongodb://unused.invalid:27017';
  const mongo = await loadLib('mongodb');
  await assert.rejects(() => mongo.getDb(), /MONGODB_DB/);
  delete process.env.MONGODB_URI;
});
