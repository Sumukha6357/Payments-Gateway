"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { APP_NAME } from "@/lib/constants";
import { clearToken } from "@/lib/auth/token";
import { cn } from "@/lib/utils/cn";

const nav = [
  ["/dashboard", "Dashboard"],
  ["/users", "Users"],
  ["/wallets", "Wallets"],
  ["/transfers", "Transfers"],
  ["/payments", "Payments"],
  ["/webhooks", "Webhooks"],
  ["/ledger", "Ledger"],
  ["/audit-logs", "Audit Logs"],
  ["/developer-tools", "Developer Tools"],
  ["/settings", "Settings"]
] as const;

export function SidebarNav() {
  const pathname = usePathname();
  return (
    <aside className="hidden w-64 border-r border-slate-200 bg-white p-4 lg:block">
      <p className="mb-6 text-sm font-semibold text-slate-500">{APP_NAME}</p>
      <nav className="space-y-1">
        {nav.map(([href, label]) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "block rounded-md px-3 py-2 text-sm transition",
                active ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100"
              )}
            >
              {label}
            </Link>
          );
        })}
      </nav>
      <button
        onClick={() => {
          clearToken();
          window.location.href = "/login";
        }}
        className="mt-8 text-sm text-rose-600"
      >
        Logout
      </button>
    </aside>
  );
}
