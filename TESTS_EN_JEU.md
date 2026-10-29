# Compiler et tester en jeu (session cloud sans écran)

1. **JDK 25** : `mkdir -p /opt/jdk25 && curl -sSL https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz | tar xz -C /opt/jdk25 --strip-components=1` ; puis `export JAVA_HOME=/opt/jdk25`. Gradle : `./gradlew -Dorg.gradle.java.installations.paths=/opt/jdk25 …` (le wrapper télécharge Gradle).
2. **Rendu** : `apt-get install -y xvfb xdotool libegl1 libgl1-mesa-dri` (client dans Xvfb avec Mesa llvmpipe, déjà réglé dans les scripts).
3. **Compiler + tests** : `./gradlew … compileJava test`, puis tests serveur : `./gradlew … runServer -PnetworkChecks -PsharedChecks -PfishingChecks -PweeklyChecks` (chercher `PASS`, aucune `Exception caught`).
4. **Captures solo** (écrans) : `tools/screens/capture.sh ecrans3d peche …` → `docs/screens/<mode>/`. Les modes obsolètes (`metiers`, `quetes`) ne marchent plus tels quels.
5. **Test multijoueur réel** (vrai joueur dans un vrai monde) : `T=600 tools/screens/mp.sh` lance un serveur dédié avec les vérifications (`/tmp/mp-server.log`) et un client Xvfb connecté (`/tmp/mp-client.log`). Il vérifie les PNJ de quêtes du spawn (posés, espacés, au sol), visite chacun et prend une capture F2 (`run/screenshots/`). Monde neuf : déplacer `run/world`.
6. Ne jamais compiler pendant qu'un serveur ou client tourne. Arrêter les processus Java de jeu avant de relancer.

Pièges connus : `run/ops.json` doit contenir `VisualChecks` (le script s'en charge) ; la liste blanche est désactivée par le script ; le joueur de test ne doit pas être mort ou monté pour être téléporté.
