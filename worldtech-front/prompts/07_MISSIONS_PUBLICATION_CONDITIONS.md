# FE07 — Missions, conditions financières et publication

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Permettre à l'administration de préparer et publier une mission traçable, reliée au ticket ou au contrat approprié.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE07 ; contrat de création, révisions et publication.

## Lecture obligatoire
- docs/ALIGNEMENT_BACKEND.md
- docs/API_CONTRATS.md
- docs/UX_ECRANS.md
- docs/ARBITRAGES.md

## Travail demandé
1. Implémente liste, fiche et formulaire de mission avec site, compétences, fenêtre d'intervention et origine prévue. Un ticket peut posséder plusieurs missions ; une mission préventive peut avoir un contrat sans ticket.
2. Présente la proposition initiale client à l'administration comme référence distincte. Modifie les conditions publiées par les commandes prévues, en conservant auteur, date, motif et révision.
3. Contrôle les champs obligatoires avant publication, tout en laissant l'API vérifier les invariants. L'échec affiche les corrections utiles et conserve le brouillon.
4. Affiche les états et prochaines actions réels ; aucune sélection manuelle de statut ne remplace les commandes de publication, annulation ou révision.
5. Après publication, affiche la révision courante et ses conséquences sur les candidatures. Après confirmation, ne modifie pas silencieusement l'accord ; le mécanisme d'avenant exige son contrat.
6. Si une prévisualisation de contenu technicien est prévue, utilise le DTO de découverte anonymisé. L'accès ADMIN au site précis ne justifie pas son inclusion dans un aperçu public.
7. Traite pagination, filtre par état/date/site et concurrence ETag. Une publication périmée doit recharger l'état utile sans écraser la saisie.
8. Charge l'agenda ou la carte uniquement si le besoin de ce lot est confirmé et la bibliothèque vérifiée ; une liste avec créneau suffit à démarrer.

## Vérifications et critères d'acceptation
- Plusieurs missions d'un ticket affichées sans duplication de l'incident.
- Exemple 150+20 client et 180+35 publié présenté sans ambiguïté.
- Double publication, version périmée et contrat incomplet correctement traités.
- Aucun statut ou montant accepté réécrit par une commande générique.

## Livrables
- Gestion des brouillons, conditions et publication ADMIN.
- Historique des révisions et états lisibles.
- Compte rendu FE07 avec scénario nominal et conflits.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
La découverte et la candidature du technicien restent dans Flutter ; aucune marketplace web publique n'est créée.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

