# FE08 — Candidatures, sélection et suivi de confirmation

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Aider l'administrateur à sélectionner un candidat puis à suivre la confirmation réelle, sans faux succès ni double affectation.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE08 ; commandes concurrentes, délais et conditions acceptées.

## Lecture obligatoire
- docs/API_CONTRATS.md
- docs/ALIGNEMENT_BACKEND.md
- docs/SECURITE.md
- docs/TESTS_RECETTE.md

## Travail demandé
1. Implémente la liste des candidatures et la comparaison des informations réellement disponibles : profil, compétences, disponibilité déclarée et état. N'invente pas un score IA.
2. Présente la révision financière concernée et la conséquence de la sélection. Une candidature sélectionnée n'est pas encore une affectation confirmée.
3. Appelle la commande de sélection avec version/idempotence prévues. Bloque les interactions répétées dans l'UI, sans présenter cela comme la garantie d'unicité serveur.
4. Affiche réservation, confirmation attendue, refus et expiration. L'heure serveur tranche l'échéance ; le compte à rebours navigateur est une aide d'affichage.
5. Ne confirme pas à la place du technicien depuis Angular. La confirmation provient de Flutter et de l'API autorisée, puis apparaît après actualisation.
6. Traite 409 mission déjà réservée, 412 révision périmée, sélection concurrente, montant changé et timeout après succès potentiel.
7. Après refus ou expiration, propose uniquement les commandes de remise en publication ou nouvelle sélection réellement autorisées.
8. Présente les informations de planning utiles. Un glisser-déposer de créneau ne doit pas modifier silencieusement une affectation confirmée.
9. Documente les arbitrages de délai et de réaffectation manquants ; réalise les éléments indépendants et laisse les actions dépendantes explicites.

## Vérifications et critères d'acceptation
- Deux administrateurs sélectionnent en concurrence : résultat final cohérent avec une seule place active.
- Confirmation versus expiration, version financière changée et refus testés via le contrat réel.
- Timeout ne provoque pas une deuxième sélection différente par retry automatique.
- L'UI distingue candidatures, réservation et confirmation.

## Livrables
- Écrans de comparaison/sélection et suivi d'affectation.
- Messages de conflit et mécanisme de reprise déterministe.
- Compte rendu FE08 avec preuves de concurrence réelles ou manque explicite.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucun endpoint mobile utilisé avec la session web pour simuler la confirmation du technicien.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

