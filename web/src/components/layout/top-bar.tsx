"use client";

import { APP_NAME } from "@/lib/constants";

export function TopBar() {
  return (
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-500">Enterprise Workspace</p>
          <p className="text-sm font-semibold">{APP_NAME}</p>
        </div>
        <div className="rounded-md bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">Acme Payments Org</div>
      </div>
    </header>
  );
}
