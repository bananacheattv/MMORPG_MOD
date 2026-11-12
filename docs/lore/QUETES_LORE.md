# Quêtes du lore d'Aeloria

Source : `docs/lore/Quetes_Aeloria_v1.md` (plan v1). Conversion : `python3 tools/quetes/build_lore_quests.py`
→ `src/main/resources/data/mmorpg/lore/lore_quests.json` (chargé par `LoreQuests`). Après une modification du plan,
relancer le script puis augmenter `DefaultQuests.REVISION` pour que les `quests.json` existants soient mis à jour.

## Contenu
- **Trame** `qp_001` à `qp_250` (4 actes, 20 chapitres) : proposée automatiquement après le tutoriel de la Mascotte
  (`tuto_fin`), rendue automatiquement. Chaque 1re quête de chapitre joue une carte de chapitre (rejouable dans le journal) ;
  la dernière lance l'épilogue.
- **Secondaires** `qs_*` (452 : les 48 quêtes de classe du plan sont écartées, les classes du mod restent) et
  **événements** `ev_noel_*`, `ev_val_*`, `ev_paq_*` (dates du plan) : donnés par le PNJ dont le **nom contient**
  celui du donneur (Ysméralde, Thalgrim, Brokkar, Orvald, Sélian, Halvine, Ilda, Ombrelle, Bondimonde, « Maître des métiers »,
  « Tableau des primes »…) ou dont le groupe vaut `lore_<cle>` (ex. `lore_tableau_des_primes`).
- Objectifs suivis : chasse (`kill *`), cible nommée / donjon / boss (`kill boss`), niveau, métiers mineur/fermier/pêcheur.
  Les autres (dialogue, exploration, énigme, escorte, livraison, choix…) se valident en parlant au donneur.
  Tant qu'aucun PNJ d'un donneur n'existe dans le monde, Flammèche (clic sur lui) le remplace.
- Répétables : quotidiennes et hebdomadaires (remise à 4 h, lundi). Créneaux horaires (heure serveur), dimanche, nuit/aube
  (ciel sombre), orage.

## Mises en scène de Flammèche (`FlammecheScenes`)
- QP-002 : l'œuf brûlant tremble, fume, se fissure (sons), éclate en flammes ; le dragonnet grandit. Valide l'étape.
- Chapitre 1 : il suit le joueur ; QP-012 : il s'envole vers le Seuil.
- Éveils (QP-063, 125, 188, 250) : il revient, spirale de feu, grandit (stades 2 à 5), puis repart.
- Noël / Pâques : une fois retrouvé, il suit le joueur avec une aura (neige / pétales) jusqu'à la fin de l'événement.
- Captures : `tools/screens/capture.sh flammeche` → `docs/screens/flammeche/`.

## Non géré (texte affiché seulement)
Marques du Seuil, réputations, monnaies d'événement, titres, coffres, durée limite des quêtes chronométrées, choix à fins
multiples (pas d'embranchement), lieux d'exploration précis, pleine lune.
