# FE10 — Interventions, versions de rapport et revue administrative

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Consulter le travail réalisé, contrôler les rapports et restituer au client les versions autorisées sans altérer les attestations.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE10 et FE08–09 ; génération PDF/attestation définies par le backend.

## Lecture obligatoire
- docs/ALIGNEMENT_BACKEND.md
- docs/API_CONTRATS.md
- docs/UX_ECRANS.md
- docs/ARBITRAGES.md
- docs/TESTS_RECETTE.md

## Travail demandé
1. Construis l'historique d'intervention et la lecture des rapports : actions, résultat, temps déclarés, matériel, photos et version. Les données proviennent de Flutter et du backend.
2. Présente auteur, date, état et attestation de chaque version. Une correction conserve les versions soumises/attestées précédentes.
3. Implémente revue ADMIN, demande motivée de correction et clôture de mission selon les commandes et préconditions réelles.
4. Conserve la résolution du ticket comme action distincte de la clôture mission. Une intervention sans résolution complète reste compréhensible pour le client.
5. Implémente téléchargement du PDF généré côté serveur et suivi de traitement 202 si nécessaire. L'état de génération ne doit pas bloquer toute la fiche.
6. L'attestation est recueillie principalement dans Flutter. N'ajoute pas de signature admin au nom du client. Une attestation web n'est réalisée que si elle fait partie du contrat produit et des habilitations effectivement retenues.
7. Traite absence/refus du signataire et demandes de correction avec les parcours prévus. Ne qualifie pas une signature dessinée de signature qualifiée.
8. Gère revue concurrente, nouvelle version pendant consultation, erreur de téléchargement et accès client limité aux rapports autorisés.
9. Préserve la lisibilité des rapports longs, le zoom, les photos et la navigation de versions dans les deux thèmes.

## Vérifications et critères d'acceptation
- Version attestée inchangée après correction ; ancien PDF/attestation reliés à la bonne version.
- Approbation concurrente et état déjà modifié traités proprement.
- Client A n'accède pas au rapport/pièces de B.
- Mission terminée avec ticket encore ouvert affichée correctement.
- PDF 202 puis prêt, échec et reprise suivis.

## Livrables
- Consultation Client/Admin, revue et versions de rapport.
- Clôture mission et résolution ticket distinctes.
- Compte rendu FE10 et preuve des liens version/attestation.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Le frontend ne produit pas un PDF de démonstration présenté comme le rapport serveur attesté.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

