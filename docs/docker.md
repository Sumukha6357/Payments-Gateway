# Docker Guide

This repository uses a root-level canonical Docker setup:

- Dockerfiles: `api/Dockerfile`, `web/Dockerfile`
- API entrypoint: `api/entrypoint.sh`
- Compose base: `docker-compose.yml`
- Compose overlays: `docker-compose.local.yml`, `docker-compose.dev.yml`, `docker-compose.prod.yml`
- Central env: root `.env` and `.env.example`

## Run Commands

Local developer mode:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Team dev-like mode:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

Prod-like mode:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## How Migrations Run

Migrations run automatically via the `flyway` service in `docker-compose.yml`.

- Migration source path: `api/src/main/resources/db/migration`
- Flyway command: `migrate`
- API startup dependency:
  `api` waits for `postgres` health and `flyway` completion before starting.

To add a migration, create a new file under:

`api/src/main/resources/db/migration`

on next stack start, Flyway applies pending migrations automatically.

## Notes

- Spring JPA DDL mode is forced to `validate` in container env.
- Root compose is the single source of truth.
