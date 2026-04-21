.PHONY: up down wait-schema wait-app seed run run-test

MK_FILE := $(abspath $(lastword $(MAKEFILE_LIST)))
MK_DIR := $(dir $(MK_FILE))
ROOT_DIR := $(abspath $(MK_DIR)..)
ENV_FILE := $(MK_DIR)helpers/.envK6
SEED_FILE := $(MK_DIR)helpers/seed.sql
RUN_FILE := $(MK_DIR)run.js
COMPOSE_FILE := $(MK_DIR)helpers/docker-compose-k6.yaml
COMPOSE := docker compose -f "$(COMPOSE_FILE)"
WAIT_URL := http://localhost:52720/api/v1/users/is-email-available?email=healthcheck%40example.com

up:
	$(COMPOSE) up -d --wait

down:
	$(COMPOSE) down --remove-orphans

wait-schema:
	@set -e; \
	i=0; \
	until $(COMPOSE) exec -T store-auth-db psql -U k6 -d k6db -tAc "SELECT (to_regclass('public.users') IS NOT NULL) AND (to_regclass('public.refresh_tokens') IS NOT NULL)" | grep -q t; do \
		i=$$((i+1)); \
		if [ $$i -ge 90 ]; then \
			echo "Schema not ready: expected tables users and refresh_tokens"; \
			exit 1; \
		fi; \
		echo "Waiting for schema... ($$i/90)"; \
		sleep 1; \
	done

wait-app:
	@set -e; \
	i=0; \
	ok=0; \
	until [ $$ok -ge 5 ]; do \
		if curl -fsS "$(WAIT_URL)" > /dev/null; then \
			ok=$$((ok+1)); \
			echo "App HTTP endpoint ready check $$ok/5"; \
		else \
			ok=0; \
			i=$$((i+1)); \
			if [ $$i -ge 90 ]; then \
				echo "App HTTP endpoint not ready: $(WAIT_URL)"; \
				exit 1; \
			fi; \
			echo "Waiting for app HTTP endpoint... ($$i/90)"; \
		fi; \
		sleep 1; \
	done

seed:
	$(COMPOSE) exec -T store-auth-db psql -v ON_ERROR_STOP=1 -U k6 -d k6db < "$(SEED_FILE)"

run-test:
	cd "$(ROOT_DIR)" && set -a; . ./k6/helpers/.envK6; set +a; k6 run "$(RUN_FILE)"

run:
	@set -e; \
	trap '$(MAKE) -f "$(MK_FILE)" down' EXIT INT TERM; \
	$(MAKE) -f "$(MK_FILE)" up; \
	$(MAKE) -f "$(MK_FILE)" wait-schema; \
	$(MAKE) -f "$(MK_FILE)" seed; \
	$(MAKE) -f "$(MK_FILE)" wait-app; \
	$(MAKE) -f "$(MK_FILE)" run-test
