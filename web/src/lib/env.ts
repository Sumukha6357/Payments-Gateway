import { z } from "zod";

const envSchema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_ENABLE_MANUAL_TOKEN: z
    .string()
    .optional()
    .transform((v) => v === "true")
});

const parsed = envSchema.safeParse({
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  NEXT_PUBLIC_ENABLE_MANUAL_TOKEN: process.env.NEXT_PUBLIC_ENABLE_MANUAL_TOKEN
});

if (!parsed.success) {
  throw new Error(`Invalid frontend env: ${parsed.error.message}`);
}

export const appEnv = parsed.data;
