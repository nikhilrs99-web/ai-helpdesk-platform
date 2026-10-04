// Regenerates the README screenshots (docs/images/*.png) from a running local stack.
//
//   1. docker compose up -d --build postgres redis kafka keycloak mailhog ticket-service \
//        kb-service notification-service analytics-service api-gateway
//   2. (cd frontend && npm run dev)            # http://localhost:5173
//   3. cd scripts/screenshots && npm install && npx playwright install chromium
//   4. npm run capture                         # seeds demo data, then takes the screenshots
//
// Local-dev only: signs in as the seeded Keycloak test users from the realm export.
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { seed, credentialsFor } from './seed.mjs';

const APP = process.env.APP_URL ?? 'http://localhost:5173';
const MAILHOG = process.env.MAILHOG_URL ?? 'http://localhost:8025';
const here = path.dirname(fileURLToPath(import.meta.url));
const out = path.join(here, '../../docs/images');
mkdirSync(out, { recursive: true });

const shot = async (page, name, opts = {}) => {
  await page.screenshot({ path: path.join(out, `${name}.png`), ...opts });
  console.log('saved', `${name}.png`);
};

async function signIn(page, username) {
  const { password } = credentialsFor(username);
  await page.goto(APP);
  await page.waitForSelector('input[type=password]');
  await page.fill('input[name=username], #username', username);
  await page.fill('input[type=password]', password);
  await page.keyboard.press('Enter');
  await page.waitForSelector('nav', { timeout: 30000 });
}

const demo = await seed();
console.log('demo data seeded');

const browser = await chromium.launch();
const ctxOpts = { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 };

// --- Keycloak sign-in page (before logging in)
{
  const ctx = await browser.newContext(ctxOpts);
  const page = await ctx.newPage();
  await page.goto(APP);
  await page.waitForSelector('input[type=password]');
  await shot(page, 'login');
  await ctx.close();
}

// --- Agent session
{
  const ctx = await browser.newContext(ctxOpts);
  const page = await ctx.newPage();
  await signIn(page, 'test-agent');

  await page.setViewportSize({ width: 1440, height: 960 }); // fits the whole dashboard; fullPage would resize and restart the chart animation
  await page.waitForSelector('text=Tickets Created');
  await page.waitForSelector('text=Online');
  await page.waitForSelector('.recharts-bar-rectangle');
  await page.waitForTimeout(3500); // let chart animations finish
  await shot(page, 'dashboard');

  await page.goto(`${APP}/tickets`);
  await page.waitForSelector(`text=${demo.login1.subject}`);
  await shot(page, 'tickets');

  await page.goto(`${APP}/tickets/${demo.login1.id}`);
  await page.waitForSelector('text=Escalations');
  await page.waitForSelector('text=APPROVED');
  await page.waitForSelector('text=First-response SLA');
  await shot(page, 'ticket-detail', { fullPage: true });

  await page.goto(`${APP}/tickets/${demo.bill.id}`);
  await page.waitForSelector('text=First-response SLA');
  await page.goto(`${APP}/tickets/new`);
  await page.selectOption('select', 'BUG');
  await page.fill('input[maxlength="200"]', 'Export button shows a blank page');
  await page.fill('textarea', 'Clicking Export on the monthly report opens an empty page.');
  await page.waitForSelector('text=App version');
  await shot(page, 'new-ticket');

  await page.goto(`${APP}/kb`);
  await page.fill('input[placeholder="Search articles..."]', 'password');
  await page.click('button:has-text("Search")');
  await page.waitForSelector('text=How to reset your password');
  await shot(page, 'knowledge-base');

  await ctx.close();
}

// --- Customer session: only their own tickets, no agent controls
{
  const ctx = await browser.newContext(ctxOpts);
  const page = await ctx.newPage();
  await signIn(page, 'test-customer');
  await page.goto(`${APP}/tickets`);
  await page.waitForSelector(`text=${demo.login1.subject}`);
  await shot(page, 'customer-tickets');
  await ctx.close();
}

// --- Emails the platform sent (MailHog)
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 800 }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  await page.goto(MAILHOG);
  await page.waitForSelector('text=[Helpdesk]');
  await shot(page, 'emails');
  await ctx.close();
}

await browser.close();
console.log('done ->', out);
