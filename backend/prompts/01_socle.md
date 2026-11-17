# Prompt 01 — Squelette compilable et frontières d’architecture

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

À partir des ADR acceptés, crée ou complète le squelette Maven Spring Boot. Java 25, parent Boot du référentiel et Maven Wrapper 3.10.0 vérifié ; wrapper et checksum documentés. Choisis les starters Boot 4 actuels, sans pinner les bibliothèques gérées. Adapte la référence POM au dépôt ; n'ajoute pas encore les dépendances métier inutilisées.

Organise les packages par domaine et couches. Crée une configuration HTTP refusant par défaut les routes non prévues, un endpoint de santé minimal intentionnel et une gestion ProblemDetail des erreurs. Pas d'API métier ouverte pour faciliter le front. Le domaine ne dépend pas de Spring/JPA ; entités de persistance et DTO distincts.

Ajoute ArchUnit pour les dépendances interdites et des tests HTTP de refus anonyme. Initialise CI compilation/tests sur JDK 25 et un README local. Actuator détaillé et documentation interactive ne sont pas publics par défaut.

Recette : ./mvnw verify exécuté, endpoint prévu seulement, route inconnue/protégée refusée, tests d'architecture exécutés. Si JDK ou réseau manque, ne modifie pas le majeur ni ne supprime les contrôles : rapporte le blocage exact. Pas d'onboarding, de paiement ou de modèle métier complet dans ce lot.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
