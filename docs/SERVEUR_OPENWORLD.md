# Serveur Openworld

Le rôle `OPENWORLD` héberge les cartes d'exploration sans construire le hub, le spawn ni les instances de mines/plaines/donjons. Il utilise les profils, personnages, guildes et autres données partagées du réseau existant.

## Installation dans le panneau local

Compiler ce mod (`gradlew build`), puis, avec une session ouverte dans le panneau :

```powershell
node tools/Add-AeloriaOpenworldServer.cjs "C:/chemin/serveur manager" "build/libs/aeloria-0.0.0.jar"
```

Le script conserve une sauvegarde de la configuration du panneau et de Velocity. Il crée `servers/openworld` à partir du moteur et de la configuration du serveur principal, sans copier ses mondes ni ses joueurs. Il refuse d'écraser un serveur existant.

- Serveur : `openworld`, rôle `OPENWORLD`, port interne `127.0.0.1:25576`.
- Mémoire : `-Xms4G -Xmx16G` dans `user_jvm_args.txt`.
- Monde initial : génération normale dans `servers/openworld/world`.
- WorldEdit : même dossier partagé de schématics que les autres serveurs de jeu.
- Panneau : commandes, mesures de consommation et cible de mise à jour du mod ajoutées automatiquement.
- Velocity : destination `openworld` ajoutée ; l'entrée par le lobby reste identique.

Installer le même JAR sur tous les serveurs et clients, démarrer Openworld dans le panneau, puis exécuter `velocity reload` dans la console du proxy. Un administrateur disposant de la permission Velocity peut rejoindre cette destination avec `/server openworld`. Les déplacements ordinaires des joueurs restent à relier aux accès prévus pour les maps.

Pour installer une sauvegarde de carte complète, arrêter **Openworld**, sauvegarder son dossier `world`, puis installer la carte dans ce dossier avant de redémarrer. Pour un schématic, utiliser la rubrique WorldEdit du panneau puis le charger/coller sur Openworld.
