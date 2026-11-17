# FE13 — Mesures de performance et accessibilité des parcours

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Mesurer les parcours existants, corriger les causes observées et documenter les résultats sans refonte générale ni scores fabriqués.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- Parcours réels disponibles ; BE13 pour les mesures de temps API et charge associées.

## Lecture obligatoire
- docs/PERFORMANCE.md
- docs/TESTS_RECETTE.md
- docs/DESIGN_SYSTEM.md
- docs/SECURITE.md

## Travail demandé
1. Relève le build de production, matériel, navigateur, réseau, fixtures, routes et conditions de mesure. Mesure l'état initial avant optimisation.
2. Examine JS initial et chunks de routes, CSS/fonts/images, requêtes inutiles, tâches longues et changements de layout sur connexion, tickets, formulaire, mission et rapport.
3. Sépare temps API et rendu. Pour SPA, mesure les navigations applicatives ; ne déduis pas la fluidité de toutes les routes d'un seul score de connexion.
4. Compare les budgets proposés et les résultats réellement observés. Si les données de terrain manquent, note cette limite ; ne remplace pas INP p75 par un score Lighthouse.
5. Corrige uniquement les causes mesurées : imports, traitement répétitif, nœuds excessifs, images, fuite d'abonnements, polling ou cache. La virtualisation et le cache nécessitent une politique et des tests appropriés.
6. Vérifie WCAG ciblé : clavier, focus non masqué, labels, contrastes, erreurs, zoom et téléphone. Complète les outils automatisés par vérification manuelle.
7. Contrôle clair/sombre, menus, dialogues, listes et graphiques. Utilise unslop pour les défauts visuels localisés dans la direction retenue ; ne réinvente pas une charte sans besoin.
8. Compare après correction dans les mêmes conditions, rejoue les contrôles affectés et documente les régressions ou budgets non atteints.

## Vérifications et critères d'acceptation
- Rapport avant/après avec conditions et outils, sans métriques inventées.
- Absence de chargement carte/agenda/graphiques sur la connexion si inutiles.
- Navigation clavier et formulaires inchangés fonctionnellement après optimisation.
- Contrôles significatifs et build après les changements.

## Livrables
- Rapport mesuré de performance et accessibilité.
- Corrections ciblées et preuves reproductibles.
- Compte rendu FE13 avec budgets atteints/non atteints et actions restantes.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Une mesure sur données simulées ou un appareil performant est identifiée comme telle ; elle n'est pas une garantie pour tous les utilisateurs.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

