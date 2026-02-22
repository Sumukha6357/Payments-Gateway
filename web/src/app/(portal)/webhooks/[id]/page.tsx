"use client";

import { useParams } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { ColumnDef } from "@tanstack/react-table";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { StatusPill } from "@/components/common/status-pill";
import { useWebhook } from "@/hooks/useWebhook";
import { useWebhookDeliveries } from "@/hooks/queries";
import { endpoints } from "@/lib/api/endpoints";
import { WebhookDelivery } from "@/lib/api/types";

const columns: ColumnDef<WebhookDelivery>[] = [
  { header: "Event ID", accessorKey: "eventId" },
  { header: "Attempt", accessorKey: "attempt" },
  { header: "Status", accessorKey: "status", cell: ({ row }) => <StatusPill status={row.original.status} /> },
  { header: "Code", accessorKey: "responseCode" },
  { header: "Created", accessorKey: "createdAt" }
];

export default function WebhookDetailPage() {
  const params = useParams<{ id: string }>();
  const endpoint = useWebhook(params.id);
  const deliveries = useWebhookDeliveries(params.id);

  const replay = useMutation({
    mutationFn: (eventId: string) => endpoints.replayDelivery(params.id, eventId),
    onSuccess: () => toast.success("Replay requested"),
    onError: () => toast.error("Replay failed")
  });

  return (
    <div>
      <PageHeader title="Webhook Endpoint" subtitle={params.id} />
      {endpoint.isLoading ? <LoadingState /> : null}
      {endpoint.isError || !endpoint.data ? <ErrorState title="Endpoint not found" /> : null}
      {endpoint.data ? (
        <Card className="mb-4 grid gap-2 md:grid-cols-2">
          <div>
            <p className="text-xs text-slate-500">URL</p>
            <p className="text-sm">{endpoint.data.url}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Status</p>
            <StatusPill status={endpoint.data.active ? "ACTIVE" : "DISABLED"} />
          </div>
        </Card>
      ) : null}
      {deliveries.isLoading ? <LoadingState label="Loading deliveries" /> : null}
      {deliveries.isError ? <ErrorState title="Could not load deliveries" /> : null}
      {deliveries.data && deliveries.data.length === 0 ? <EmptyState title="No deliveries yet" /> : null}
      {deliveries.data && deliveries.data.length > 0 ? (
        <div className="space-y-3">
          <DataTable columns={columns} data={deliveries.data} />
          <Button onClick={() => replay.mutate(deliveries.data[0]?.eventId ?? "")}>Replay Latest Event</Button>
        </div>
      ) : null}
    </div>
  );
}
