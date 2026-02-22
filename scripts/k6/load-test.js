import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m", target: 50 },
    { duration: "30s", target: 0 }
  ]
};

const base = __ENV.BASE_URL || "http://localhost:8080";
const token = __ENV.TOKEN || "";

function headers(idempotencyKey) {
  return {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "Idempotency-Key": idempotencyKey,
      "X-Correlation-Id": `${__VU}-${__ITER}-${Date.now()}`
    }
  };
}

export default function () {
  const transferPayload = JSON.stringify({
    fromWalletId: __ENV.FROM_WALLET_ID,
    toWalletId: __ENV.TO_WALLET_ID,
    amount: 1.0
  });

  const transferRes = http.post(`${base}/transactions/transfer`, transferPayload, headers(`${__VU}-${__ITER}`));
  check(transferRes, { "transfer accepted": (r) => r.status === 200 || r.status === 409 || r.status === 429 });

  const retryRes = http.post(`${base}/transactions/transfer`, transferPayload, headers(`${__VU}-${__ITER}`));
  check(retryRes, { "idempotent retry safe": (r) => r.status === 200 || r.status === 409 });

  if (__ENV.WEBHOOK_EXTERNAL_REFERENCE) {
    const webhookBody = JSON.stringify({
      externalReference: __ENV.WEBHOOK_EXTERNAL_REFERENCE,
      status: "SUCCESS",
      amount: 1.0
    });
    const webhookRes = http.post(`${base}/payments/webhook`, webhookBody, {
      headers: {
        "Content-Type": "application/json",
        "X-Signature": __ENV.WEBHOOK_SIGNATURE || "invalid",
        "X-Event-Id": `${__VU}-${__ITER}`,
        "X-Timestamp": new Date().toISOString()
      }
    });
    check(webhookRes, { "webhook throttled or accepted": (r) => [200, 400, 429].includes(r.status) });
  }

  sleep(0.2);
}
