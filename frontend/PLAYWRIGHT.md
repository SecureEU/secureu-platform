# PLAYWRIGHT.md — Full Application Testing & Auto-Fix Instructions

> **To Claude Code**: Read this file completely before starting. Follow every phase in order. Do NOT skip any step.

---

## App Info

- **App URL**: `http://localhost:3000`
- **Screenshots Folder**: `./printscreens/` (create if it doesn't exist)
- **The app is already running.** Do NOT try to start the dev server.

---

## General Rules

- Save ALL screenshots to `./printscreens/` with clear, descriptive filenames
- Use the format: `page-name--element--state.png` (e.g., `dashboard--sidebar-open.png`, `settings--profile-tab--filled-form.png`)
- After taking screenshots, ALWAYS open and look at them to visually verify what's on screen
- If something looks broken, wrong, or off in a screenshot — fix it in the source code immediately
- After every fix, re-test and re-screenshot to confirm the fix works
- Be EXHAUSTIVE — test every single page, tab, subtab, button, link, dropdown, modal, form, and interactive element
- Use proper waits (`waitForSelector`, `waitForLoadState`, `waitForTimeout`) — never assume instant loads
- Prefer `data-testid` selectors → then accessible roles/labels → then text content → last resort: CSS selectors
- Never use fragile selectors like `nth-child` or deeply nested CSS paths

---

## Phase 1: Setup

1. Install Playwright if not already installed: `npm init playwright@latest` (accept defaults, skip if already set up)
2. Install `@axe-core/playwright` for accessibility checks: `npm install -D @axe-core/playwright`
3. Create the `./printscreens/` folder if it doesn't exist
4. Clear any old screenshots from `./printscreens/` before starting a fresh run
5. Configure `playwright.config.ts` with:
   - Base URL: `http://localhost:3000`
   - Screenshots on every failure
   - Timeout: 30 seconds per test
   - Start with Chromium only
   - Video recording off (screenshots are enough)
   - `webServer` section should be **removed or commented out** — the app is already running

---

## Phase 2: App Discovery & Route Mapping

1. Navigate to the app root URL
2. Screenshot the landing/home page → `printscreens/home--default.png`
3. Crawl the entire app and build a complete map of:
   - Every route/page
   - Every navigation item (top nav, sidebar, footer links)
   - Every tab and subtab
   - Every dropdown menu
   - Every button and clickable element
   - Every form
   - Every modal/dialog trigger
   - Every accordion/collapsible section
   - Every toggle, switch, checkbox, radio button
   - Every search bar, filter, and sort control
   - Every pagination control
   - Every context menu or hover-triggered element
4. Save this map as `printscreens/_APP_MAP.md` — a markdown file listing every discovered element organized by page

---

## Phase 3: Systematic Screenshot Testing

Go through EVERY page and EVERY element found in Phase 2. For each:

### Pages & Navigation
- Screenshot each page in its default state
- Click every nav item → screenshot the resulting page
- Click every tab → screenshot the tab content
- Click every subtab → screenshot the subtab content
- Click every sidebar link → screenshot

### Interactive Elements
- Click every button → screenshot the result (modal opened? action happened? page changed?)
- Open every dropdown → screenshot it in open state → click each option → screenshot the result
- Open every modal/dialog → screenshot it → interact with its contents → screenshot → close it
- Expand every accordion → screenshot expanded state
- Toggle every switch/toggle → screenshot both ON and OFF states
- Check/uncheck every checkbox → screenshot both states
- Select every radio option → screenshot

### Forms
- Screenshot each form empty
- Fill every form with valid test data → screenshot filled state
- Submit the form → screenshot success state
- Clear and fill with INVALID data → submit → screenshot validation errors
- Test required field validation (submit empty) → screenshot error messages

### States
- Empty states (pages with no data) → screenshot
- Loading states (if visible) → screenshot
- Error states (trigger errors if possible) → screenshot
- Hover states on buttons, links, cards → screenshot

### Responsive
- Screenshot every page at these viewports:
  - Desktop: `1920x1080`
  - Laptop: `1366x768`
  - Tablet: `768x1024`
  - Mobile: `375x667`
- Save as: `pagename--desktop.png`, `pagename--tablet.png`, `pagename--mobile.png`

---

## Phase 4: Workflow Testing

Test these full end-to-end user workflows. Screenshot EVERY step.

### Authentication (if applicable)
- Sign up with new test user → screenshot each step
- Log out → screenshot
- Log in with test user → screenshot
- Try invalid credentials → screenshot error
- Password reset flow (if exists) → screenshot

### Navigation
- Click through every link in the app — verify no broken links (no 404s, no dead ends)
- Use browser back/forward → verify correct behavior
- Screenshot any broken navigation

### CRUD Operations (for every entity/resource in the app)
- **Create**: Fill form → submit → screenshot the new item
- **Read**: View the item/detail page → screenshot
- **Update**: Edit the item → save → screenshot updated version
- **Delete**: Delete the item → confirm → screenshot that it's gone

### Search & Filters
- Use every search bar → type a query → screenshot results
- Apply every filter → screenshot filtered results
- Combine filters → screenshot
- Clear filters → screenshot reset state
- Search for something with no results → screenshot empty results

### Pagination
- Navigate to page 2, 3, etc. → screenshot each
- Go to last page → screenshot
- Go back to first page → screenshot

---

## Phase 5: Health Checks

For EVERY page, check and log:

- [ ] **Console errors**: Capture all `console.error` and `console.warn` messages → save to `printscreens/_CONSOLE_ERRORS.md`
- [ ] **Broken images**: Verify every `<img>` loads (no broken image icons)
- [ ] **Broken links**: Verify every `<a href>` returns a 200 status
- [ ] **JavaScript exceptions**: Catch and log any uncaught exceptions
- [ ] **Page load time**: Log how long each page takes to load → flag anything over 3 seconds
- [ ] **Accessibility**: Run axe-core on every page → save violations to `printscreens/_ACCESSIBILITY_REPORT.md`
- [ ] **Layout issues**: Look at screenshots for overflow, misalignment, overlapping elements, cut-off text

---

## Phase 6: Bug Detection & Auto-Fix

When you find ANY issue:

### 1. Document It
- Screenshot the bug
- Note the exact page, element, and steps to reproduce
- Save to `printscreens/_BUGS_FOUND.md` with:
  ```
  ## Bug: [Short description]
  - **Page**: /route
  - **Element**: button/form/link/etc.
  - **Severity**: Critical / High / Medium / Low
  - **Screenshot**: bug--pagename--description.png
  - **Steps to reproduce**: ...
  - **Console errors**: (if any)
  ```

### 2. Fix It
- Go to the source code and fix the issue
- Common fixes include:
  - Broken routes/links → fix href/route paths
  - Console errors → fix the underlying JS/TS issue
  - Form validation bugs → fix validation logic
  - UI/layout bugs → fix CSS/styling
  - Broken API calls → fix endpoint URLs, error handling
  - Missing error states → add proper error handling UI
  - Accessibility violations → add aria labels, fix contrast, add alt text

### 3. Re-Test
- After fixing, re-run the specific test
- Take a new screenshot to confirm the fix
- Save as: `fix--pagename--description--after.png`

### 4. Document the Fix
- Update `printscreens/_BUGS_FOUND.md` with:
  ```
  - **Status**: FIXED
  - **Fix**: [What you changed and in which file]
  - **Screenshot after fix**: fix--pagename--description--after.png
  ```

---

## Phase 7: Final Regression Run

After ALL fixes are applied:

1. Run the ENTIRE test suite one final time from scratch
2. Take fresh screenshots of everything
3. Verify zero console errors
4. Verify all workflows still work
5. Confirm no new bugs were introduced by fixes

---

## Phase 8: Generate Report

Create `printscreens/_FINAL_REPORT.md` with:

```markdown
# App Test Report — [Date]

## Summary
- Total pages tested: X
- Total screenshots taken: X
- Total interactive elements tested: X
- Total workflows tested: X
- Total bugs found: X
- Total bugs fixed: X
- Remaining issues: X

## Pages Tested
| Page | Route | Status | Screenshots |
|------|-------|--------|-------------|
| Home | / | ✅ Pass | home--default.png |
| ... | ... | ... | ... |

## Bugs Found & Fixed
| # | Description | Severity | Status | Fix |
|---|-------------|----------|--------|-----|
| 1 | ... | High | FIXED | ... |
| ... | ... | ... | ... | ... |

## Console Errors
(list all or "None found")

## Accessibility Issues
(summary from axe-core report)

## Performance
| Page | Load Time | Status |
|------|-----------|--------|
| / | 1.2s | ✅ |
| ... | ... | ... |

## Responsive Design
| Page | Desktop | Tablet | Mobile |
|------|---------|--------|--------|
| / | ✅ | ✅ | ⚠️ overflow |
| ... | ... | ... | ... |

## Overall Health Score: X/100
```

---

## IMPORTANT REMINDERS

- **The app is already running at `http://localhost:3000`** — do not start it
- **ALL screenshots go in `./printscreens/`** — create the folder first
- **LOOK at every screenshot you take** — use them to visually verify the app
- **If ANYTHING looks wrong in a screenshot — FIX IT immediately**
- **Be exhaustive** — every page, every tab, every button, every state
- **After all fixes, do a full regression run**
- **Generate the final report when done**

Start now. Begin with Phase 1.

