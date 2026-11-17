# ADR004 — Réalisation par lots et preuves distinctes
Statut : retenu pour le dossier réel au FE00, 10/11/2026.

## Décision
Un lot FE00–FE14 à la fois, avec résultat concret, contrôles proportionnés et compte rendu.
Distinguer développement, démonstration simulée, intégration Spring Boot, vérification visuelle et déploiement réel.

## Conséquences
Possibilité d'avancer pendant l'absence d'une API sans annoncer une garantie fictive. Les dépendances bloquantes sont suivies au bon lot.
Les contrôles de sécurité, accessibilité et performance commencent avec leur fonctionnalité ; les derniers lots consolident et mesurent.

## Livraison
Artefact statique reproductible, configuration publique, proxy testé, cache maîtrisé et retour à une version précédente documenté.
Aucune publication externe n'est déclenchée par le simple téléchargement du pack.

## Première application au FE00
Audit et documentation seulement : ni build Angular ni recette Spring Boot exécutés. Les tests backend historiques ne sont pas attribués au frontend. [Compte rendu](../comptes-rendus/FE00_AUDIT_ET_DECISIONS.md) et [commandes/limites](../../verification/FE00_COMMANDES_ET_CONTROLES.md).

Le manifeste de l'archive reste inchangé. Son contrôle strict après adaptations documentaires échoue de façon attendue ; ne pas régénérer le manifeste ou modifier le vérificateur pour cacher cet écart. Conserver les contrôles d'origine et relever explicitement les fichiers adaptés et la préservation des sources tierces. Git absent : aucun commit ou run distant supposé.
