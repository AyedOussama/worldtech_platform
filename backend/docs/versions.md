# Référentiel revalidé — versions et images

Consultation : **27 octobre 2026**, état initial du lot 00 complété par les suivis des lots 01/02 en fin de document. Pas de changement de génération ; l'exécution du lot 02 révèle un écart de patch Temurin à ne pas masquer. `references/` et `infra/source-images.env` restent intacts ; l'infrastructure active a été intégrée.

Une documentation de compatibilité, une publication d'artefact, une compilation et une exécution sont quatre preuves différentes. Leur portée est indiquée ci-dessous.

## 1. Java, build et bibliothèques

| Composant | Référence conservée | Vérification obtenue | Limite / prochain contrôle |
|---|---|---|---|
| JDK cible | Temurin 25 LTS, patch repéré `25.0.5+8` | API Adoptium officielle : release `jdk-25.0.5+8`, Windows x64 disponible [V03] | Pas installé par ce lot ; CI/runtime à aligner au lot 01/02 |
| JDK local | Oracle `25.0.5.0.2+1-LTS-2` | `java -version` et `javac -version` ; sonde Argon2 exécutée | Ce n'est pas Temurin ; pas de remplacement ni de conclusion sur les licences de déploiement |
| Spring Boot | `4.1.2` | Publication Maven présente ; documentation : Java 17 à 27 inclus, Framework >= 7.0.10, Maven >= 3.6.3 [V01, V02] | Java 25 dans la plage annoncée ; application non compilée |
| Maven | Distribution `3.10.0` | Version stable recommandée par Apache [V04] | Maven global et wrapper absents ; wrapper à livrer au lot 01 avec checksum de distribution vérifié |
| springdoc | `3.2.0` | Matrice éditeur : Boot 4.1.x avec springdoc 3.1.x–3.2.x [V05] | Intégration à tester quand utilisée ; UI non publique en production |
| ArchUnit | `1.5.1` | POM publié `archunit-junit5` accessible [V06] | Exécution sous JDK 25/JUnit du BOM au lot 01, non démontrée par une lecture XML |
| Bouncy Castle | `bcprov-jdk18on:1.86` | Page éditeur, POM et JAR contrôlés ; sonde Spring Crypto/JDK 25 réussie [V07, V08] | Scan, intégration Boot et charge non exécutés |

`3.10.0` est la version de **Maven**, pas le numéro de version du plugin Maven Wrapper. La version stable du générateur wrapper, ses fichiers et le checksum de la distribution seront documentés au lot 01. Aucun checksum de wrapper fictif n'est fourni.

Les métadonnées Maven présentent aussi `4.2.0-RC1` comme `latest/release` pour Boot : ces champs ne suffisent pas à choisir une version de production. La préversion est exclue ; `4.1.2` est conservé.

### Versions lues dans le BOM Boot 4.1.2

Source : [POM officiel `spring-boot-dependencies:4.1.2`](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom), HTTP 200.

| Propriété du BOM | Valeur lue |
|---|---|
| `spring-framework.version` | `7.0.10` |
| `spring-security.version` | `7.1.2` |
| `spring-session.version` | `4.1.1` |
| `hibernate.version` | `7.4.13.Final` |
| `jackson-bom.version` | `3.1.7` |
| `flyway.version` | `12.4.0` |
| `postgresql.version` | `42.7.14` (pilote JDBC, pas version du serveur) |
| `testcontainers.version` | `2.0.5` |
| `junit-jupiter.version` | `6.0.3` |
| `commons-logging.version` | `1.3.6` |

Ces valeurs servent à contrôler le référentiel, **pas à ajouter des surcharges de versions** au POM applicatif. Parent/BOM unique. Starter MVC `spring-boot-starter-webmvc`, tests MVC/sécurité de génération Boot 4 et artefacts Testcontainers `testcontainers-junit-jupiter`/`testcontainers-postgresql`. Pas de mélange avec des exemples Boot 2/3, Jackson d'une autre génération ou Testcontainers 1.x.

Le POM de référence couvre plusieurs lots. Le lot 01 n'en copiera que les dépendances utilisées : MVC, validation, sécurité et tests/ArchUnit ; pas de JPA, fournisseur Google, S3, PDF ou mail ajoutés simplement pour remplir le squelette. Un éventuel module de gestion de santé doit rester minimal et contrôlé.

## 2. Revalidation Bouncy Castle / Argon2id

1. L'éditeur publie `bcprov-jdk18on:1.86` pour **JDK 1.8 et suivants** ; `jdk18on` ne signifie pas « Java 18 minimum » [V07]. La release 1.86 est datée du 11 septembre 2026.
2. Son POM publié ne déclare aucune dépendance transitive. `bcpkix`, `bcutil` ou `bctls` ne sont pas nécessaires au seul encodage Argon2. Ne pas les ajouter préventivement.
3. Le source de `Argon2PasswordEncoder` de Spring Security **7.1.2**, version du BOM, importe `Argon2BytesGenerator` et `Argon2Parameters` de BC [V09]. BC n'apparaît pas comme dépendance gérée dans le BOM inspecté ; la version directe explicite `1.86` de la référence est conservée.
4. Le défaut Spring inspecté utilise `m=16384 KiB`. Le contrat projet impose explicitement `m=19456, t=2, p=1` au minimum, conformément au point de départ OWASP [V10]. Ne pas utiliser la méthode `defaults...` en supposant qu'elle satisfait ce contrat.
5. La [sonde isolée](preuves/Argon2CompatibilityProbe.java) utilise réellement l'encodeur Spring, BC 1.86, Spring Core 7.0.10 et Commons Logging 1.3.6 sous Java 25. Elle contrôle le format/les paramètres, une correspondance correcte, le refus d'un mot de passe erroné et des sels distincts. **Résultat : succès**, détails et checksums dans [la preuve](preuves/lot-00-verifications.md).

Cela démontre une compatibilité fonctionnelle restreinte de cette combinaison, pas l'authentification de l'application, sa résistance ni une résolution Maven complète. Les JAR sont uniquement dans le dossier temporaire de sonde, hors backend. Aucun nouveau fournisseur cryptographique global ni changement de la JVM n'a été installé.

À faire au lot 03 : arbre de dépendances Maven effectif, détection de doublons BC, scan daté/SBOM, limites de longueur et Unicode, migration de coût, mesure sur runtime cible et sous charge avec concurrence bornée. Revue humaine obligatoire. Les checksums de téléchargement ne remplacent pas la validation des signatures ou une analyse de vulnérabilités.

## 3. Images candidates : tag, digest et contenu

- **Tag** : nom mutable, éventuellement de branche, par exemple `18-3.6`.
- **Digest du tag/index** : contenu référencé par le registre lors de la consultation, éventuellement multiarchitecture.
- **Digest de manifeste plateforme** : image propre à `linux/amd64`, distincte de l'index.
- **Version réellement contenue/exécutée** : à obtenir en lançant l'image par digest et en interrogant le binaire/serveur ; elle ne se déduit pas du tag ou du seul digest.

Les quatre endpoints de tags du registre ont répondu HTTP 200, avec une image `linux/amd64` active. **Aucun pull, lock local ou démarrage des candidats n'a été effectué.** Les images d'autres projets déjà présentes dans Docker n'ont pas été modifiées.

| Candidat conservé | Source éditeur / contenu annoncé | Version exécutée ici |
|---|---|---|
| `postgis/postgis:18-3.6` | README projet : PostgreSQL 18, PostGIS 3.6.4, amd64 ; PostgreSQL officiel annonce le patch 18.6 [V11, V12] | Non vérifiée ; le tag ne garantit pas le patch PostgreSQL |
| `axllent/mailpit:v1.32.0` | Release stable publiée le 22/10/2026, corrections de sécurité incluses [V13] | Non vérifiée |
| `dxflrs/garage:v2.5.0` | Quick-start éditeur utilisant ce tag, mode local un nœud [V14] | Non vérifiée |
| `eclipse-temurin:25-jre-noble` | Tag de JRE 25 / Ubuntu Noble ; patch Temurin repéré 25.0.5+8 [V03, V15] | Non vérifiée ; le JDK Oracle local n'est pas cette image |

### Digests observés au registre (pas un fichier de verrouillage)

| Image | Digest du tag/index | Digest du manifeste `linux/amd64` |
|---|---|---|
| PostGIS | `sha256:4a43e3c3e5d55b9208240fc71db543b79c638766280c625a425bd0a2390989c4` | `sha256:530bb81ad6c5993ae870ea66095d9dc758ef5866007186833f31fb2bebdd5791` |
| Mailpit | `sha256:0790692be935ea4420d085694f7985b1f692ca6a8771ab0e0bd8ba75ac180498` | `sha256:938ce54b7b946ef3f45a03e119952cf7e67a7969d61f45a3ed055442a77e00f4` |
| Garage | `sha256:677e5b758e4425a325b811c425c026bf35a4bf42f7ddbaa773de3f4fd779a5a5` | `sha256:23b16d34e0348d9aca9b960a7429a7e6d494974b5288bfb8f6c3ab856895f71e` |
| Temurin | `sha256:ce8543ddd3070ccbeaac2106a74c9387ddeb8d47a1c43348345715c1ef245c7f` | `sha256:4b027e62bb745df72c783a5675645738bc445b9c9cf2ee8ee9719b0e0ca60ba2` |

Réponses de registre consultées le 27/10/2026 : [PostGIS](https://hub.docker.com/v2/repositories/postgis/postgis/tags/18-3.6), [Mailpit](https://hub.docker.com/v2/repositories/axllent/mailpit/tags/v1.32.0), [Garage](https://hub.docker.com/v2/repositories/dxflrs/garage/tags/v2.5.0), [Temurin](https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/25-jre-noble). Les tags peuvent changer après cette date.

Au lot 02 : `scripts/lock-images.py` résoudra réellement les candidats avec Docker, sans inventer de lock. Enregistrer plateforme, digests, scans et versions obtenues par `SELECT version(), PostGIS_Full_Version();`, `mailpit version`, `garage --version`, `java -version`. Tester aussi privilèges, volume PostgreSQL 18 et Compose. Une image figée peut contenir des vulnérabilités ; l'immuabilité n'est pas une approbation.

## Sources officielles consultées le 27 octobre 2026

- V01 — [Boot, exigences](https://docs.spring.io/spring-boot/system-requirements.html).
- V02 — [BOM publié 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom) et [métadonnées](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/maven-metadata.xml).
- V03 — [API Adoptium, JDK 25 Windows x64](https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=windows&vendor=eclipse).
- V04 — [Apache Maven, téléchargements](https://maven.apache.org/download.cgi).
- V05 — [springdoc, matrice de compatibilité §13.80](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
- V06 — [ArchUnit 1.5.1, POM](https://repo.maven.apache.org/maven2/com/tngtech/archunit/archunit-junit5/1.5.1/archunit-junit5-1.5.1.pom).
- V07 — [Bouncy Castle, téléchargement et notes de version](https://www.bouncycastle.org/download/bouncy-castle-java/).
- V08 — [BC 1.86, POM](https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/1.86/bcprov-jdk18on-1.86.pom).
- V09 — [Source de l'encodeur, tag Spring Security 7.1.2](https://github.com/spring-projects/spring-security/blob/7.1.2/crypto/src/main/java/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.java), [POM Spring Crypto 7.1.2](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-crypto/7.1.2/spring-security-crypto-7.1.2.pom).
- V10 — [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- V11 — [PostgreSQL, versions supportées](https://www.postgresql.org/support/versioning/).
- V12 — [README docker-postgis](https://github.com/postgis/docker-postgis).
- V13 — [Mailpit v1.32.0, release](https://github.com/axllent/mailpit/releases/tag/v1.32.0).
- V14 — [Garage, quick-start](https://garagehq.deuxfleurs.fr/documentation/quick-start/).
- V15 — [Métadonnées officielles de l'image Eclipse Temurin](https://github.com/docker-library/official-images/blob/master/library/eclipse-temurin).

## 4. Suivi vérifié du lot 01

Le tableau initial §1 décrit la situation au cadrage ; le backend est désormais compilé/testé localement sous JDK 25 avec parent Boot **4.1.2 inchangé**. ArchUnit **1.5.1** est réellement exécuté. Aucun ajout des dépendances métier du POM de référence avant leur lot.

Wrapper Apache **3.3.4**, type `only-script`, généré par le plugin officiel 3.3.4 ; Maven distribué **3.10.0**, SHA-256 explicite et test négatif de checksum. Les deux scripts sont identiques aux fichiers de la distribution officielle. [Origines et empreintes](preuves/lot-01-verifications.md).

Le BOM gère également le plugin Maven Dependency **3.10.0** et Mockito **5.23.0** utilisés pour charger l'agent de test au démarrage des JVM Surefire/Failsafe : pas de version transitive surchargée, pas d'agent au runtime applicatif, pas de flag autorisant le chargement dynamique. Source consultée le 27/10/2026 : [documentation dans le source Mockito v5.23.0](https://github.com/mockito/mockito/blob/v5.23.0/mockito-core/src/main/java/org/mockito/Mockito.java).

CI définie pour Temurin 25, non exécutée à distance. Les images et SQL restent non exécutés au lot 01 ; Bouncy Castle reste hors du POM applicatif tant que l'identité n'est pas développée. Sa sonde historique n'est pas intégrée artificiellement au runtime du socle.

## 5. Suivi du lot 02

Les quatre digests du tableau §3 ont été **réellement téléchargés** et sont désormais dans `infra/images.lock.env`. Toutes les images inspectées sont `linux/amd64`. Le lock existant est conservé sans nouvelle résolution sauf `--refresh` ; chaque mise à jour demande relecture, scan et tests, puis régénération explicite du manifeste public.

| Image exécutée par digest | Version constatée | Portée |
|---|---|---|
| PostGIS | PostgreSQL **18.6**, PostGIS **3.6.4** | Serveur Compose et conteneurs d'intégration ; fonction spatiale exécutée |
| Mailpit | **v1.32.0**, Go **1.27.3**, linux/amd64 | Service local, contrôle `mailpit version` ; `--version` n'est pas une option supportée |
| Garage | **v2.5.0** | Binaire `--version` uniquement ; serveur/bucket non démarrés |
| Temurin JRE Noble | **25.0.4.1+1-LTS** | `java -version`, migrateur et backend non root réellement exécutés |

**Écart Temurin non résolu :** l'API Adoptium Linux JRE 25 annonce `25.0.5+8`, mais le catalogue officiel Docker consulté publie encore `25.0.4.1_1-jre-noble` / `25-jre-noble`, avec le digest verrouillé. Le candidat explicite `25.0.5_8-jre-noble` renvoie HTTP **404**. Aucun digest de patch corrigé n'est inventé, aucun autre fournisseur n'est substitué silencieusement. Image retenue uniquement pour la recette locale ; avant production, résoudre un patch approprié réellement publié, reconstruire et refaire les scans/tests.

Sources : [Adoptium Linux JRE 25](https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jre&jvm_impl=hotspot&os=linux&vendor=eclipse), [catalogue Docker officiel Temurin](https://github.com/docker-library/official-images/blob/master/library/eclipse-temurin), [candidat Docker vérifié](https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/25.0.5_8-jre-noble).

Ajouts Maven utilisés, gérés sans surcharge par Boot 4.1.2 : JPA/Hibernate **7.4.13.Final**, Flyway **12.4.0** et son module PostgreSQL, JDBC **42.7.14**, Testcontainers **2.0.5**. Deux avertissements de métadonnées HTTP 401 issus de dépôts transitifs Flyway/Redgate n'ont pas empêché la résolution ni les tests ; ils ne sont pas dissimulés par un skip.

Validation du CLI : [parseur d'URL du pilote PostgreSQL 42.7.14](https://github.com/pgjdbc/pgjdbc/blob/REL42.7.14/pgjdbc/src/main/java/org/postgresql/Driver.java) consulté ; noms de paramètres non percent-encodés, pas de propriété de service/TLS personnalisée autorisée. Les tests négatifs ne remplacent pas le test d'une chaîne de certificats réelle.

Scans datés, SBOM et empreinte de l'image finale : [preuve du lot 02](preuves/lot-02-verifications.md). Aucun résultat de scan seul ne rend l'image apte à la production.
