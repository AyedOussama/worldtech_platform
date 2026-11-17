# FE14 — CI, préparation au déploiement et recette intégrée

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Obtenir une livraison reproductible et une recette complète, en distinguant préparation locale et déploiement effectivement exécuté.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- Backend et environnement cible compatibles, contrats stabilisés et scénario mobile disponible.

## Lecture obligatoire
- docs/DEPLOIEMENT.md
- docs/SECURITE.md
- docs/TESTS_RECETTE.md
- docs/DEVELOPPEMENT.md
- templates/deploy/nginx-spa.conf.example

## Travail demandé
1. Consolide les scripts et la CI réellement présents : installation verrouillée, lint, types, tests nécessaires, génération API et build. Ne masque pas un manque de serveur ou de navigateur par un succès conditionnel silencieux.
2. Produis l'artefact du commit, sa configuration publique et les éléments de traçabilité nécessaires. Vérifie qu'aucune fixture, route de catalogue, secret ou endpoint de test ne rejoint la production.
3. Adapte le serveur statique/reverse proxy à la sortie réelle Angular. Teste fallback SPA, 404 de ressource, API non transformée en HTML, OAuth/callback et politique de cache.
4. Vérifie TLS/cookies/CSRF et CSP dans l'environnement effectivement autorisé et accessible. Ne déclare pas une politique valide sans tester les styles injectés, images et origines utilisées.
5. Exécute le parcours intégré : invitation → validation → ticket → mission → candidature/confirmation Flutter → intervention/attestation → revue web → lecture client. Ne remplace pas les étapes technicien par une permission admin.
6. Rejoue les refus les plus importants : deux clients, pré-MFA, CSRF, pièces, version périmée et réaffectation. Les étapes sans API réelle restent explicitement ouvertes.
7. Prépare et éprouve le retour à l'artefact précédent si l'environnement le permet ; la base et le stockage ne sont pas restaurés implicitement avec le frontend.
8. Documente surveillance d'erreurs sans données privées, diagnostic de configuration et traitement d'un onglet ancien après nouvelle publication.
9. Préserve toute audience ou infrastructure existante. Les opérations externes suivent le périmètre effectivement autorisé ; cette préparation de pack n'a elle-même publié aucun service.

## Vérifications et critères d'acceptation
- Artefact lié au commit, configuration et dépendances reproductibles.
- Recette nominale et refus documentés contre API réelle ou non exécutés explicitement.
- Liens directs, login/logout/MFA, callback conditionnel et fichiers statiques vérifiés.
- Plan de rollback concret et limitations connues.

## Livrables
- Pipeline, configuration de livraison et documentation d'exploitation.
- Rapport de recette intégré avec résultats exacts.
- Compte rendu FE14 et suivi des lots mis à jour ; aucune mise en production annoncée sans observation.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucune suppression de données, migration destructive, changement d'audience ou publication non autorisée pour terminer une case du suivi.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

