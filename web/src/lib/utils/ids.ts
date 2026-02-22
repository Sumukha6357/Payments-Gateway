import { v4 as uuidv4 } from "uuid";

export function getCorrelationId(existing?: string | null): string {
  if (existing && existing.trim().length > 0) return existing;
  return uuidv4();
}

export function createIdempotencyKey(): string {
  return uuidv4();
}
