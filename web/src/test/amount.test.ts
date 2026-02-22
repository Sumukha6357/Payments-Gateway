import { test, expect } from "vitest";
import { formatAmount } from "@/lib/utils/amount";

test("formats amount in USD", () => {
  expect(formatAmount(1200.5, "USD")).toContain("1,200.50");
});
