"use client";

import dynamic from "next/dynamic";
import { PageHeader } from "@/components/common/page-header";
import { StatCard } from "@/components/common/stat-card";
import { Card } from "@/components/ui/card";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { useDashboard } from "@/hooks/queries";

const TransferVolumeChart = dynamic(
  () => import("@/components/common/transfer-volume-chart").then((m) => m.TransferVolumeChart),
  { ssr: false }
);

export default function DashboardPage() {
  const { data, isLoading, isError } = useDashboard();

  if (isLoading) return <LoadingState label="Loading dashboard" />;
  if (isError || !data) return <ErrorState title="Dashboard unavailable" />;

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Operational KPIs and transaction health" />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total Users" value={String(data.totalUsers)} />
        <StatCard label="Total Wallets" value={String(data.totalWallets)} />
        <StatCard label="Transfers (24h)" value={String(data.transferCount24h)} />
        <StatCard label="Payment Success Rate" value={`${data.paymentSuccessRate.toFixed(1)}%`} />
      </div>
      <Card className="mt-4 h-80">
        <p className="mb-2 text-sm font-semibold">Transfer Volume by Hour</p>
        <TransferVolumeChart data={data.transferVolumeByHour} />
      </Card>
    </div>
  );
}
