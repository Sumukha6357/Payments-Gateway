import { test, expect } from "@playwright/test";

test("login page renders", async ({ page }) => {
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await expect(page.getByText("Payment Gateway Portal Login")).toBeVisible({ timeout: 30000 });
});

test("dashboard renders with mocked token mode", async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("pg.portal.token", "mock-token");
    window.localStorage.setItem("pg.portal.token.exp", String(Date.now() + 30 * 60 * 1000));
  });
  await page.goto("/dashboard");
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible({ timeout: 10000 });
});

test("wallets page renders with mocked api fallback", async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("pg.portal.token", "mock-token");
    window.localStorage.setItem("pg.portal.token.exp", String(Date.now() + 30 * 60 * 1000));
  });
  for (let attempt = 0; attempt < 2; attempt += 1) {
    try {
      await page.goto("/wallets", { waitUntil: "domcontentloaded" });
      break;
    } catch (err) {
      if (attempt === 1) {
        throw err;
      }
    }
  }
  await expect(page.getByRole("heading", { name: "Wallets" })).toBeVisible();
});
