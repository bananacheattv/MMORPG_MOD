'use strict';
// Mots de passe (scrypt), jetons de session, signatures HMAC : uniquement node:crypto.
const crypto = require('node:crypto');
const { promisify } = require('node:util');

const scrypt = promisify(crypto.scrypt);
const SCRYPT = { N: 32768, r: 8, p: 1, maxmem: 64 * 1024 * 1024 };
const KEY_LENGTH = 64;

async function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const key = await scrypt(password.normalize('NFKC'), salt, KEY_LENGTH, SCRYPT);
  return ['scrypt', SCRYPT.N, SCRYPT.r, SCRYPT.p, salt.toString('base64url'), key.toString('base64url')].join('$');
}

async function verifyPassword(password, stored) {
  const [kind, N, r, p, salt, key] = String(stored || '').split('$');
  if (kind !== 'scrypt' || !salt || !key) return false;
  const expected = Buffer.from(key, 'base64url');
  const actual = await scrypt(password.normalize('NFKC'), Buffer.from(salt, 'base64url'), expected.length, {
    N: Number(N), r: Number(r), p: Number(p), maxmem: SCRYPT.maxmem,
  });
  return crypto.timingSafeEqual(actual, expected);
}

// Hachage factice : même coût qu'une vraie vérification quand l'identifiant n'existe pas (pas d'énumération par le temps).
let dummyHash = null;
async function burnPasswordCheck(password) {
  dummyHash ??= await hashPassword('aeloria-dummy-password');
  await verifyPassword(password, dummyHash);
  return false;
}

const randomId = (prefix, bytes = 18) => prefix + crypto.randomBytes(bytes).toString('base64url');
const sha256 = (value) => crypto.createHash('sha256').update(value).digest('hex');
const hmacHex = (secret, value) => crypto.createHmac('sha256', secret).update(value).digest('hex');

function safeEqualHex(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length || !/^[a-f0-9]+$/i.test(a + b)) return false;
  return crypto.timingSafeEqual(Buffer.from(a, 'hex'), Buffer.from(b, 'hex'));
}

module.exports = { hashPassword, verifyPassword, burnPasswordCheck, randomId, sha256, hmacHex, safeEqualHex };
