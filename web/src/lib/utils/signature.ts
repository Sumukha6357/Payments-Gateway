async function hmacSha256(payload: string, secret: string): Promise<string> {
  const encoder = new TextEncoder();
  const keyData = encoder.encode(secret);
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyData,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", cryptoKey, encoder.encode(payload));
  return Array.from(new Uint8Array(signature))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export async function verifyWebhookSignature(input: {
  payload: string;
  timestamp: string;
  secret: string;
  signature: string;
}): Promise<boolean> {
  const computed = await hmacSha256(`${input.payload}|${input.timestamp}`, input.secret);
  return computed === input.signature;
}

export async function generateWebhookSignature(payload: string, timestamp: string, secret: string) {
  return hmacSha256(`${payload}|${timestamp}`, secret);
}
