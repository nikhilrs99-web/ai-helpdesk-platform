// Fills a freshly started local stack with realistic demo data so the screenshots look like a
// real support desk. Local-dev only: logs in as the seeded Keycloak test users from this repo's
// realm export and talks to the gateway like any client.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const GATEWAY = process.env.GATEWAY_URL ?? 'http://localhost:8000';
const KEYCLOAK = process.env.KEYCLOAK_URL ?? 'http://localhost:8080';
const here = path.dirname(fileURLToPath(import.meta.url));
const realm = JSON.parse(
  readFileSync(path.join(here, '../../infrastructure/docker/keycloak/realm-export/helpdesk-realm.json'), 'utf8'),
);

export async function login(username) {
  const u = realm.users.find((x) => x.username === username);
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
  if (!res.ok) throw new Error(`login ${username}: ${res.status}`);
  return (await res.json()).access_token;
}

export function credentialsFor(username) {
  const u = realm.users.find((x) => x.username === username);
  return { username, password: u.credentials[0].value };
}

async function api(token, method, url, body) {
  const res = await fetch(`${GATEWAY}${url}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const json = text ? JSON.parse(text) : undefined;
  if (!res.ok) throw new Error(`${method} ${url} -> ${res.status} ${text}`);
  return json;
}

const ARTICLES = [
  ['How to reset your password', 'ACCESS',
    'Open the sign-in page and choose "Forgot password". Enter the email address on your account and follow the link we send you. The link expires after 30 minutes. If it does not arrive, check your spam folder or contact support.'],
  ['Understanding your invoice', 'BILLING',
    'Each invoice lists the plan, the billing period and any usage charges. Invoices are issued on the first day of the month. You can download a PDF copy from Billing > Invoices, and finance contacts can be added under Billing > Settings.'],
  ['Troubleshooting login errors', 'ACCESS',
    'If you see "invalid credentials", make sure caps lock is off. After five failed attempts the account is locked for 15 minutes. Clearing cookies or trying a private window often fixes single-sign-on loops.'],
  ['Exporting your data to CSV', 'HOW_TO',
    'Go to Reports, pick a date range and choose Export > CSV. Large exports are prepared in the background and emailed to you when ready. Exports include all ticket fields except private notes.'],
  ['Known issue: dashboard charts load slowly in Safari', 'BUG',
    'We are aware that dashboard charts can take several seconds to render in Safari 17. As a workaround use Chrome or Firefox. A fix is planned for the next release.'],
  ['Requesting a new feature', 'FEATURE_REQUEST',
    'Describe the problem you are trying to solve rather than the solution. Include how often it happens and how you work around it today. Requests are reviewed every two weeks by the product team.'],
];

export async function seed() {
  const customer = await login('test-customer');
  const customer2 = await login('test-customer-2');
  const agent = await login('test-agent');

  for (const [title, category, body] of ARTICLES) {
    await api(agent, 'POST', '/api/articles', { title, body, category });
  }

  const t = (token, subject, description, category, metadata = {}) =>
    api(token, 'POST', '/api/tickets', { subject, description, category, metadata });

  const login1 = await t(customer, 'Cannot log in after password reset',
    'I reset my password but the new one is rejected and the reset link now says it has expired.', 'ACCESS');
  const bill = await t(customer, 'Charged twice for the October invoice',
    'My card shows two charges for invoice INV-2041. Please refund the duplicate.', 'BILLING', { invoiceId: 'INV-2041' });
  const bug = await t(customer2, 'Dashboard crashes when exporting a report',
    'Clicking Export on the monthly report shows a blank page. Works on the previous release.', 'BUG',
    { browser: 'Firefox 131', appVersion: '4.12.0' });
  const how = await t(customer2, 'How do I add a teammate to my workspace?',
    'I could not find where to invite a new colleague.', 'HOW_TO');
  const feat = await t(customer, 'Dark mode for the customer portal',
    'It would be great to have a dark theme, I use the portal late in the evening.', 'FEATURE_REQUEST');

  // Lifecycle: an agent works some tickets, the customer escalates one, one is resolved by the AI.
  await api(agent, 'PATCH', `/api/tickets/${bug.id}/status`, { status: 'AI_TRIAGED' });
  await api(agent, 'PATCH', `/api/tickets/${bug.id}/status`, { status: 'ASSIGNED' });
  await api(agent, 'PATCH', `/api/tickets/${bug.id}/status`, { status: 'IN_PROGRESS' });

  await api(agent, 'PATCH', `/api/tickets/${bill.id}/status`, { status: 'AI_TRIAGED' });
  await api(agent, 'PATCH', `/api/tickets/${bill.id}/status`, { status: 'ASSIGNED' });

  const esc = await api(customer, 'POST', `/api/tickets/${login1.id}/escalations`,
    { reason: 'Locked out of my account for two days and I have a deadline.' });
  await api(agent, 'PATCH', `/api/tickets/${login1.id}/escalations/${esc.id}/approve`);

  await api(customer2, 'POST', `/api/tickets/${how.id}/ai-resolve`);

  return { login1, bill, bug, how, feat };
}

if (import.meta.url === `file://${process.argv[1].replace(/\\/g, '/')}` || process.argv[1]?.endsWith('seed.mjs')) {
  const result = await seed();
  console.log('Seeded', Object.keys(result).length, 'tickets and', ARTICLES.length, 'articles.');
}
