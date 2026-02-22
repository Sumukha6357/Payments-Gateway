"use client";

import { useParams } from "next/navigation";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/common/page-header";
import { StatusPill } from "@/components/common/status-pill";
import { Amount } from "@/components/common/amount";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { usePayment } from "@/hooks/usePayment";

export default function PaymentDetailPage() {
  const params = useParams<{ id: string }>();
  const { data, isLoading, isError } = usePayment(params.id);

  return (
    <div>
      <PageHeader title="Payment Detail" subtitle={params.id} />
      {isLoading ? <LoadingState /> : null}
      {isError || !data ? <ErrorState title="Payment not found" /> : null}
      {data ? (
        <div className="space-y-4">
          <Card className="grid gap-3 md:grid-cols-2">
            <div>
              <p className="text-xs text-slate-500">External Reference</p>
              <p className="text-sm font-medium">{data.externalReference}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Amount</p>
              <Amount value={data.amount} />
            </div>
            <div>
              <p className="text-xs text-slate-500">Status</p>
              <StatusPill status={data.status} />
            </div>
          </Card>
          <Card>
            <p className="mb-3 text-sm font-semibold">Status Timeline</p>
            <ol className="space-y-2 border-l border-slate-200 pl-4">
              <li className="text-sm">{data.createdAt}: Payment record created</li>
              <li className="text-sm">Awaiting webhook confirmation</li>
              <li className="text-sm">Final status: {data.status}</li>
            </ol>
          </Card>
        </div>
      ) : null}
    </div>
  );
}
