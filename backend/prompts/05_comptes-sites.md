# Prompt 05 — Comptes clients, sites et équipements cloisonnés

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente client_account BUSINESS/INDIVIDUAL, membres, portée de sites, sites/contacts et équipements. Conserve les trois rôles métier. Les limites de sites sont des habilitations, pas un nouveau rôle organisateur.

Toutes les lectures et écritures sont filtrées par compte/site autorisé dans la requête avant pagination, total ou agrégation. Pas de filtre après chargement complet. Vérifie en base les liens compte/site/équipement par clés étrangères composites ou garantie équivalente testée. Les DTO publics ne permettent pas de transférer arbitrairement un objet vers un autre compte.

Ajoute pagination bornée, versions optimistes, ETag/If-Match pour les modifications sensibles, validation des attributs d'équipement et erreurs 404/403 selon la politique décidée. Contacts et données privées ne font pas partie du futur DTO de découverte technicien.

Recette : deux comptes et plusieurs sites ; permutation des identifiants dans GET/POST/PATCH, rattachement d'équipement à mauvais site, compteurs et filtres hors périmètre, version périmée, pagination. Les contraintes SQL doivent aussi refuser des relations incohérentes hors HTTP. Pas de suppression en cascade d'historiques d'intervention.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
