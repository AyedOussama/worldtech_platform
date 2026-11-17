# FE04 — Invitation client, dossiers et validations administratives

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Permettre l'entrée contrôlée des clients et l'examen administratif des dossiers client/technicien.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE04 ; contrats de décision, justificatifs et état du dossier.

## Lecture obligatoire
- docs/UX_ECRANS.md
- docs/ALIGNEMENT_BACKEND.md
- docs/API_CONTRATS.md
- docs/ARBITRAGES.md

## Travail demandé
1. Implémente création et renvoi d'invitation par ADMIN, avec résultat utilisateur clair et absence de jeton brut dans journaux/analytics.
2. Traite lien valide, expiré, consommé et invalide. Évite la réactivation d'un compte via un vieux lien ; l'API décide de son état.
3. Construis le dossier client avec les champs et contraintes réels. Prépare brouillon, soumission, demande de complément et suivi de validation sans confondre email vérifié et compte approuvé.
4. Limite un client en attente à son parcours autorisé ; la création d'incident n'est pas ouverte avant approbation réelle.
5. Construis les files d'examen ADMIN avec recherche/pagination et fiches. Affiche les éléments nécessaires à la décision et les motifs, sans données sensibles inutiles.
6. Pour les techniciens, consulte profil/compétences/justificatifs issus de Flutter et implémente les décisions administratives prévues. N'ajoute pas une application web technicien complète.
7. Les documents privés en attente de BE09/FE09 restent indisponibles explicitement ; ne simule pas un téléchargement propre avec une URL publique.
8. Gère soumission/décision concurrente, erreur de champ et conservation de saisie. Un refus ou une demande de complément est une commande motivée selon le contrat.
9. Documente uniquement les pièces réellement exigées par le produit ; ne crée pas une liste prétendument réglementaire sans validation.

## Vérifications et critères d'acceptation
- Invitation expirée/consommée et renvoi testés selon le contrat.
- Client en attente refusé sur un endpoint métier réel.
- Décision répétée ou dossier modifié pendant revue : état cohérent et message utile.
- Séparation des dossiers, fichiers et permissions vérifiée.

## Livrables
- Parcours client d'entrée, listes de revue et fiches ADMIN.
- Table des champs/états réellement exposés et décisions motivées.
- Compte rendu FE04 avec dépendances documentaires encore ouvertes.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Un compte approuvé ne reçoit pas implicitement tous les sites ni de nouveaux rôles métier.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

