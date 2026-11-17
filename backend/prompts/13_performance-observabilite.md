# Prompt 13 — Mesures et optimisation démontrée

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Prépare un jeu réaliste et un scénario de charge authentifié représentant au moins 1 000 techniciens, 10 000 missions et les parcours retenus. Les cibles de p95/concurrence du cahier sont des hypothèses à vérifier. Mesure avant optimisation et conserve protocole, ressources et résultats.

Cherche N+1, pages non bornées, scans spatiaux, verrous longs, saturation Hikari/exécuteurs et agrégations coûteuses. Optimise avec projections/index/SQL explicite justifiés par EXPLAIN et mesures. Ne pas ajouter Redis, microservices ou virtual threads par réflexe. Recalculer le budget de connexions pour toutes les instances et jobs.

Ajoute métriques et logs structurés utiles, traceId, latence/erreurs/pools/outbox/scan/PDF/SLA, sans secrets ni labels utilisateur non bornés. Health/readiness séparées, endpoints de gestion privés. Vérifie que les optimisations ne contournent pas les périmètres d'accès ou réintroduisent un cache partagé non cloisonné.

Recette : comparer avant/après et réexécuter les tests touchés ; charge avec au moins deux comptes clients distincts ; panne d'un service externe ; files bornées et dégradation contrôlée. Si les outils de charge manquent, livrer scénario exécutable et statut non mesuré, jamais un p95 inventé.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
