import { test, expect } from '@playwright/test';
import path from 'path';

const SCREENSHOTS = path.resolve('printscreens');

const ADMIN_EMAIL = 'admin@testcorp.com';
const ADMIN_PASSWORD = 'SecurePass123';

async function login(page) {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');
  await page.fill('#email', ADMIN_EMAIL);
  await page.fill('#password', ADMIN_PASSWORD);
  await page.click('button[type="submit"]');
  await page.waitForURL('/', { timeout: 15000 });
  await page.waitForLoadState('networkidle');
}

test.describe('SIEM Dashboard (SEUXDR)', () => {

  test('1. Navigate to Defensive Solutions page', async ({ page }) => {
    await login(page);
    await page.goto('/defsec');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('h1:has-text("Defensive Solutions")', { timeout: 10000 });
    await page.screenshot({ path: `${SCREENSHOTS}/siem--defsec-landing.png`, fullPage: true });
    await expect(page.locator('h1:has-text("Defensive Solutions")')).toBeVisible();
  });

  test('2. Defensive Solutions shows SIEM Dashboard as active tool', async ({ page }) => {
    await login(page);
    await page.goto('/defsec');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('text=Active Tools')).toBeVisible();
    await expect(page.locator('text=SIEM Dashboard')).toBeVisible();
    await expect(page.locator('a[href="/defsec/siem"]')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--active-tool-card.png`, fullPage: true });
  });

  test('3. Navigate to SIEM Dashboard page', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('h1:has-text("SIEM Dashboard")', { timeout: 10000 });
    await page.screenshot({ path: `${SCREENSHOTS}/siem--dashboard-page.png`, fullPage: true });
    await expect(page.locator('h1:has-text("SIEM Dashboard")')).toBeVisible();
  });

  test('4. SIEM page shows SEUXDR manager status', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    const statusBanner = page.locator('text=SEUXDR Manager is running').or(page.locator('text=SEUXDR Manager is not reachable'));
    await expect(statusBanner).toBeVisible({ timeout: 10000 });
    await page.screenshot({ path: `${SCREENSHOTS}/siem--manager-status.png`, fullPage: true });
  });

  test('5. SIEM page has navigation tabs', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');

    await expect(page.getByRole('button', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Alerts' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Agents' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Organizations' })).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--tabs.png`, fullPage: true });
  });

  test('6. Dashboard tab shows stats cards', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Total Alerts')).toBeVisible();
    await expect(page.locator('text=Critical Alerts')).toBeVisible();
    await expect(page.locator('text=Active Agents')).toBeVisible();
    await expect(page.getByText('Organizations', { exact: true }).first()).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--stats-cards.png`, fullPage: true });
  });

  test('7. Alerts tab shows alerts table', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: 'Alerts' }).click();
    await expect(page.locator('text=Security Alerts')).toBeVisible();
    await expect(page.locator('input[placeholder="Search alerts..."]')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--alerts-tab.png`, fullPage: true });
  });

  test('8. Agents tab shows agents table', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: 'Agents' }).click();
    await expect(page.locator('text=Security Agents')).toBeVisible();
    await expect(page.locator('button:has-text("Generate Agent")')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--agents-tab.png`, fullPage: true });
  });

  test('9. Organizations tab shows org view with create button', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: 'Organizations' }).click();
    await expect(page.locator('button:has-text("New Organization")')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--orgs-tab.png`, fullPage: true });
  });

  test('10. SEUXDR proxy API returns status', async ({ page }) => {
    await login(page);
    const response = await page.request.get('/api/seuxdr?endpoint=status');
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.status === 'ok' || data.message === 'Ok').toBeTruthy();
  });

  test('11. Refresh button works', async ({ page }) => {
    await login(page);
    await page.goto('/defsec/siem');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    const refreshButton = page.locator('button:has-text("Refresh")');
    await expect(refreshButton).toBeVisible();
    await refreshButton.click();
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/siem--after-refresh.png`, fullPage: true });
  });

  test('12. Navigate from Defensive Solutions to SIEM via card click', async ({ page }) => {
    await login(page);
    await page.goto('/defsec');
    await page.waitForLoadState('networkidle');

    await page.click('a[href="/defsec/siem"]');
    await page.waitForURL('/defsec/siem', { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await expect(page.locator('h1:has-text("SIEM Dashboard")')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/siem--navigated-from-defsec.png`, fullPage: true });
  });
});
