# FE09 — Pièces jointes privées, upload et état antivirus

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Permettre l'ajout et la consultation des pièces autorisées avec une distinction claire entre transfert, quarantaine et disponibilité.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE09 et périmètre parent métier des pièces ; peut être avancé après FE05.

## Lecture obligatoire
- docs/API_CONTRATS.md
- docs/SECURITE.md
- docs/UX_ECRANS.md
- docs/TESTS_RECETTE.md

## Travail demandé
1. Implémente les composants de sélection, progression, liste et téléchargement pour les parents métier autorisés. Les limites réelles de taille/type/nombre viennent du serveur ; la validation navigateur aide sans faire foi.
2. Branche upload intent, transfert, finalisation et suivi du scan. Vérifie le contrat de CSRF sur les requêtes métier ; les uploads signés externes suivent leurs propres headers autorisés.
3. Affiche PENDING_UPLOAD, QUARANTINED, CLEAN, REJECTED et états réels. Un 200 du stockage ne transforme pas la pièce en CLEAN.
4. Gère erreur de transfert, reprise ou nouvel essai selon idempotence, annulation locale et nettoyage des références de prévisualisation. Ne promets pas une reprise byte à byte si le protocole ne l'offre pas.
5. Prévisualise seulement les types traitables en sécurité ; ne rends pas du HTML/SVG arbitraire dans le contexte applicatif. Le PDF reste une donnée non fiable.
6. Obtiens le téléchargement après contrôle du parent et de l'utilisateur. Ne conserve pas une URL présignée comme lien permanent ; renouvelle-la si nécessaire.
7. Ne journalise pas URL signée, nom personnel inutile, jeton ou contenu de pièce. Évite la transmission de documents à un service tiers d'optimisation non prévu.
8. Intègre les composants à onboarding, ticket et rapport quand leurs lots sont disponibles, sans dupliquer le protocole dans chaque écran.

## Vérifications et critères d'acceptation
- Taille/type rejetés et validation serveur réellement observée.
- Scanner indisponible : pièce reste en quarantaine.
- Client A ne télécharge pas le document de B, même via une URL d'objet métier connue.
- Envoi interrompu et finalisation répétée ne créent pas de faux succès.
- Téléphone et clavier pour ajout/suppression autorisée de pièce.

## Livrables
- Flux documents partagé et intégrations utiles.
- Gestion des erreurs et statuts de scan.
- Compte rendu FE09 et scénarios de confidentialité.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucun bucket public, bypass antivirus ou conversion locale annoncée comme preuve de validation serveur.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

