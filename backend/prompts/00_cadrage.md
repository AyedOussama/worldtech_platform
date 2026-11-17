# Prompt 00 — Inspection du dépôt et décisions techniques

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Inspecte le dépôt et ses instructions avant toute création. Identifie le code existant, les modifications locales, les outils disponibles et les conventions. Lis AGENTS.md et le cahier technique. Ne génère pas encore toute l'application.

Produis les ADR 001 architecture modulaire/MVC, 002 PostgreSQL/PostGIS face à MongoDB et alternatives, 003 identité applicative Spring Security : interfaces propres, sessions web JDBC/CSRF, jetons opaques mobiles, Google optionnel et MFA ADMIN. Respecte ces décisions utilisateur ; ne réintroduis pas un serveur de connexion externe. Vérifie dans les sources officielles les versions stables compatibles avec Java 25 / Boot 4.1.2, le BOM et les images candidates. Si une version a évolué, explique le changement et mets le référentiel à jour sans mélange de générations. Distingue un tag, un digest et la version réellement contenue.

Crée aussi la correspondance initiale ASVS/SECURITE.md et un modèle de preuves, sans déclarer de certification. Revalide Bouncy Castle 1.86 pour Argon2id, ses dépendances et sa compatibilité. Crée aussi la correspondance initiale ASVS/SECURITE.md et un modèle de preuves, sans déclarer de certification. Revalide Bouncy Castle 1.86 pour Argon2id, ses dépendances et sa compatibilité. Fixe une carte des modules et des dépendances, une matrice d'autorisation initiale et le plan des lots. Liste les arbitrages produit du prochain lot uniquement. PostgreSQL est une recommandation argumentée ; ne présente pas MongoDB comme dépourvu de transactions ou de géospatial. Aucun nouveau rôle métier.

Recette : aucun travail existant écrasé ; sources datées et liens présents ; décisions applicables à trois développeurs ; tests et limites du prochain lot identifiés. Livrer les documents et une liste concise des commandes qui seront utilisées, sans prétendre avoir compilé une application absente.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
