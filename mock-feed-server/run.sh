#!/bin/sh
# Builds and starts the mock feed server on :8081.
set -e
cd "$(dirname "$0")/.."
./gradlew -q :mock-feed-server:installDist
exec mock-feed-server/build/install/mock-feed-server/bin/mock-feed-server "$@"
