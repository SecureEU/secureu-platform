// Guards the bcryptjs -> native bcrypt swap. Every password already in Mongo was
// hashed by bcryptjs at cost 12; if native bcrypt could not verify those, the
// swap would lock every existing user out.
//
// Run with: node --test tests/bcrypt-compat.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import bcrypt from 'bcrypt';

// Produced by bcryptjs@3 at cost 12. Checked in deliberately: the point is to
// verify against a hash this library did not create.
const BCRYPTJS_HASH = '$2b$12$34E2iWaZWpxFiRcodt0HbeT8qoC9Tp5Hr1t9n4.8v6roywCzmz34a';
const PASSWORD = 'correct horse battery staple';

test('native bcrypt verifies a hash produced by bcryptjs', async () => {
  assert.equal(await bcrypt.compare(PASSWORD, BCRYPTJS_HASH), true);
});

test('native bcrypt rejects the wrong password against a bcryptjs hash', async () => {
  assert.equal(await bcrypt.compare('wrong password', BCRYPTJS_HASH), false);
});

test('cost 12 round-trips through native bcrypt', async () => {
  const hash = await bcrypt.hash(PASSWORD, 12);
  assert.match(hash, /^\$2[ab]\$12\$/);
  assert.equal(await bcrypt.compare(PASSWORD, hash), true);
});
