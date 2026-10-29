#!/usr/bin/env bash
# F2 (capture) 4 s après chaque « VISITE » de PNJ journalisée par le serveur (à lancer dans le même DISPLAY que le client)
n=0
while true; do
  c=$(grep -ac 'SPAWN NPC CHECKS\] VISITE' /tmp/mp-server.log 2>/dev/null)
  if [ "${c:-0}" -gt "$n" ]; then n=$c; sleep 4; xdotool key F2; fi
  sleep 1
done
