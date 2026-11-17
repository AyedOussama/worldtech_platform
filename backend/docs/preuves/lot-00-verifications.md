# Preuves de préparation — lot 00

Date : **27 octobre 2026**. Révision : dossier hors Git ; référence d'entrée `MANIFEST.sha256`, 37 fichiers contrôlés. Contrôles exécutés pendant ce lot, distincts du fichier historique `VERIFICATIONS.txt`. Les données de la sonde sont synthétiques et jamais affichées.

## E00-01 — Inspection et conservation

- Commandes : `git rev-parse --show-toplevel`, inventaire des chemins ; calcul SHA-256 Python de chaque entrée du manifeste et comparaison.
- Résultat initial : **37/37 concordants** ; aucun `pom.xml` applicatif, source Java ou wrapper. Contrôle rejoué après les ajouts : **37/37 inchangés**. Git répond « not a git repository » ; aucun statut Git propre revendiqué.
- Modifications : ajouts seulement sous `docs/` ; aucun original modifié, aucune initialisation Git, aucun `.local` dans backend.
- Exigences : SEC-17/19, preuve de préparation/intégrité seulement. Ne démontre pas une analyse de secrets exhaustive ni la sécurité de la chaîne logicielle.

## E00-02 — Sources de versions, BOM et images

- Méthode : requêtes HTTP en lecture seule vers les sources de [versions.md](../versions.md), lecture des POM/métadonnées et des quatre endpoints de tags Docker Hub.
- Résultat : Boot 4.1.2/BOM, BC 1.86, ArchUnit 1.5.1 publiés ; Maven 3.10.0 stable annoncé ; Temurin 25.0.5+8 disponible ; springdoc 3.2.0 documenté pour Boot 4.1.x. **Versions du pack conservées**.
- BOM observé : Framework 7.0.10, Security 7.1.2, Session 4.1.1, Hibernate 7.4.13.Final, Jackson 3.1.7, Flyway 12.4.0, JDBC PostgreSQL 42.7.14, Testcontainers 2.0.5, JUnit 6.0.3, Commons Logging 1.3.6.
- Les quatre tags ont répondu HTTP 200 ; digests index/amd64 observés consignés dans le référentiel. Aucun pull ni génération de `infra/images.lock.env`, aucun test de version dans ces images, aucun scan.
- Exigence : SEC-19 **partielle/documentaire**, pas preuve d'absence de vulnérabilités, de provenance signée ou de compatibilité Boot complète.

## E00-03 — Validateur du pack

### Échecs observés

1. Python Windows 3.11.15/PyYAML 6.0.3, temporaire dans le dossier Windows approuvé. `python -B scripts/validate-pack.py` termine en erreur : Bash WSL ne trouve pas `C:\...\infra\postgres\init.sh` (code Bash 127, processus Python 1).
2. WSL/Python 3.12.3, `TMPDIR` sur le dossier NTFS approuvé : le validateur atteint le test des modes et échoue sur `mode == 0700` (sortie 1). Une ACL Windows n'est pas démontrée par `chmod` POSIX sur ce montage.

Aucun test n'a été désactivé ni réécrit. Ces résultats ne sont pas masqués par le contrôle suivant.

### Exécution réussie

```powershell
bash -c 'python3 -B scripts/validate-pack.py'
```

Le même script non modifié est exécuté depuis backend, sous Bash 5.2.21 / Python WSL 3.12.3, avec répertoire temporaire **sur le système de fichiers Linux** afin de tester réellement les modes POSIX. Ce choix est nécessaire après l'échec NTFS ; les répertoires sont gérés et nettoyés par `TemporaryDirectory`, pas par une suppression large.

Sortie observée :

```text
OK : Python/XML/YAML sans doublon/TOML/Bash ; 3 services/4 images ; 9 secrets stables, clé MFA 32 octets ; 15 prompts ; références actives et manifeste si présent.
NON exécuté : Docker/SQL/Maven/Spring Boot, authentification locale/Google, tests métier et pentest.
```

Code de sortie : **0**. Contrôles de syntaxe/structure et scripts de préparation, pas tests d'intégration applicative. Aucun secret brut affiché ou conservé dans backend. Les permissions Windows/OneDrive restent à vérifier avant l'usage local réel au lot 02 ; le succès Linux ne les valide pas. Exigences SEC-17/19 : portée de préparation seulement.

## E00-04 — Compatibilité fonctionnelle Argon2 / JDK 25

- Source : [Argon2CompatibilityProbe.java](Argon2CompatibilityProbe.java), sonde hors `src/`, sans application Spring ni endpoint. Aucun compte réel, mot de passe ou hash imprimé.
- Runtime : Oracle JDK `25.0.5.0.2+1-LTS-2`, Windows amd64.
- Fichiers binaires et `.class` : uniquement dans `$env:LOCALAPPDATA\Temp\opencode\maintenance-lot00-20261027`, hors backend ; aucune dépendance globale installée.
- Artefacts téléchargés de Maven Central via HTTPS ; checksums comparés avant compilation. Les JAR ne sont pas joints au pack.

| Coordonnées de la sonde | SHA-256 du JAR observé |
|---|---|
| `org.bouncycastle:bcprov-jdk18on:1.86` | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |
| `org.springframework.security:spring-security-crypto:7.1.2` | `4aaa60bb612d5fece004c07f893b3a8d707c207bbd8585d10e0015b5301cd864` |
| `org.springframework:spring-core:7.0.10` | `a236f31a7c07e3a1a8e7839ec5c0d47b5cc75709132db50416d5dad320341f12` |
| `commons-logging:commons-logging:1.3.6` | `f8ead8943401081dea0aa824b5b1ba40a0e4ed297a572a0f02258150a0b62357` |

Origines : `https://repo.maven.apache.org/maven2/` suivi du chemin group/artifact/version/JAR, notamment [BC](https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/1.86/), [Crypto](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-crypto/7.1.2/), [Core](https://repo.maven.apache.org/maven2/org/springframework/spring-core/7.0.10/), [Logging](https://repo.maven.apache.org/maven2/commons-logging/commons-logging/1.3.6/).

Pour BC/Crypto/Core, comparaison avec les `.jar.sha256` publiés. Pour Commons Logging, `.sha256`/`.sha512` non disponibles : comparaison au `.jar.sha1` publié (`63e78ca6cd446c0ad166d14f03ed99e7efb3896d`), puis calcul SHA-256 consigné ci-dessus. Ce dernier contrôle ne vaut pas preuve d'authenticité/signature ; aucune signature PGP ni analyse de vulnérabilités n'a été vérifiée pour la sonde.

Commandes réellement exécutées après téléchargement/vérification des quatre JAR (reproduction : disposer des mêmes fichiers dans `probeDir`) :

```powershell
$probeDir = Join-Path $env:LOCALAPPDATA 'Temp\opencode\maintenance-lot00-20261027'
javac -encoding UTF-8 -cp "$probeDir\*" -d "$probeDir" docs/preuves/Argon2CompatibilityProbe.java
java -cp "$probeDir;$probeDir\*" Argon2CompatibilityProbe
```

Résultat : compilation et exécution **sortie 0**.

```text
OK: Argon2id m=19456,t=2,p=1; correspondance positive/négative; sels distincts.
Deux encodages et deux vérifications, durée indicative totale: 473 ms. Ce résultat n'est pas un benchmark.
```

Ce résultat démontre une petite compatibilité d'API/runtime de l'encodeur Spring avec BC 1.86 et des paramètres explicites. Il ne mesure pas un p95, une charge soutenue, le runtime Temurin conteneurisé, l'unicité distribuée ni le service de connexion. Pas de résolution Maven complète ni de backend compilé. SEC-04 / ASVS `V11.4.2` : **preuve partielle, exigence applicative non achevée**. Intégration, longueur/Unicode complets, rehash, charge et revue à réaliser au lot 03.

## E00-05 — Traçabilité documentaire

- Source des identifiants ASVS : export JSON stable [5.0.0](https://github.com/OWASP/ASVS/releases/download/v5.0.0_release/OWASP_Application_Security_Verification_Standard_5.0.0_en.flat.json), 345 exigences, pas la branche de travail.
- Mapping initial de SEC-01 à SEC-20, niveaux minimaux et limites explicites ; aucun contrôle applicatif validé par la seule présence d'une ligne dans la table.
- Contrôle local Python après les ajouts, sortie **0** : 13 nouveaux fichiers UTF-8, fences Markdown équilibrées et liens locaux existants ; SEC-01 à SEC-20 présentes (20/20) ; les **57 identifiants ASVS distincts** cités dans le mapping existent dans l'export stable de 345 exigences. Ce contrôle d'identifiants ne démontre pas une conformité aux exigences.
- Contrôle de périmètre, sortie **0** : seuls des fichiers sous `docs/` ajoutés ; absence de `pom.xml`, `src`, `.local` ou fichier inattendu. Validateur du pack rejoué sous WSL/Linux après les ajouts : **sortie 0**, sans modification des contrôles.
- Le modèle de preuve impose prévu/implémenté/vérifié/non vérifié, tests négatifs et effets DB/outbox. Écart MFA L2 et sensibilité du code en lien explicités.

## Non exécuté / non démontré

Application Maven/Boot, démarrage ou scan des images candidates, Compose de ce backend, SQL/PostGIS/Flyway/privilèges, identité web/mobile/Google/MFA, droits sur objets/fichiers, concurrence métier, antivirus, SBOM, charge, restauration, revue humaine indépendante et pentest. Le lot 01 reste à exécuter. Aucun résultat de ce document ne vaut approbation de production.
