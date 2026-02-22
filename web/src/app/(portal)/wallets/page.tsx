"use client";

import Link from "next/link";
import { ColumnDef } from "@tanstack/react-table";
import { Wallet } from "@/lib/api/types";
import { useWallets } from "@/hooks/useWallets";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";
import { StatusPill } from "@/components/common/status-pill";
import { CreateWalletModal } from "@/components/forms/create-wallet-modal";

const columns: ColumnDef<Wallet>[] = [
  {
    header: "Wallet ID",
    accessorKey: "id",
    cell: ({ row }) => (
      <Link href={`/wallets/${row.original.id}`} className="text-brand-700 hover:underline">
        {row.original.id}
      </Link>
    )
  },
  { header: "User", accessorKey: "userId" },
  { header: "Currency", accessorKey: "currency" },
  {
    header: "Status",
    accessorKey: "status",
    cell: ({ row }) => <StatusPill status={row.original.status} />
  },
  { header: "Created", accessorKey: "createdAt" }
];

export default function WalletsPage() {
  const { data, isLoading, isError } = useWallets();

  return (
    <div>
      <PageHeader title="Wallets" subtitle="Create and monitor user wallets" action={<CreateWalletModal />} />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load wallets" /> : null}
      {!isLoading && !isError && data?.length === 0 ? <EmptyState title="No wallets found" /> : null}
      {data && data.length > 0 ? <DataTable columns={columns} data={data} /> : null}
    </div>
  );
}
