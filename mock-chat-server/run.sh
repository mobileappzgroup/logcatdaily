#!/bin/sh
# Builds and starts the mock server on :8080. Extra args go to the server,
# e.g. ./mock-chat-server/run.sh --drop-next-reply
set -e
cd "$(dirname "$0")/.."
./gradlew -q :mock-chat-server:installDist
exec mock-chat-server/build/install/mock-chat-server/bin/mock-chat-server "$@"
