import { chromium } from 'playwright';

const BASE = 'http://localhost:3000';

async function testPage(page, url, name) {
  console.log(`\n--- Testing ${name} at ${url} ---`);
  const response = await page.goto(url, { waitUntil: 'networkidle', timeout: 15000 });
  console.log(`  Status: ${response.status()}`);
  await page.waitForTimeout(2000);

  const h1 = await page.locator('h1').allTextContents();
  console.log(`  H1 elements: ${h1.join(' | ')}`);

  const tabButtons = await page.locator('button').filter({ hasText: /^(Overview|Alerts|DDoS|HTTP|Network|Instances|Asset Discovery|AD Config)$/ }).count();
  console.log(`  Tab buttons found: ${tabButtons}`);

  const errorEl = await page.locator('text=Connection Error').count();
  console.log(`  Connection error: ${errorEl > 0 ? 'yes (expected — backends have no data)' : 'no'}`);

  const screenshotPath = `/tmp/screenshot-${name.toLowerCase().replace(/\s+/g, '-')}.png`;
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`  Screenshot: ${screenshotPath}`);

  return { name, status: response.status(), h1, tabButtons, hasError: errorEl > 0 };
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  const email = `playwright-${Date.now()}@test.com`;
  const password = 'TestPass123!';

  // Register
  console.log('=== Registering ===');
  await page.goto(`${BASE}/register`, { waitUntil: 'networkidle', timeout: 10000 });
  await page.waitForTimeout(1000);

  // Fill all visible inputs by label or placeholder
  // Full name
  const allInputs = await page.locator('input').all();
  for (const input of allInputs) {
    const type = await input.getAttribute('type') || '';
    const placeholder = (await input.getAttribute('placeholder') || '').toLowerCase();
    const name = (await input.getAttribute('name') || '').toLowerCase();

    if (type === 'email' || name === 'email' || placeholder.includes('email')) {
      await input.fill(email);
    } else if (type === 'password') {
      await input.fill(password);
    } else if (name === 'name' || placeholder.includes('name') || placeholder.includes('full')) {
      await input.fill('Playwright Test');
    } else if (name === 'organization' || placeholder.includes('organization') || placeholder.includes('company')) {
      await input.fill('Test Org');
    }
  }

  await page.screenshot({ path: '/tmp/screenshot-register-filled.png' });

  // Submit
  const createBtn = page.locator('button:has-text("Create"), button:has-text("Sign up"), button:has-text("Register"), button[type="submit"]').first();
  await createBtn.click();
  await page.waitForTimeout(3000);
  console.log(`  After register URL: ${page.url()}`);

  // If redirected to login, log in
  if (page.url().includes('/login') || page.url() === `${BASE}/`) {
    if (page.url().includes('/login')) {
      console.log('  Logging in...');
      await page.locator('input[type="email"], input[placeholder*="email" i]').first().fill(email);
      await page.locator('input[type="password"]').first().fill(password);
      await page.locator('button[type="submit"], button:has-text("Sign in")').first().click();
      await page.waitForTimeout(3000);
    }
  }

  console.log(`  Authenticated URL: ${page.url()}`);
  await page.screenshot({ path: '/tmp/screenshot-authenticated.png', fullPage: true });

  // Check navigation
  console.log('\n=== Navigation Check ===');
  await page.goto(BASE, { waitUntil: 'networkidle', timeout: 10000 });
  await page.waitForTimeout(1000);

  const sqsBtn = page.locator('nav button:has-text("SQS")');
  const dtmadBtn = page.locator('nav button:has-text("DTM & AD")');
  console.log(`  SQS nav: ${await sqsBtn.count() > 0 ? 'FOUND' : 'NOT FOUND'}`);
  console.log(`  DTM & AD nav: ${await dtmadBtn.count() > 0 ? 'FOUND' : 'NOT FOUND'}`);
  await page.screenshot({ path: '/tmp/screenshot-nav.png' });

  // Test both dashboards
  const results = [];
  results.push(await testPage(page, `${BASE}/defsec/sqs`, 'SQS'));

  // Click SQS sub-tabs
  for (const tab of ['Alerts', 'DDoS', 'HTTP', 'Network', 'Overview']) {
    const btn = page.locator('button').filter({ hasText: new RegExp(`^${tab}$`) }).first();
    if (await btn.isVisible({ timeout: 1000 }).catch(() => false)) {
      await btn.click();
      await page.waitForTimeout(300);
      console.log(`  -> ${tab} tab OK`);
    }
  }

  results.push(await testPage(page, `${BASE}/defsec/dtmad`, 'DTM-AD'));

  // Click DTM&AD sub-tabs
  for (const tab of ['Alerts', 'Asset Discovery', 'AD Config', 'Instances']) {
    const btn = page.locator('button').filter({ hasText: new RegExp(tab) }).first();
    if (await btn.isVisible({ timeout: 1000 }).catch(() => false)) {
      await btn.click();
      await page.waitForTimeout(300);
      console.log(`  -> ${tab} tab OK`);
    }
  }

  console.log('\n=== RESULTS ===');
  results.forEach(r => {
    const status = r.tabButtons > 0 ? 'PASS' : 'FAIL (no tabs — likely auth redirect)';
    console.log(`  ${r.name}: ${status} (HTTP ${r.status}, tabs=${r.tabButtons})`);
  });

  await browser.close();
})();
