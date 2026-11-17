# Preuves — lot 01, socle

Date : **27 octobre 2026**. Environnement local Windows amd64, Oracle JDK `25.0.5.0.2+1-LTS-2`. Dossier hors Git : les 37 fichiers du manifeste du pack sont conservés ; pas de commit ou de succès CI distant revendiqué. Données de tests synthétiques exclusivement.

## E01-01 — Build et distribution vérifiés (SEC-19 partielle)

Versions effectives : Java 25, Maven **3.10.0**, Spring Boot **4.1.2**, ArchUnit **1.5.1**. Le parent/BOM gère les versions de Framework, Security, Jackson, plugins et dépendances de test. Pas de version métier anticipée dans le POM. Le projet `0.1.0-SNAPSHOT` est une version de développement, pas une release de production.

Wrapper : générateur Apache **3.3.4**, distribution `only-script` **3.3.4**. Scripts `mvnw`/`mvnw.cmd` comparés octet par octet à l'archive officielle téléchargée par le plugin : **identiques**. Pas de JAR wrapper ajouté.

L'archive Maven 3.10.0 a été téléchargée dans le dossier temporaire approuvé, comparée au SHA-512 officiel Apache puis utilisée pour le bootstrap du générateur. Aucun Maven global installé.

| Élément | Empreinte observée |
|---|---|
| Maven ZIP SHA-512 officiel vérifié | `22d31676d5b92ed53308c19ecded3245d0efd0cddfec51ec7bab62f4f13ad70ddddceff8e62940e0e8e2421f3cde8106737f4becec0aa034ffeff2a754e88337` |
| Maven ZIP SHA-256 calculé, conservé dans les propriétés du wrapper | `1f6d9909266510f039f59aa0e13dcd2c66da85f043e56276e41f2918f8bddaff` |
| ZIP officiel wrapper 3.3.4, SHA-256 observé | `6cb584c2bc907b849a0b931d8266d3ff3214cdd3127115ed4f49fb7176413d36` |
| `mvnw`, SHA-256 des octets locaux | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` |
| `mvnw.cmd`, SHA-256 des octets locaux | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` |
| `.mvn/wrapper/maven-wrapper.properties`, SHA-256 des octets locaux | `664a0ebfcbee0ba44a2556533ad6a7085cd787d0a360202c1a750c91e7c2f4d8` |

URL conservée : `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.10.0/apache-maven-3.10.0-bin.zip`. Les empreintes des fichiers texte reflètent les fins de ligne locales ; `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

Génération réussie, arguments `-D` cités pour PowerShell :

```powershell
# Le Maven de bootstrap vérifié se trouve hors du projet.
& $maven -B -ntp org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper '-Dmaven=3.10.0' '-Dtype=only-script' '-DdistributionSha256Sum=1f6d9909266510f039f59aa0e13dcd2c66da85f043e56276e41f2918f8bddaff'
```

**Test négatif réellement exécuté** : copie isolée de `mvnw.cmd` hors backend, checksum remplacé par 64 zéros dans cette copie uniquement, `MAVEN_USER_HOME` pointant vers un cache neuf. Invocation `-version` : **sortie 1**, message « Failed to validate Maven distribution SHA-256 ». Le contrôle refuse donc réellement une distribution de checksum différent. Propriétés du backend et cache normal inchangés. Le checksum est contrôlé au téléchargement ; il ne revalide pas tout un cache existant à chaque lancement.

Portée : intégrité de téléchargement et origine des scripts contrôlées, pas preuve de provenance signée, d'absence de vulnérabilités ou de certification. Signatures PGP non vérifiées.

## E01-02 — Maven verify sous Java 25

Commandes réellement terminées avec **BUILD SUCCESS / sortie 0** :

```powershell
.\mvnw.cmd -B -ntp verify
```

```bash
# Git Bash 5.2.37, utilisant le JDK Windows 25, pas une JVM Linux/WSL.
./mvnw -B -ntp clean verify
```

La seconde commande reconstruit depuis un `target` nettoyé par Maven. Une nouvelle invocation Windows `verify` avec la configuration finale d'agent de test a également réussi. Aucun test désactivé, exclusion de classe ou skip pour cacher une dépendance externe.

| Suite / rapport XML local | Tests | Échecs | Erreurs | Ignorés |
|---|---:|---:|---:|---:|
| `ArchitectureRulesTest` | 5 | 0 | 0 | 0 |
| `ProductionArchitectureTest` | 4 | 0 | 0 | 0 |
| `SkeletonHttpTest` | 34 | 0 | 0 | 0 |
| `ApiExceptionHandlerTest` | 6 | 0 | 0 | 0 |
| `RequestTraceFilterTest` | 1 | 0 | 0 | 0 |
| Surefire — total | **50** | **0** | **0** | **0** |
| Failsafe — `SkeletonServerIT` | **5** | **0** | **0** | **0** |

Rapports : `target/surefire-reports/TEST-*.xml`, `target/failsafe-reports/TEST-*.xml`. Totaux relus par analyse XML, pas déduits de la présence d'un fichier JAR.

Erreurs intermédiaires corrigées avant cette recette : première génération mal citée sous PowerShell (`-Dmaven` tronqué), signatures d'API adaptées à Spring 7 et construction du dispatch ERROR du MockMvc. Ni version Java/Boot abaissée, ni checksum retiré, ni test supprimé. La première exécution réussie signalait l'auto-attachement Mockito : remplacé par son chargement **statique dans les JVM de tests** avec le plugin Dependency et les versions du BOM. L'avertissement JVM CDS lié à l'instrumentation des tests subsiste ; aucun flag de contournement du chargement dynamique n'est ajouté, et aucun agent Mockito n'est embarqué dans l'application.

## E01-03 — Portée des tests HTTP, erreurs et architecture

- **SEC-01** : santé GET 200 minimal ; inconnus, chemins privés, routes de login/gestion/docs non ouvertes ; HEAD/OPTIONS de santé non implicitement autorisés ; 401 sans redirection. Rôles ADMIN/CLIENT/TECHNICIAN simulés : toujours 403 sur une opération non prévue. Les rôles simulés ne sont pas un parcours de connexion réel.
- **SEC-06 partielle** : POST/PUT/PATCH/DELETE sans CSRF refusés ; token CSRF invalide refusé ; token valide ne donne pas une permission métier. Aucun test de session d'authentification/MFA n'est revendiqué.
- **SEC-14/17 partielles**, ASVS `V16.5.1` : messages internes, marqueurs de valeur rejetée, messages personnalisés de validation et diagnostics JSON absents des réponses. Test du log d'exception sans message brut. Contexte MDC restauré même si la chaîne échoue ; trace serveur corrélée au corps/header, non choisie par l'appelant.
- **Architecture** : classes de production réellement importées ; `shared` sans dépendance métier/bootstrap, contrôleurs sans persistance. Fixtures positives acceptées et quatre violations réellement détectées : framework dans domaine, repository intermodule, retour HTTP générique contenant un type d'infrastructure, cycle de contrats publics.
- **Limite architecture** : aucun domaine métier de production encore présent ; les règles de domaine/intermodule sans sujet actuel sont autorisées explicitement, avec métatests de détection. Ce n'est pas une couverture d'un modèle inexistant. Aucun JPA factice ajouté pour faire passer un test.
- **Serveur réel** : cinq scénarios Failsafe sur Tomcat **11.0.27**, port local aléatoire, sans MockMvc ni serveur simulé : santé, lecture privée 401, mutation sans CSRF 403, négociation 406 et firewall 400. Exemple de santé et champs obligatoires ProblemDetail comparés au fichier OpenAPI.

Les contrôleurs de sonde sont hors component scan ; le test de contexte vérifie que seuls les contrôleurs de santé et d'erreur interne sont installés. Le dispatch ERROR autorisé n'ouvre pas `/error` aux appels HTTP directs.

## E01-04 — JAR et lancement autonome

- `target/maintenance-api.jar` réellement construit et repackagé ; manifeste Boot **4.1.2**, start class `fr.entreprise.maintenance.MaintenanceApplication`, Java 25 ; bytecode de la classe principale **major 69** vérifié.
- SHA-256 du JAR observé après la recette : `41ce799d0c25d4111349d0eb7f87b07c1cc630da82c305dfec0c821871b27382`. Une nouvelle construction peut changer l'empreinte ; aucune reproductibilité binaire entre environnements n'est revendiquée.
- Inspection ZIP : pas de fixture/test, Mockito/JUnit, Hibernate ORM/Spatial, JDBC PostgreSQL/Spring JDBC, H2, Flyway ou springdoc ; aucune dépendance runtime SNAPSHOT. **Hibernate Validator est présent intentionnellement**, ce n'est pas Hibernate ORM.
- Smoke test du **JAR autonome**, lancement par le binaire du JDK 25 avec `-jar target/maintenance-api.jar --server.port=0 --server.address=127.0.0.1` : santé 200 avec JSON exact et sans Set-Cookie ; route privée 401 ProblemDetail ; absence de mot de passe de sécurité généré dans le log de démarrage. Processus lancé par la sonde arrêté et attendu, aucune instance volontairement laissée en service.

Une première sonde autonome utilisant l'alias Oracle `javapath` a atteint le serveur mais a dépassé le délai lors du nettoyage du processus enfant. Elle n'est pas comptée comme réussite. La JVM appartenant à cette sonde, identifiée par son PID/log et sa commande exacte, a seule été arrêtée ; relance avec le binaire direct du JDK : **sortie 0**, nettoyage terminé. Aucun autre processus Java n'a été arrêté.

## E01-05 — Préservation, documentation et CI

- Contrôle SHA-256 du manifeste initial avant/après : **37/37 originaux inchangés**. Aucun secret `.local`, compte, base, volume ou service Docker créé. Pas de reset Git ; dossier non versionné. Vérification finale des processus : aucune JVM `maintenance-api.jar` restante après les sondes.
- Contrôles Python locaux, sortie **0** : 15 Markdown vérifiés (UTF-8, fences et liens locaux), JSON OpenAPI lisible/références internes résolues, clés YAML uniques, événements/matrice CI et SHAs d'actions contrôlés ; POM XML valide, 28 sources Java sous `src`, aucun `@Disabled` ou réglage `skipTests`. Ce sont des contrôles de structure/références, pas un validateur OpenAPI complet ni une exécution GitHub Actions.
- `bash -c 'python3 -B scripts/validate-pack.py'` rejoué sous WSL/Linux après les ajouts : **sortie 0**. Le texte fixe « NON exécuté : Maven/Spring Boot… » décrit uniquement les contrôles absents de ce script de préparation ; les exécutions Maven du lot 01 sont prouvées séparément en E01-02. Les ACL Windows ne sont pas validées par ce contrôle Linux.
- CI : Temurin 25, Ubuntu 24.04 / Windows, `verify` sans skip, rapports conservés. Actions vérifiées dans les releases officielles puis épinglées aux commits : checkout v7.0.1, setup-java v6.1.0, upload-artifact v7.0.2. Permissions lecture seule, pas de credentials de push conservés.
- Relecture du code introduit : exceptions publiques limitées à GET santé et dispatch ERROR interne ; aucune désactivation globale de CSRF, aucun DTO/entité métier, aucun secret réel ou message d'exception recopié. Ce contrôle humain de l'auteur ne vaut pas revue indépendante ou secret scan spécialisé.

## Sources consultées le 27 octobre 2026

- [Apache Maven, téléchargements](https://maven.apache.org/download.cgi) ; [SHA-512 officiel](https://downloads.apache.org/maven/maven-3/3.10.0/binaries/apache-maven-3.10.0-bin.zip.sha512).
- [Maven Wrapper](https://maven.apache.org/wrapper/) ; [paramètres du générateur](https://maven.apache.org/wrapper/maven-wrapper-plugin/wrapper-mojo.html) ; [distribution 3.3.4](https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/).
- [Boot 4.1.2, exigences](https://docs.spring.io/spring-boot/system-requirements.html) et [BOM publié](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom).
- [Spring MVC, ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).
- [Mockito v5.23.0, documentation du chargement statique pour les JDK récents](https://github.com/mockito/mockito/blob/v5.23.0/mockito-core/src/main/java/org/mockito/Mockito.java).
- Releases [checkout v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1), [setup-java v6.1.0](https://github.com/actions/setup-java/releases/tag/v6.1.0), [upload-artifact v7.0.2](https://github.com/actions/upload-artifact/releases/tag/v7.0.2).

## Non vérifié et prochain lot

CI distante/Temurin/Linux natif, scans de vulnérabilités/secrets/images, SBOM complet, signatures PGP, SQL/PostGIS/Flyway/privilèges, sessions JDBC/Google/mobile/MFA, autorisations multi-comptes sur données métier, concurrence, documents, charge, restauration, revue humaine indépendante et pentest : **non exécutés**. Aucun succès de production ou certification déduit des 55 tests.

Prochaine tranche : lot 02, avec vérification de la protection effective des secrets Windows/OneDrive avant leur génération. Aucun arbitrage produit nouveau ne bloque ce lot indépendant.
