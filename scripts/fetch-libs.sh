#!/usr/bin/env bash
# Fetch third-party jars required to build MAT.
# Requires: gh (GitHub CLI), authenticated.
#
# Run from project root:  ./scripts/fetch-libs.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIBS_DIR="$ROOT_DIR/libs"
BARITONE_VERSION="v1.10.4"

mkdir -p "$LIBS_DIR"

echo "Fetching Baritone $BARITONE_VERSION (Fabric, MC 1.20.4) into libs/ ..."
gh release download "$BARITONE_VERSION" \
    --repo cabaletta/baritone \
    --pattern "baritone-api-fabric-1.10.4.jar" \
    --pattern "baritone-standalone-fabric-1.10.4.jar" \
    --dir "$LIBS_DIR" \
    --clobber

echo "Done. Contents of libs/:"
ls -la "$LIBS_DIR"
