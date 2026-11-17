# FE06 — Contrats, incidents et propositions financières client

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Livrer la déclaration d'incident client puis sa qualification administrative avec des informations compréhensibles et les montants correctement séparés.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE06 ; FE09/BE09 nécessaires avant de déclarer la recette des pièces jointes intégrée.

## Lecture obligatoire
- docs/UX_ECRANS.md
- docs/API_CONTRATS.md
- docs/ALIGNEMENT_BACKEND.md
- docs/ARBITRAGES.md
- docs/TESTS_RECETTE.md

## Travail demandé
1. Construis liste et fiche d'incident intégrées à partir de l'écran de référence. Donne une URL propre aux fiches et conserve les filtres au retour.
2. Implémente déclaration : site autorisé, équipement optionnel cohérent, objet, description, priorité demandée et proposition financière si prévue. Les pièces utilisent le flux FE09 ou restent explicitement en attente d'intégration.
3. Rapproche les arbitrages HT/TTC, obligation du montant, base forfait/horaire et frais avec le backend. N'invente ni zéro pour null, ni montant facturé à partir d'une rémunération.
4. Affiche couverture contractuelle et engagements réellement exposés. Les calendriers/SLA sont calculés au serveur ; le navigateur ne décide pas seul d'une pause ou d'un retard.
5. Implémente la qualification ADMIN : priorité retenue, informations manquantes, contrat et résolution à distance ou préparation de mission selon les commandes réelles.
6. Sépare les échanges publics et notes internes dans données, affichage et actions. L'historique client ne reçoit que les événements publics autorisés.
7. Traite états OPEN, QUALIFIED, IN_PROGRESS et autres états réels sans fusion avec ceux des missions. Une mission future ou terminée ne résout pas automatiquement le ticket.
8. Préserve les montants décimaux et la saisie en cas d'erreur. Soumission avec idempotence/version selon contrat, sans retry POST générique.
9. Contrôle la déclaration sur téléphone, zoom, clavier et erreurs de formulaire.

## Vérifications et critères d'acceptation
- Client approuvé et bon périmètre ; client en attente refusé.
- Incident sans équipement et rattachement invalide testés.
- Proposition client et montants administrateur ne sont pas confondus.
- Erreur réseau, double action et 412 traités sans perte de formulaire.
- Note interne absente du trafic client.

## Livrables
- Tickets Client/Admin, qualification et données contractuelles nécessaires.
- Contrats financiers/UI documentés, pièces intégrées ou prérequis explicite.
- Compte rendu FE06 et recette de déclaration.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Pas de comptabilité, devis commercial ou calcul de marge ajouté en interprétant les montants de mission.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

