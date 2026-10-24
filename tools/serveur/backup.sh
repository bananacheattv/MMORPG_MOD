#!/usr/bin/env bash
# Sauvegarde d'un serveur Aeloria : le monde et les reglages du mod, en une archive datee, avec rotation.
#
# usage : backup.sh <dossier_du_serveur> <dossier_des_sauvegardes> [nombre_a_garder=14]
#
# A lancer serveur ARRETE (ex. : juste apres le redemarrage quotidien), ou apres « save-off » puis « save-all flush » dans la console
# (et « save-on » ensuite) : copier un monde en cours d'ecriture peut donner une sauvegarde inutilisable.
# Sauvegarde : world/ (monde et donnees des joueurs), config/mmorpg/ (reglages, admin.json compris), server.properties, ops, whitelist, bans.
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
tar --exclude='world/session.lock' -czf "$FILE" "${FILES[@]}"

# verification : l'archive doit se relire en entier
tar -tzf "$FILE" > /dev/null
echo "Sauvegarde : $FILE ($(du -h "$FILE" | cut -f1))"

# rotation : on ne garde que les plus recentes
ls -1t "$DEST"/aeloria-*.tar.gz 2>/dev/null | tail -n +"$((KEEP + 1))" | while read -r old; do
  rm -f -- "$old"
  echo "Ancienne sauvegarde supprimee : $old"
done
