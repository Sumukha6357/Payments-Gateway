"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function GlobalError({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="grid min-h-screen place-items-center p-6">
      <div className="max-w-lg rounded-xl border border-rose-200 bg-rose-50 p-6">
        <h2 className="text-lg font-semibold text-rose-700">Unexpected error</h2>
        <p className="mt-2 text-sm text-rose-600">{error.message}</p>
        <p className="mt-2 text-xs text-rose-500">
          Correlation ID: {typeof window !== "undefined" ? window.sessionStorage.getItem("last-error-correlation-id") : "n/a"}
        </p>
        <Button className="mt-4" onClick={() => reset()}>
          Retry
        </Button>
      </div>
    </div>
  );
}
