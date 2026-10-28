#!/usr/bin/env bash
# Regénère les captures du mod (client Xvfb + rendu logiciel Mesa).
# Usage : tools/screens/capture.sh [mode ...]   (défaut : liste de modes ci-dessous)
# Prérequis : JDK 25, xvfb, libegl1, libgl1-mesa-dri. Sortie : docs/screens/*.png
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
JDK="${JDK25:-/opt/jdk25}"
MODES=("$@"); [ ${#MODES[@]} -eq 0 ] && MODES=(ecrans onglets competences reprise_tab reprise_quetes armes social_ui services pvp_polish)
export JAVA_HOME="$JDK" XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/tmp/xdg}" LIBGL_ALWAYS_SOFTWARE=1 \
  MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 SDL_VIDEO_FORCE_EGL=1
mkdir -p "$XDG_RUNTIME_DIR" run docs/screens; chmod 700 "$XDG_RUNTIME_DIR"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dneoforge.disableBlaze3DValidation=true"
grep -q '^onboardAccessibility' run/options.txt 2>/dev/null && sed -i 's/^onboardAccessibility:.*/onboardAccessibility:false/' run/options.txt || echo onboardAccessibility:false >> run/options.txt
G="./gradlew -Dorg.gradle.java.installations.paths=$JDK"
# Monde solo « dev » : copie du monde du serveur de dev (run/world) s'il n'existe pas
[ -d run/saves/dev ] || { mkdir -p run/saves; cp -r run/world run/saves/dev; }
for m in "${MODES[@]}"; do
  echo "== $m"
  rm -f run/screenshots/showcase_*.png
  # xdotool valide l'avertissement « paramètres expérimentaux » du monde au premier chargement (bouton 798,430)
  timeout 900 xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" bash -c \
    "(for i in \$(seq 1 12); do sleep 15; xdotool mousemove 798 430 click 1 2>/dev/null; done) & \
     $G runClient -PquickWorld=dev -Pusername=VisualChecks -Pshowcase=$m" > /tmp/screens-client-$m.log 2>&1
  mkdir -p docs/screens/$m && cp run/screenshots/showcase_*.png docs/screens/$m/ 2>/dev/null || echo "  (aucune capture pour $m)"
done
