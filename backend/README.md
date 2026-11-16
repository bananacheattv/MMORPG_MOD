# Back-end Aéloria : comptes + boutique de crédits

Petit service Node.js (aucune dépendance, Node 22.13 ou plus) qui relie le site, le launcher et le serveur Minecraft :

- **Comptes Aéloria** créés sur le site (`site/compte.html`), utilisés pour se connecter au launcher.
- **Liaison du personnage Minecraft** faite par le launcher au premier lancement, prouvée par le serveur de session Mojang (le jeton Minecraft du joueur n’est jamais envoyé au back-end).
- **Boutique de crédits** (`site/boutique.html`) : paiement Stripe Checkout, webhook signé, livraison des crédits au serveur par la passerelle `CreditBridge` du mod (HMAC, boucle locale, idempotente).
- **Mot de passe oublié** sans e-mail : depuis le launcher, la connexion Microsoft prouve qu’on possède le personnage lié.

**Paiements réels désactivés** : une clé Stripe `sk_live_…` est refusée tant que `ALLOW_LIVE_PAYMENTS=oui` n’est pas mis volontairement dans `.env`.

## Schéma

```
Site (Netlify) ──HTTPS──► back-end (cette machine) ──127.0.0.1:8099──► serveur MAIN (CreditBridge)
Launcher ───────HTTPS──►      │  ▲
                              │  └── webhook Stripe (paiement confirmé)
                              └────► Stripe Checkout (page de paiement)
```

Le back-end doit tourner **sur la même machine que le serveur MAIN** : la passerelle crédits n’écoute qu’en local.

## Installation (machine du serveur)

1. Installer Node.js 22 LTS ou plus récent.
2. `cd backend && cp .env.example .env`, puis remplir `.env` (voir ci-dessous).
3. Serveur Minecraft : dans `world/data/commerce/boutique.json`, mettre `"paymentBridgeEnabled": true` et `"websiteUrl": "https://<site>/boutique.html"`, puis définir la variable d’environnement `AELORIA_CREDIT_SECRET` (même valeur que dans `.env`, 32 caractères minimum) avant de lancer le serveur. Les quantités de crédits de `creditPacks` doivent correspondre à `packs.json`.
4. `npm start` (écoute sur 127.0.0.1:8787).
5. Exposer le back-end en HTTPS, par exemple :
   - **Cloudflare Tunnel** (gratuit, sans ouvrir de port) : `cloudflared tunnel --url http://127.0.0.1:8787`, puis un nom de domaine fixe (ex. `api.aeloria.fr`) ;
   - ou **Caddy** en proxy inverse (`api.aeloria.fr { reverse_proxy 127.0.0.1:8787 }`) avec le port 443 ouvert.
6. Site : dans `site/script.js`, `CONFIG.api.url = '/'` et activer le bloc proxy `/api/*` de `site/netlify.toml` (ou mettre l’adresse complète et l’ajouter à `connect-src` dans `compte.html` et `boutique.html`).
7. Launcher (`gfrpg-launcher/launcher-config.json`) : `"accountApi": "https://api.aeloria.fr"`, et `links.shop` / `links.account` vers les pages du site. Une nouvelle version du launcher doit ensuite être publiée.

## Stripe en mode test

1. Créer un compte Stripe, rester en **mode test**.
2. Développeurs → Clés API : copier la clé secrète `sk_test_…` dans `STRIPE_SECRET_KEY`.
3. Développeurs → Webhooks → Ajouter un endpoint : `https://api.aeloria.fr/api/stripe/webhook`, événements `checkout.session.completed`, `checkout.session.async_payment_succeeded`, `checkout.session.async_payment_failed`, `checkout.session.expired`, `charge.refunded`. Copier le secret `whsec_…` dans `STRIPE_WEBHOOK_SECRET`.
4. Carte de test : `4242 4242 4242 4242`, date future, CVC au choix.

## Variables (`.env`)

| Variable | Rôle |
|---|---|
| `PUBLIC_SITE_URL` | Adresse du site (CORS + retours de paiement) |
| `HOST`, `PORT` | Écoute locale (127.0.0.1:8787) |
| `TRUST_PROXY` | `oui` derrière un tunnel ou un proxy (limitation des tentatives par IP) |
| `DATABASE_FILE` | Base SQLite (comptes, sessions, commandes) : à sauvegarder |
| `PACKS_FILE` | Packs vendus : nom, crédits, prix en centimes |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe (sans clé : boutique fermée, comptes actifs) |
| `ALLOW_LIVE_PAYMENTS` | `oui` seulement pour accepter de vrais paiements |
| `AELORIA_CREDIT_SECRET` | Secret partagé avec le serveur Minecraft |
| `CREDIT_BRIDGE_URL` | Passerelle du serveur MAIN (http://127.0.0.1:8099) |

## API

| Route | Usage |
|---|---|
| `POST /api/auth/register` | Inscription (pseudo, e-mail, mot de passe ≥ 10, règlement accepté) |
| `POST /api/auth/login` · `POST /api/auth/logout` | Session (jeton Bearer, 30 jours site, 180 jours launcher) |
| `GET /api/account` | Compte, personnage lié, solde de crédits (lu sur le serveur), commandes |
| `POST /api/account/password` | Changement de mot de passe (déconnecte les autres sessions) |
| `POST /api/account/minecraft/challenge` puis `POST /api/account/minecraft` | Liaison du personnage (launcher) |
| `POST /api/auth/minecraft-challenge` puis `POST /api/auth/reset-with-minecraft` | Mot de passe oublié (launcher) |
| `GET /api/shop/packs` · `POST /api/shop/checkout` · `GET /api/shop/orders` | Boutique |
| `POST /api/stripe/webhook` | Confirmation de paiement Stripe |

Sécurité : mots de passe scrypt, jetons stockés hachés, CORS limité au site, limitation des tentatives, webhook vérifié (signature + montant + session), commande livrée une seule fois (référence idempotente côté serveur), nouvel essai automatique si le serveur Minecraft est arrêté. Un remboursement Stripe marque la commande « remboursée » mais ne retire pas les crédits : à traiter à la main en jeu.

## Tests

`npm test` : comptes, liaison Minecraft, mot de passe oublié, achat de bout en bout (Stripe et Mojang simulés), livraison différée, montant incohérent, CORS, refus des clés live.
