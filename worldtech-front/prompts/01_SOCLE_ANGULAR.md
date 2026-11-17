# FE01 — Socle Angular, navigation et contrôles initiaux

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Obtenir une application minimale qui compile, possède des commandes reproductibles et organise déjà les futurs espaces sans leur accorder de faux privilèges.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- Contrat d'API identifié par FE00 ; l'authentification réelle sera intégrée à FE03.

## Lecture obligatoire
- docs/ARCHITECTURE.md
- docs/DEVELOPPEMENT.md
- docs/DEPENDANCES.md
- docs/SECURITE.md
- templates/config/runtime-config.example.json

## Travail demandé
1. Lis les décisions et résultats réels FE00. Crée ou adapte l'application dans le chemin retenu, avec les versions effectivement compatibles. Utilise le CLI pertinent ; ne copie pas une configuration obsolète du pack.
2. Active TypeScript et templates stricts, composants standalone et routes lazy cohérentes. Prépare core, layouts, shared, features et api/generated sans remplir ces dossiers de classes génériques inutiles.
3. Crée les routes publiques minimales, les entrées Client/Admin et les pages 404/refus/session indisponible nécessaires. Tant que l'identité réelle manque, les routes privées restent protégées ; une démo n'est possible que dans un mode de développement explicitement isolé.
4. Définis une configuration publique typée et validée au démarrage, API relative /api/web/v1 si le proxy de même origine est retenu. Aucun secret ni URL de production de repli non documentée.
5. Prépare HttpClient et une représentation des erreurs issue du contrat réel. Les règles de retry restent limitées ; aucun JWT web, token mobile ou interceptor Bearer par défaut.
6. Définis les scripts du dépôt pour lint, typecheck, test unitaire, test navigateur et build. Ajoute les outils de test seulement dans les versions compatibles. Documente ceux qui requièrent un serveur réel.
7. Résous et conserve le lockfile réel ; fixe le gestionnaire dans packageManager. Ajoute les premiers contrôles CI adaptés aux scripts existants, sans désactiver de tests pour obtenir du vert.
8. Prépare un catalogue de développement minimal ou un exemple de page stable pour FE02, exclu du build de production. Vérifie les erreurs de configuration et le refus des routes privées avant identité.

## Vérifications et critères d'acceptation
- Installation verrouillée et build production exécutés.
- Lint et typecheck exécutés selon les outils réellement configurés.
- Contrôle ciblé de route inconnue, configuration invalide et accès privé non authentifié.
- Les scripts documentés existent et le mode démonstration ne peut pas s'activer en production.

## Livrables
- Socle compilable et lockfile.
- Commandes de travail et contrôle CI initial.
- Compte rendu FE01 avec versions effectives, résultat des vérifications et limites d'intégration.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Pas d'implémentation des tickets, missions ou rapports dans ce lot ; les structures nécessaires restent petites.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

