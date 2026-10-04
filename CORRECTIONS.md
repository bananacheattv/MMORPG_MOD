# Correctif des erreurs de compilation — NeoForge 26.3.0.39-beta

## Installation

1. Extraire ce ZIP dans un nouveau dossier.
2. Ouvrir le dossier modmc dans IntelliJ avec le JDK 25 comme Project SDK et Gradle JVM.
3. Recharger le projet Gradle.
4. Dans le terminal du projet : `./gradlew.bat compileJava`, puis `./gradlew.bat runClient` (PowerShell).

Java 25 est deja configure dans build.gradle. Les sources et ressources d’origine sont conservees ; les caches, les sorties build et la configuration locale IntelliJ sont exclus du ZIP.

## Modifications

- MenuScreen : utiliser Minecraft.getInstance().gui.setScreen(...).
- CharacterScreen et TeleporterSetupScreen : utiliser minecraft.hasShiftDown().
- Skill.sec accepte un long pour le resultat de Math.round(double), sans conversion reductrice en int.
- Keys.java ajoute : categorie MMORPG, enregistrement client et traitement des touches. Menu : M ; competences 1 a 6 : R, F, G, H, J, V. Les touches sont modifiables dans les options du jeu (elles peuvent entrer en conflit avec les raccourcis vanilla).

## Verification et limites

Les signatures Minecraft ont ete controlees dans le JAR de sources 26.3.0.39-beta inclus dans le ZIP original. Les enregistrements de touches suivent la documentation NeoForge. Les neuf erreurs signalees ont chacune une correction dans les sources.

La compilation complete n’a pas pu etre executee ici : le wrapper Gradle ne peut pas telecharger sa distribution (Network is unreachable). Le lancement en jeu n’est donc pas valide.

## Deuxieme correctif : demarrage / reseau client

Ajout de ClientPayloadHandlers.java : gestionnaires des SIX paquets declares serveur-vers-client dans Net. Synchronisation des donnees RPG, vie/mana, bestiaire, notifications et ouverture des ecrans existants. L'enregistrement est limite au client physique, via RegisterClientPayloadHandlersEvent (execution principale par defaut).

Verification statique : les six types declares ont chacun exactement un gestionnaire ; signatures GUI comparees aux sources Minecraft 26.3 fournies. Compilation et lancement toujours non valides ici faute d'acces au telechargement de Gradle.

Limites du projet fourni : ForgeScreen et le rendu de texte flottant n'existent pas. La demande de forge affiche donc un message explicite ; les textes de combat utilisent provisoirement la barre d'action, sans placement dans le monde. Les renderers des entites personnalisees sont ajoutes dans la v4. Ce correctif traite le registre reseau, pas la completion de toutes les fonctionnalites annoncees par le mod.

Le rapport de crash du 1er octobre montre aussi Missing uniform tex dans ErrorDisplayWindow : cette exception intervient pendant l'affichage de l'erreur fatale et masque le detail de la premiere erreur reseau.

## V4 : rendu des entites

Les deux rapports 18:33 et 18:34 signalent un renderer null dans EntityRenderDispatcher.shouldRender. ClientRenderers enregistre les 15 types d'entites : 13 monstres/boss, le familier et le projectile magique. Modeles vanilla choisis selon les textures existantes (humanoide, squelette, loup, blaze, golem). Les familiers et projectiles utilisent leurs sprites d'objets. Aucun type n'est masque ou desactive. Les animations propres aux boss et les couches d'armure personnalisees restent a developper.

Archive construite directement a partir de la v3, en conservant tous ses fichiers Java sans modification. Verification statique : couverture exacte 15/15, aucun doublon, toutes les textures presentes, corrections precedentes conservees. Compilation et essai en jeu non disponibles ici (telechargement Gradle bloque).
