import { Card } from "@/components/ui/card";

export function LoadingState({ label = "Loading" }: { label?: string }) {
  return <Card className="text-sm text-slate-500">{label}...</Card>;
}
