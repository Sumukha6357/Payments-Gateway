"use client";

import { Input, Select } from "@/components/ui/form-controls";

export function FilterBar({
  search,
  onSearch,
  status,
  onStatus
}: {
  search: string;
  onSearch: (value: string) => void;
  status?: string;
  onStatus?: (value: string) => void;
}) {
  return (
    <div className="mb-4 grid gap-3 rounded-xl border border-slate-200 bg-white p-3 md:grid-cols-3">
      <Input placeholder="Search..." value={search} onChange={(e) => onSearch(e.target.value)} />
      <Input type="date" />
      {onStatus ? (
        <Select value={status} onChange={(e) => onStatus(e.target.value)}>
          <option value="">All statuses</option>
          <option value="SUCCESS">Success</option>
          <option value="PENDING">Pending</option>
          <option value="FAILED">Failed</option>
        </Select>
      ) : (
        <Input placeholder="Date range" readOnly value="Last 30 days" />
      )}
    </div>
  );
}
