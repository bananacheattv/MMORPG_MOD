-- Schema de la base Aeloria (MariaDB / MySQL, utf8mb4).
-- Le mod cree et met a jour ces tables tout seul au demarrage (db/Database.java, « schema_version ») : ce fichier sert de reference
-- pour le panel web, les statistiques et les sauvegardes. Ne le lancez pas a la main sur une base deja utilisee.

CREATE DATABASE IF NOT EXISTS aeloria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- compte du mod (adaptez le mot de passe ; le panel en genere un dans servers/db/init.sql) :
-- CREATE USER IF NOT EXISTS 'aeloria'@'127.0.0.1' IDENTIFIED BY '<mot de passe>';
-- GRANT ALL PRIVILEGES ON aeloria.* TO 'aeloria'@'127.0.0.1';

CREATE TABLE IF NOT EXISTS aeloria_meta (
  k VARCHAR(64) NOT NULL PRIMARY KEY,
  v VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Un profil par joueur (cle : UUID). `data` est le profil complet en texte (SNBT) ; les autres colonnes en sont des copies
-- pour pouvoir chercher et trier. `version` augmente a chaque ecriture ; `server_id` / `online` disent quel serveur tient le joueur.
CREATE TABLE IF NOT EXISTS players (
  uuid CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  class VARCHAR(32) NOT NULL,
  level INT NOT NULL,
  xp BIGINT NOT NULL,
  gold BIGINT NOT NULL,
  mines_unlocked INT NOT NULL,
  plains_unlocked INT NOT NULL,
  evolution INT NOT NULL,
  data MEDIUMTEXT NOT NULL,
  inventory MEDIUMTEXT NULL,                  -- schema v2 : inventaire du profil
  version BIGINT NOT NULL DEFAULT 1,
  server_id VARCHAR(32) NOT NULL,
  online TINYINT(1) NOT NULL DEFAULT 0,
  first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_players_name (name),
  KEY idx_players_level (level),
  KEY idx_players_server (server_id, online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Compte bancaire du joueur : or en banque et coffre de 3 pages (texte SNBT).
CREATE TABLE IF NOT EXISTS bank_accounts (
  uuid CHAR(36) NOT NULL PRIMARY KEY,
  gold BIGINT NOT NULL,
  data MEDIUMTEXT NOT NULL,
  version BIGINT NOT NULL DEFAULT 1,
  updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Journal : une ligne par evenement. kind : login, logout, level_up, death, boss_kill, dungeon_win, bank_deposit, bank_withdraw,
-- auction_list, auction_buy, auction_sale, shop_buy, shop_sell, quest_done, unlock, mount, admin.
CREATE TABLE IF NOT EXISTS events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ts TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  server_id VARCHAR(32) NOT NULL,
  uuid CHAR(36) NULL,
  name VARCHAR(32) NULL,
  kind VARCHAR(32) NOT NULL,
  amount BIGINT NULL,
  detail VARCHAR(512) NULL,
  KEY idx_events_player (uuid, ts),
  KEY idx_events_kind (kind, ts)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Transferts en attente entre serveurs (schema v2) : serveur de destination et type de voyage d'un joueur.
CREATE TABLE IF NOT EXISTS transfers (
  uuid CHAR(36) NOT NULL PRIMARY KEY,
  dest_server VARCHAR(32) NOT NULL,
  kind VARCHAR(16) NOT NULL,
  arg INT NOT NULL DEFAULT 0,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Documents JSON partages (guildes, hotel des ventes, registres de coordination, groupes) : cree automatiquement par le mod.
CREATE TABLE IF NOT EXISTS shared_docs (
  name VARCHAR(64) NOT NULL PRIMARY KEY,
  data MEDIUMTEXT NOT NULL,
  revision BIGINT NOT NULL DEFAULT 1,
  updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------ requetes utiles
-- Joueurs en ligne sur le reseau :
--   SELECT name, class, level, server_id FROM players WHERE online = 1 AND last_seen > NOW() - INTERVAL 5 MINUTE;
-- Classement par niveau :
--   SELECT name, class, level, xp FROM players ORDER BY level DESC, xp DESC LIMIT 20;
-- Or total en circulation (porte-monnaie + banques) :
--   SELECT (SELECT COALESCE(SUM(gold),0) FROM players) + (SELECT COALESCE(SUM(gold),0) FROM bank_accounts) AS or_total;
-- Historique d'un joueur :
--   SELECT ts, server_id, kind, amount, detail FROM events WHERE name = 'Pseudo' ORDER BY id DESC LIMIT 100;
-- Ventes a l'hotel des ventes de la semaine :
--   SELECT ts, name, amount, detail FROM events WHERE kind = 'auction_sale' AND ts > NOW() - INTERVAL 7 DAY ORDER BY ts DESC;
-- Purge du journal de plus de 90 jours (a planifier) :
--   DELETE FROM events WHERE ts < NOW() - INTERVAL 90 DAY;

-- Classements des panneaux (schema v3) : une ligne par joueur et par classement (board : niveau, fortune, monstres, boss, donjons, quetes, pvp_victoires, pvp_elims).
CREATE TABLE IF NOT EXISTS leaderboard_stats (
  board VARCHAR(32) NOT NULL,
  uuid CHAR(36) NOT NULL,
  name VARCHAR(64) NOT NULL,
  value BIGINT NOT NULL DEFAULT 0,
  extra BIGINT NOT NULL DEFAULT 0,
  updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (board, uuid),
  KEY idx_lb_rank (board, value, extra)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
