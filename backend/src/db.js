'use strict';
// Stockage SQLite (node:sqlite, intégré à Node 22.13+) : comptes, sessions, défis Minecraft, commandes.
const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

const SCHEMA = `
CREATE TABLE IF NOT EXISTS accounts (
  id INTEGER PRIMARY KEY,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  email TEXT NOT NULL UNIQUE COLLATE NOCASE,
  password TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  mc_uuid TEXT UNIQUE,
  mc_name TEXT,
  linked_at INTEGER
);
CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,
  account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  client TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_account ON sessions(account_id);
CREATE TABLE IF NOT EXISTS mc_challenges (
  server_id TEXT PRIMARY KEY,
  account_id INTEGER REFERENCES accounts(id) ON DELETE CASCADE,
  purpose TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS orders (
  id TEXT PRIMARY KEY,
  account_id INTEGER NOT NULL REFERENCES accounts(id),
  mc_uuid TEXT NOT NULL,
  pack TEXT NOT NULL,
  credits INTEGER NOT NULL,
  amount INTEGER NOT NULL,
  currency TEXT NOT NULL,
  status TEXT NOT NULL,
  stripe_session TEXT UNIQUE,
  payment_intent TEXT,
  live INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  paid_at INTEGER,
  delivered_at INTEGER,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at INTEGER,
  last_error TEXT
);
CREATE INDEX IF NOT EXISTS orders_account ON orders(account_id, created_at);
CREATE INDEX IF NOT EXISTS orders_delivery ON orders(status, next_attempt_at);
CREATE TABLE IF NOT EXISTS stripe_events (
  id TEXT PRIMARY KEY,
  received_at INTEGER NOT NULL
);
`;

function openStore(file) {
  if (file !== ':memory:') fs.mkdirSync(path.dirname(file), { recursive: true });
  const db = new DatabaseSync(file);
  db.exec('PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000;');
  db.exec(SCHEMA);
  const q = (sql) => db.prepare(sql);
  const st = {
    insertAccount: q('INSERT INTO accounts (username, email, password, created_at) VALUES (?, ?, ?, ?)'),
    accountById: q('SELECT * FROM accounts WHERE id = ?'),
    accountByLogin: q('SELECT * FROM accounts WHERE username = ? OR email = ?'),
    accountByUuid: q('SELECT * FROM accounts WHERE mc_uuid = ?'),
    usernameTaken: q('SELECT 1 FROM accounts WHERE username = ?'),
    emailTaken: q('SELECT 1 FROM accounts WHERE email = ?'),
    setPassword: q('UPDATE accounts SET password = ? WHERE id = ?'),
    linkMinecraft: q('UPDATE accounts SET mc_uuid = ?, mc_name = ?, linked_at = ? WHERE id = ? AND (mc_uuid IS NULL OR mc_uuid = ?)'),
    insertSession: q('INSERT INTO sessions (token_hash, account_id, client, created_at, expires_at) VALUES (?, ?, ?, ?, ?)'),
    sessionAccount: q('SELECT a.*, s.client AS session_client FROM sessions s JOIN accounts a ON a.id = s.account_id WHERE s.token_hash = ? AND s.expires_at > ?'),
    deleteSession: q('DELETE FROM sessions WHERE token_hash = ?'),
    deleteSessions: q('DELETE FROM sessions WHERE account_id = ?'),
    purgeSessions: q('DELETE FROM sessions WHERE expires_at <= ?'),
    insertChallenge: q('INSERT INTO mc_challenges (server_id, account_id, purpose, expires_at) VALUES (?, ?, ?, ?)'),
    takeChallenge: q('DELETE FROM mc_challenges WHERE server_id = ? AND purpose = ? AND expires_at > ? RETURNING account_id'),
    purgeChallenges: q('DELETE FROM mc_challenges WHERE expires_at <= ?'),
    insertOrder: q(`INSERT INTO orders (id, account_id, mc_uuid, pack, credits, amount, currency, status, live, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?)`),
    setOrderSession: q('UPDATE orders SET stripe_session = ? WHERE id = ?'),
    orderById: q('SELECT * FROM orders WHERE id = ?'),
    ordersOf: q('SELECT * FROM orders WHERE account_id = ? ORDER BY created_at DESC LIMIT ?'),
    recentOrders: q("SELECT COUNT(*) AS n FROM orders WHERE account_id = ? AND created_at > ?"),
    markPaid: q("UPDATE orders SET status = 'paid', paid_at = ?, payment_intent = ?, next_attempt_at = ? WHERE id = ? AND status IN ('pending', 'expired')"),
    markStatus: q('UPDATE orders SET status = ?, last_error = ? WHERE id = ? AND status = ?'),
    markDelivered: q("UPDATE orders SET status = 'delivered', delivered_at = ?, last_error = NULL WHERE id = ? AND status = 'paid'"),
    markRetry: q("UPDATE orders SET attempts = attempts + 1, next_attempt_at = ?, last_error = ? WHERE id = ? AND status = 'paid'"),
    markRefunded: q("UPDATE orders SET status = 'refunded' WHERE payment_intent = ? AND status IN ('paid', 'delivered')"),
    dueOrders: q("SELECT * FROM orders WHERE status = 'paid' AND next_attempt_at <= ? ORDER BY paid_at LIMIT 20"),
    insertEvent: q('INSERT OR IGNORE INTO stripe_events (id, received_at) VALUES (?, ?)'),
  };
  return { db, st, close: () => db.close() };
}

module.exports = { openStore };
