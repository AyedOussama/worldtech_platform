# Preuves du lot 02 — infrastructure locale

27 octobre 2026. Exécution locale sous Windows, Docker Desktop Linux/amd64. Ce document distingue les contrôles obtenus, les échecs et les contrôles non terminés. **Aucune approbation de production.**

## E02-01 — provenance et périmètre

Le dossier `backend` n'est pas un dépôt Git ; aucun `git init`, commit, push ou déploiement effectué. Avant modification, les 37 fichiers de l'ancien manifeste étaient conformes et un snapshot public sans `target`/secrets a été conservé hors dépôt. L'ancien manifeste et la sortie initiale sont archivés dans `MANIFEST-pack-initial.sha256` et `VERIFICATIONS-pack-initial.txt`.

Les fichiers infra/scripts du pack ont ensuite été intégrés par modifications ciblées ; ils ne sont plus tous inchangés. `references/`, les prompts et le cahier restent historiques. Le manifeste racine couvre désormais les sources publiques courantes, sans secrets, sorties de build, rapports Trivy bruts ou archives binaires ; régénération uniquement après revue via `scripts/public_manifest.py --write`.

Aucun compte applicatif, table d'identité, API métier ou service d'authentification externe ajouté. La migration V1 est limitée à l'audit technique. Google n'est pas activé.

## E02-02 — secrets privés et Compose (SEC-17/18)

- Neuf secrets présents hors OneDrive sous `%LOCALAPPDATA%/maintenance-api/dev/.local` ; aucune valeur affichée. Clé MFA base64url décodant **32 octets**.
- `python -B scripts/init-dev.py`, réexécution et `--verify` réussis : aucune valeur remplacée. Six tests Python réussis : idempotence, permissions, refus OneDrive, conservation d'une configuration Garage existante et validation des locks.
- Windows : ACL effectives contrôlées pour répertoires et fichiers, propriétaire utilisateur courant, aucun principal étranger autorisé hors comptes privilégiés Windows et OWNER RIGHTS vérifié. Une première approche `Set-Acl` a été refusée par l'OS avant génération ; le script utilise les DACL privées de `mkdir(0700)` puis les vérifie. Aucun contournement de ce contrôle.
- Génération désormais LF, y compris Windows. Les anciens fichiers CRLF sont conservés ; le bootstrap retire les fins de ligne pour ses deux rôles applicatifs. Après constat d'un échec d'authentification, ces rôles du **seul volume neuf créé par ce lot** ont été alignés sur les valeurs existantes par l'opérateur local, sans réinitialiser le volume ni réécrire V1. La fixture PostGIS injecte volontairement des secrets CRLF pour prévenir la régression.
- `docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet` réussi, avec `MAINT_LOCAL_DIR` exporté ; aucune configuration secrète imprimée.
- Projet Compose `maintenance-dev`, volume `maintenance-dev_db_data`, réseau `maintenance-dev_default`. PostgreSQL et Mailpit lancés et sains ; ports publiés **127.0.0.1:5432/1025/8025**. Garage non démarré.

Les fichiers montés sont lisibles par le conteneur non root ; les répertoires privés protègent leur accès sur l'hôte. Ce n'est pas une preuve de rotation, sauvegarde ou gestion des secrets de production.

## E02-03 — images, versions et données réelles (SEC-19)

Les quatre images de `infra/images.lock.env` ont été téléchargées avec Docker et inspectées `linux/amd64`. Les digests complets sont publics dans ce lock, conservé sans rafraîchissement implicite.

| Composant | Version réellement exécutée | Contrôle |
|---|---|---|
| PostgreSQL | **18.6** | `SELECT version()` et connexions JDBC réelles |
| PostGIS | **3.6.4** | `PostGIS_Full_Version()`, fonction `ST_DWithin` réelle |
| Mailpit | **v1.32.0**, Go **1.27.3** | `docker exec maintenance-dev-mailpit-1 /mailpit version` |
| Garage | **v2.5.0** | Exécution isolée de `/garage --version` ; pas de serveur/bucket testé |
| Temurin JRE | **25.0.4.1+1-LTS** | `java -version`, migrateur et application en conteneur |

Volume monté sur **`/var/lib/postgresql`**, chemin serveur **`/var/lib/postgresql/18/docker`**, conformément au layout PostgreSQL 18.

**Écart de patch Temurin :** le repère Adoptium est `25.0.5+8`, mais le catalogue officiel Docker publie encore `25.0.4.1_1-jre-noble`/`25-jre-noble`. Le tag explicite `25.0.5_8-jre-noble` répond 404. Image gardée pour la recette locale seulement ; aucun digest de patch non publié inventé. Sources et différences tag/index/manifeste : [versions.md](../versions.md#5-suivi-du-lot-02).

## E02-04 — SQL et migrations (SEC-17/20)

`DatabaseInfrastructureIT` : **17 tests** sur un conteneur PostGIS réel, avec opérations SQL négatives/positives détaillées ci-dessous ; aucun H2 ni Docker manquant transformé en succès.

- Bootstrap privilégié distinct ; `maint_migrator` propriétaire de `app`, `maint_app` sans superuser/createdb/createrole/replication/bypassrls.
- V1 appliquée sur base vide, historique Flyway créé, validation Hibernate `validate`, absence d'OpenEntityManagerInView.
- Insertion JPA avec `maint_app`, lecture SQL de l'événement ; test spatial réel.
- Refus SQLSTATE **42501** pour création de table `app`/`public`/temporaire/schéma, ALTER TABLE, SET ROLE migrateur/postgres et lecture/modification/suppression/DDL de l'historique.
- Audit existant inchangé après tentatives UPDATE/DELETE/TRUNCATE runtime ; SELECT/INSERT seuls autorisés.
- Nouvelle table créée par migrateur sans grant runtime implicite : SELECT refusé, puis table de sonde retirée par son propriétaire.
- Deuxième `migrate()` : **0 migration**, checksum/date/historique inchangés.
- Copie temporaire de V1 modifiée : échec attendu de validation du checksum ; **fichier appliqué original inchangé**, aucun `repair`.

Sur la base Compose persistante : entrée CLI `DatabaseMigrationRunner` du JAR exécutée avec **seulement `migrator_password`**, sans Spring/HTTP. Résultats successifs **`applied=1` puis `applied=0`**. Le rôle runtime et le superuser n'ont pas exécuté les migrations.

`DatabaseMigrationRunnerTest` contrôle les URLs locales admises, `sslmode=verify-full` requis hors `dev`, refus des credentials/paramètres encodés ou dupliqués/service/custom TLS. Il ne prouve pas un handshake avec un serveur/certificat réel.

## E02-05 — démarrage du runtime et persistance (SEC-17/20)

Image construite à partir du JAR, base imposée depuis le lock. Dockerfile : **`USER 10001:10001`**, JAR propriété de cet utilisateur/mode 0444, contexte whitelisté au JAR uniquement. Avertissement Docker connu `InvalidDefaultArgInFrom` : le build exige volontairement une base fournie, sans tag par défaut flottant.

Conteneur local de vérification, profil `prod`, rootfs readonly, tmpfs `/tmp`, `cap-drop=ALL`, `no-new-privileges`, mémoire 512 MiB, port éphémère lié à loopback. **Seul `runtime_password` est monté.** Vérifications obtenues :

- UID effectif **10001**, fichier `migrator_password` et environnement migrateur absents ; écriture dans `/app` refusée.
- JPA initialisé, Flyway web désactivé, aucun mot de passe/utilisateur Spring auto-généré.
- `GET /health` **200** et corps exact `{"status":"UP"}` ; route protégée **401** ProblemDetail corrélé.
- Arrêt et retrait du seul conteneur temporaire, sans toucher au volume.

Image finale reconstruite après la dernière vérification Maven, puis même contrôle de démarrage rejoué avec succès :

| Empreinte | SHA-256 |
|---|---|
| Index OCI local (également ID retourné par `docker image inspect` ici) | `9a804533612caf1d4cb639e2932ca16dc8c55d1cb95254bd4f58af1f25580299` |
| Manifeste plateforme construit | `0e25b3da9028bc1181dd61610f782291eb02146fe21935a747b07dc2064860da` |
| Configuration image construite | `031414cc2db889c2a6822808b238b2c3db628e6c4506ab8ff470f26329135e2c` |
| JAR `target/maintenance-api.jar`, identique à `/app/application.jar` dans l'image | `f36c435a33f2652236b78e45c02e80c3b2ca0f1cfdf042ea85a7ce08b4646ccc` |

Le migrateur de cette image finale refuse le mode production avec URL plaintext (**exit 1**, erreur expurgée), puis réussit en mode local explicite avec **`applied=0`**. Ces digests identifient un build local, pas une image publiée, signée ou reproductible démontrée.

Recherche séparée des neuf valeurs secrètes locales connues et de la clé MFA décodée dans toutes les sources publiques et dans **chaque entrée du JAR** : aucune correspondance, valeurs non imprimées. Fixtures de tests/Mockito absents du JAR. Ce contrôle ciblé et la whitelist de build ne remplacent pas l'analyse complète des couches de l'image.

Un événement synthétique `PERSISTENCE_PROBE` a été inséré via le rôle runtime. Après redémarrage du **seul `maintenance-dev-db-1`**, la ligne (UUID/action/date) et l'historique Flyway (version/checksum/date) étaient identiques ; une migration réussie, aucun effacement. Les états des conteneurs `taxikiwi-*` préexistants sont restés inchangés/arrêtés.

Ce redémarrage n'est **pas** un test de sauvegarde/restauration. Le profil `prod` sur réseau local plaintext ne prouve ni TLS, ni proxy/cookies d'authentification de production.

## E02-06 — scans datés et limites de ressources (SEC-19)

Trivy **0.76.1** : archive Windows officielle vérifiée par SHA-256 `1051f49cb22d5284f43425f72aad9cda63ca12ceb92e7caa06903490c82dc9bf`. Le téléchargement/extraction de sa base a échoué par manque d'espace disque ; aucun scan natif de vulnérabilités d'image présenté comme terminé.

Alternative : image officielle `aquasec/trivy@sha256:790fd66d1697395d8693d1acaa1ea702406a20f109b6fc7bac8db623eca6c06c`, caches en tmpfs/RAM, rapports hors dépôt. Scanners **vuln,secret**, toutes sévérités, **sans `ignore-unfixed` ni allowlist de CVE**. Résultats JSON achevés le 27/10/2026 vers 15:32 UTC :

| Image | CRITICAL | HIGH | MEDIUM | LOW | UNKNOWN | Secrets signalés | Couverture |
|---|---:|---:|---:|---:|---:|---:|---|
| PostGIS verrouillé | **6** | **126** | 232 | 367 | 11 | 0 | 241 paquets Debian + 4 Go ; 742 observations, 392 identifiants de vulnérabilité distincts |
| Mailpit verrouillé | 0 | 0 | 0 | 0 | **1** | 0 | 17 paquets Alpine + 53 Go ; `GO-2026-5932`, `golang.org/x/crypto v0.58.0`, sans correctif indiqué |
| Garage verrouillé | — | — | — | — | — | 0 | **Aucun inventaire de paquets/langage détecté : analyse de vulnérabilités non concluante**, pas « zéro vulnérabilité » |
| Temurin verrouillé | — | — | — | — | — | — | Scan interrompu pendant le téléchargement de la base Java |
| Backend | — | — | — | — | — | — | Scan image complet non terminé |

Les nombres comptent des **observations paquet/CVE**, pas des attaques confirmées ou des CVE uniques. PostGIS : six observations critiques concernent `libmariadb3`/`mariadb-common` (`CVE-2026-44172`, `CVE-2026-49261`), `libxml2` (`CVE-2026-6653`) et Go stdlib (`CVE-2025-68121`). **26 observations critiques/élevées disposent d'une version corrigée indiquée** ; leur applicabilité, les images corrigées et les retests restent à traiter. Aucune observation n'a été masquée comme faux positif.

La base Java a épuisé les ressources disponibles, interrompant Docker et une vérification Maven concurrente. DB/Mailpit ont redémarré automatiquement ; les projets étrangers sont restés arrêtés. Les échecs de ressources ne sont pas des scans réussis. Ne pas lancer des caches Trivy RAM volumineux en parallèle de Maven dans cet environnement. Aucun prune, suppression de volume ou nettoyage des données d'autres projets.

Rapports bruts conservés hors dépôt sous `%LOCALAPPDATA%/Temp/opencode/maintenance-lot02-20261027/reports/`. Cette synthèse ne publie aucun extrait de secret. **Remédiation/retest des constats critiques/élevés et scans Java/images complets exigés avant production**, en plus du patch Temurin, de la revue et du pentest. Le lot local ne comporte aucune dérogation à ces conditions.

Contrôle séparé des secrets des sources : `trivy fs --scanners secret --skip-dirs target --skip-dirs .git --skip-files '*.log' ... .`, Trivy natif 0.76.1, **0 constat** le 27/10/2026 à 16:45 locale. Les artefacts/logs sont exclus de ce scan de sources ; ce n'est pas le scan image complet resté interrompu.

## E02-07 — build complet, scripts et SBOM

Dernière vérification Maven complète réussie le 27/10/2026 à **16:38:06 heure locale** : **68 tests Surefire + 22 Failsafe = 90**, **zéro échec, erreur ou skip**. Cela inclut les 18 tests du validateur d'URL du migrateur, les 17 tests SQL et les 5 tests Tomcat réel. Java local Oracle 25.0.5.0.2, Boot 4.1.2, wrapper Apache 3.3.4 et distribution Maven 3.10.0.

Après l'échec de mémoire concurrent, relance seule avec budgets explicites, pas de skip :

```powershell
$env:MAVEN_OPTS = "$env:MAVEN_OPTS -Xmx384m -XX:MaxMetaspaceSize=256m"
.\mvnw.cmd -B -ntp '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' verify
```

Les six tests Python de `scripts/tests/test_local_infrastructure.py` ont également réussi. `validate-pack.py` a réussi sous Windows avec PyYAML et Git Bash via `MAINT_BASH` ; manifeste public **107 fichiers**, YAML sans doublon, Python/XML/TOML/Bash, profils, quatre digests, neuf secrets stables et 15 prompts contrôlés. Ce validateur est distinct des tests Maven/SQL.

SBOM de dépendances résolues généré avec le plugin officiel **CycloneDX Maven 2.9.3**, release stable consultée sur [Maven Central](https://repo.maven.apache.org/maven2/org/cyclonedx/cyclonedx-maven-plugin/maven-metadata.xml) :

```powershell
.\mvnw.cmd -B -ntp org.cyclonedx:cyclonedx-maven-plugin:2.9.3:makeAggregateBom -DoutputFormat=json -DincludeTestScope=false
```

Résultat : **CycloneDX 1.6, 89 composants**, sans scope test. Fichier `target/bom.json`, copie hors dépôt `reports/backend-dependencies.cdx.json` ; SHA-256 **`b7539835c473d4955c2ef6ad8341b5dcb6209f6f331da1bfb197f9a1badf7290`**. Il décrit les dépendances Maven, **pas l'OS/JRE de l'image**. Le plugin signale deux avertissements sur des mots-clés de schéma ; aucune validation indépendante complète du format ni couverture intégrale de l'image revendiquée.

Scan de ce SBOM avec Trivy **achevé séparément** le 27/10/2026 à **15:44:09 UTC**, dans un conteneur borné à 3 GiB sans Maven concurrent et sans téléchargement de la base Java : **0 observation de vulnérabilité sur les 90 entrées Maven lues** (89 composants et le projet). Base de vulnérabilités schéma 2, mise à jour **2026-10-27T13:12:49Z**. Rapport `backend-dependencies.trivy.json`, SHA-256 **`c3202d1649d7ecd70c41c07ca5a66dd131467a5699719f736c5279b509c00152`**.

Trivy avertit que le SBOM tiers peut réduire la précision et que certaines empreintes SHA-384/SHA3 sont ignorées ; avertissements conservés. Ce résultat dépend de l'inventaire/purl et de la base de ce jour, pas d'une analyse complète du JRE, des binaires ou des couches de l'image. Une première limite tmpfs de 256 MiB était trop petite pour les copies temporaires de l'archive ; avec 512 MiB, le scan de SBOM seul s'est terminé. **Les scans complets Temurin/backend et l'analyse Garage restent incomplets/non concluants**, malgré ce contrôle des dépendances résolues.

Deux avertissements Maven HTTP 401 pour des métadonnées transitives Flyway/Redgate sont visibles ; résolution/build/tests et génération du SBOM réussis malgré eux. CI mise à jour, mais **non exécutée à distance** : Linux impose Docker puis `verify`, Windows exécute les unitaires/scripts séparément. Le workflow suppose `backend` à la racine du futur dépôt Git.

## Vérifications non exécutées et suite

CI distante et exécution Linux complète du build, architecture ARM, identité/Google/MFA/session/refresh, stockage/bucket Garage, antivirus documentaire, droits métier multi-comptes, concurrence métier, TLS/proxy de préproduction, charge, rotation de secrets, sauvegarde/restauration, revue humaine et pentest : **non démontrés**.

Prochaine tranche fonctionnelle : **lot 03**, sans modifier V1. La recette locale de 02 ne permet pas de déployer l'application en production.
