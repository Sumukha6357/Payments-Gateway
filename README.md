# Payment Gateway

- Backend: `api/` (Spring Boot + Flyway)
- Frontend: `web/` (Next.js)

## Canonical Docker Setup

Use the root-level compose stack only:

- `docker-compose.yml`
- `docker-compose.local.yml`
- `docker-compose.dev.yml`
- `docker-compose.prod.yml`

Detailed docs: `docs/docker.md`

### Local

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

### Dev-like

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

### Prod-like

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## Environment

Root env files:

- `.env` (active values for local)
- `.env.example` (template for new environments)
