"use client";

import { Card } from "@/components/ui/card";

export function ErrorState({ title = "Request failed", message }: { title?: string; message?: string }) {
  const correlationId =
    typeof window !== "undefined"
      ? window.sessionStorage.getItem("last-error-correlation-id") ?? window.sessionStorage.getItem("correlation-id")
      : null;

  return (
    <Card className="border-rose-200 bg-rose-50">
      <h3 className="text-sm font-semibold text-rose-700">{title}</h3>
      <p className="mt-1 text-sm text-rose-600">{message ?? "Try again or contact support."}</p>
      <p className="mt-3 text-xs text-rose-500">Correlation ID: {correlationId ?? "n/a"}</p>
    </Card>
  );
}
