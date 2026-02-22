import { Card, CardDescription, CardTitle } from "@/components/ui/card";

export function StatCard({ label, value, trend }: { label: string; value: string; trend?: string }) {
  return (
    <Card>
      <CardDescription>{label}</CardDescription>
      <CardTitle className="mt-1 text-2xl font-bold font-tabular">{value}</CardTitle>
      {trend ? <p className="mt-1 text-xs text-slate-500">{trend}</p> : null}
    </Card>
  );
}
