"use client";

import { useMemo, useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { Transfer } from "@/lib/api/types";
import { useTransfers } from "@/hooks/useTransfers";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { FilterBar } from "@/components/common/filter-bar";
import { Amount } from "@/components/common/amount";
import { StatusPill } from "@/components/common/status-pill";
import { CreateTransferModal } from "@/components/forms/create-transfer-modal";

const columns: ColumnDef<Transfer>[] = [
  { header: "Transaction", accessorKey: "transactionId" },
  { header: "From", accessorKey: "fromWalletId" },
  { header: "To", accessorKey: "toWalletId" },
  { header: "Amount", accessorKey: "amount", cell: ({ row }) => <Amount value={row.original.amount} /> },
  { header: "Status", accessorKey: "status", cell: ({ row }) => <StatusPill status={row.original.status} /> },
  { header: "Created", accessorKey: "createdAt" }
];

export default function TransfersPage() {
  const { data, isLoading, isError } = useTransfers();
  const [search, setSearch] = useState("");
  const filtered = useMemo(
    () =>
      (data ?? []).filter((item) =>
        [item.transactionId, item.fromWalletId, item.toWalletId].some((v) => v?.toLowerCase().includes(search.toLowerCase()))
      ),
    [data, search]
  );

  return (
    <div>
      <PageHeader title="Transfers" subtitle="Money movement with idempotency" action={<CreateTransferModal />} />
      <FilterBar search={search} onSearch={setSearch} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load transfers" /> : null}
      {!isLoading && !isError && filtered.length === 0 ? <EmptyState title="No transfers found" /> : null}
      {filtered.length > 0 ? <DataTable columns={columns} data={filtered} /> : null}
    </div>
  );
}
