export type ApiList<T> = {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
};

export type User = {
  id: string;
  name: string;
  email: string;
  status: string;
  createdAt: string;
};

export type Wallet = {
  id: string;
  userId: string;
  currency: string;
  status: string;
  createdAt: string;
  balance?: number;
};

export type Transfer = {
  transactionId: string;
  fromWalletId: string;
  toWalletId: string;
  amount: number;
  status: string;
  createdAt: string;
};

export type Payment = {
  paymentId: string;
  transactionId: string;
  externalReference: string;
  amount: number;
  status: string;
  createdAt: string;
};

export type WebhookEndpoint = {
  id: string;
  url: string;
  active: boolean;
  createdAt: string;
};

export type WebhookDelivery = {
  id: string;
  eventId: string;
  endpointId: string;
  attempt: number;
  status: string;
  responseCode?: number;
  errorMessage?: string;
  createdAt: string;
};

export type LedgerEntry = {
  id: string;
  walletId: string;
  type: "DEBIT" | "CREDIT";
  amount: number;
  referenceId: string;
  createdAt: string;
};

export type AuditLog = {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  payload: string;
  createdAt: string;
};

export type DashboardMetrics = {
  totalUsers: number;
  totalWallets: number;
  transferCount24h: number;
  paymentSuccessRate: number;
  transferVolumeByHour: Array<{ hour: string; volume: number }>;
};
