"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Input, Select, Textarea } from "@/components/ui/form-controls";
import { generateWebhookSignature, verifyWebhookSignature } from "@/lib/utils/signature";

export default function DeveloperToolsPage() {
  const [method, setMethod] = useState("POST");
  const [path, setPath] = useState("/payments");
  const [token, setToken] = useState("");
  const [body, setBody] = useState('{"walletId":"...","amount":10.00}');
  const [curl, setCurl] = useState("");

  const [payload, setPayload] = useState('{"event":"PAYMENT_SUCCESS"}');
  const [timestamp, setTimestamp] = useState(new Date().toISOString());
  const [secret, setSecret] = useState("");
  const [signature, setSignature] = useState("");
  const [verification, setVerification] = useState<string>("");

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Developer Tools</h1>

      <Card className="space-y-3">
        <CardTitle>cURL Generator</CardTitle>
        <div className="grid gap-3 md:grid-cols-3">
          <Select value={method} onChange={(e) => setMethod(e.target.value)}>
            <option>GET</option>
            <option>POST</option>
            <option>PUT</option>
            <option>DELETE</option>
          </Select>
          <Input value={path} onChange={(e) => setPath(e.target.value)} placeholder="/payments" />
          <Input value={token} onChange={(e) => setToken(e.target.value)} placeholder="JWT token" />
        </div>
        <Textarea value={body} onChange={(e) => setBody(e.target.value)} placeholder="JSON body" />
        <Button
          onClick={() => {
            const generated = `curl -X ${method} \"$NEXT_PUBLIC_API_BASE_URL${path}\" -H \"Authorization: Bearer ${token}\" -H \"Content-Type: application/json\" -H \"X-Correlation-Id: $(uuidgen)\" -d '${body}'`;
            setCurl(generated);
          }}
        >
          Generate cURL
        </Button>
        {curl ? <Textarea value={curl} readOnly /> : null}
      </Card>

      <Card className="space-y-3">
        <CardTitle>Webhook Signature Verifier</CardTitle>
        <Textarea value={payload} onChange={(e) => setPayload(e.target.value)} placeholder="Payload" />
        <div className="grid gap-3 md:grid-cols-3">
          <Input value={timestamp} onChange={(e) => setTimestamp(e.target.value)} placeholder="Timestamp" />
          <Input value={secret} onChange={(e) => setSecret(e.target.value)} placeholder="Secret" />
          <Input value={signature} onChange={(e) => setSignature(e.target.value)} placeholder="Signature" />
        </div>
        <div className="flex gap-2">
          <Button
            onClick={async () => {
              const sig = await generateWebhookSignature(payload, timestamp, secret);
              setSignature(sig);
              setVerification("Generated");
            }}
          >
            Generate Signature
          </Button>
          <Button
            onClick={async () => {
              const ok = await verifyWebhookSignature({ payload, timestamp, secret, signature });
              setVerification(ok ? "Signature is valid" : "Signature mismatch");
            }}
          >
            Verify
          </Button>
        </div>
        {verification ? <p className="text-sm text-slate-600">{verification}</p> : null}
      </Card>
    </div>
  );
}
