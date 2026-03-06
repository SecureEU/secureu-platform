import { test, expect } from '@playwright/test';
import path from 'path';

const SCREENSHOTS = path.resolve('printscreens');

// Test data
const COMPANY_NAME = 'TestCorp Security';
const ADMIN_NAME = 'Admin User';
const ADMIN_EMAIL = 'admin@testcorp.com';
const ADMIN_PASSWORD = 'SecurePass123';

test.describe('Setup Wizard & Settings', () => {
  test('1. Home page shows setup button when no workspace exists', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    // Wait for setup status to resolve and button to appear
    await page.waitForSelector('a[href="/setup"]', { timeout: 10000 });
    await page.screenshot({ path: `${SCREENSHOTS}/home--setup-button.png`, fullPage: true });
    await expect(page.locator('a[href="/setup"]').first()).toBeVisible();
  });

  test('2. Setup wizard - step 1: company name', async ({ page }) => {
    await page.goto('/setup');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${SCREENSHOTS}/setup--step1-empty.png`, fullPage: true });

    // Verify step 1 elements
    await expect(page.locator('text=Welcome to SECUR-EU')).toBeVisible();
    await expect(page.locator('#companyName')).toBeVisible();

    // Try to continue with empty name
    await page.click('button:has-text("Continue")');
    await page.screenshot({ path: `${SCREENSHOTS}/setup--step1-validation-error.png`, fullPage: true });

    // Fill company name
    await page.fill('#companyName', COMPANY_NAME);
    await page.screenshot({ path: `${SCREENSHOTS}/setup--step1-filled.png`, fullPage: true });

    // Continue to step 2
    await page.click('button:has-text("Continue")');
    await page.waitForSelector('text=Create admin account');
    await page.screenshot({ path: `${SCREENSHOTS}/setup--step2-empty.png`, fullPage: true });
  });

  test('3. Setup wizard - step 2: admin account creation', async ({ page }) => {
    await page.goto('/setup');
    await page.waitForLoadState('networkidle');

    // Go through step 1
    await page.fill('#companyName', COMPANY_NAME);
    await page.click('button:has-text("Continue")');
    await page.waitForSelector('text=Create admin account');

    // Verify company name is shown
    await expect(page.getByText(COMPANY_NAME)).toBeVisible();

    // Fill admin account
    await page.fill('#name', ADMIN_NAME);
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.fill('#confirmPassword', ADMIN_PASSWORD);
    await page.screenshot({ path: `${SCREENSHOTS}/setup--step2-filled.png`, fullPage: true });

    // Submit
    await page.click('button:has-text("Complete Setup")');

    // Should redirect to home (full page reload via window.location.href)
    await page.waitForURL('/', { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    // Wait for the auth state to hydrate — should show the dashboard, not the landing page
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENSHOTS}/setup--complete-dashboard.png`, fullPage: true });

    // Should be authenticated — verify we see the dashboard, not the landing page
    await expect(page.locator('text=Security Overview')).toBeVisible({ timeout: 10000 });
  });

  test('4. Setup wizard rejects second setup attempt', async ({ page }) => {
    await page.goto('/setup');
    // Should redirect away since setup is already done
    await page.waitForURL('/', { timeout: 15000 });
    await page.screenshot({ path: `${SCREENSHOTS}/setup--already-done-redirect.png`, fullPage: true });
  });

  test('5. Login as admin', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${SCREENSHOTS}/login--empty.png`, fullPage: true });

    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.screenshot({ path: `${SCREENSHOTS}/login--filled.png`, fullPage: true });

    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${SCREENSHOTS}/login--success-dashboard.png`, fullPage: true });
  });

  test('6. Settings page - admin can access', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // Navigate to settings
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('h1:has-text("Org Settings")');
    await page.screenshot({ path: `${SCREENSHOTS}/settings--default.png`, fullPage: true });

    // Verify company name is shown
    await expect(page.getByText(COMPANY_NAME).first()).toBeVisible();

    // Verify the admin user email is listed
    await expect(page.getByText(ADMIN_EMAIL)).toBeVisible();
  });

  test('7. Settings page - edit company name', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });

    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('text=Company Information');

    // Click the edit button (pencil icon next to company name display)
    const editBtn = page.locator('div.bg-gray-50.rounded-xl').filter({ hasText: 'TestCorp Security' }).locator('button');
    await editBtn.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREENSHOTS}/settings--editing-name.png`, fullPage: true });

    // Change name — after clicking edit, an input appears
    const nameInput = page.locator('input[type="text"]').first();
    await nameInput.clear();
    await nameInput.fill('TestCorp Security Updated');
    await page.click('button:has-text("Save")');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${SCREENSHOTS}/settings--name-updated.png`, fullPage: true });

    // Verify updated name
    await expect(page.getByText('TestCorp Security Updated').first()).toBeVisible();
  });

  test('8. Settings link in nav - admin only', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // Click the user menu button (the one with the user's initial and name in the top right)
    const userMenuButton = page.locator('button').filter({ hasText: ADMIN_NAME }).first();
    await userMenuButton.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREENSHOTS}/nav--user-dropdown-admin.png`, fullPage: true });

    // Settings link should be visible
    await expect(page.locator('a[href="/settings"]').first()).toBeVisible();
  });

  test('9. Register page - no organization field', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${SCREENSHOTS}/register--no-org-field.png`, fullPage: true });

    // Organization field should NOT exist
    await expect(page.locator('#organization')).not.toBeVisible();
    await expect(page.locator('label:has-text("Organization name")')).not.toBeVisible();

    // But other fields should exist
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('10. Register as normal user and verify no Settings link', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');

    await page.fill('#name', 'Test Regular User');
    await page.fill('#email', 'testregular@testcorp.com');
    await page.fill('#password', 'SecurePass456');
    await page.fill('#confirmPassword', 'SecurePass456');
    await page.click('button[type="submit"]');

    // Registration logs out and redirects to login
    await page.waitForURL('**/login', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // Login as the new user
    await page.fill('#email', 'testregular@testcorp.com');
    await page.fill('#password', 'SecurePass456');
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${SCREENSHOTS}/dashboard--regular-user.png`, fullPage: true });

    // Click the user menu button
    const userMenuButton = page.locator('button').filter({ hasText: 'Test Regular User' }).first();
    await userMenuButton.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREENSHOTS}/nav--user-dropdown-regular-user.png`, fullPage: true });

    // Settings link should NOT be visible for regular user
    await expect(page.locator('a[href="/settings"]')).not.toBeVisible();
  });

  test('11. Profile page - no organization block', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });

    await page.goto('/profile');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('text=Profile Settings');
    await page.screenshot({ path: `${SCREENSHOTS}/profile--no-org-block.png`, fullPage: true });

    // Organization block should NOT exist
    await expect(page.locator('text=Member of an organization')).not.toBeVisible();
  });

  test('12. Settings page - user management (change role, delete)', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('#email', ADMIN_EMAIL);
    await page.fill('#password', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/', { timeout: 15000 });

    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('text=User Management');
    await page.screenshot({ path: `${SCREENSHOTS}/settings--user-management.png`, fullPage: true });

    // Find the test regular user row by email (use bg-gray-50 to target inner row, not outer section)
    const regularUserRow = page.locator('div.bg-gray-50.rounded-xl').filter({ hasText: 'testregular@testcorp.com' });
    await expect(regularUserRow).toBeVisible();

    // Change regular user's role to admin
    const roleSelect = regularUserRow.locator('select');
    await roleSelect.selectOption('admin');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${SCREENSHOTS}/settings--role-changed.png`, fullPage: true });

    // Change back to user
    await roleSelect.selectOption('user');
    await page.waitForTimeout(1000);

    // Delete the regular user
    page.on('dialog', dialog => dialog.accept());
    const deleteButton = regularUserRow.locator('button');
    await deleteButton.click();
    await page.waitForTimeout(1000);
    await page.screenshot({ path: `${SCREENSHOTS}/settings--user-deleted.png`, fullPage: true });

    // Regular user should be gone
    await expect(page.getByText('testregular@testcorp.com')).not.toBeVisible();
  });
});
