"use client";

import { SidebarNav } from "@/components/layout/sidebar-nav";
import { TopBar } from "@/components/layout/top-bar";
import { AuthGuard } from "@/components/layout/auth-guard";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <div className="min-h-screen bg-slate-50 lg:flex">
        <SidebarNav />
        <div className="flex-1">
          <TopBar />
          <main className="mx-auto w-full max-w-7xl p-4 lg:p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  );
}
