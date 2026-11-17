# FE00 — Inspection et décisions de départ

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Produire une base de réalisation exacte, adaptée au dépôt, avant de créer l'application ou de migrer ses dépendances.

## Prérequis
- Début du projet frontend ; inspecter avant toute création.
- Aucun prérequis backend pour l'inspection ; son dépôt ou son OpenAPI restent à identifier.

## Lecture obligatoire
- README.md
- DEMARRER_ICI.md
- Cahier_frontend_Angular_WorldTech_v1.md
- docs/ARCHITECTURE.md
- docs/DEPENDANCES.md
- docs/ALIGNEMENT_BACKEND.md
- docs/ARBITRAGES.md

## Travail demandé
1. Inspecte cwd, fichiers d'instructions applicables, état Git et modifications existantes. Identifie monorepo, frontend éventuel, backend, gestionnaire, scripts, tests, environnements et conventions. Une absence de fichier se constate ; elle ne justifie pas d'écraser un autre projet.
2. Si Angular existe, relève version, builder, structure standalone/NgModule, stratégie de rendu et dépendances UI. Propose conservation ou migration avec raison concrète. Si rien n'existe, fixe le sous-dossier de création de FE01.
3. Vérifie dans les sources officielles et le registre réellement accessibles les versions stables/supportées et leurs peerDependencies : Angular/CLI/Node/TypeScript/RxJS, PrimeNG et PrimeUIX, CDK si nécessaire, Lucide, tests. Sépare les versions repérées dans le pack des versions effectivement résolubles.
4. Vérifie la licence du paquet/édition UI exacts et les conditions applicables à l'organisation. Ne prends pas les critères de l'équipe projet comme preuve d'éligibilité et n'engage aucun achat. Documente les travaux qui peuvent avancer si ce point reste ouvert.
5. Localise le contrat réel de session, CSRF, MFA, /me et les commandes métier. Indique clairement les endpoints implémentés, proposés et absents. Ne crée pas un faux OpenAPI pour cocher l'intégration.
6. Repère les actifs de marque et maquettes disponibles. Conserve le logo validé ; relève ce qui manque sans choisir une refonte hors périmètre. Écris la direction de départ avec ses cinq éléments dans le compte rendu.
7. Rédige ou adapte les ADR proposés, le chemin du frontend et la stratégie d'intégration du pack. Préserve les instructions existantes ; rapproche AGENTS.md au lieu de le remplacer.
8. Produis un plan FE01 concret, limité et exécutable. Ne lance pas automatiquement FE01 et ne crée pas les écrans métier pendant l'audit.

## Vérifications et critères d'acceptation
- État Git initial et final examinés ; modifications de l'utilisateur préservées.
- Chaque version proposée a une source/date et une plage de compatibilité, ou est explicitement non vérifiée.
- Les décisions encore ouvertes ont une conséquence et un lot limite.
- Le chemin de création du frontend ne chevauche pas les sources du backend.

## Livrables
- Compte rendu FE00, inventaire et contraintes du dépôt.
- Matrice des versions/licences et décisions d'architecture rapprochées.
- Plan de FE01, contrats API disponibles et prérequis manquants.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
L'inspection et la documentation sont le résultat de ce lot ; aucun frontend complet ni installation globale d'outil n'est demandé.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

