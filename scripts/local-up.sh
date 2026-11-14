#!/usr/bin/env bash
set -euo pipefail

docker compose up -d --build
echo "Wallet service: http://localhost:8080"
echo "Swagger:        http://localhost:8080/swagger-ui.html"
echo "Health:         http://localhost:8080/actuator/health"
