import { Card } from "@/components/ui/card";

export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <Card>
      <p className="text-sm font-semibold">{title}</p>
      <p className="mt-1 text-sm text-slate-500">{description ?? "No records available."}</p>
    </Card>
  );
}
