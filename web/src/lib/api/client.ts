import axios, { AxiosError } from "axios";
import { API_BASE_URL } from "@/lib/constants";
import { getToken, clearToken, setToken } from "@/lib/auth/token";
import { getCorrelationId } from "@/lib/utils/ids";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  const current = typeof window !== "undefined" ? window.sessionStorage.getItem("correlation-id") : null;
  const correlationId = getCorrelationId(current);
  if (typeof window !== "undefined") {
    window.sessionStorage.setItem("correlation-id", correlationId);
  }
  config.headers["X-Correlation-Id"] = correlationId;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string; correlationId?: string }>) => {
    const original = error.config as (typeof error.config & { _retry?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      try {
        const refreshResponse = await apiClient.post<{ token: string }>("/auth/refresh");
        if (refreshResponse.data?.token) {
          setToken(refreshResponse.data.token);
          original.headers = original.headers ?? {};
          original.headers.Authorization = `Bearer ${refreshResponse.data.token}`;
          return apiClient(original);
        }
      } catch {
        // refresh failed, redirect to login
      }
    }

    if (error.response?.status === 401 && typeof window !== "undefined") {
      clearToken();
      window.location.href = "/login";
    }

    const correlationId = error.response?.headers["x-correlation-id"];
    if (correlationId && typeof window !== "undefined") {
      window.sessionStorage.setItem("last-error-correlation-id", String(correlationId));
    }

    return Promise.reject(error);
  }
);
