# Backend de maintenance — développement

Le backend Boot 4.1.2/Java 25 fournit identité/admission/parc, contrats immuables, calendriers, tickets et missions du lot 07. CLIENT déclare dans son site autorisé, propose facultativement des forfaits HT/EUR et confirme la résolution ; ADMIN qualifie, traite et publie les conditions d'une mission par révisions immuables ; TECHNICIAN actif découvre les missions publiées dans un rayon borné. Notes internes séparées, aucun droit site implicite ou transfert intercompte. **Approbation technicien toujours bloquée sans pièces contrôlées (09), Google désactivé.** Aucun compte/ADMIN par défaut, frontend, affectation ou upload. [Lot 07](docs/lot-07-missions.md), [preuves et limites](docs/preuves/lot-07-verifications.md).

## Prérequis et commandes

- JDK **25** ; Temurin 25 prévu en CI, JDK Oracle 25 utilisé lors de la vérification locale.
- Accès HTTPS à Maven Central au premier lancement. Maven global n'est pas requis : le wrapper fournit **Maven 3.10.0**.
- Docker Engine/Desktop en mode conteneurs Linux et Compose ; `verify` exige Docker pour les tests SQL réels.
- Python 3.11+ ; sous Windows, 3.11.10+ ou version ultérieure prenant en charge les ACL privées de `mkdir(0700)`. Les ACL sont vérifiées, pas présumées correctes.
- Les scripts refusent OneDrive pour les secrets. Aucun compte administrateur n'est requis pour compiler ou tester. Un ADMIN de démonstration local a été créé hors dépôt pour la recette BE07.1 ; aucun compte ni secret par défaut n'est livré.

Depuis `backend`, sous PowerShell :

```powershell
.\mvnw.cmd -version
python -B scripts/init-dev.py
$env:MAINT_LOCAL_DIR = python -B scripts/init-dev.py --print-local-dir
$env:MAINT_SECRETS_DIR = "$env:MAINT_LOCAL_DIR/secrets"
python -B scripts/lock-images.py
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml up -d --wait db mailpit
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Sous Bash avec un JDK 25 accessible :

```bash
chmod +x mvnw
./mvnw -version
python3 -B scripts/init-dev.py
export MAINT_LOCAL_DIR="$(python3 -B scripts/init-dev.py --print-local-dir)"
export MAINT_SECRETS_DIR="$MAINT_LOCAL_DIR/secrets"
python3 -B scripts/lock-images.py
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml up -d --wait db mailpit
./mvnw verify
./mvnw spring-boot:run
```

Le Bash Windows vérifié est **Git Bash**, pas le Bash WSL associé au Python du pack. Sous WSL/Linux, installer/configurer son propre JDK 25 ; le JDK Windows ne démontre pas sa présence dans WSL. `unzip` et `sha256sum`/`shasum` sont nécessaires pour télécharger et vérifier la distribution ZIP avec le script Unix. Ne pas retirer le checksum pour contourner une erreur.

Le wrapper Apache **3.3.4**, type `only-script`, ne nécessite pas de JAR wrapper dans Git. Sa distribution Maven est figée par SHA-256, calculé après comparaison de l'archive au SHA-512 officiel Apache. Origines, génération et preuve négative de checksum : [preuve du lot 01](docs/preuves/lot-01-verifications.md).

`verify` exécute Surefire (unitaires, HTTP, architecture, validation du migrateur), puis Failsafe (bases PostGIS/Mailpit éphémères, HTTP/Tomcat réel, bootstrap sans HTTP et double OIDC local signé), et produit `target/maintenance-api.jar`. Ces tests ne migrent pas le volume Compose et ne contactent pas Google ni des destinataires réels. Une absence de Docker ou de tests attendus échoue, sans skip automatique. `mvnw test` est un contrôle **unitaire seulement**, pas un remplacement de `verify`. Mockito est chargé comme agent uniquement dans les JVM de tests.

Pour démarrer le JAR déjà construit :

```powershell
java -jar target/maintenance-api.jar
```

Profil par défaut `dev`, secrets lus via `MAINT_SECRETS_DIR`. Flyway utilise `maint_migrator` ; JPA utilise exclusivement `maint_app`, `ddl-auto=validate`, `open-in-view=false`. Écoute **127.0.0.1:8080**, arrêt par Ctrl+C. L'application exige désormais une base disponible et ses secrets pour démarrer ; `/health` n'interroge pas la base à chaque appel.

## Infrastructure, secrets et image

- Windows : `%LOCALAPPDATA%/maintenance-api/dev/.local` ; Linux : `${XDG_STATE_HOME:-~/.local/state}/maintenance-api/dev/.local`. `init-dev.py --verify` vérifie neuf secrets sans les afficher. Ne jamais remplacer des fichiers pour réinitialiser un volume existant.
- PostgreSQL `127.0.0.1:5432`, SMTP Mailpit `127.0.0.1:1025`, UI `http://127.0.0.1:8025`. Aucun email réel envoyé. Ports configurables par `MAINT_DB_PORT`, `MAINT_SMTP_PORT`, `MAINT_MAILPIT_PORT` ; adapter aussi `MAINT_JDBC_URL` si le port DB change.
- Trois rôles SQL : bootstrap `postgres`, propriétaire/migrations `maint_migrator`, runtime `maint_app`. V1 conserve l'audit `SELECT, INSERT` uniquement ; V2–V7 ajoutent l'identité et leurs droits explicites. Pas de DDL/superuser ni de modification de l'audit/historique Flyway au runtime.
- V8–V11 ajoutent les invitations/dossiers et leurs historiques immuables, les réceptions d'idempotence, l'intégrité du destinataire, le blocage documentaire technicien et l'inscription Google explicite. Aucun de ces changements n'a été appliqué automatiquement au volume Compose persistant.
- V12–V13 ajoutent comptes/adhésions, parc privé et habilitations, avec FK composites et historiques append-only ; aucun backfill métier ni modification de V1–V11 ou du volume Compose.
- V14–V16 ajoutent contrats/calendriers/couvertures figées, tickets/propositions/échanges, événements transactionnels et protection SQL contre l'ajout tardif à une ancienne révision. POM et V1–V13 conservés, aucune migration Compose persistante dans la recette.
- V17–V18 ajoutent missions/compétences/révisions financières, index spatial partiel et index de filtre des compétences. Le point public approximatif est séparé du point privé du site ; runtime sans `DELETE` sur missions, compétences ou révisions.
- `init.sh` ne s'applique qu'à un volume neuf. Les migrations appliquées sont append-only. Ne jamais lancer `down -v` sur un volume utile ; arrêter sans effacer avec `docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml stop db mailpit`.
- Garage reste optionnel (`--profile documents`), non démarré pour la recette du lot 02 ; son stockage/bucket n'est pas présenté comme testé.

Construire l'image après `verify`, sous PowerShell :

```powershell
$javaImage = (Get-Content infra/images.lock.env | Where-Object { $_ -like 'JAVA_RUNTIME_IMAGE=*' }).Substring(19)
docker build --build-arg "JAVA_RUNTIME_IMAGE=$javaImage" -t maintenance-api:lot07 .
```

Le build échoue sans base fournie ; l'avertissement Docker `InvalidDefaultArgInFrom` est connu car aucun tag par défaut non verrouillé n'est choisi. `USER 10001:10001`, seul le JAR entre dans le contexte. Les secrets restent des montages externes.

Migrateur one-shot **sur la base de développement locale**, sans serveur web ni secret runtime :

```powershell
docker run --rm --network maintenance-dev_default --read-only --tmpfs /tmp:rw,noexec,nosuid,size=64m --cap-drop ALL --security-opt no-new-privileges `
  --mount "type=bind,source=$env:MAINT_LOCAL_DIR/secrets/migrator_password,target=/run/secrets/migrator_password,readonly" `
  -e MAINT_JDBC_URL=jdbc:postgresql://db:5432/maintenance -e MAINT_MIGRATION_ENV=dev -e MAINT_MIGRATOR_PASSWORD_FILE=/run/secrets/migrator_password `
  --entrypoint java maintenance-api:lot07 -Dloader.main=fr.entreprise.maintenance.bootstrap.database.DatabaseMigrationRunner -cp /app/application.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Après application des migrations du JAR, une réexécution affiche `applied=0`. V1–V13 ne sont pas réécrites. Les exemples ci-dessus nécessitent une exécution volontaire : **aucune image lot 07 construite ni migration Compose persistante exécutée dans cette recette**. En production, **ne pas fournir `MAINT_MIGRATION_ENV=dev`** : `sslmode=verify-full` et certificat de confiance exigés ; paramètres dupliqués, credentials en URL et vérificateurs TLS alternatifs refusés. Paramètres admis : `sslmode`, `sslrootcert`, `connectTimeout`, `socketTimeout`, `ApplicationName`. Certificat monté hors image ; le test local ne prouve pas TLS en production.

Contrôle local du profil `prod`, uniquement après migration ; ce n'est pas un déploiement public :

```powershell
docker run --rm --network maintenance-dev_default --read-only --tmpfs /tmp:rw,noexec,nosuid,size=128m --cap-drop ALL --security-opt no-new-privileges --memory 512m -p 127.0.0.1:8080:8080 `
  --mount "type=bind,source=$env:MAINT_LOCAL_DIR/secrets/runtime_password,target=/run/secrets/runtime_password,readonly" `
  --mount "type=bind,source=$env:MAINT_LOCAL_DIR/secrets/mfa_encryption_key,target=/run/secrets/mfa_encryption_key,readonly" `
  -e SPRING_PROFILES_ACTIVE=prod -e MAINT_JDBC_URL=jdbc:postgresql://db:5432/maintenance maintenance-api:lot07
```

Flyway est désactivé dans cette JVM web, le secret migrateur n'y est pas monté. En vraie production, fournir aussi une URL JDBC avec TLS vérifié ; TLS/proxy, gestion de secrets et restauration restent à éprouver en préproduction.

## Contrat HTTP courant

| Requête | Comportement |
|---|---|
| `GET /health` | 200, `{"status":"UP"}`, sans détails internes ni dépendance DB |
| GET `/api/web/v1/auth/csrf` | Crée/conserve une session anonyme et renvoie le token CSRF, `no-store` |
| POST `/api/web/v1/auth/login`, `/auth/logout` | Session JDBC, CSRF obligatoire ; cookie renouvelé au login |
| GET `/api/web/v1/me` | Compte de la session courante seulement ; ADMIN pré-MFA refusé |
| Vérification/reset/MFA web | Commandes POST explicites, CSRF ; usage unique et preuves serveur |
| `/api/mobile/v1/auth/login`, `/auth/refresh`, `/auth/logout` | JSON sans cookie/CSRF ; preuves password/refresh, jetons opaques |
| `/api/mobile/v1/me`, `/auth/logout-all` | Bearer opaque, rôle/statut/version/révocation courants |
| `/api/web/v1/admin/client-invitations` et `/{id}/resend` | ADMIN web actif avec email vérifié et preuves primaire/MFA de moins de 5 min ; lien uniquement dans l'outbox chiffrée |
| `/api/web/v1/onboarding/client/*` | Invitation personnelle, identité CLIENT sans autre rôle ; brouillon, profil versionné et soumission après vérification email |
| `/api/{web,mobile}/v1/onboarding/technician/*` | Inscription PENDING, profil déclaré et soumission ; pas d'approbation sans justificatifs contrôlés |
| `/api/web/v1/admin/onboardings/{clients,technicians}/*` | Revue, complément/refus motivés ; approbation client uniquement ; version et statut courant recontrôlés |
| `/api/web/v1/admin/client-accounts/*` | ADMIN récent/MFA : comptes, membres, droits, parc et archivage ; POST/PUT/PATCH avec CSRF |
| GET `/api/web/v1/client/accounts/*` | CLIENT actif vérifié : comptes/sites/équipements accordés ; pagination, filtres et compteurs scoped |
| `/api/web/v1/admin/client-accounts/{accountId}/contracts/*` | Création/révisions immuables, calendriers et règles SLA ; ADMIN récent/MFA |
| GET `/api/web/v1/client/accounts/{accountId}/sites/{siteId}/contract-revisions/*` | Projection de ce seul site autorisé, aucune note interne/autre couverture |
| `/api/web/v1/{client/accounts,admin/client-accounts}/{accountId}/sites/{siteId}/tickets/*` | Déclaration/proposition et confirmation CLIENT ; qualification/traitement ADMIN, notes internes distinctes ; If-Match/Idempotency-Key |
| `/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/missions/*` | ADMIN récent/MFA : brouillon, publication et révisions financières immuables ; If-Match/Idempotency-Key |
| GET `/api/mobile/v1/technician/missions/nearby` | TECHNICIAN actif vérifié en session mobile : missions publiées par rayon 10–200 km, compétences, fenêtre et curseur ; projection publique sans adresse/contact |
| Mutation web sans CSRF | 403 JSON ; aucune désactivation globale |
| Requête avec CSRF mais route non autorisée | Toujours refusée ; le CSRF ne donne aucune permission |
| Rôle authentifié simulé sur une route non prévue | 403, même avec ADMIN ; aucun privilège métier ouvert |
| Format de réponse incompatible sur `/health` | 406 `application/problem+json` |
| URL rejetée par le firewall Spring | 400 expurgé `application/problem+json` |

Les routes non prévues restent refusées. Pas d'utilisateur/password auto-généré, HTTP Basic, page de login externe, API candidature/affectation/document, Actuator ou Swagger UI. Aucun écran frontend généré. `/error` est accessible seulement par redistribution d'erreur interne au conteneur ; l'appel HTTP direct reste refusé. Google désactivé sans recette réelle ; ses routes sont conditionnelles et le double local ne valide pas le fournisseur réel.

Les erreurs partagent `type`, `title`, `status`, `detail`, `instance`, `code` et `traceId`. `instance` est une URN de corrélation, pas l'URL demandée. L'en-tête `X-Trace-Id` est généré côté serveur et corrélé au corps/MDC ; un identifiant fourni par l'appelant n'est pas repris. Aucun SQL, message d'exception, valeur rejetée ou contenu privé dans ces réponses. La validation ajoute au plus 20 paires `field`/`code`, sans recopier le message personnalisé du validateur.

`/health` prouve seulement que le processus HTTP répond ; ce n'est pas une readiness métier. Une session technique peut être créée par CSRF sur une mutation refusée : cela ne constitue pas une session d'authentification. Le cookie local HTTP `MAINT_DEV_SESSION` diffère du cookie sécurisé `__Host-maint-session` de production. Angular doit relire le CSRF après login/MFA/réauthentification, car les anciennes valeurs sont invalidées. [Contrat complet et bootstrap ADMIN interactif](docs/lot-03-identite.md), [client Flutter](docs/contrat-flutter-auth.md), [Google](docs/google-optionnel.md).

Contrat statique : [OpenAPI](openapi/maintenance-api.json), 3.1.0/version 0.7.0, **122 chemins/94 schémas**. Non servi publiquement ; inventaire GET/POST/PUT/PATCH testé et structure validée indépendamment, sans validation exhaustive de toutes les réponses contre JSON Schema. `If-Match: "<version>"` sur modifications ; Idempotency-Key pour admission, commandes tickets et missions. Un reçu rejoué reste historique : relire le détail courant. [Admission](docs/lot-04-onboarding.md), [parc/PUT/PATCH](docs/lot-05-comptes-parc.md), [contrats, forfaits HT/EUR et cycle ticket](docs/lot-06-contrats-tickets.md), [missions et découverte](docs/lot-07-missions.md).

## Structure et tests

```text
src/main/java/fr/entreprise/maintenance/
  MaintenanceApplication.java
  bootstrap/health/       santé technique et DTO
  bootstrap/security/     refus HTTP par défaut
  bootstrap/http/         rendu des erreurs du conteneur
  shared/http/            ProblemDetail, traitement d'erreurs et corrélation
src/test/java/
  fr/entreprise/maintenance/          tests du socle
  fr/entreprise/architecturefixtures/ exemples valides/interdits pour ArchUnit
  fr/entreprise/httpfixtures/         contrôleur réservé aux tests MVC isolés
```

02 ajoute base/audit ; 03 identité/outbox et chaînes web/mobile/OAuth ; 04 admission/idempotence ; 05 comptes/adhésions et `features/assets/` ; 06 `features/contracts/` et `features/tickets/` ; 07 `features/missions/`, autorisation `ActiveTechnician` et requête de découverte PostGIS. Les autres domaines restent à leur lot selon [la carte](docs/architecture/modules.md). ArchUnit contrôle les classes réelles et les violations interdites sur fixtures isolées, hors component scan/JAR.

Dernière recette complète : **320 tests Java (154 Surefire + 166 Failsafe)**, zéro échec/erreur/skip au 18/11/2026 ; PostgreSQL/PostGIS et Mailpit éphémères. Rapports : `target/surefire-reports/`, `target/failsafe-reports/`. Python : `python -B -m unittest discover -s scripts/tests -v`. Statique (PyYAML) : `python -B scripts/validate-pack.py` ; Windows : `MAINT_BASH=C:/Program Files/Git/bin/bash.exe`. Une démonstration ADMIN locale a ensuite créé, publié et révisé une mission ; le compte ADMIN et les données client de démonstration proviennent de fixtures locales hors dépôt. [Résultats courants et limites du lot 07](docs/preuves/lot-07-verifications.md).

SBOM des dépendances Maven (pas de l'OS/JRE de l'image) : `mvnw org.cyclonedx:cyclonedx-maven-plugin:2.9.3:makeAggregateBom -DoutputFormat=json -DincludeTestScope=false`, résultat `target/bom.json`. Ne pas lancer les gros scans Trivy et Maven en parallèle sur cette machine : un manque de ressources a été constaté. Une relance avec budgets JVM explicites et ses résultats sont consignés dans la preuve.

## CI et suite

`.github/workflows/ci.yml` exige `docker info` puis `verify` complet sur Ubuntu/Temurin 25. Un job Windows distinct exécute les unitaires et les scripts/ACL, **pas l'intégration SQL**. Actions épinglées, permissions de lecture seules. Le workflow suppose `backend` à la racine du futur dépôt ; adapter les chemins si le dépôt englobe aussi le frontend. Aucun Git/remote/commit/push ni run CI distant créé.

Suite possible : **08 — candidatures et affectations**, sans démarrage automatique. Une mission publiée n'est pas encore réservable ; le lot 08 devra garantir une seule affectation active et les conditions acceptées. Approbation technicien toujours bloquée sans pièces/contrôles privés-antivirus (09), Google désactivé avant recette réelle.

Les tests d'identité ne prouvent ni les futurs droits métier, ni le stockage documentaire, ni la restauration, ni l'absence de vulnérabilité. Écarts de versions/scans, limites et conditions avant production : [compte rendu du lot 02](docs/lot-02-infrastructure.md), [SECURITE.md](SECURITE.md).
