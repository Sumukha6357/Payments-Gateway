import { test, expect } from "vitest";
import { generateWebhookSignature, verifyWebhookSignature } from "@/lib/utils/signature";

test("signature helper generates and verifies", async () => {
  const payload = "{\"a\":1}";
  const timestamp = "2026-02-22T00:00:00Z";
  const secret = "secret-1234567890";

  const signature = await generateWebhookSignature(payload, timestamp, secret);
  const valid = await verifyWebhookSignature({ payload, timestamp, secret, signature });

  expect(signature.length).toBeGreaterThan(10);
  expect(valid).toBe(true);
});
