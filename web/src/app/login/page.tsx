"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input, Textarea } from "@/components/ui/form-controls";
import { setToken } from "@/lib/auth/token";
import { endpoints } from "@/lib/api/endpoints";
import { ENABLE_MANUAL_TOKEN } from "@/lib/constants";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("admin@example.com");
  const [password, setPassword] = useState("admin");
  const [manualToken, setManualToken] = useState("");
  const [error, setError] = useState("");

  async function handleLogin() {
    setError("");
    try {
      const result = await endpoints.login(username, password);
      setToken(result.token);
      router.replace("/dashboard");
    } catch {
      setError("Authentication failed");
    }
  }

  return (
    <div className="grid min-h-screen place-items-center bg-gradient-to-br from-slate-100 to-brand-50 p-4">
      <Card className="w-full max-w-xl space-y-4">
        <div>
          <CardTitle className="text-xl">Payment Gateway Portal Login</CardTitle>
          <CardDescription>JWT authentication with refresh-token support</CardDescription>
        </div>

        <div className="grid gap-3">
          <Input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" />
          <Input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" type="password" />
          <Button onClick={handleLogin}>Login with Backend</Button>
        </div>

        {ENABLE_MANUAL_TOKEN ? (
          <div className="space-y-2 border-t border-slate-200 pt-4">
            <p className="text-sm font-medium">Manual token mode (non-production)</p>
            <Textarea
              value={manualToken}
              onChange={(e) => setManualToken(e.target.value)}
              placeholder="Paste JWT token"
            />
            <p className="text-xs text-amber-700">
              Warning: token is persisted in localStorage with short TTL (30 minutes).
            </p>
            <Button
              onClick={() => {
                if (!manualToken.trim()) return;
                setToken(manualToken.trim());
                router.replace("/dashboard");
              }}
            >
              Continue with Pasted Token
            </Button>
          </div>
        ) : null}

        {error ? <p className="text-sm text-rose-600">{error}</p> : null}
      </Card>
    </div>
  );
}
