# Preuves et limites — lot 07

18/11/2026. Recette locale sous Windows, JDK **25**, Maven Wrapper, PostgreSQL/PostGIS réel via Testcontainers. Aucune CI distante, aucun déploiement, aucune approbation de production.

## Commandes exécutées

```powershell
$env:JAVA_HOME='C:\Users\MSI\AppData\Local\Temp\opencode\jdk25\jdk-25.0.5+8'
.\mvnw.cmd '-Dtest=IdentityContractTest' test
.\mvnw.cmd verify
```

Résultat final de `verify` :

```text
Tests run: 154, Failures: 0, Errors: 0, Skipped: 0   (Surefire)
Tests run: 166, Failures: 0, Errors: 0, Skipped: 0   (Failsafe)
BUILD SUCCESS
Total time: 03:28 min
```

Soit **320 tests Java**, zéro échec/erreur/skip. Le contrat OpenAPI statique est contrôlé par `IdentityContractTest` : il doit documenter exactement les routes livrées, avec sécurité explicite et CSRF sur les mutations WEB.

## Couverture fonctionnelle réellement exercée

- `MissionDomainTest` (16 tests) : publication, révisions immuables, montants exacts sans arrondi, frais facultatifs distincts de zéro, compétences dédupliquées et triées, coordonnées/fenêtre/textes bornés.
- `MissionsIT` (10 tests) : création/publication/révision idempotentes, refus de mauvais site/ticket/contrat, absence de fuite de texte privé dans la projection ADMIN, concurrence de publication, contraintes SQL de coordonnées/montants et privilèges runtime, découverte bornée, requêtes invalides, techniciens inactifs, plan d'exécution sur une grille réaliste.
- `MissionsMigrationIT` : montée jusqu'à **V18** et présence de l'index de découverte.
- `DatabaseInfrastructureIT` : 18 migrations appliquées.
- Tests de migration des lots antérieurs : compteurs relevés à 7 (comptes/parc), 11 (admission), 13 (contrats/tickets), 16 (missions).

Les logs Flyway de la suite montrent PostgreSQL **18.6**, puis :

```text
Migrating schema "app" to version "17 - missions publication and financial revisions"
Migrating schema "app" to version "18 - mission discovery indexes"
Successfully applied 5 migrations to schema "app", now at version v18
Successfully validated 18 migrations
```

La base de développement a ensuite été migrée par un redémarrage réel du JAR local : `Current version 17` → `Migrating to 18` → `Successfully applied 1 migration, now at v18`. Le backend écoute sur `127.0.0.1:18080` et `GET /health` répond `{"status":"UP"}`.

## Démonstration ADMIN locale BE07.1

Un ADMIN local de démonstration (`admin.demo@worldtech.local`) a été créé ponctuellement par un fixture hors API utilisant `AdministratorBootstrapService`. Le fixture source et sa classe compilée ont ensuite été supprimés ; aucun compte par défaut ni secret n'est livré dans le dépôt. Le mot de passe et le secret TOTP ont été générés localement, stockés hors workspace avec ACL privées, puis jamais affichés dans les preuves.

Parcours réellement exécuté :

1. Vérification email via le lien reçu dans Mailpit, puis enrôlement MFA TOTP et confirmation.
2. `GET /api/web/v1/me` → `200`, statut `ACTIVE`, rôle `ADMIN`, email vérifié.
3. Après expiration de la session : `POST /auth/login` → `200 {"status":"MFA_PENDING"}`, `POST /auth/mfa/challenge` → `204`, puis `/me` → `200`. La protection MFA et la rotation de session sont donc exercées hors tests.
4. Un compte client et un site synthétiques ont été préparés en base de développement par SQL (fixture de démonstration), faute de dossier client APPROVED existant. Les APIs ADMIN existantes ont ensuite créé le compte et le site : `POST /api/web/v1/admin/client-accounts` → `201` (`8bbb9301-a0b6-4b99-b0fd-7eb5804fa4c3`), `POST .../sites` → `201` (`d421d526-4e9c-47d1-9048-aca8777882f0`).
5. `POST /api/web/v1/admin/client-accounts/{account}/sites/{site}/missions` → `201` avec `DRAFT`/version `0` ; le détail montre `financial: null`.
6. `POST .../missions/{mission}/publish` avec `If-Match: "0"` → `200`, `PUBLISHED`/version `1`, rémunération `150.00`, frais `20.50`.
7. `POST .../missions/{mission}/financial-revisions` avec `If-Match: "1"` → `200`, `PUBLISHED`/version `2`, rémunération `180.00`, frais `null`.
8. `GET .../missions/{mission}` → version courante avec révision financière n°1 et frais `null` ; `GET .../financial-revisions` → deux entrées (`150.00/20.50`, puis `180.00/null`), auteur ADMIN.

La mission de démonstration est `5e2f07b9-6fea-430b-b57c-b63fc74c8b19`. Les données client ont été injectées par SQL uniquement pour la recette locale : cela ne prouve pas le parcours complet invitation → onboarding → approbation client.

## Incidents de recette corrigés

1. **BadSqlGrammar au premier test de découverte.** Le pilote PostgreSQL `42.7.14` ne gère pas `java.time.Instant` dans `PgPreparedStatement.setObject(int,Object)` et refuse d'inférer le type SQL. `JdbcMissionDiscovery` convertit désormais les bornes en `java.sql.Timestamp`. Vérifié par `javap` sur le pilote et par la suite d'intégration.
2. **Découverte voyant trois missions au lieu d'une.** La base de test est partagée et le rôle runtime `maint_app` n'a volontairement pas `DELETE`; un nettoyage par suppression n'est pas autorisé. Les tests de découverte utilisent désormais des coordonnées distantes et propres à chaque test (`58.8566/12.3522` puis `38.8566/-7.6478`), sans modifier les données des autres tests.
3. **Contrat OpenAPI incomplet après ajout des routes.** `IdentityContractTest` a refusé les sept opérations missions absentes de la liste des contrôleurs inspectés. Le test inclut maintenant `MissionController` et `MissionDiscoveryController` ; la documentation statique a été portée à 0.7.0 (122 chemins, 94 schémas).

## Non vérifié / limites

- Aucun test de charge, de disponibilité, de reprise après sinistre ou de dimensionnement du rayon maximal.
- Aucune revue humaine indépendante, aucun scan de l'image finale ni SBOM pour ce lot.
- Aucun parcours réel d'approbation TECHNICIAN (bloqué avant le lot 09) ; les tests activent une fixture par SQL.
- Aucune affectation, aucun détail privé de mission, aucun document, frontend, email ou notification complète.
- L'ADMIN local de démonstration n'a pas été créé via le bootstrap interactif `System.console()` : un fixture temporaire hors API a été utilisé, puis supprimé. Aucun compte par défaut ni secret n'est livré ; ce contournement local ne constitue pas une procédure de production.
- Le compte client et son site de démonstration sont des fixtures SQL, pas le parcours complet invitation → onboarding → approbation. Aucune affectation, découverte réelle avec TECHNICIAN ACTIVE, notification, facture ou paiement n'est démontrée. Google, staging, charge et revue indépendante restent non vérifiés.
