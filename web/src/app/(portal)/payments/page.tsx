"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { Payment } from "@/lib/api/types";
import { usePayments } from "@/hooks/usePayments";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { FilterBar } from "@/components/common/filter-bar";
import { Amount } from "@/components/common/amount";
import { StatusPill } from "@/components/common/status-pill";
import { CreatePaymentModal } from "@/components/forms/create-payment-modal";

const columns: ColumnDef<Payment>[] = [
  {
    header: "Payment ID",
    accessorKey: "paymentId",
    cell: ({ row }) => (
      <Link href={`/payments/${row.original.paymentId}`} className="text-brand-700 hover:underline">
        {row.original.paymentId}
      </Link>
    )
  },
  { header: "Reference", accessorKey: "externalReference" },
  { header: "Amount", accessorKey: "amount", cell: ({ row }) => <Amount value={row.original.amount} /> },
  { header: "Status", accessorKey: "status", cell: ({ row }) => <StatusPill status={row.original.status} /> },
  { header: "Created", accessorKey: "createdAt" }
];

export default function PaymentsPage() {
  const { data, isLoading, isError } = usePayments();
  const [search, setSearch] = useState("");
  const filtered = useMemo(
    () => (data ?? []).filter((item) => item.paymentId.toLowerCase().includes(search.toLowerCase())),
    [data, search]
  );

  return (
    <div>
      <PageHeader title="Payments" subtitle="Collections and lifecycle tracking" action={<CreatePaymentModal />} />
      <FilterBar search={search} onSearch={setSearch} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load payments" /> : null}
      {!isLoading && !isError && filtered.length === 0 ? <EmptyState title="No payments found" /> : null}
      {filtered.length > 0 ? <DataTable columns={columns} data={filtered} /> : null}
    </div>
  );
}
