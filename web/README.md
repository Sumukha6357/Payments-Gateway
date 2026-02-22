# Payment Gateway Developer/Admin Portal

Enterprise-grade frontend for the Spring Boot payment gateway.

## Stack
- Next.js App Router + TypeScript
- Tailwind CSS + shadcn-style UI primitives
- TanStack Query + TanStack Table
- React Hook Form + Zod
- Recharts
- Axios client with auth/correlation interceptors

## Setup
1. `cd web`
2. `npm install`
3. Copy env: `cp .env.local.example .env.local` (or create manually)
4. `npm run dev`

## Environment Variables
- `NEXT_PUBLIC_API_BASE_URL` default: `http://localhost:8080`
- `NEXT_PUBLIC_ENABLE_MANUAL_TOKEN` default: `false` (set `true` only for local troubleshooting)

Example `.env.local`:
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_ENABLE_MANUAL_TOKEN=false
```

## Available Routes
- `/login`
- `/dashboard`
- `/users`
- `/wallets`
- `/wallets/[id]`
- `/transfers`
- `/payments`
- `/payments/[id]`
- `/webhooks`
- `/webhooks/[id]`
- `/ledger`
- `/audit-logs`
- `/developer-tools`
- `/settings`

## Backend Assumptions
- Backend is available at `http://localhost:8080`
- JWT auth endpoint exists at `/auth/login` and refresh endpoint at `/auth/refresh`
- Some endpoints may be missing; UI has safe fallback behavior and TODO notes in API layer

## Mock Token Mode
- Disabled by default for production posture.
- To enable locally, set `NEXT_PUBLIC_ENABLE_MANUAL_TOKEN=true`.

## Scripts
- `npm run dev`
- `npm run lint`
- `npm run build`
- `npm run test:unit`
- `npm run test:e2e`

## Docker
- Production image uses `web/Dockerfile`.
- Compose modes are documented in root `README.md`.
