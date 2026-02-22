"use client";

import { useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { AuditLog } from "@/lib/api/types";
import { useAuditLogs } from "@/hooks/useAuditLogs";
import { PageHeader } from "@/components/common/page-header";
import { FilterBar } from "@/components/common/filter-bar";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";

const columns: ColumnDef<AuditLog>[] = [
  { header: "Entity", accessorKey: "entityType" },
  { header: "Entity ID", accessorKey: "entityId" },
  { header: "Action", accessorKey: "action" },
  { header: "Created", accessorKey: "createdAt" }
];

export default function AuditLogsPage() {
  const [search, setSearch] = useState("");
  const { data, isLoading, isError } = useAuditLogs(search ? { q: search } : undefined);

  return (
    <div>
      <PageHeader title="Audit Logs" subtitle="Actor/action/entity traceability" />
      <FilterBar search={search} onSearch={setSearch} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load audit logs" /> : null}
      {!isLoading && !isError && data?.length === 0 ? <EmptyState title="No audit logs" /> : null}
      {data && data.length > 0 ? <DataTable columns={columns} data={data} /> : null}
    </div>
  );
}
