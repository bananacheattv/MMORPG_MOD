# Pare-feu et protection du réseau (Velocity + serveurs + MariaDB)

Principe : **seul le port du proxy Velocity (25565 TCP) est joignable depuis Internet.** Les serveurs de jeu (25570 à 25574), MariaDB (3306) et RCON (25575) ne répondent qu'à la machine elle-même.

Ports du réseau Aeloria : proxy 25565, serveur 25570, hub 25571, mines 25572, plaines 25573, donjons 25574, MariaDB 3306.

## 1. Pare-feu du système

### Linux (ufw)
```
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp            # SSH (ou votre port SSH)
sudo ufw allow 25565/tcp         # Velocity uniquement
sudo ufw enable
sudo ufw status verbose
```
Tout le reste (25570-25574, 3306, 25575) est refusé par la règle par défaut. Si les serveurs sont sur une autre machine que le proxy, n'autoriser que l'IP du proxy :
```
sudo ufw allow from IP_DU_PROXY to any port 25570:25574 proto tcp
sudo ufw allow from IP_DU_PROXY to any port 3306 proto tcp   # seulement si la base est ailleurs
```

### Linux (iptables)
```
sudo iptables -P INPUT DROP
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 25565 -j ACCEPT
# limite par IP : 5 connexions simultanées et 6 nouvelles connexions par 10 s
sudo iptables -I INPUT -p tcp --dport 25565 -m connlimit --connlimit-above 5 --connlimit-mask 32 -j DROP
sudo iptables -I INPUT -p tcp --dport 25565 -m conntrack --ctstate NEW -m recent --set --name mc
sudo iptables -I INPUT -p tcp --dport 25565 -m conntrack --ctstate NEW -m recent --update --seconds 10 --hitcount 6 --name mc -j DROP
sudo apt install iptables-persistent && sudo netfilter-persistent save
```

### Windows (Pare-feu Windows Defender, PowerShell administrateur)
```
# autoriser seulement Velocity
New-NetFirewallRule -DisplayName "Aeloria Velocity" -Direction Inbound -Protocol TCP -LocalPort 25565 -Action Allow
# bloquer les ports internes depuis l'extérieur (le trafic local 127.0.0.1 n'est pas filtré)
New-NetFirewallRule -DisplayName "Aeloria interne (bloqué)" -Direction Inbound -Protocol TCP -LocalPort 25570-25575,3306 -Action Block
# vérifier
Get-NetFirewallRule -DisplayName "Aeloria*" | Get-NetFirewallPortFilter
```
Attention : une règle Block l'emporte sur Allow. Ne jamais bloquer 25565. Si une règle Allow « Java » existe déjà pour tous les ports, la supprimer : `Get-NetFirewallRule -DisplayName "*Java*"` puis `Remove-NetFirewallRule`.

Box / routeur : rediriger (NAT) **uniquement** 25565 TCP vers la machine du proxy.

## 2. Écoute locale uniquement (en plus du pare-feu)
- Serveurs de jeu : `server.properties` → `server-ip=127.0.0.1` (si le proxy est sur la même machine), `enable-rcon=false`, `query.port` inutilisé.
- MariaDB : `bind-address=127.0.0.1` dans `my.cnf` / `my.ini`, utilisateur `aeloria@localhost` (pas `%`), mot de passe fort et changé.

## 3. Velocity : identité et authentification
`velocity.toml` :
```
online-mode = true
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"   # fichier secret, jamais publié
```
Chaque serveur de jeu : `online-mode=false` dans `server.properties`, et le même secret côté NeoForge (mod/config de forwarding moderne Velocity, ou `config/neoforge-server.toml` selon la version du loader) ; sans secret, le serveur refuse les connexions. Le serveur de jeu ne doit être joignable que par le proxy (section 1), sinon son `online-mode=false` permet l'usurpation de pseudo, de profil, d'or et de banque.

Réglages anti-bot utiles dans `velocity.toml` :
```
connection-timeout = 5000
read-timeout = 30000
login-ratelimit = 3000          # ms entre deux connexions depuis la même IP
haproxy-protocol = false
```
Avec un tunnel/hébergeur qui masque l'IP réelle, activer le protocole PROXY seulement si le fournisseur l'impose, et limiter alors les connexions entrantes à ses adresses.

## 4. Contre les attaques de bots et DDoS
- Plugin Velocity anti-bot (par ex. **Sonar** ou **BotSentry**) : vérification des nouvelles connexions, liste d'IP bloquées, mode attaque.
- Fail2ban pour SSH : `sudo apt install fail2ban`.
- DDoS volumétrique : seul l'hébergeur ou un service de protection (tunnel avec filtrage) peut l'absorber ; un pare-feu local ne suffit pas. Cacher l'IP réelle des serveurs de jeu et de la base.

## 5. Vérifier
Depuis une autre machine (pas le serveur) :
```
nmap -Pn -p 25565,25570-25575,3306 IP_PUBLIQUE
```
Attendu : 25565 ouvert, tous les autres `filtered` ou `closed`. Puis se connecter en jeu via le proxy et passer hub → mines → plaines pour confirmer que les transferts fonctionnent.
