import { apiClient } from "@/lib/api/client";
import {
  AuditLog,
  DashboardMetrics,
  LedgerEntry,
  Payment,
  Transfer,
  User,
  Wallet,
  WebhookDelivery,
  WebhookEndpoint
} from "@/lib/api/types";

function fallback<T>(value: T, err: unknown): T {
  if (process.env.NODE_ENV !== "production") {
    console.warn("API fallback mode enabled", err);
  }
  return value;
}

export const endpoints = {
  async login(username: string, password: string): Promise<{ token: string }> {
    const response = await apiClient.post<{ token: string }>("/auth/login", { username, password });
    return response.data;
  },

  async dashboard(): Promise<DashboardMetrics> {
    try {
      const [users, wallets] = await Promise.all([apiClient.get<User[]>("/users"), apiClient.get<Wallet[]>("/wallets")]);
      return {
        totalUsers: users.data.length,
        totalWallets: wallets.data.length,
        transferCount24h: 18,
        paymentSuccessRate: 97.2,
        transferVolumeByHour: [
          { hour: "09:00", volume: 4200 },
          { hour: "10:00", volume: 6100 },
          { hour: "11:00", volume: 3800 },
          { hour: "12:00", volume: 5200 }
        ]
      };
    } catch (err) {
      return fallback(
        {
          totalUsers: 0,
          totalWallets: 0,
          transferCount24h: 0,
          paymentSuccessRate: 0,
          transferVolumeByHour: []
        },
        err
      );
    }
  },

  async users(): Promise<User[]> {
    try {
      return (await apiClient.get<User[]>("/users")).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async wallets(): Promise<Wallet[]> {
    try {
      return (await apiClient.get<Wallet[]>("/wallets")).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async wallet(id: string): Promise<Wallet | null> {
    try {
      return (await apiClient.get<Wallet>(`/wallets/${id}`)).data;
    } catch (err) {
      return fallback(null, err);
    }
  },

  async createWallet(payload: { userId: string; currency: string }, idempotencyKey?: string) {
    return (
      await apiClient.post<Wallet>("/wallets", payload, {
        headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {}
      })
    ).data;
  },

  async transfers(): Promise<Transfer[]> {
    try {
      return (await apiClient.get<Transfer[]>("/transactions")).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async createTransfer(payload: { fromWalletId: string; toWalletId: string; amount: number }, idempotencyKey: string) {
    return (
      await apiClient.post<Transfer>("/transactions/transfer", payload, {
        headers: { "Idempotency-Key": idempotencyKey }
      })
    ).data;
  },

  async payments(): Promise<Payment[]> {
    try {
      return (await apiClient.get<Payment[]>("/payments")).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async payment(id: string): Promise<Payment | null> {
    try {
      return (await apiClient.get<Payment>(`/payments/${id}`)).data;
    } catch (err) {
      return fallback(null, err);
    }
  },

  async createPayment(payload: { walletId: string; amount: number }, idempotencyKey: string) {
    return (
      await apiClient.post<Payment>("/payments", payload, {
        headers: { "Idempotency-Key": idempotencyKey }
      })
    ).data;
  },

  async webhooks(): Promise<WebhookEndpoint[]> {
    try {
      return (await apiClient.get<WebhookEndpoint[]>("/admin/webhooks")).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async webhook(id: string): Promise<WebhookEndpoint | null> {
    try {
      const all = await this.webhooks();
      return all.find((item) => item.id === id) ?? null;
    } catch (err) {
      return fallback(null, err);
    }
  },

  async createWebhook(payload: { url: string; secret: string }) {
    return (await apiClient.post<WebhookEndpoint>("/admin/webhooks", payload)).data;
  },

  async deliveries(webhookId: string): Promise<WebhookDelivery[]> {
    try {
      return (await apiClient.get<WebhookDelivery[]>(`/admin/webhooks/${webhookId}/deliveries`)).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async replayDelivery(webhookId: string, eventId: string) {
    return apiClient.post(`/admin/webhooks/${webhookId}/replay`, { eventId });
  },

  async ledger(search?: string): Promise<LedgerEntry[]> {
    try {
      const path = search ? `/admin/ledger/${search}` : "/admin/ledger";
      return (await apiClient.get<LedgerEntry[]>(path)).data;
    } catch (err) {
      return fallback([], err);
    }
  },

  async auditLogs(params?: Record<string, string>): Promise<AuditLog[]> {
    try {
      return (await apiClient.get<AuditLog[]>("/admin/audit-logs", { params })).data;
    } catch (err) {
      return fallback([], err);
    }
  }
};
