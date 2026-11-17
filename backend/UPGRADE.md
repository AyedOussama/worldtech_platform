# Passer du pack 1.0 au pack 1.1

Ces instructions concernent un pack de préparation. Elles ne supposent pas qu'un backend métier ait déjà été développé. Lire l'état du dépôt et sauvegarder les données utiles avant toute adaptation ; ne pas écraser des changements locaux.

## Nouveau départ

Décompresser le pack 1.1 dans un répertoire propre, lire README.md, intégrer les instructions dans le dépôt cible et commencer par le prompt 00. Le répertoire de travail contient seulement les références actuelles. Ne pas superposer deux versions puis utiliser au hasard d'anciens fichiers.

## Si l'infrastructure du pack 1.0 a déjà été lancée

1. Conserver les volumes PostgreSQL et objets utiles ainsi que les mots de passe runtime/migrateur déjà utilisés. Le nouveau script d'initialisation ne doit pas leur substituer de nouvelles valeurs.
2. Mettre à jour Compose, références d'images et scripts. Relancer init-dev.py : il conserve les secrets existants et ajoute la clé MFA locale nécessaire. Aucun secret Google fictif n'est généré.
3. Résoudre à nouveau les quatre images actuelles avec lock-images.py. Le lock précédent n'est pas le référentiel de cette révision.
4. Valider Compose puis appliquer la configuration. Pour retirer le conteneur Keycloak devenu orphelin, utiliser avec le même projet :

```bash
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml up -d --remove-orphans
```

5. Cette opération ne supprime pas les volumes PostgreSQL. Sur un volume existant, l'ancienne base et l'ancien rôle SQL d'identité peuvent subsister : le nouveau script init n'est pas rejoué. Ils ne sont plus utilisés par cette architecture. Leur suppression éventuelle exige inventaire, sauvegarde et opération explicite ; aucun DROP ou down -v automatique n'est fourni.
6. Les anciens fichiers de secrets peuvent également subsister localement. Ne pas les partager. Vérifier leur inutilisation avant un nettoyage explicite ; le nouveau pack ne les référence plus.

Les images/services n'ont pas été démarrés dans l'environnement de préparation ; cette procédure reste à exécuter sur une machine équipée. Le mode Garage un nœud reste limité au développement.

## Si un backend OIDC a déjà été implémenté à partir de l'ancien dossier

Ne pas simplement changer une variable d'issuer. Il faut réaliser le lot identité de la révision 1.1 : comptes UUID internes, identifiants locaux, identités Google, sessions web et jetons opaques, MFA, récupération et migrations.

Préserver les identifiants métier déjà référencés par les clients, missions et rapports, ou construire une correspondance explicitement contrôlée. Ne pas fusionner automatiquement par email. Les mots de passe existants ne sont pas supposés exportables ou convertibles ; prévoir une activation/récupération vérifiée si nécessaire. Révoquer les sessions anciennes lors de la bascule, avec parcours utilisateur explicite. Tester retour applicatif et données avant production ; pas de migration destructive improvisée.

Les documents SECURITE.md et AUTHENTIFICATION.md, le nouveau POM et les prompts remplacent les instructions d'identité du pack précédent. Le starter OAuth2 Client est conservé pour Google ; il ne réintroduit pas le serveur d'identité supprimé.
