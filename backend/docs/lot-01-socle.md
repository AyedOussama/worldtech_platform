# Compte rendu — lot 01, socle compilable

27 octobre 2026. Demande : exécuter `prompts/01_socle.md` après le cadrage, sans générer les fonctions des lots suivants.

## Prévu et implémenté

- Un module Maven `fr.entreprise:maintenance-api`, Java 25, parent/BOM Boot 4.1.2, ArchUnit 1.5.1. Starters MVC/validation/sécurité et tests de génération Boot 4 ; aucune surcharge de version transitive.
- Wrapper Apache 3.3.4 `only-script` pour la distribution Maven 3.10.0, URL HTTPS et SHA-256 explicite. Un Maven de bootstrap a été téléchargé/vérifié dans le dossier temporaire approuvé, sans installation globale.
- `GET /health` minimal et refus de toutes les autres opérations ; CSRF conservé, authentifications par défaut désactivées et auto-création d'utilisateur exclue. Pas de rôle, prix, DTO métier ou migration ajouté.
- ProblemDetail commun pour la sécurité, le firewall, MVC et le dispatch d'erreur interne ; messages constants, instance URN et trace serveur. Journal d'erreur limité au type d'exception et à la corrélation, sans message/cause brute.
- Tests de droits négatifs, validation/réponses expurgées, contexte MDC, frontières et capacité de détection d'ArchUnit ; tests HTTP sur un vrai Tomcat, sans H2 ou mocks de serveur.
- OpenAPI statique du seul endpoint livré, README de développement et workflow CI initial Temurin 25, Ubuntu/Windows, actions épinglées. Chargement statique de l'agent Mockito limité aux tests, selon la documentation de sa version gérée.

Les classes techniques sont dans `bootstrap`/`shared.http`. Les futurs domaines et leurs entités JPA ne sont pas créés artificiellement ; les fixtures d'architecture sont uniquement sous `src/test` et ne sont pas des modules métier livrés.

## Préservation

À la reprise : aucun projet applicatif, wrapper, workflow ou source d'application ajouté par un tiers. Dossier toujours hors Git ; aucune assertion « dépôt propre », aucun `git init`, remote, commit, reset ou écrasement de travail existant.

Les 37 fichiers du manifeste initial sont conservés à l'identique, dont le README historique, les références et l'infrastructure. Ajouts applicatifs nouveaux ; mises à jour seulement des documents de cadrage produits au lot 00. Le README courant du code est `README-dev.md`. Aucun `.local`, secret réel, compte, base, volume ou service Docker créé/modifié.

## Contrats et fichiers principaux

| Fichiers | Portée |
|---|---|
| `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Build, vérification de distribution, tests et JAR exécutable |
| `src/main/java/fr/entreprise/maintenance/MaintenanceApplication.java` | Démarrage, exclusion de l'utilisateur automatique |
| `bootstrap/health`, `bootstrap/security`, `bootstrap/http` sous ce package | Santé, politique HTTP et erreurs internes |
| `shared/http` sous ce package | Réponses expurgées, validation et corrélation |
| `src/test/java`, `.github/workflows/ci.yml` | Tests locaux et définition de CI ; pas de succès CI distant revendiqué |
| `openapi/maintenance-api.json`, `README-dev.md` | Contrat HTTP minimal et utilisation du socle |
| `docs/preuves/lot-01-verifications.md` | Commandes, résultats réels, provenance et limites |

Différence API : santé intentionnellement publique, aucune API métier. Pas de changement de règle financière, d'admission ou d'identité ; les futures autorisations restent fermées.

## Vérifié et non vérifié

Les résultats définitifs, les éventuelles erreurs intermédiaires corrigées et la portée des contrôles sont consignés dans [la preuve du lot 01](preuves/lot-01-verifications.md). La recette locale exécute réellement Maven et les tests ; la syntaxe YAML du workflow ne vaut pas exécution GitHub Actions.

| SEC | Contrôle introduit | Limite |
|---|---|---|
| SEC-01 | Refus par défaut ; seule santé autorisée ; anonyme et rôles simulés testés | Statut, compte/site et affectation sans domaine métier : non implémentés |
| SEC-06 | CSRF conservé et mutations refusées ; pas de redirection HTML | Pas encore de login, session JDBC, cookie d'authentification ou MFA |
| SEC-14 | Erreurs MVC/validation et firewall expurgés, listes de champs bornées | Aucune garantie SQL, fichier, XSS frontend ou PDF démontrée |
| SEC-17 | Aucun utilisateur/mot de passe automatique ; pas d'endpoint de gestion ; pas de messages sensibles dans erreurs/logs testés | Pas de secret scan spécialisé, de coffre, de rôle SQL ou de TLS staging vérifié |
| SEC-19 | Parent/BOM, wrapper vérifié, tests CI définis, bibliothèques de test absentes du JAR | Scans/SBOM/provenance signée et CI distante non exécutés |

L'ensemble d'une SEC n'est pas réputé validé parce qu'un contrôle initial est présent. Les exigences financières, documents et identité restent celles du cahier et des ADR ; aucune certification ou aptitude production n'est affirmée.

## Prochaine tranche

**Lot 02**, sans arbitrage produit bloquant ses DTO/contraintes : protection effective des secrets Windows/OneDrive, résolution/scans d'images, Compose, PostgreSQL/PostGIS, privilèges, migrations initiales et packaging. Les services Docker existants d'autres projets ne doivent pas être réinitialisés. Le lot 02 n'a pas été exécuté par anticipation.
