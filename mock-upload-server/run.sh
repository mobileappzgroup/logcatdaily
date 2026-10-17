#!/bin/sh
# Builds and starts the mock upload server on :8083. Extra args go to the server (--keep-partial).
set -e
cd "$(dirname "$0")/.."
./gradlew -q :mock-upload-server:installDist
exec mock-upload-server/build/install/mock-upload-server/bin/mock-upload-server "$@"
