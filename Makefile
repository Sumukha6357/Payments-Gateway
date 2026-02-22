SHELL := /bin/sh

# ==============================
# Project Docker Operations
# ==============================

DC = docker compose
BASE = -f docker-compose.yml
DEV = -f docker-compose.dev.yml
PROD = -f docker-compose.prod.yml
LOCAL = -f docker-compose.local.yml

# Default environment (dev|prod|local)
ENV ?= dev
SERVICE ?= api

ifeq ($(ENV),prod)
	ENV_FILE = $(PROD)
else ifeq ($(ENV),local)
	ENV_FILE = $(LOCAL)
else
	ENV_FILE = $(DEV)
endif

# ==============================
# ENVIRONMENTS
# ==============================

dev:
	$(DC) $(BASE) $(DEV) up -d --build

prod:
	$(DC) $(BASE) $(PROD) up -d --build

local:
	$(DC) $(BASE) $(LOCAL) up -d --build

run:
	$(DC) $(BASE) $(ENV_FILE) up -d --build

# ==============================
# BASIC OPERATIONS
# ==============================

up:
	$(DC) $(BASE) up -d

build:
	$(DC) $(BASE) build

down:
	$(DC) $(BASE) down

stop:
	$(DC) $(BASE) stop

start:
	$(DC) $(BASE) start

restart:
	$(DC) $(BASE) down
	$(DC) $(BASE) up -d --build

# ==============================
# LOGGING & DEBUGGING
# ==============================

logs:
	$(DC) $(BASE) logs -f

logs-dev:
	$(DC) $(BASE) $(DEV) logs -f

logs-prod:
	$(DC) $(BASE) $(PROD) logs -f

logs-local:
	$(DC) $(BASE) $(LOCAL) logs -f

logs-api:
	$(DC) $(BASE) logs -f api

logs-web:
	$(DC) $(BASE) logs -f web

ps:
	$(DC) $(BASE) ps

exec:
	$(DC) $(BASE) exec $(SERVICE) sh

exec-api:
	$(DC) $(BASE) exec api sh

exec-web:
	$(DC) $(BASE) exec web sh

# ==============================
# CLEAN & REBUILD
# ==============================

rebuild:
	$(DC) $(BASE) down --remove-orphans
	$(DC) $(BASE) up -d --build

rebuild-no-cache:
	$(DC) $(BASE) build --no-cache
	$(DC) $(BASE) up -d

reset:
	$(DC) $(BASE) down --rmi local --volumes --remove-orphans

clean:
	docker system prune -f

clean-all:
	docker system prune -a --volumes -f

# ==============================
# IMAGE OPERATIONS
# ==============================

images:
	docker images

pull:
	$(DC) $(BASE) pull

push:
	$(DC) $(BASE) push

# ==============================
# HEALTH CHECK
# ==============================

status:
	$(DC) $(BASE) ps

top:
	$(DC) $(BASE) top

stats:
	docker stats

# ==============================
# HELP
# ==============================

help:
	@echo "Usage:"
	@echo "  make run ENV=dev|prod|local"
	@echo "  make exec SERVICE=api|web|postgres|redis"
	@echo "  make logs-api | make logs-web"
	@echo ""
	@echo "Main targets:"
	@echo "  dev prod local run up build down stop start restart"
	@echo "  logs logs-dev logs-prod logs-local logs-api logs-web"
	@echo "  ps exec exec-api exec-web"
	@echo "  rebuild rebuild-no-cache reset clean clean-all"
	@echo "  images pull push status top stats"

.PHONY: dev prod local run up build down stop start restart logs logs-dev logs-prod logs-local logs-api logs-web ps exec exec-api exec-web rebuild rebuild-no-cache reset clean clean-all images pull push status top stats help
