# Preuves du lot 04 — invitations et admission

01/11/2026, Windows/JDK Oracle 25.0.5.0.2, Boot 4.1.2, Maven Wrapper 3.10.0, PostgreSQL 18.6/PostGIS 3.6.4 en conteneurs éphémères. **Pas d'approbation de production ni de clôture fonctionnelle de l'admission technicien sans justificatifs.**

## E04-01 — décisions et périmètre

Le responsable a confirmé les champs minimaux client/technicien dans [le contrat](../lot-04-onboarding.md). Les justificatifs sont **obligatoires pour TECHNICIAN uniquement**. Les pièces exactes et la chaîne documentaire/antivirus du lot 09 manquent : l'approbation technicien refuse `TECHNICIAN_DOCUMENTS_REQUIRED`, également par contrainte SQL. Aucun switch permettant de déclarer les documents facultatifs ou CLEAN.

`backend` reste sans Git ; l'état a été inspecté sans `git init`, commit ou push. Snapshot public pré-lot sous `%LOCALAPPDATA%/Temp/opencode/maintenance-lot04-20261101/baseline`, ancien manifeste de 201 fichiers vérifié avant modification. Aucun fichier préexistant supprimé ; migrations V1–V7 et POM inchangés. Aucun frontend, ticket, site, compte métier client/membre, upload, vrai compte utilisateur, email externe ou déploiement. Aucun conteneur/volume d'autre projet modifié.

## E04-02 — vérifications exécutées

```powershell
$env:TEMP = Join-Path $env:LOCALAPPDATA 'Temp/opencode'
$env:TMP = $env:TEMP
$env:MAVEN_OPTS = '-Xmx384m -XX:MaxMetaspaceSize=256m'
.\mvnw.cmd -B -ntp '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' verify
python -B -m unittest discover -s scripts/tests -v
```

- Recette locale ciblée à **19:29:56** : `clean verify -Dit.test=OnboardingIT,AdmissionMigrationIT`, **98 Surefire + 16 Failsafe**, zéro échec/erreur/skip.
- Première vérification globale à **19:37:03** : **98 Surefire + 106 Failsafe = 204 tests**, zéro échec/erreur/skip ; inclut les 24 scénarios OIDC locaux signés (19 historiques + 5 nouveaux).
- Six tests Python réussis (ACL, conservation/idempotence des secrets, verrou d'images). Le validateur OpenAPI indépendant **openapi-spec-validator 0.9.0** déjà isolé hors dépôt valide **71 chemins, 35 schémas**. Aucun package Python supplémentaire n'a été ajouté au projet.
- Vérification globale après compléments SQL/droits à **19:41:10** : **98 Surefire + 112 Failsafe = 210 tests**, zéro échec/erreur/skip.
- Revue finale : deux tests supplémentaires ferment le cumul CLIENT+TECHNICIAN à l'accès, au rejeu et à la décision (23 `OnboardingIT` réussis). La relance globale à **20:20:23** a néanmoins échoué sur `AdmissionMigrationIT` avant migration : PostgreSQL encore en démarrage (`57P03`) après détection du port. Le signal de disponibilité de la fixture éphémère a été renforcé par un vrai `SELECT 1` TCP avec le migrateur ; délai borné de 120 s, aucun skip/retry d'assertion métier, aucune modification d'infrastructure persistante.
- **Vérification complète finale réussie à 20:24:13 : 98 Surefire + 114 Failsafe = 212 tests Java, zéro échec, erreur ou skip.** Totaux relus depuis les XML Surefire/Failsafe ; 11 tests de domaine d'admission, 23 `OnboardingIT`, 1 `AdmissionMigrationIT`, 24 scénarios OIDC locaux et suites identité/SQL historiques conservées. Rapport complet hors dépôt `reports/maven-verify-validated.log`, synthèse `reports/java-test-summary.json`. Six tests Python relancés avec succès après la revue.

Un échec intermédiaire concernait une assertion SMTP qui attendait `SENT` au lieu de l'état réellement défini `DELIVERED`. Assertion corrigée en conservant l'envoi SMTP réel, l'acquittement SQL et l'effacement du corps ; aucun test supprimé, skip ajouté ou droit SQL désactivé pour obtenir un succès. Les deux avertissements Maven de métadonnées Flyway/Redgate HTTP 401 persistent sans bloquer les builds.

Les deux erreurs applicatives `BadSqlGrammarException` du build final sont attendues dans le scénario retirant temporairement INSERT sur l'outbox de la base éphémère ; droits rétablis en `finally`, rollback complet des invitations/comptes vérifié. Ce ne sont ni des erreurs de test ni des modifications des droits de la base persistante.

## E04-03 — matrice SEC et attaques

| Exigences | Comportement / menace | Preuves |
|---|---|---|
| SEC-01/03/10 | Seul ADMIN actif avec email et preuve primaire/MFA récents invite/décide ; absence d'auto-approbation | `OnboardingIT.allAdministrativeOperationsNeedMfaAndFreshProofAndCannotReactivateSuspension`, matrice de refus de chaque route administrative, DTO fermés ; tests MFA historiques conservés |
| SEC-02 | Dossier propre issu du principal, listes/détails ADMIN uniquement, pagination bornée | Identité CLIENT erronée, deux clients et deux techniciens ; changement d'UUID ou champ userId sans lecture/écriture croisée ; listes sans total ou profil privé |
| SEC-03/08 | Ni rôle ni propriétaire ni APPROVED/CLEAN depuis formulaire ; refus technicien sans pièces | `technicianEvidenceIsMandatoryAndCannotBeClaimedByADtoOrAnAdministrator`, contrainte SQL `technician_evidence_required`, rôle/statut/outbox inchangés après refus |
| SEC-03/08 | Pas d'activation technicien indirecte par le statut global d'un compte client | `clientApprovalCannotActivateAnAddedTechnicianRoleOrReplayItsOldSubmission`, `aMixedTechnicianCannotAcceptAClientInvitationOrReplayAnEarlierMobileSubmission` : cumul ajouté après authentification/soumission refusé, PENDING conservé, aucune nouvelle décision/outbox ni consommation d'invitation |
| SEC-04/05 | Réutilisation Argon2 borné et quotas SQL avant calcul ; pas d'énumération explicite par doublon technicien | `technicianRegistrationDoesNotRevealExistingMailboxesOrChangeTheirCredentials`, limites partagées et en-tête X-Forwarded-For sans contournement ; pas de garantie statistique sur le timing |
| SEC-06/07 | CSRF web, mobile sans cookie, autorisation courante par canal | Matrice sans CSRF/pré-MFA, `cookiesNeverAuthenticateMobileAndTwoApplicantsCannotReadPrivateDossiers`, tous les tests web/mobile du lot 03 |
| SEC-08/09 | Invitation expirée/remplacée/double/concurrente ; identité/destinataire et statut actuels | Tests d'invitation de `OnboardingIT`, FK composite destinataire ; aucune activation par GET, vérification email séparée, suspension/refus non annulés |
| SEC-11/12 | Inscription Google explicite sans fusion ; preuve OIDC et handoff conservés | Cinq nouveaux `GoogleIdentityIT` : inscriptions web/mobile PENDING/DRAFT, client invité, mauvais email/lien remplacé, conflits email/triplet sans compte parasite ; fixtures signées locales seulement |
| SEC-09/11 | Pas de reset ajoutant un password à un accès externe seul ; unlink sans alternative refusé | `explicitGoogleTechnicianRegistrationCreatesOnlyPendingIdentityAndDraftNotApprovalOrPassword`, contrôles à l'émission/prévalidation/consommation du reset |
| SEC-14/16 | Versions fortes, concurrence et idempotence réautorisée | Sauvegardes/décisions simultanées, révision unique, 412 ; soumission commune web/mobile, même clé/version même résultat, autre version 409 et rejet après révocation |
| SEC-17/20 | Effets externes atomiques, audit sans corps privé, historiques immuables | Mailpit éphémère, outbox chiffrée puis effacée après livraison, rollback complet lors d'une panne INSERT outbox ; UPDATE/DELETE/TRUNCATE des historiques et réceptions refusés au runtime |

La suite n'invente aucune route ticket/candidature pour faire passer la recette : ces chemins restent fermés, l'identité PENDING n'a pas d'autorité métier active. Les futurs tests multi-sites/compteurs/exports et les compétences effectivement vérifiées appartiennent aux lots suivants.

## E04-04 — données, migrations et compatibilité

- V8 : invitations/liens, dossiers client/technicien, snapshots d'historique et réceptions techniques de soumission. Droits DML explicites ; historique/réceptions append-only pour `maint_app`.
- V9 : FK composite dossier → invitation/destinataire accepté ; blocage SQL d'approbation technicien sans contrôle documentaire.
- V10 : contexte d'inscription Google explicite, distinct du LOGIN ordinaire qui reste incapable de créer un compte.
- V11 : contrôle total du contexte optionnel, empêchant le `NULL` SQL de faire passer un tuple d'inscription incomplet.
- `AdmissionMigrationIT` migre une base neuve jusqu'à V7, insère utilisateur/session avec l'ancienne forme INSERT, puis applique les migrations nouvelles. Les checksums V1–V7, l'utilisateur et la session sont conservés ; revalidation et seconde migration sans opération.

Ordre : migrateur séparé, puis runtime sans credentials migrateur. Pas de clean/repair Flyway, migration descendante, suppression de volume ou initialisation SQL rejouée sur les données persistantes. Le test d'évolution ne vaut pas restauration réelle ni test d'un ancien binaire complet.

## E04-05 — revue et scans ciblés

Contrat courant : `openapi/maintenance-api.json`, inventaire exact des routes/filtres comparé par `IdentityContractTest`, CSRF/security de chaque opération explicites. La validation indépendante contrôle la structure ; les tests HTTP/SQL apportent les preuves de comportement, pas l'inverse.

POM inchangé : aucune nouvelle bibliothèque/dépendance, aucun téléchargement de grosse base de vulnérabilités et aucun nouveau scan lourd d'image. Le contrôle OSV/SBOM daté du lot 03 reste historique et ne devient pas un nouveau scan production. Le `clean` de recette a retiré l'ancien `target/bom.json` ; sa copie historique est conservée hors dépôt, sans la présenter comme un nouveau SBOM du lot 04.

- Sources : scan léger **Trivy natif 0.76.1**, `fs --scanners secret --offline-scan --skip-db-update --skip-java-db-update --skip-dirs target --skip-dirs .git`, **zéro constat**. Aucune exclusion cachée du corpus public de mots de passe ; rapport `reports/source-secrets.trivy.json` hors dépôt.
- Recherche ciblée des **neuf secrets privés connus et de la clé MFA décodée de 32 octets** : aucune correspondance dans les sources publiques et **42 202 entrées du JAR, bibliothèques imbriquées incluses**. Aucune fixture de test dans le JAR. Rapport `reports/known-secret-check.json` ; aucune valeur secrète imprimée.
- JAR final `target/maintenance-api.jar`, SHA-256 **`944b1a36227f8035cc32a8b56c24a6415afe492afc208d68efdaef3a7d592778`**. Empreinte vérifiée après succès du build, identique à l'artefact contrôlé. L'empreinte de la preuve du lot 03 décrit uniquement l'ancien JAR.
- Manifeste public courant : **239 fichiers**, sans artefact, secret, rapport brut ou configuration privée. Diff depuis le snapshot pré-lot : 38 fichiers ajoutés, aucun supprimé ; V1–V7/POM inchangés. `scripts/public_manifest.py --write` puis `scripts/validate-pack.py` contrôlent la cohérence du pack, pas la sécurité de production.

Contrôle Docker final **en lecture seule** : DB/Mailpit `maintenance-dev` sains ; autres conteneurs `taxikiwi-*` toujours arrêtés, aucun conteneur de test restant. Requête SQL sous transaction READ ONLY : historique persistant **une seule ligne, version 1**, donc aucune migration du volume Compose par ce lot. Aucun démarrage/arrêt/redémarrage/suppression de ces services demandé. Rapport `reports/docker-final.txt` hors dépôt.

## Non vérifié / blocages conservés

- **Approbation TECHNICIAN non livrable en succès** avant choix des pièces exigées et mise en œuvre/test du stockage privé, de l'antivirus et du contrôle d'admission documentaire. Aucun justificatif réel demandé ou téléversé dans ce lot.
- Frontends et stockage Flutter sur appareil ; Google réel/staging/App Links, DNS/TLS/proxy/cookies/APM effectifs.
- Nouvelle image runtime, CI distante, charge cible, rétention/purge complète des dossiers/invitations/historiques, rotation/restauration opérationnelle, revue humaine et pentest indépendants.
- Constats PostGIS du lot 02 (6 observations critiques/126 élevées), patch Temurin et scans images incomplets : non résolus par ces tests.

Suite métier possible : lot 05, sans exécution automatique ; le lot 09 devient une dépendance explicite de l'approbation technicien. Aucune approbation de production, conformité ASVS intégrale ou clôture sans réserve du lot 04 n'est revendiquée.
