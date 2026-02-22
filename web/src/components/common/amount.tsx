import { formatAmount } from "@/lib/utils/amount";

export function Amount({ value, currency = "USD" }: { value: number | string; currency?: string }) {
  return <span className="font-tabular text-right font-medium">{formatAmount(value, currency)}</span>;
}
