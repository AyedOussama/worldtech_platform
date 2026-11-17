# Compte rendu — prompt 00

27 octobre 2026. Périmètre : `backend`, inspection et décisions techniques ; pas de génération de l'application.

## 1. Existant et préservation

- Pack de préparation révision 1.1 : cahier backend, authentification/sécurité, quinze prompts, références POM/YAML/Dockerfile, infrastructure et trois scripts Python.
- À l'entrée : **aucun `pom.xml` applicatif, `src`, source Java, wrapper ou ADR existant**. Le fichier `references/pom.xml.reference` n'est pas un projet Maven à remplacer.
- `git rev-parse --show-toplevel` échoue avec « not a git repository ». Il n'y a pas d'index/historique permettant un `git status` ou un diff de référence ; **ne pas en déduire un dépôt propre**. Aucune initialisation Git effectuée.
- Les 37 fichiers couverts par `MANIFEST.sha256` correspondent à leurs empreintes initiales ; le manifeste et ces originaux sont conservés. Les ajouts sont exclusivement sous `docs/`, dont une sonde Java documentaire/diagnostique. Aucun code d'application préexistant n'a été écrasé.
- Aucun secret généré sous `backend/.local`, aucun compte créé, aucun conteneur existant arrêté/modifié, aucune image candidate tirée et aucun déploiement.
- Instructions lues : `AGENTS.md`, cahier backend complet, `AUTHENTIFICATION.md`, `SECURITE.md`, prompt 00/01, références/scripts et historique de validation ; cahier produit v0.3 du dossier parent consulté. Aucun autre `AGENTS.md` trouvé dans l'espace de travail inspecté.

Les anciens `VALIDATION.md`/`VERIFICATIONS.txt` décrivent l'environnement de préparation du pack, **pas le poste actuel** ; ils sont conservés comme historique. Le présent compte rendu et [ses preuves](preuves/lot-00-verifications.md) décrivent la vérification actuelle.

## 2. Outils constatés

| Outil | Résultat actuel |
|---|---|
| Git | `2.52.0.windows.1` ; dossier non versionné |
| Java / javac | Oracle `25.0.5.0.2`, build runtime `25.0.5.0.2+1-LTS-2` |
| Maven / wrapper | Commande `mvn` non trouvée ; `mvnw`/`mvnw.cmd` absents |
| Python Windows / PyYAML | `3.11.15` / `6.0.3` |
| Bash / Python WSL | Bash `5.2.21`, Python `3.12.3` ; PyYAML utilisable par le validateur |
| Docker | Client/Engine `29.1.5`, serveur Linux/amd64 joignable ; Docker Desktop `4.58.0` |
| Compose | `v5.0.1` ; aucune validation Compose de cette application effectuée |

Ne pas utiliser les images PostgreSQL/PostGIS plus anciennes déjà présentes pour d'autres projets comme preuve des candidats de ce pack.

## 3. Prévu / implémenté / vérifié / non vérifié

| État | Portée |
|---|---|
| Prévu | Lots 01–14 et critères de recette, sans engagement de date ni budget |
| Implémenté | Trois ADR ; carte/propriété des modules ; matrice fonction/champs ; référentiel de versions ; mapping ASVS initial et modèle de preuves ; plan et ce compte rendu ; sonde isolée |
| Vérifié | Intégrité des originaux, inventaire/outils, sources officielles/BOM et existence de tags, validateur du pack sous WSL/Linux, compatibilité Argon2 restreinte sous JDK 25 |
| Non vérifié | Application Maven/Boot inexistante, auth web/mobile/Google/MFA, SQL/Flyway/privilèges, démarrage des images, Compose effectif, droits métier, concurrence métier, antivirus, scans/SBOM, charge, restauration, revue indépendante et pentest |

Fichiers/contrats : voir l'[index](README.md). **Aucun contrat API existant ni migration n'a été modifié** ; la santé minimale et les erreurs du prochain socle sont cadrées techniquement dans le [plan](plan-lots.md). Les règles financières et les nouvelles habilitations ne sont pas inventées.

Exigences SEC concernées : SEC-01/02/03 (droits conçus), SEC-04 (sonde de compatibilité seulement), SEC-06 à SEC-12 (décisions d'identité), SEC-13 à SEC-16 (frontières/règles prévues), SEC-17/19 (préservation, versions et preuves de préparation). Aucune de ces exigences n'est annoncée entièrement vérifiée dans l'application. SEC-05/18/20 restent également à implémenter/éprouver ; traçabilité dans le [mapping](securite/asvs-sec.md).

## 4. Vérifications réellement exécutées et résultats

1. Inspection de fichiers et des outils, contrôle SHA-256 des 37 originaux avant/après : **inchangés**. Contrôle des 13 ajouts : UTF-8, fences Markdown et liens locaux valides ; 20 SEC présentes et 57 identifiants ASVS cités retrouvés dans l'export officiel stable.
2. Lectures HTTP des sources officielles et artefacts : versions initiales confirmées ; BOM et identifiants ASVS contrôlés ; quatre tags disponibles au registre, **pas de lock Docker exécuté**.
3. `python -B scripts/validate-pack.py` sous Windows : **échec**, Bash WSL ne lit pas les chemins `C:\...` passés par Python natif. Aucun contrôle n'a été supprimé pour contourner cet échec.
4. Même validateur sous WSL avec temporaire sur NTFS : **échec** du contrôle POSIX `0700`. Puis `bash -c 'python3 -B scripts/validate-pack.py'` avec temporaire Linux : **succès, sortie 0**. La différence de permissions Windows/WSL est une limite réelle, à traiter au lot 02 avant de conserver des secrets sur Windows/OneDrive.
5. Sonde `Argon2CompatibilityProbe` compilée/exécutée avec les JAR retenus et JDK 25 : **succès** ; paramètres explicites, bons/mauvais mots de passe et sels distincts. Ni build du backend, ni benchmark, ni test HTTP.

Les preuves détaillent la portée et les échecs, pas uniquement la dernière commande réussie. Aucun contrôle d'intégration non exécuté n'est converti en succès.

## 5. Commandes utiles, depuis `backend`

Déjà exécutées (inventaire et contrôles) :

```powershell
git rev-parse --show-toplevel
git --version
java -version
javac -version
python --version
docker version
docker compose version
python -B scripts/validate-pack.py
bash -c 'python3 -B scripts/validate-pack.py'
```

`git rev-parse` et le validateur natif ont les échecs décrits ci-dessus. Compilation/exécution de la sonde et préconditions : [preuve E00-04](preuves/lot-00-verifications.md).

**Commandes prévues, non exécutées**, seulement après création/intégration dans leur lot :

```powershell
# Lot 01 : après obtention du wrapper officiel et vérification de sa distribution.
.\mvnw.cmd -version
.\mvnw.cmd verify
# Lot 02 : après contrôle de la protection locale des secrets Windows.
python scripts/init-dev.py
python scripts/lock-images.py
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml up -d
```

Sous Linux/CI : `./mvnw verify`. Le validateur de distribution `validate-pack.py` exige un pack sans `.local` ; après initialisation réelle, conserver sa recette dans une copie de préparation sans secrets, et ne pas supprimer `.local` pour le faire passer. Les checks applicatifs/Compose prennent le relais dans le dépôt intégré.

Ni `down -v`, ni reset Git, ni migration destructive, ni envoi réel, ni déploiement ne figurent dans ce plan.

## 6. Prochaine tranche et décisions produit

**Lot 01 — socle compilable et frontières d'architecture**. Aucun point produit ne bloque ses DTO/contraintes : santé, erreurs et refus sont des contrats techniques sans finance ni admission métier. Les lots indépendants ne sont pas suspendus en attendant les décisions de lots futurs. Le lot 01 n'a pas été exécuté par anticipation.

Points de vigilance techniques : wrapper absent à fournir ; vérifier les ACL Windows/OneDrive avant les secrets au lot 02 ; scans et revue d'identité encore nécessaires. Écarts ASVS documentés, sans revendication de certification.
