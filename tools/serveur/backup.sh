#!/usr/bin/env bash
# Sauvegarde d'un serveur Aeloria : le monde et les reglages du mod, en une archive datee, avec rotation.
#
# usage : backup.sh <dossier_du_serveur> <dossier_des_sauvegardes> [nombre_a_garder=14]
#
# A lancer serveur ARRETE (ex. : juste apres le redemarrage quotidien), ou apres « save-off » puis « save-all flush » dans la console
# (et « save-on » ensuite) : copier un monde en cours d'ecriture peut donner une sauvegarde inutilisable.
# Sauvegarde : world/ (monde et donnees des joueurs), config/mmorpg/ (reglages, admin.json compris), server.properties, ops, whitelist, bans.
# Reseau actif (config/mmorpg/network.json, "enabled": true) : aussi le dossier partage entre les serveurs (parent de sharedDirectory :
# personnages, guildes, hotel des ventes...), range dans l'archive sous son propre nom. Le lancer alors serveurs du reseau arretes.
set -euo pipefail

SERVER="${1:?usage : backup.sh <dossier_du_serveur> <dossier_des_sauvegardes> [nombre_a_garder]}"
DEST="${2:?usage : backup.sh <dossier_du_serveur> <dossier_des_sauvegardes> [nombre_a_garder]}"
KEEP="${3:-14}"

[ -d "$SERVER/world" ] || { echo "Pas de dossier world dans $SERVER" >&2; exit 1; }
mkdir -p "$DEST"
FILE="$DEST/aeloria-$(date +%Y%m%d-%H%M%S).tar.gz"

# une sauvegarde qui echoue (ex. : monde modifie pendant la copie) ne laisse pas d'archive partielle
trap 'rm -f -- "$FILE"' ERR

cd "$SERVER"
FILES=(world config/mmorpg)
for f in server.properties ops.json whitelist.json banned-players.json banned-ips.json; do
  [ -e "$f" ] && FILES+=("$f")
done
# dossier partage du reseau : chemin relatif au dossier du serveur, comme le fait le mod
SHARED=()
NET=config/mmorpg/network.json
if [ -f "$NET" ] && grep -Eq '"enabled"[[:space:]]*:[[:space:]]*true' "$NET"; then
  DIR=$(sed -n 's/.*"sharedDirectory"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$NET" | head -n 1)
  DIR="${DIR:-../../shared/players}"
  case "$DIR" in /*) ;; *) DIR="$PWD/$DIR" ;; esac
  if PARENT=$(cd "$DIR/.." 2>/dev/null && pwd); then
    SHARED=(-C "$(dirname "$PARENT")" "$(basename "$PARENT")")
    echo "Reseau actif : dossier partage $PARENT inclus"
  else
    echo "Reseau actif mais dossier partage introuvable ($DIR) : non sauvegarde" >&2
  fi
fi
tar --exclude='world/session.lock' -czf "$FILE" "${FILES[@]}" ${SHARED[@]+"${SHARED[@]}"}

# verification : l'archive doit se relire en entier
tar -tzf "$FILE" > /dev/null
echo "Sauvegarde : $FILE ($(du -h "$FILE" | cut -f1))"

# rotation : on ne garde que les plus recentes
ls -1t "$DEST"/aeloria-*.tar.gz 2>/dev/null | tail -n +"$((KEEP + 1))" | while read -r old; do
  rm -f -- "$old"
  echo "Ancienne sauvegarde supprimee : $old"
done
