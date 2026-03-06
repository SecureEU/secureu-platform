import { chromium } from 'playwright';

const BASE = 'http://localhost:3000';
const DIR = '/tmp/dtmad-screenshots';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // Collect console errors throughout
  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });

  const email = `dtmad-test-${Date.now()}@test.com`;
  const password = 'TestPass123!';

  // --- Register ---
  console.log('=== Registering ===');
  await page.goto(`${BASE}/register`, { waitUntil: 'networkidle', timeout: 15000 });
  await page.waitForTimeout(1000);

  // Fill registration form using explicit name selectors
  await page.fill('input[name="name"]', 'DTMAD Test User');
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="organization"]', 'Test Org');
  await page.fill('input[name="password"]', password);
  await page.fill('input[name="confirmPassword"]', password);
  await page.waitForTimeout(500);

  await page.screenshot({ path: `${DIR}/00-register-filled.png`, fullPage: true });

  // Click "Create account" button
  await page.click('button:has-text("Create account")');
  await page.waitForTimeout(4000);
  console.log(`  After register URL: ${page.url()}`);
  await page.screenshot({ path: `${DIR}/00-after-register.png`, fullPage: true });

  // --- Login if needed ---
  if (page.url().includes('/login') || page.url().includes('/register')) {
    console.log('  Navigating to login...');
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 15000 });
    await page.waitForTimeout(1000);

    await page.fill('input[name="email"], input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button:has-text("Sign in")');
    await page.waitForTimeout(4000);
    console.log(`  After login URL: ${page.url()}`);
  }

  // --- Navigate to DTMAD ---
  console.log('\n=== DTMAD Dashboard ===');
  console.log('1. Loading DTMAD page (Live Traffic tab)...');
  await page.goto(`${BASE}/defsec/dtmad`, { waitUntil: 'load', timeout: 30000 });
  await page.waitForTimeout(6000);

  // Check if we got redirected to login
  if (page.url().includes('/login')) {
    console.log('   ERROR: Redirected to login! Auth failed.');
    await page.screenshot({ path: `${DIR}/01-auth-failed.png`, fullPage: true });
    await browser.close();
    process.exit(1);
  }

  await page.screenshot({ path: `${DIR}/01-traffic-tab.png`, fullPage: true });
  console.log('   Screenshot: 01-traffic-tab.png');

  // Check for connection error banner
  const errorBanner = await page.$('.bg-red-50');
  if (errorBanner) {
    const errorText = await errorBanner.textContent();
    console.log('   WARNING: Error banner:', errorText.substring(0, 150));
  }

  // Check for stat cards on traffic tab
  const statCards = await page.$$('.text-2xl.font-bold');
  console.log(`   Stat cards found: ${statCards.length}`);

  // 2. Click Alerts tab
  console.log('2. Clicking Alerts tab...');
  const alertsBtn = page.locator('button').filter({ hasText: /^Alerts$/ }).first();
  // Use locator with exact text match to avoid ambiguity
  await alertsBtn.click({ timeout: 5000 });
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${DIR}/02-alerts-tab.png`, fullPage: true });
  console.log('   Screenshot: 02-alerts-tab.png');

  // Check alert count
  const alertRows = await page.$$('tbody tr');
  console.log(`   Alert rows: ${alertRows.length}`);

  // 3. Expand first alert row
  const firstAlertRow = await page.$('tbody tr.cursor-pointer');
  if (firstAlertRow) {
    console.log('3. Expanding first alert...');
    await firstAlertRow.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${DIR}/03-alert-expanded.png`, fullPage: true });
    console.log('   Screenshot: 03-alert-expanded.png');
  } else {
    console.log('3. No alert rows to expand');
  }

  // 4. Click Instances tab
  console.log('4. Clicking Instances tab...');
  await page.locator('button').filter({ hasText: /^Instances$/ }).first().click({ timeout: 5000 });
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${DIR}/04-instances-tab.png`, fullPage: true });
  console.log('   Screenshot: 04-instances-tab.png');

  // 5. Expand first instance
  const firstInstance = await page.$('.cursor-pointer');
  if (firstInstance) {
    console.log('5. Expanding first instance...');
    await firstInstance.click();
    await page.waitForTimeout(1500);
    await page.screenshot({ path: `${DIR}/05-instance-expanded.png`, fullPage: true });
    console.log('   Screenshot: 05-instance-expanded.png');
  } else {
    console.log('5. No instances to expand');
  }

  // 6. Click Asset Discovery tab
  console.log('6. Clicking Asset Discovery tab...');
  await page.locator('button').filter({ hasText: /Asset Discovery/ }).first().click({ timeout: 5000 });
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${DIR}/06-assets-tab.png`, fullPage: true });
  console.log('   Screenshot: 06-assets-tab.png');

  // 7. Click AD Config tab
  console.log('7. Clicking AD Config tab...');
  await page.locator('button').filter({ hasText: /AD Config/ }).first().click({ timeout: 5000 });
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${DIR}/07-adconfig-tab.png`, fullPage: true });
  console.log('   Screenshot: 07-adconfig-tab.png');

  // --- Summary ---
  console.log('\n=== Summary ===');
  if (consoleErrors.length > 0) {
    console.log(`Console errors detected (${consoleErrors.length}):`);
    // Dedupe
    const unique = [...new Set(consoleErrors)];
    unique.forEach(e => console.log('  -', e.substring(0, 120)));
  } else {
    console.log('No console errors detected.');
  }

  await browser.close();
  console.log(`\nDone. Screenshots saved to ${DIR}`);
})();
