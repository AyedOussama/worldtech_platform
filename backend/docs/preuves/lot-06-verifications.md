# Preuves du lot 06 — contrats et tickets

02/11/2026. **Recette locale finale réussie : 293 tests Java + 6 Python**, sans échec/erreur/skip. Pas d'approbation de production. [Décisions produit et contrat](../lot-06-contrats-tickets.md).

## E06-01 — décisions et conservation

Six arbitrages confirmés : forfaits HT EUR distincts, montants chacun facultatifs/null non assimilé à zéro, déclaration hors contrat autorisée ; couvertures/révisions explicites ; calendriers/SLA sans engagements ou jours fériés inventés ; cycle ADMIN puis confirmation/réouverture CLIENT avant clôture. Aucun prix publié technicien, TVA ou facture déduit.

Manifeste pré-lot **268 fichiers** vérifié ; snapshot public hors dépôt `%LOCALAPPDATA%/Temp/opencode/maintenance-lot06-20261102/baseline`. Snapshots antérieurs préservés. Dossier sans Git ; aucun init, commit, push, déploiement ou migration du volume Compose. Comparaison au snapshot : POM et V1–V13 inchangés ; seules des migrations additives V14–V16 introduites.

## E06-02 — exécutions

```powershell
$env:TEMP = Join-Path $env:LOCALAPPDATA 'Temp/opencode'
$env:TMP = $env:TEMP
$env:MAVEN_OPTS = '-Xmx384m -XX:MaxMetaspaceSize=256m'
.\mvnw.cmd -B -ntp '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' verify
python -B -m unittest discover -s scripts/tests -v
```

- Compilation séparée réussie à **10:38:55**, distincte de la recette.
- Première recette : 35 tests domaine/architecture réussis et migration V13→V15 réussie ; démarrage HTTP échoué sur une option Jackson 2 déplacée dans Jackson 3. Correction vérifiée sur les classes réellement résolues (`EnumFeature`, propriété `spring.jackson.datatype.enum`) ; aucune dépendance changée, aucun skip de contournement.
- Recette ciblée à **10:48:08** : **137 Surefire + 21 Failsafe**, zéro échec/erreur/skip. Elle précède V16 et les compléments de concurrence, et n'est pas présentée comme le résultat final.
- Validation indépendante OpenAPI 0.6.0 / 3.1.0 : **117 chemins, 83 schémas**. Inventaire exact contrôleurs/filtres GET/POST/PUT/PATCH étendu aux nouvelles routes.
- **Vérification globale V1–V16 réussie à 10:54:23 : 138 Surefire + 155 Failsafe = 293 tests Java**, zéro échec/erreur/skip. Dont 10 `ContractCalendarDomainTest`, 17 `TicketDomainTest`, 22 `ContractsTicketsIT` et 1 `ContractsTicketsMigrationIT` ; suites antérieures conservées et retestées.
- **Six tests Python réussis**, commande séparée. Validation OpenAPI indépendante relancée après la mise à jour finale des codes d'erreur, sans nouvelle dépendance Maven.

Les erreurs de retrait temporaire d'INSERT audit/outbox sont intentionnelles : base éphémère seulement, droits rétablis en `finally`, état final et rollback vérifiés. Les avertissements Maven de métadonnées Flyway/Redgate 401 restent visibles ; les artefacts utilisés sont résolus et les builds acquis ci-dessus ont réellement terminé.

## E06-03 — matrice SEC et tests

| SEC | Menace / contrôle | Preuves exécutables |
|---|---|---|
| 01/02/08 | Compte/site avant détails, filtres, total et curseurs ; perte de droit au rejeu | `currentSiteRightsRejectCreationWrongEquipmentAndEveryPrivateSubresourceBeforeTotals`, `listsAndCountsUseTheSameSiteAndRequestedPriorityRemainsDistinct`, `lossOfSiteOrMembershipAccessAlsoRefusesAReceiptReplayWithTheSameSession` ; deux comptes, plusieurs sites et mauvais équipements |
| 02/03/10 | Contrats ADMIN, projection CLIENT d'un seul site sans notes/autres couvertures | `clientContractProjectionContainsNeitherOtherSitesNorInternalNotesOrReasons`, `contractCoverageCommandsRejectOtherAccountsOrSitesAndCalendarErrorsWithoutCreatingRows`, `everyAdministrativeAndClientMutationRefusesWrongRolesPendingAndPreMfa` |
| 03/14/16 | Forfaits HT/EUR, valeurs facultatives/zero, pas de mass assignment, argent exact | `proposalsPreserveNullAndZeroAndRejectNumericJsonNegativeOrExcessPrecision`, `TicketDomainTest`, `csrfBodyLimitsDtosAndTerminalStatesDoNotAllowMassAssignmentOrBypass` ; SQL scale/maximum/NaN/négatif refusés |
| 02/14/16 | FK composites de couverture et immutabilité SQL | `postgresEnforcesMoneyScaleCompositeRelationsAndImmutableRuntimePrivileges` : transactions SQL fraîches isolent les vraies erreurs FK 23503, puis ajouts tardifs aux définitions refusés, UPDATE/DELETE/TRUNCATE runtime refusés |
| 06/08/10 | CSRF, MFA/preuve courante, attente/suspension et archivage | Matrice de toutes les mutations nouvelles et `archivedSitesSuspensionAndStaleAdministrativeProofCloseAllNewAccessPaths` ; pas de route mobile métier |
| 14/16 | Réception autorisée avant rejeu, contenu différent/clé expirée, concurrence réelle | `declarationWithoutEquipmentContractOrMoneyIsIdempotentAndPreparesOnlyMetadata`, `mismatchedAndExpiredIdempotencyKeysNeverCreateAdditionalEffects`, `concurrentCreationWithOneKeyHasOneEffectAndConcurrentQualificationHasOneWinner` |
| 16 | Pas d'écrasement de proposition ou révision concurrente | `concurrentProposalAndQualificationCannotSilentlyOverwriteTheVersionSeenByEitherActor`, `concurrentContractRevisionsKeepExactlyOneNewImmutableDefinition` ; état SQL final et un seul gagnant |
| 02/17/20 | Notes internes séparées jusqu'aux curseurs/versions et absence de contenu dans logs | `publicExchangesAndCursorsNeverIncludeInternalNotesOrTheirCounts`, `auditOrOutboxFailureRollsBackTicketProposalHistoryAndReceiptTogether` ; aucun contenu libre dans outbox/audit technique |
| 14/16/20 | Historique du contrat et fuseau figés ; pas de SLA ou clôture implicite | `aContractRevisionAndTheSiteTimeZoneRemainFrozenForAnAlreadyQualifiedTicket`, `qualificationChecksHalfOpenValidityAtDeclarationNotAtTheLaterAdminRequest`, `equipmentOnlyCoverageCannotCoverAGeneralIncidentOrAnotherEquipment`, `waitingAndResumptionKeepAnEventTrailWithoutInventingASlaPause`, résolution/réouverture/clôture sans mission |

`ContractCalendarDomainTest` vérifie bornes [début,fin), couverture générale/sélection, objectifs/plages dupliqués, fermeture/ouverture datées, absence de jour férié implicite, fuseaux Europe/Paris et Indian/Reunion, journées 23/25 h et plage partielle répétée (aucun faux comptage entre les deux occurrences). Le calcul borné des secondes de service n'est pas un calcul d'échéance, un worker d'alerte ou un résultat statistique de production.

## E06-04 — migrations et compatibilité

`ContractsTicketsMigrationIT` prépare V13 avec identité/réception d'admission préexistantes, applique V14–V16, compare les anciens checksums, valide Flyway et rejoue sans nouvelle migration. Ancienne réception conservée (version/état inchangés, nouvelle référence nullable), aucun ticket/contrat créé implicitement. Les suites de montées depuis V7/V11 et les parcours identité/admission/parc sont conservés.

Runtime : pas de DDL, suppression ou mise à jour des propositions/révisions/historiques. Enfants d'une définition insérables uniquement dans la transaction de création de la révision (V16), sans fonction SECURITY DEFINER. Retour à l'application pré-lot 06 possible sur les nouvelles tables laissées en place, sans down-migration. Aucun scénario de déploiement réel revendiqué.

## E06-05 — contrôles de clôture

Rapports bruts privés sous `%LOCALAPPDATA%/Temp/opencode/maintenance-lot06-20261102/reports` : `maven-verify-final.log`, `java-test-summary.json`, `source-diff-summary.json`, `known-secret-check.json`, `source-secrets.trivy.json`, `docker-final.txt`. Aucun de ces rapports ou secret n'entre dans le manifeste public.

- XML des rapports relus indépendamment : **138 + 155 = 293**, zéro échec/erreur/skip ; fin du build le **02/11/2026 à 10:54:23+01:00**. Six tests Python et validation OpenAPI indépendante exécutés séparément.
- Comparaison au snapshot : **32 fichiers ajoutés, aucun supprimé**, POM/dépendances et toutes les migrations V1–V13 inchangés. Les tests de migration/inventaire existants sont étendus, pas supprimés ou ignorés.
- Scan sources léger **Trivy natif 0.76.1**, secret uniquement/hors ligne : **zéro constat** le 02/11 à 10:58 ; `target`/`.git` exclus, pas les sources/tests ou le corpus public. Aucun téléchargement de base de vulnérabilités ou gros scan d'images relancé.
- Recherche sans affichage de **neuf secrets privés connus + clé MFA décodée de 32 octets** : aucune correspondance dans les sources ni **42 345 entrées du JAR, bibliothèques imbriquées comprises** ; aucune fixture de test dans l'artefact.
- JAR `target/maintenance-api.jar` SHA-256 : **`279ec3b2ae9fd550a3c0ac7d568ef53c9b646b23b7a6eda836dcc401ccd5c641`**. Les empreintes des lots antérieurs décrivent leurs artefacts historiques.
- Docker final, lecture seule : DB/Mailpit `maintenance-dev` sains, `taxikiwi-*` toujours arrêtés, aucun conteneur de test restant. Transaction SQL READ ONLY : base persistante **une ligne Flyway, version 1**. Ne pas confondre réussite V16 sur Testcontainers et migration de ce volume. Aucun service persistant démarré/arrêté/redémarré ou supprimé.
- Corpus public courant : **300 fichiers**. Liens locaux, `public_manifest.py --write` puis `validate-pack.py` contrôlent cohérence et scripts, pas la sécurité de production. Aucun artefact/rapport brut/configuration privée dans cet inventaire.

## Non vérifié et réserves

Frontends, jobs/calculs d'échéances/alertes/statistiques SLA, notifications complètes, missions/affectations, documents/quarantaine/antivirus, charge/p95, rétention/purge complète, image runtime renouvelée, scan dépendances/images actualisé, CI distante, TLS/proxy/staging, restauration, revue humaine indépendante et pentest non exécutés. Validation de structure OpenAPI et inventaire exact ne démontrent pas toutes les réponses contre JSON Schema.

POM inchangé : ne pas transformer les scans dépendances/SBOM historiques du lot 03 en scans courants. Google réel/staging non validé et désactivé ; approbation technicien toujours bloquée sans justificatifs contrôlés au lot 09. Constats PostGIS du lot 02 (6 observations critiques/126 élevées), patch Temurin et scans images incomplets toujours ouverts. Aucun succès de tests n'annule ces réserves de production.
