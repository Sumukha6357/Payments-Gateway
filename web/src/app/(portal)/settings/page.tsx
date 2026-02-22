import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/common/page-header";

export default function SettingsPage() {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return (
    <div>
      <PageHeader title="Settings" subtitle="Read-only operational configuration" />
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <p className="text-sm font-semibold">Environment</p>
          <p className="mt-2 text-sm text-slate-600">API Base URL: {apiBaseUrl}</p>
          <p className="text-sm text-slate-600">Mode: Developer Portal</p>
        </Card>
        <Card>
          <p className="text-sm font-semibold">Rate Limits</p>
          <p className="mt-2 text-sm text-slate-600">Transfer/Payment: 5 req/min per user</p>
          <p className="text-sm text-slate-600">Webhook delivery: 60 req/min per endpoint</p>
        </Card>
        <Card>
          <p className="text-sm font-semibold">Retry Policies</p>
          <p className="mt-2 text-sm text-slate-600">Outbox exponential backoff enabled</p>
          <p className="text-sm text-slate-600">Max attempts: 8</p>
        </Card>
      </div>
    </div>
  );
}
