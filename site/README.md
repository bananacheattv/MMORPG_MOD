# Site officiel d’Aéloria

Site vitrine one-page (HTML/CSS/JS vanilla, sans dépendance ni build).
Objectif : faire passer un visiteur du site au jeu en moins de 2 minutes.

```
site/
├── index.html          page unique (9 sections + appel final)
├── style.css           styles (BEM, custom properties, container queries)
├── script.js           CONFIG + interactions
├── netlify.toml        en-têtes et cache pour Netlify
├── robots.txt / sitemap.xml / site.webmanifest
├── assets/fonts/       Cinzel + Inter (WOFF2 variables, licence OFL)
├── assets/img/         images générées (AVIF/WebP/PNG/JPEG)
└── outils/generer_assets.py   regénère assets/img depuis les illustrations du mod
```

## Modifier le contenu

Tout se change dans l’objet `CONFIG`, en haut de `script.js` :

| Clé | Rôle |
|---|---|
| `slogan` | Slogan (hero, appel final, pied de page) |
| `server.address` | Adresse publique du serveur. **Vide** = « Connexion via le launcher » ; remplie = compteur de joueurs en temps réel (api.mcsrvstat.us) + bouton « Copier l’IP » |
| `game.minecraftVersion`, `game.loader` | Version requise affichée partout |
| `launcher.url`, `launcher.platform` | Lien de téléchargement du launcher (doit être **public**) |
| `links.discord` | Invitation Discord |
| `socials` | Réseaux supplémentaires du pied de page |
| `news`, `newsCount` | Actualités (les 3 plus récentes s’affichent) |
| `staff` | Membres du staff (`skin` = pseudo Minecraft, tête via mc-heads.net) |
| `legal.host` | Hébergeur cité dans les mentions légales |

Les classes, le règlement et la FAQ sont rédigés directement dans `index.html` (meilleur pour le SEO ; la FAQ porte le balisage schema.org `FAQPage`).

## Assets

Tous les visuels viennent des illustrations déjà présentes dans le mod ; rien n’est à fournir pour mettre le site en ligne.
Pour les regénérer après avoir changé une source : `python3 site/outils/generer_assets.py` (depuis la racine du dépôt, `pip install pillow fonttools brotli`).

| Rôle | Fichier(s) `assets/img/` | Dimensions exactes | Format | Source |
|---|---|---|---|---|
| Logo (hero) | `logo-560`, `logo-840`, `logo-1120` | 560×231, 840×346, 1120×461 | AVIF + WebP transparents | `art/logo/aeloria_logo.png` |
| Emblème (menu, pied de page, staff) | `embleme-96`, `embleme-256` | 96×96, 256×256 | WebP transparent | `art/logo/aeloria_emblem.png` |
| Favicon | `favicon-32.png` | 32×32 | PNG | emblème |
| Icône iOS | `apple-touch-icon.png` | 180×180 | PNG, fond #0B0E14 | emblème |
| Icônes d’application | `icon-192.png`, `icon-512.png` | 192×192, 512×512 | PNG, fond #0B0E14 | emblème |
| Fond du hero | `hero-citadelle-960`, `hero-citadelle-1672` | 960×540, 1672×941 | AVIF + WebP | `textures/gui/brand/citadel_twilight.png` |
| Univers (3 blocs) | `univers-seuil-*`, `univers-terres-brisees-*`, `univers-malvorn-*` | 480×270 et 836×470 | AVIF + WebP | `textures/gui/illustrations/` (portail, arène de feu, crypte) |
| Classes (4 portraits) | `classe-guerrier/mage/archer/tank-480` | 480×780 | AVIF + WebP | `textures/gui/illustrations/classes_atlas.png` (découpé) |
| Partage (Open Graph / Twitter) | `og-aeloria.jpg` | 1200×630 | JPEG | généré (citadelle + logo + slogan) |
| Icônes de classes et d’interface | sprite SVG dans `index.html` | viewBox 24×24 | SVG inline | dessinées pour le site |

**Optionnel, pour encore plus de netteté :**

| À fournir | Dimensions | Pourquoi |
|---|---|---|
| Fond du hero en HD (même cadrage, citadelle vers 65 % de la largeur) | 2560×1440 (idéal 3840×2160) | La source actuelle (1672 px) est agrandie sur écrans QHD/4K |
| Portraits de classes en HD | 720×1170 chacun | 480 px est un peu doux sur écrans Retina |
| Logo vectoriel | SVG | Netteté parfaite à toutes les tailles |

## Mise en ligne

Le site est statique : il suffit de publier le dossier `site/`. Avant de publier, si l’adresse finale n’est pas `https://aeloria.netlify.app/`, remplacez-la (balises SEO/partage, robots, sitemap) :

```bash
cd site
sed -i 's#https://aeloria.netlify.app#https://VOTRE-ADRESSE#g' index.html robots.txt sitemap.xml
```

### Option A — Netlify (recommandé : en-têtes de sécurité et cache inclus)

1. app.netlify.com → **Add new site → Import an existing project → GitHub** → dépôt `mmorpg_mod`.
2. **Base directory : `site`** ; Build command : vide ; Publish directory : `site` (déjà fixé par `site/netlify.toml`).
3. **Deploy**, puis *Site configuration → Change site name* → `aeloria` (adresse `aeloria.netlify.app` si libre).
4. Domaine perso : *Domain management → Add a domain* puis suivre les DNS indiqués (HTTPS automatique).

Sans compte GitHub lié : glisser le dossier `site/` sur app.netlify.com/drop.

### Option B — GitHub Pages

GitHub Pages ne publie pas un sous-dossier `site/` directement : ajouter ce workflow dans `.github/workflows/site.yml`, puis *Settings → Pages → Source : GitHub Actions*.

```yaml
name: Site
on:
  push:
    branches: [main]
    paths: ['site/**']
  workflow_dispatch:
permissions:
  contents: read
  pages: write
  id-token: write
concurrency:
  group: pages
  cancel-in-progress: true
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with:
          path: site
      - id: deployment
        uses: actions/deploy-pages@v4
```

Adresse obtenue : `https://bananacheattv.github.io/mmorpg_mod/` (tous les chemins du site sont relatifs, ils fonctionnent dans ce sous-dossier). GitHub Pages ne lit pas `netlify.toml` : la sécurité repose alors sur la CSP déjà présente dans `index.html`.

### Après la mise en ligne

- Vérifier que `launcher.url` est accessible sans compte GitHub (dépôt du launcher public, ou lien direct vers l’installeur).
- Déclarer `sitemap.xml` dans Google Search Console.
- Tester le partage : opengraph.xyz ou l’aperçu d’un lien collé dans Discord.

## Qualité vérifiée

- Lighthouse (local, compression gzip) : mobile 97–99 / 100 / 100 / 100, desktop 100 / 100 / 100 / 100 ; CLS 0.
- Largeurs 360, 390, 768, 1024, 1440, 2560 px : aucun défilement horizontal.
- Clavier : lien d’évitement, focus visibles, menu mobile (Échap, reste de la page inerte), cartes de classes (Entrée/Échap), accordéons natifs, mentions légales en `<dialog>`.
- `prefers-reduced-motion` : particules, parallaxe, animations et transitions coupées.
- Statut serveur testé en ligne / hors ligne / API en erreur, copie de l’IP + toast.
