#!/usr/bin/env bash
# Compile l'amorce (EldoriaLauncher.jar) et le coeur (launcher-core.jar) du launcher dans $1 (defaut : launcher/build).
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-build}"
rm -rf "$OUT/classes" && mkdir -p "$OUT/classes/boot" "$OUT/classes/core"
javac --release 21 -encoding UTF-8 -d "$OUT/classes/boot" $(find bootstrap/src -name '*.java')
javac --release 21 -encoding UTF-8 -d "$OUT/classes/core" $(find core/src -name '*.java')
jar --create --file "$OUT/EldoriaLauncher.jar" --main-class eldoria.boot.Bootstrap -C "$OUT/classes/boot" .
jar --create --file "$OUT/launcher-core.jar" -C "$OUT/classes/core" .
echo "Jars : $OUT/EldoriaLauncher.jar $OUT/launcher-core.jar"
