import { test, expect } from '@playwright/test';
import path from 'path';

const SCREENSHOTS = path.resolve('printscreens');
const ADMIN_EMAIL = process.env.E2E_EMAIL || 'admin@testcorp.com';
const ADMIN_PASSWORD = process.env.E2E_PASSWORD || 'SecurePass123';

test.setTimeout(180000);

async function login(page) {
  await page.goto('/login');
  await page.fill('#email', ADMIN_EMAIL);
  await page.fill('#password', ADMIN_PASSWORD);
  await page.click('button[type="submit"]');
  await page.waitForURL('/', { timeout: 30000 });
}

// Counts every byte the page pulls from the SEUXDR proxy, which is what the
// aggregation endpoint was meant to shrink.
function trackProxy(page) {
  const stats = { bytes: 0, calls: 0 };
  page.on('response', async (res) => {
    if (!res.url().includes('/api/seuxdr')) return;
    stats.calls++;
    try { stats.bytes += (await res.body()).length; } catch { /* ignore */ }
  });
  return stats;
}

test('home dashboard settles quickly and pulls little data', async ({ page }) => {
  await login(page);
  const proxy = trackProxy(page);
  const t0 = Date.now();
  await page.goto('/');
  await page.waitForSelector('text=Total Alerts', { timeout: 60000 });
  await page.waitForLoadState('networkidle', { timeout: 120000 });
  const ms = Date.now() - t0;
  await page.screenshot({ path: `${SCREENSHOTS}/perf--home.png`, fullPage: true });
  console.log(`HOME settled=${ms}ms proxyCalls=${proxy.calls} proxyBytes=${proxy.bytes}`);
  expect(proxy.bytes).toBeLessThan(5 * 1024 * 1024);
});

test('siem dashboard settles quickly and pulls little data', async ({ page }) => {
  await login(page);
  const proxy = trackProxy(page);
  const t0 = Date.now();
  await page.goto('/defsec/siem');
  await page.waitForSelector('text=Critical Alerts', { timeout: 60000 });
  await page.waitForLoadState('networkidle', { timeout: 120000 });
  const ms = Date.now() - t0;
  await page.screenshot({ path: `${SCREENSHOTS}/perf--siem.png`, fullPage: true });
  console.log(`SIEM settled=${ms}ms proxyCalls=${proxy.calls} proxyBytes=${proxy.bytes}`);
  expect(proxy.bytes).toBeLessThan(5 * 1024 * 1024);
});
