#!/usr/bin/env sh
# Format check and tests for the backend. Run from anywhere.
set -e
cd "$(dirname "$0")/../../backend"
./mvnw -B spotless:check test
