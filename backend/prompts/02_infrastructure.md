# Prompt 02 — Infrastructure locale, migrations et privilèges

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Intègre puis vérifie les références infra du pack. Génère des secrets locaux sans les afficher, résous les digests des images depuis le registre et exécute docker compose config. Lance PostgreSQL/PostGIS et Mailpit en local ; aucun service de connexion externe. Ne publie pas ces services sur toutes les interfaces. Garage reste optionnel avant le lot documents.

Contrôle PostgreSQL/PostGIS réels, architecture de l'image, chemin de volume PostgreSQL 18 et persistance après redémarrage. Vérifie les rôles séparés bootstrap/migrateur/runtime et les privilèges explicites de chaque migration. Crée la première migration Flyway du schéma métier et une intégration de test PostgreSQL/PostGIS réelle. Le rôle runtime doit pouvoir faire les opérations attendues mais pas créer une table, modifier l'historique Flyway ou réécrire l'audit. Vérifie les neuf secrets locaux, dont la clé MFA encodant 32 octets, sans les afficher. Google reste désactivé sans profil dédié.

Configure ddl-auto=validate et open-in-view=false. Local : identifiants Flyway séparés. Prod : migration one-shot, Flyway désactivé dans la JVM, runtime sans DDL. Prépare l'image backend non root depuis l'artefact construit et un contrôle de démarrage sans secrets dans l'image.

Recette : migrations sur base vide et démarrage suivant sans modification ; test de refus DDL runtime ; extension présente ; données conservées après redémarrage ; image et versions effectives consignées. Pas de down -v sur volume utile. Ne transforme pas les fichiers de référence en preuve de démarrage sans exécution réelle.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
