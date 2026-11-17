# Preuves du lot 05 — comptes et parc cloisonnés

01/11/2026, Windows/JDK Oracle 25.0.5.0.2, Maven Wrapper 3.10.0, Boot 4.1.2, PostgreSQL 18.6/PostGIS 3.6.4 éphémères. **Pas d'approbation de production.** [Contrat et décisions](../lot-05-comptes-parc.md).

## E05-01 — périmètre et conservation

Responsable : ADMIN seul écrit, CLIENT lit son périmètre ; champs minimaux confirmés, création explicite depuis APPROVED, INDIVIDUAL limité au titulaire et BUSINESS multi-membres déjà validés. Aucun site par défaut, liste ou ALL explicite seulement. Pas d'enrôlement membre, fusion email ou suppression définitive.

Manifeste pré-lot **239 fichiers** vérifié et snapshot public hors dépôt : `%LOCALAPPDATA%/Temp/opencode/maintenance-lot05-20261101/baseline`. Toujours sans Git ; aucun init, commit, push ou déploiement. POM/dépendances et V1–V11 conservés ; V12–V13 additives. Aucune migration du volume Compose.

## E05-02 — exécutions

```powershell
$env:TEMP = Join-Path $env:LOCALAPPDATA 'Temp/opencode'
$env:TMP = $env:TEMP
$env:MAVEN_OPTS = '-Xmx384m -XX:MaxMetaspaceSize=256m'
.\mvnw.cmd -B -ntp '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' verify
python -B -m unittest discover -s scripts/tests -v
```

- Compilation `test-compile` réussie à 22:18:19, distincte de la recette.
- Recette ciblée à **22:21:17** : **111 Surefire + 15 Failsafe**, zéro échec/erreur/skip (13 nouveaux tests domaine, 14 HTTP/SQL, 1 migration V11→V13).
- Six tests Python réussis. Validateur indépendant **openapi-spec-validator 0.9.0** : **92 chemins/52 schémas**, OpenAPI 3.1/version 0.5.0. Inventaire contrôleurs/filtres étendu à GET/POST/PUT/PATCH ; CSRF explicite sur les mutations.
- **Vérification globale réussie à 22:26:29 : 111 Surefire + 132 Failsafe = 243 tests Java, zéro échec/erreur/skip.** Dont 13 tests domaine du lot 05, 17 `AccountsAssetsIT` et 1 migration V11→V13 ; suites des lots précédents conservées, y compris les 24 scénarios OIDC signés locaux et les 23 admissions HTTP/SQL.

`BadSqlGrammarException` lors du retrait temporaire d'INSERT sur `asset_change` est attendu : rollback testé, permission rétablie en `finally` sur la seule base éphémère. Avertissements Maven de métadonnées Flyway/Redgate HTTP 401 conservés, sans bloquer les builds acquis.

## E05-03 — matrice SEC, menaces et preuves

| SEC | Menace / contrôle | Tests et observations |
|---|---|---|
| 01/03/08/10 | Droits web courants, ADMIN actif/vérifié, preuves primaire/MFA <5 min ; CLIENT lecture seule | `clientIsReadOnlyAndEveryAdministrativeVerbRequiresMfaAndCsrf`, `everyNewAdministrativeMutationRefusesTechnicianAndPendingClientWithoutSideEffects`, expiration/suspension/cumul ; DB finale vérifiée |
| 02 | Portée comptes/sites avant pagination, total, filtres et projection privée | `twoAccountsAndHiddenSitesNeverLeakRowsTotalsContactsOrFilteredEquipment` : deux comptes, sites/UUID permutés, filtre site étranger/catégorie cachée, aucun contact/adresse/consigne dans le refus |
| 02/08 | Ni droit implicite ni restauration après réadhésion | `approvedDossierCreatesOneAccountAndNoDefaultSiteAccess`, `changedSiteRightsApplyWithTheSameSessionAndAllSitesExplicitlyIncludesFutureSites`, `memberRemovalAndReadditionCannotRestoreOldAllSitesRights` |
| 03/08 | APPROVED + identité active/vérifiée, type/titulaire fixes, pas de promotion par création de compte | `accountCreationCannotPromoteUnapprovedPendingOrUnverifiedIdentities`, `individualCannotAcquireAnotherMemberAndUnapprovedSuspendedOrMixedTargetsAreRefused` |
| 02/14/16 | Intégrité structurelle aussi hors HTTP | `postgresRejectsIncoherentRelationsInvalidAttributesAndHistoryMutation`, `postgresEnforcesIndividualOwnershipAndApprovedSourceWithoutRelyingOnHttp` : FK composites et CHECK |
| 03/14 | DTO fermés, aucun transfert par POST/PATCH ; attributs sans coercition | `equipmentCannotAttachOrMoveAcrossAccountsAndAttributesAreStrictlyValidated`, tests domaine des clés/fuseaux et champs privilégiés |
| 06/14/16 | CSRF, ETag/If-Match, concurrence et entrées bornées | `versionsProtectPatchReplacementAndConcurrentUpdatesWithoutChangingOwnership` : un gagnant et un 412 ; `scopeChangesRejectForeignSitesWrongAccountsAndStaleVersionsAtomically` ; limites de corps/pages |
| 17/20 | Historique append-only, références conservées, audit atomique et logs minimisés | `archivesHideTheWholePrivateSiteWithoutDeletingEquipmentOrAuditHistory`, `auditFailureRollsBackTheEntireSiteMutationAndDoesNotLogPrivateContent`, refus SQL DELETE/UPDATE/TRUNCATE |

Les événements de portée conservent les UUID de sites accordés, pas les coordonnées. Contacts de site et utilisateurs sont distincts ; aucune adresse ne rattache/authentifie automatiquement une personne.

## E05-04 — migrations et architecture

`AccountsAssetsMigrationIT` prépare V11 avec identité/session, applique exactement V12/V13, compare les anciens checksums, valide Flyway et rejoue sans action. Aucun compte créé par la migration ; session conservée. `AdmissionMigrationIT` vérifie aussi la montée depuis V7 vers le schéma courant.

Propriétaires : clients = comptes/adhésions ; assets = sites/contacts/équipements/habilitations ; identité = sessions/rôles/statuts ; audit = trace technique. Ports publics `BusinessIdentity`/`ClientAccountAccess`, sans SQL interrepository ni cycle. ArchUnit conservé. Aucun clean/repair Flyway, migration descendante ou réécriture d'une migration appliquée.

## E05-05 — fin de lot

Résultats bruts et synthèses privés sous `%LOCALAPPDATA%/Temp/opencode/maintenance-lot05-20261101/reports`, jamais dans les sources publiques : `maven-verify-final.log`, `java-test-summary.json`, `source-diff-summary.json`, `known-secret-check.json`, `source-secrets.trivy.json`, `docker-final.txt`.

- XML Surefire/Failsafe relus : **111 + 132 = 243**, zéro échec/erreur/skip ; build final du 01/11/2026 à **22:26:29**.
- Comparaison au snapshot : **29 fichiers ajoutés, aucun supprimé**, POM/dépendances et toutes les migrations V1–V11 inchangés. Les corrections de contrat/tests existants sont ciblées ; aucun test ancien supprimé ou ignoré.
- Sources : scan léger **Trivy natif 0.76.1**, secret uniquement et hors ligne, **zéro constat** ; `target`/`.git` exclus, pas le corpus public de mots de passe. Aucun nouveau scan lourd d'image/dépendance ni téléchargement de grosse base. SBOM/OSV historique du lot 03 conservé hors dépôt, pas présenté comme un scan courant.
- **Neuf secrets privés connus et clé MFA décodée de 32 octets** recherchés sans impression de leurs valeurs : aucune correspondance dans les sources et **42 259 entrées JAR, bibliothèques imbriquées comprises**. Aucune fixture de test dans le JAR.
- JAR final SHA-256 : **`d1eea0aa43d5dfec47a5acace74ad3f4e827991addf5e0995f551dea0668ddcc`**. Les empreintes des preuves des lots 03/04 décrivent leurs anciens artefacts.
- Docker final en lecture seule : services `maintenance-dev` DB/Mailpit sains, autres projets `taxikiwi-*` toujours arrêtés, aucun conteneur de test restant. Transaction SQL READ ONLY : historique persistant **une seule ligne, version 1** ; aucune migration de ce volume par le lot 05. Aucun démarrage/arrêt/redémarrage ou suppression de service persistant.
- Manifeste courant **268 fichiers publics** ; `public_manifest.py --write` puis `validate-pack.py` contrôlent les sources et les scripts, pas la sécurité de production. Aucun artefact, secret, rapport brut ou configuration privée dans cet inventaire.

## Non vérifié / limites

Frontends, charge/p95 et contention à l'échelle, exports/documents/téléchargements, historique d'intervention, géocodage, nouvelle image, CI distante, TLS/proxy de staging, restauration, rétention/purge, revue indépendante et pentest non exécutés. Une structure OpenAPI valide ne démontre pas les droits HTTP.

Google réel/staging non validé et désactivé. Approbation technicien bloquée sans justificatifs et chaîne documentaire du lot 09. Constats PostGIS du lot 02 (6 observations critiques/126 élevées), patch Temurin et scans images incomplets toujours ouverts. Aucune conformité ASVS intégrale ou approbation production revendiquée.
