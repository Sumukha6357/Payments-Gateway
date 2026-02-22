"use client";

import { ColumnDef } from "@tanstack/react-table";
import { User } from "@/lib/api/types";
import { useUsers } from "@/hooks/useUsers";
import { PageHeader } from "@/components/common/page-header";
import { DataTable } from "@/components/common/data-table";
import { LoadingState } from "@/components/common/loading-state";
import { ErrorState } from "@/components/common/error-state";
import { EmptyState } from "@/components/common/empty-state";

const columns: ColumnDef<User>[] = [
  { header: "Name", accessorKey: "name" },
  { header: "Email", accessorKey: "email" },
  { header: "Status", accessorKey: "status" },
  { header: "Created", accessorKey: "createdAt" }
];

export default function UsersPage() {
  const { data, isLoading, isError } = useUsers();

  return (
    <div>
      <PageHeader title="Users" subtitle="Admin-only user directory" />
      {isLoading ? <LoadingState /> : null}
      {isError ? <ErrorState title="Could not load users" /> : null}
      {!isLoading && !isError && data?.length === 0 ? <EmptyState title="No users found" /> : null}
      {data && data.length > 0 ? <DataTable columns={columns} data={data} /> : null}
    </div>
  );
}
