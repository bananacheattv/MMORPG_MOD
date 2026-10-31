#!/usr/bin/env bash
# Test multijoueur local : serveur dédié (checks activés) + client Xvfb/Mesa qui s'y connecte.
# Usage : tools/screens/mp.sh [mode_showcase]   Logs : /tmp/mp-server.log, /tmp/mp-client.log (arrêt auto après $T s, défaut 300)
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; cd "$ROOT"
JDK="${JDK25:-/opt/jdk25}"; export JAVA_HOME="$JDK" XDG_RUNTIME_DIR=/tmp/xdg LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 SDL_VIDEO_FORCE_EGL=1
mkdir -p run /tmp/xdg; chmod 700 /tmp/xdg
sed -i 's/^white-list=.*/white-list=false/;s/^enforce-whitelist=.*/enforce-whitelist=false/' run/server.properties 2>/dev/null
G="./gradlew -Dorg.gradle.java.installations.paths=$JDK"
$G classes --console=plain -q >/dev/null 2>&1   # compiler AVANT de lancer quoi que ce soit
CH="-Dmmorpg.checkQuests=true -Dmmorpg.checkLoot=true -Dmmorpg.checkInventory=true -Dmmorpg.checkQuestEditor=true -Dmmorpg.checkSpawnNpcs=true -Dmmorpg.checkTutorial=true"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $CH" timeout "${T:-300}" $G runServer --console=plain > /tmp/mp-server.log 2>&1 &
for i in $(seq 1 60); do grep -aq 'Done (' /tmp/mp-server.log && break; sleep 3; done
SC=""; [ -n "${1:-}" ] && SC="-Pshowcase=$1"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dneoforge.disableBlaze3DValidation=true" timeout "${T:-300}" xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" \
  bash -c "$ROOT/tools/screens/watch_visites.sh & $G runClient -PmpServer=127.0.0.1:25565 -Pusername=VisualChecks $SC" > /tmp/mp-client.log 2>&1
wait
