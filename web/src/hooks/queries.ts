"use client";

import { useQuery } from "@tanstack/react-query";
import { endpoints } from "@/lib/api/endpoints";

export const useUsers = () => useQuery({ queryKey: ["users"], queryFn: () => endpoints.users() });

export const useWallets = () => useQuery({ queryKey: ["wallets"], queryFn: () => endpoints.wallets() });

export const useWallet = (id: string) =>
  useQuery({ queryKey: ["wallet", id], queryFn: () => endpoints.wallet(id), enabled: Boolean(id) });

export const useTransfers = () =>
  useQuery({ queryKey: ["transfers"], queryFn: () => endpoints.transfers() });

export const usePayments = () => useQuery({ queryKey: ["payments"], queryFn: () => endpoints.payments() });

export const usePayment = (id: string) =>
  useQuery({ queryKey: ["payment", id], queryFn: () => endpoints.payment(id), enabled: Boolean(id) });

export const useWebhooks = () => useQuery({ queryKey: ["webhooks"], queryFn: () => endpoints.webhooks() });

export const useWebhook = (id: string) =>
  useQuery({ queryKey: ["webhook", id], queryFn: () => endpoints.webhook(id), enabled: Boolean(id) });

export const useWebhookDeliveries = (id: string) =>
  useQuery({
    queryKey: ["webhook-deliveries", id],
    queryFn: () => endpoints.deliveries(id),
    enabled: Boolean(id)
  });

export const useLedger = (search?: string) =>
  useQuery({ queryKey: ["ledger", search], queryFn: () => endpoints.ledger(search) });

export const useAuditLogs = (params?: Record<string, string>) =>
  useQuery({ queryKey: ["audit-logs", params], queryFn: () => endpoints.auditLogs(params) });

export const useDashboard = () =>
  useQuery({ queryKey: ["dashboard"], queryFn: () => endpoints.dashboard() });
