#!/usr/bin/env sh
# Gera o seed local (backend/src/main/resources/seed/) pra rodar o backend fora
# do Docker. O build Docker faz isso sozinho. Argumentos extras vão pro ETL.
set -eu
cd "$(dirname "$0")/.."
exec python3 tools/etl/generate_seed.py "$@"
