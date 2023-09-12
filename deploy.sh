#!/bin/bash
docker build -f Dockerfile  -t htmx-table-test:latest . && docker compose up -d && docker logs -f htmx-table-test