const TOKEN_KEY = "pg.portal.token";
const TOKEN_EXP_KEY = "pg.portal.token.exp";

let inMemoryToken: string | null = null;

export function getToken(): string | null {
  if (inMemoryToken) return inMemoryToken;
  if (typeof window === "undefined") return null;
  const expiry = window.localStorage.getItem(TOKEN_EXP_KEY);
  if (expiry && Date.now() > Number(expiry)) {
    clearToken();
    return null;
  }
  const token = window.localStorage.getItem(TOKEN_KEY);
  inMemoryToken = token;
  return token;
}

export function setToken(token: string, ttlMinutes = 30): void {
  inMemoryToken = token;
  if (typeof window === "undefined") return;
  const expiresAt = Date.now() + ttlMinutes * 60_000;
  window.localStorage.setItem(TOKEN_KEY, token);
  window.localStorage.setItem(TOKEN_EXP_KEY, String(expiresAt));
}

export function clearToken(): void {
  inMemoryToken = null;
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(TOKEN_EXP_KEY);
}
