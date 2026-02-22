"use client";

import { useParams } from "next/navigation";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/common/page-header";
import { Amount } from "@/components/common/amount";
import { StatusPill } from "@/components/common/status-pill";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { useWallet } from "@/hooks/useWallet";

export default function WalletDetailPage() {
  const params = useParams<{ id: string }>();
  const { data, isLoading, isError } = useWallet(params.id);

  return (
    <div>
      <PageHeader title="Wallet Detail" subtitle={params.id} />
      {isLoading ? <LoadingState /> : null}
      {isError || !data ? <ErrorState title="Wallet not found" /> : null}
      {data ? (
        <Card className="grid gap-3 md:grid-cols-2">
          <div>
            <p className="text-xs text-slate-500">Wallet ID</p>
            <p className="text-sm font-medium">{data.id}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">User ID</p>
            <p className="text-sm font-medium">{data.userId}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Status</p>
            <StatusPill status={data.status} />
          </div>
          <div>
            <p className="text-xs text-slate-500">Balance</p>
            <Amount value={data.balance ?? 0} currency={data.currency} />
          </div>
        </Card>
      ) : null}
    </div>
  );
}
