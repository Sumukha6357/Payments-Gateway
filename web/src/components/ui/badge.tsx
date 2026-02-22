import { cn } from "@/lib/utils/cn";

export function Badge({
  label,
  variant = "default"
}: {
  label: string;
  variant?: "default" | "success" | "warning" | "danger";
}) {
  const styles = {
    default: "bg-slate-100 text-slate-700",
    success: "bg-emerald-100 text-emerald-700",
    warning: "bg-amber-100 text-amber-700",
    danger: "bg-rose-100 text-rose-700"
  } as const;

  return <span className={cn("rounded-full px-2 py-1 text-xs font-semibold", styles[variant])}>{label}</span>;
}
