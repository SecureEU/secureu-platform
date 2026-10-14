import jwt from 'jsonwebtoken';

// Read at call time, not at module load. Next.js evaluates every route module
// while collecting page data during `next build`, so throwing here would fail
// the build against an unrelated route rather than telling you what is missing.
function secret() {
  const value = process.env.JWT_SECRET;
  if (!value) throw new Error('JWT_SECRET is not set');
  return value;
}

export function signAccessToken(payload) {
  return jwt.sign(payload, secret(), { expiresIn: '15m' });
}

export function verifyAccessToken(token) {
  return jwt.verify(token, secret());
}
