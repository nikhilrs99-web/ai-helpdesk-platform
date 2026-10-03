#!/usr/bin/env node
// End-to-end smoke test against a running `docker compose up` stack (Node 18+, no dependencies).
//
//   node scripts/e2e/smoke.mjs
//
// Drives the real system through the gateway: Keycloak login (seeded test users), ticket
// creation, outbox -> Kafka -> notification-service -> MailHog, the escalation approval gate,
// the AI-resolve path and the analytics read side. Exits non-zero on the first failed check.
//
// Local-dev only: it reads the seeded test users' passwords from the realm export in this repo.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const GATEWAY = process.env.GATEWAY_URL ?? 'http://localhost:8000';
const KEYCLOAK = process.env.KEYCLOAK_URL ?? 'http://localhost:8080';
const MAILHOG = process.env.MAILHOG_URL ?? 'http://localhost:8025';

const here = path.dirname(fileURLToPath(import.meta.url));
const realm = JSON.parse(
  readFileSync(path.join(here, '../../infrastructure/docker/keycloak/realm-export/helpdesk-realm.json'), 'utf8'),
);
const user = (name) => realm.users.find((u) => u.username === name);

let failures = 0;
function check(name, ok, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${ok || !detail ? '' : `  -> ${detail}`}`);
  if (!ok) failures++;
  return ok;
}

async function login(username) {
  const u = user(username);
  const res = await fetch(`${KEYCLOAK}/realms/helpdesk/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'helpdesk-frontend',
      username,
      password: u.credentials[0].value,
    }),
  });
  if (!res.ok) throw new Error(`login ${username} failed: ${res.status}`);
  return (await res.json()).access_token;
}

async function api(token, method, url, body) {
  const res = await fetch(`${GATEWAY}${url}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json;
  try { json = text ? JSON.parse(text) : undefined; } catch { json = text; }
  return { status: res.status, body: json };
}

async function waitFor(fn, { timeoutMs = 60000, everyMs = 2000 } = {}) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const v = await fn();
    if (v) return v;
    await new Promise((r) => setTimeout(r, everyMs));
  }
  return null;
}

async function mails() {
  const res = await fetch(`${MAILHOG}/api/v2/messages`);
  return (await res.json()).items.map((m) => ({
    to: m.Content.Headers.To ?? [],
    subject: (m.Content.Headers.Subject ?? [''])[0],
  }));
}

const findMail = (to, text) => async () =>
  (await mails()).find((m) => m.to.includes(to) && m.subject.includes(text));

try {
  await fetch(`${MAILHOG}/api/v1/messages`, { method: 'DELETE' });

  const customer = await login('test-customer');
  const agent = await login('test-agent');
  const customerEmail = user('test-customer').email;
  check('logged in as seeded customer and agent', !!customer && !!agent);

  // 1. Unauthenticated access is rejected at the gateway.
  const anon = await fetch(`${GATEWAY}/api/tickets`);
  check('gateway rejects requests without a token (401)', anon.status === 401, `got ${anon.status}`);

  // 2. Ticket creation -> email to the requester.
  const created = await api(customer, 'POST', '/api/tickets', {
    subject: 'E2E: cannot log in',
    description: 'End-to-end smoke test ticket',
    category: 'ACCESS',
    metadata: {},
  });
  check('customer creates a ticket (201)', created.status === 201, `got ${created.status}`);
  const ticketA = created.body?.id;
  const createdMail = await waitFor(findMail(customerEmail, ticketA ?? 'x'));
  check('requester receives the new-ticket email', !!createdMail);

  // 3. Escalation human-in-the-loop gate.
  const esc = await api(customer, 'POST', `/api/tickets/${ticketA}/escalations`, { reason: 'E2E escalation' });
  check('customer requests an escalation (PENDING)', esc.status === 201 && esc.body?.status === 'PENDING', `got ${esc.status}`);
  const customerApprove = await api(customer, 'PATCH', `/api/tickets/${ticketA}/escalations/${esc.body?.id}/approve`);
  check('customer cannot approve their own escalation (403)', customerApprove.status === 403, `got ${customerApprove.status}`);
  const approved = await api(agent, 'PATCH', `/api/tickets/${ticketA}/escalations/${esc.body?.id}/approve`);
  check('agent approves the escalation', approved.status === 200 && approved.body?.status === 'APPROVED', `got ${approved.status}`);
  const approvedMail = await waitFor(findMail(customerEmail, 'Escalation approved'));
  check('requester receives the escalation-approved email', !!approvedMail);

  // 4. AI-resolve is refused once a human is involved, and works otherwise.
  const refused = await api(customer, 'POST', `/api/tickets/${ticketA}/ai-resolve`);
  check('ai-resolve refused when an escalation exists (409)', refused.status === 409, `got ${refused.status}`);

  const second = await api(customer, 'POST', '/api/tickets', {
    subject: 'E2E: how do I reset my password',
    description: 'Answerable without a human',
    category: 'ACCESS',
    metadata: {},
  });
  const ticketB = second.body?.id;
  const resolved = await api(customer, 'POST', `/api/tickets/${ticketB}/ai-resolve`);
  check('ai-resolve resolves a plain ticket', resolved.status === 200 && resolved.body?.status === 'RESOLVED', `got ${resolved.status}`);

  // 5. Analytics read side (agent only) reflects both tickets and the AI resolution.
  const customerDash = await api(customer, 'GET', '/api/analytics/dashboard');
  check('customer cannot read analytics (403)', customerDash.status === 403, `got ${customerDash.status}`);
  const dash = await waitFor(async () => {
    const r = await api(agent, 'GET', '/api/analytics/dashboard');
    return r.status === 200 && r.body.aiResolutionPercentage > 0 ? r.body : null;
  });
  check('analytics reports a non-zero AI resolution rate', !!dash, 'aiResolutionPercentage stayed 0');
  const ping = await api(agent, 'POST', '/api/agents/ping');
  check('agent presence ping is routed and accepted', ping.status >= 200 && ping.status < 300, `got ${ping.status}`);

  const volume = await api(agent, 'GET', '/api/analytics/volume?days=3');
  const today = volume.body?.[volume.body.length - 1]?.count ?? 0;
  check('ticket-volume series counts today\'s tickets', volume.status === 200 && volume.body.length === 3 && today >= 2, JSON.stringify(volume.body));
} catch (err) {
  console.error('ERROR', err);
  failures++;
}

console.log(failures === 0 ? '\nAll end-to-end checks passed.' : `\n${failures} check(s) failed.`);
process.exit(failures === 0 ? 0 : 1);
