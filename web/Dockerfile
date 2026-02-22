# syntax=docker/dockerfile:1.7

FROM node:20-bookworm-slim AS deps
WORKDIR /workspace/web
COPY web/package.json web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
  npm ci

FROM node:20-bookworm-slim AS builder
WORKDIR /workspace/web
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /workspace/web/node_modules ./node_modules
COPY web/ ./
RUN npm run build

FROM node:20-bookworm-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN useradd -r -u 10001 -m appuser
COPY --from=builder /workspace/web/public ./public
COPY --from=builder /workspace/web/.next/standalone ./
COPY --from=builder /workspace/web/.next/static ./.next/static
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 3000
CMD ["node", "server.js"]
