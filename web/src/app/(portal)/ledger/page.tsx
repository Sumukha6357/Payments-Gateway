"use client";

import { useState } from "react";
import { useLedger } from "@/hooks/useLedger";
import { PageHeader } from "@/components/common/page-header";
import { FilterBar } from "@/components/common/filter-bar";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { Amount } from "@/components/common/amount";
import { JsonViewer } from "@/components/common/json-viewer";
import { ColumnDef } from "@tanstack/react-table";
import { LedgerEntry } from "@/lib/api/types";

const columns: ColumnDef<LedgerEntry>[] = [
  { header: "Entry ID", accessorKey: "id" },
  { header: "Wallet", accessorKey: "walletId" },
  { header: "Type", accessorKey: "type" },
  { header: "Amount", accessorKey: "amount", cell: ({ row }) => <Amount value={row.original.amount} /> },
  { header: "Reference", accessorKey: "referenceId" },
  { header: "Created", accessorKey: "createdAt" }
];

export default function LedgerPage() {
  const [search, setSearch] = useState("");
  const { data, isLoading, isError } = useLedger(search || undefined);

  return (
    <div>
      <PageHeader title="Ledger" subtitle="Read-only accounting records with transaction drilldown" />
      <FilterBar search={search} onSearch={setSearch} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Ledger query failed" /> : null}
      {!isLoading && !isError && data?.length === 0 ? <EmptyState title="No ledger entries" /> : null}
      {data && data.length > 0 ? (
        <div className="space-y-4">
          <DataTable columns={columns} data={data} />
          <JsonViewer value={data[0]} />
        </div>
      ) : null}
    </div>
  );
}
