"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { WebhookEndpoint } from "@/lib/api/types";
import { useWebhooks } from "@/hooks/useWebhooks";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { FilterBar } from "@/components/common/filter-bar";
import { StatusPill } from "@/components/common/status-pill";
import { CreateWebhookModal } from "@/components/forms/create-webhook-modal";

const columns: ColumnDef<WebhookEndpoint>[] = [
  {
    header: "Endpoint",
    accessorKey: "url",
    cell: ({ row }) => (
      <Link href={`/webhooks/${row.original.id}`} className="text-brand-700 hover:underline">
        {row.original.url}
      </Link>
    )
  },
  {
    header: "Status",
    accessorKey: "active",
    cell: ({ row }) => <StatusPill status={row.original.active ? "ACTIVE" : "DISABLED"} />
  },
  { header: "Created", accessorKey: "createdAt" }
];

export default function WebhooksPage() {
  const { data, isLoading, isError } = useWebhooks();
  const [search, setSearch] = useState("");
  const filtered = useMemo(
    () => (data ?? []).filter((item) => item.url.toLowerCase().includes(search.toLowerCase())),
    [data, search]
  );

  return (
    <div>
      <PageHeader title="Webhooks" subtitle="Endpoint management and delivery monitoring" action={<CreateWebhookModal />} />
      <FilterBar search={search} onSearch={setSearch} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load endpoints" /> : null}
      {!isLoading && !isError && filtered.length === 0 ? <EmptyState title="No webhook endpoints" /> : null}
      {filtered.length > 0 ? <DataTable columns={columns} data={filtered} /> : null}
    </div>
  );
}
