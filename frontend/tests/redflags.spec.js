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

test.describe('Red Flags Dashboard', () => {

  test('1. Navigate to Red Flags page', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('h1:has-text("Red Flags Dashboard")', { timeout: 10000 });
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--landing.png`, fullPage: true });
    await expect(page.locator('h1:has-text("Red Flags Dashboard")')).toBeVisible();
  });

  test('2. Analyzed Logs tab - filters and controls visible', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    // Click "Analyzed Logs" tab
    await page.click('button:has-text("Analyzed Logs")');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analyzed-logs-tab.png`, fullPage: true });

    // Verify filter controls exist
    await expect(page.locator('input[placeholder="Search logs..."]')).toBeVisible();
    await expect(page.locator('select').first()).toBeVisible();

    // Verify severity dropdown has options
    const severitySelect = page.locator('select').nth(1);
    await expect(severitySelect).toBeVisible();

    // Verify auto-refresh toggle exists
    const refreshBtn = page.locator('button').filter({ has: page.locator('svg') }).last();
    await expect(refreshBtn).toBeVisible();
  });

  test('3. Analyzed Logs tab - Apply button fetches logs', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Analyzed Logs")');
    await page.waitForTimeout(500);

    // Click Apply to fetch logs
    await page.click('button:has-text("Apply")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analyzed-logs-applied.png`, fullPage: true });

    // Should show either logs or "No logs found" message
    const hasLogs = await page.locator('.space-y-2 > div').count() > 0;
    const hasEmpty = await page.locator('text=No logs found').count() > 0;
    expect(hasLogs || hasEmpty).toBeTruthy();
  });

  test('4. Analyzed Logs tab - severity filter', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Analyzed Logs")');
    await page.waitForTimeout(500);

    // Select CRITICAL severity
    await page.selectOption('select:has(option[value="CRITICAL"])', 'CRITICAL');
    await page.click('button:has-text("Apply")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--severity-critical.png`, fullPage: true });
  });

  test('5. Pre-analysis Logs tab - layout and filters', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    // Click "Pre-analysis Logs" tab
    await page.click('button:has-text("Pre-analysis Logs")');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--preanalysis-tab.png`, fullPage: true });

    // Verify filter sidebar
    await expect(page.locator('text=Filters').first()).toBeVisible();
    await expect(page.locator('text=Number of recent logs')).toBeVisible();
    await expect(page.locator('text=Log Type')).toBeVisible();
    await expect(page.locator('text=Source Host')).toBeVisible();

    // Verify Apply Filters and Export buttons
    await expect(page.locator('button:has-text("Apply Filters")')).toBeVisible();
    await expect(page.locator('button:has-text("Export JSON")')).toBeVisible();
  });

  test('6. Pre-analysis Logs tab - fetch raw logs', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Pre-analysis Logs")');
    await page.waitForTimeout(500);

    // Click Apply Filters
    await page.click('button:has-text("Apply Filters")');
    await page.waitForTimeout(3000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--raw-logs-loaded.png`, fullPage: true });

    // Should show raw log entries or empty state
    const hasRawLogs = await page.locator('.font-mono').count() > 0;
    const hasEmpty = await page.locator('text=No logs loaded').count() > 0;
    expect(hasRawLogs || hasEmpty).toBeTruthy();
  });

  test('7. Pre-analysis Logs tab - view modes (table/json)', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Pre-analysis Logs")');
    await page.waitForTimeout(500);

    // Fetch some logs first
    await page.click('button:has-text("Apply Filters")');
    await page.waitForTimeout(3000);

    // Switch to JSON view
    await page.click('button:has-text("{ }")');
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--raw-logs-json-view.png`, fullPage: true });

    // Verify JSON view has a pre element
    const hasJsonView = await page.locator('pre').count() > 0;
    expect(hasJsonView).toBeTruthy();

    // Switch back to table view
    const tableBtn = page.locator('button').filter({ has: page.locator('svg.h-4.w-4') });
    await tableBtn.first().click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--raw-logs-table-view.png`, fullPage: true });
  });

  test('8. Pre-analysis Logs tab - expand a log entry', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Pre-analysis Logs")');
    await page.waitForTimeout(500);

    await page.click('button:has-text("Apply Filters")');
    await page.waitForTimeout(3000);

    // Click on first log entry to expand it
    const firstEntry = page.locator('.border.border-gray-200.rounded-lg.p-3.cursor-pointer').first();
    if (await firstEntry.count() > 0) {
      await firstEntry.click();
      await page.waitForTimeout(500);
      await page.screenshot({ path: `${SCREENSHOTS}/redflags--raw-log-expanded.png`, fullPage: true });
    }
  });

  test('9. Analytics tab - charts and stats', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    // Click "Analytics" tab
    await page.click('button:has-text("Analytics")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analytics-tab.png`, fullPage: true });

    // Verify time range controls
    await expect(page.locator('text=Time Range:')).toBeVisible();
    await expect(page.locator('text=Quick select:')).toBeVisible();

    // Verify stat cards exist
    await expect(page.locator('text=Total Incidents')).toBeVisible();

    // Verify chart sections exist
    await expect(page.locator('text=Incidents by Severity')).toBeVisible();
    await expect(page.locator('text=Incidents by Log Type')).toBeVisible();
  });

  test('10. Analytics tab - time range quick select buttons', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Analytics")');
    await page.waitForTimeout(2000);

    // Click 7d quick select
    await page.click('button:has-text("7d")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analytics-7d.png`, fullPage: true });

    // Click 30d quick select
    await page.click('button:has-text("30d")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analytics-30d.png`, fullPage: true });

    // Click 1y quick select
    await page.click('button:has-text("1y")');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--analytics-1y.png`, fullPage: true });
  });

  test('11. Analyzed Logs tab - auto-refresh toggle', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Analyzed Logs")');
    await page.waitForTimeout(500);

    // Verify starts as "Paused"
    await expect(page.locator('text=Paused')).toBeVisible();
    await page.screenshot({ path: `${SCREENSHOTS}/redflags--autorefresh-paused.png`, fullPage: true });

    // Click the play button to enable auto-refresh
    const playBtn = page.locator('button').filter({ has: page.locator('svg') }).filter({ hasText: '' });
    // Find the green/red toggle button near the auto-refresh indicator
    const toggleBtn = page.locator('.bg-green-500, .bg-red-500').first();
    if (await toggleBtn.count() > 0) {
      await toggleBtn.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: `${SCREENSHOTS}/redflags--autorefresh-live.png`, fullPage: true });

      // Verify "Live" indicator
      const isLive = await page.locator('text=Live').count() > 0;
      expect(isLive).toBeTruthy();

      // Stop auto-refresh
      await toggleBtn.click();
      await page.waitForTimeout(500);
    }
  });

  test('12. Analyzed Logs tab - sidebar opens on log click', async ({ page }) => {
    await login(page);
    await page.goto('/cti/redflags');
    await page.waitForLoadState('networkidle');

    await page.click('button:has-text("Analyzed Logs")');
    await page.waitForTimeout(500);

    // Fetch logs
    await page.click('button:has-text("Apply")');
    await page.waitForTimeout(2000);

    // If there are log entries, click one to open sidebar
    const logEntry = page.locator('.rounded-lg.border.cursor-pointer').first();
    if (await logEntry.count() > 0) {
      await logEntry.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: `${SCREENSHOTS}/redflags--incident-sidebar.png`, fullPage: true });

      // Verify sidebar content
      await expect(page.locator('text=Incident Details')).toBeVisible();
      await expect(page.locator('text=Basic Information')).toBeVisible();
      await expect(page.locator('text=Raw Log Message')).toBeVisible();
      await expect(page.locator('text=Full JSON Data')).toBeVisible();

      // Close sidebar
      const closeBtn = page.locator('button').filter({ has: page.locator('svg.h-6.w-6') });
      await closeBtn.first().click();
      await page.waitForTimeout(500);
      await page.screenshot({ path: `${SCREENSHOTS}/redflags--sidebar-closed.png`, fullPage: true });
    } else {
      // No logs available - take screenshot of empty state
      await page.screenshot({ path: `${SCREENSHOTS}/redflags--no-logs-for-sidebar.png`, fullPage: true });
    }
  });
});
