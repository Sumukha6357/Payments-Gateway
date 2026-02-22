import { Badge } from "@/components/ui/badge";

export function StatusPill({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const variant = normalized.includes("SUCCESS") || normalized.includes("ACTIVE")
    ? "success"
    : normalized.includes("PENDING")
      ? "warning"
      : normalized.includes("FAILED") || normalized.includes("DISABLED")
        ? "danger"
        : "default";
  return <Badge label={status} variant={variant} />;
}
