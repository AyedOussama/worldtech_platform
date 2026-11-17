# FE00 — Commandes, résultats et limites

Date : 10/11/2026. Cwd des commandes : `C:\Users\ayedo\OneDrive\Desktop\gestion de ticket\worldtech-front`, sauf indication contraire. Ce relevé concerne l'audit documentaire, pas une application.

## 1. Inspection et contrôles existants exécutés

| Commande | Résultat observé |
|---|---|
| `git status --short --branch` | Exit 1, `fatal: not a git repository (or any of the parent directories): .git` ; état initial et final examinés |
| `git -C '..\backend' status --short --branch` | Même exit 1, aucun dépôt backend attesté |
| `Get-Command git, rg, node, npm, npx, py, python, java, mvn -ErrorAction SilentlyContinue \| Select-Object Name, Source` | Git/rg/Node/npm/npx/Python/Java trouvés ; Maven global absent, wrapper backend présent ; exit global 1 dû à la commande non trouvée |
| `node --version` | `v24.13.0`, insuffisant pour Angular 22 |
| `npm --version` | `11.14.1` |
| `nvm list` | Une seule version installée : 24.13.0 x64, active |
| `py --version` | `Python 3.14.3` |
| `npm config get registry` | `https://registry.npmjs.org/` |
| `Get-CimInstance Win32_OperatingSystem \| Select-Object Caption, Version, OSArchitecture` | Windows 11 Home, 10.0.26200, 64-bit |
| `npm view @angular/build@22.2.6 version engines peerDependencies --json` | Exit 0, build 22.2.6, Node `^22.22.3 \|\| ^24.15.0 \|\| >=26`, TS `>=6.0 <6.1`, Vitest `^4.0.8 \|\| ^5.0.0` |
| `npm view primeng@22.1.4 version license peerDependencies dependencies dist.integrity --json` | Exit 0, version disponible, `SEE LICENSE IN LICENSE.md`, peers et dépendances PrimeUIX/PrimeIcons/license-manager relevés |
| `npm view npm@11.19.1 version engines license --json` | Exit 0, Node `^20.17.0 \|\| >=22.9.0`, Artistic-2.0 |
| `py -B scripts/verify_pack.py` avant toute adaptation | **PASS, exit 0**, 87 fichiers, 15 prompts, 42 liens locaux sur 51 Markdown, 44 contrastes, 23 fichiers unslop inchangés, inventaire et SHA-256 exacts |
| `node scripts/verify_theme_init.mjs` | Exit 0, `{"ok":true,"scope":"reference_theme_initializer_only","cases":9}` ; cas simulés en VM Node, pas une capture ou un test Angular |

Recherches de fichiers exécutées avec rg, en complément des outils de lecture et recherche :

~~~powershell
rg --files --hidden -g '!vendor/**' -g '!.git/**' -g '!target/**' -g '!**/node_modules/**' -g 'AGENTS.md' -g 'package.json' -g '*lock*' -g 'angular.json' -g '*config*' -g '*openapi*' -g '*swagger*' -g '*.yml' -g '*.yaml' -g 'pom.xml' -g '*.svg' -g '*.png' -g '*.jpg' -g '*.woff*' . '..\backend' '..\.vscode'
~~~

Résultat : pack frontend sans application/configuration npm ; backend Maven, configurations dev/prod, OpenAPI et CI présents. Les fichiers `target/` renvoyés par ce glob ont été distingués des sources ; ils ne définissent pas une nouvelle convention frontend.

~~~powershell
rg --files --hidden -g '!**/.git/**' -g '!**/target/**' -g '!**/vendor/**' -g '!**/node_modules/**' -g '*.svg' -g '*.png' -g '*.jpg' -g '*.jpeg' -g '*.webp' -g '*.ico' -g '*.woff' -g '*.woff2' -g '*.fig' -g '*.sketch' -g '*.pdf' ..
~~~

Aucun résultat, exit 1 normal pour absence de correspondance. Aucun actif de marque/maquette trouvé dans ce périmètre ; les images d'exemple du vendor ne sont pas des actifs WorldTech. Les recherches textuelles logo/Figma/maquette et les lectures des documents n'ont identifié aucun actif validé supplémentaire. Aucun stockage externe non fourni n'a été inspecté.

Les fichiers d'instructions ont également été recherchés avec glob ; vérification explicite des ancêtres via `Get-Item` sans résultat (exit 1 lié aux chemins absents). Le manifeste initial a été lu et son empreinte calculée :

`5ec373d5b0b368a18bb3d6a0214b3cbdc685509285dcb395d7b90db796e8da66`.

## 2. Registre et sources officielles

Requêtes HTTPS en lecture seule via `fetch`, parallélisées pour les paquets indépendants, avec lecture des tags stables, versions, engines, peerDependencies/optionnels, dépendances, licences et dist. Les URLs de documentation officielles réellement consultées sont regroupées dans [DEPENDANCES](../docs/DEPENDANCES.md).

- Métadonnées des versions retenues : accessibles dans le registre configuré ; aucun tag next/rc/canary sélectionné.
- Framework core/common/forms/router/platform-browser/compiler/compiler-cli : versions 22.2.5 effectivement consultées ; CLI/build/CDK 22.2.6.
- TypeScript `latest=7.0.2` incompatible : sélection explicite de 6.0.3 ; ancien `lucide-angular@1.0.0` écarté pour ses peers Angular 13–21.
- Node officiel `dist/index.json` : branche 24 stable la plus récente 24.22.2, npm embarqué 11.19.1 ; checksum officiel de distribution accessible, aucun Node téléchargé/exécuté par cet audit.
- Les autres versions/peers exacts sont conservés dans la matrice. Ce n'est **pas** un `npm install`, `npm ci`, `npm ls` sur l'application, lockfile ou test de résolution complète.

## 3. Lecture des licences des archives exactes

Commande exécutée, sans extraction sur disque ni exécution de code fournisseur :

~~~powershell
py -B -c @'
import urllib.request, urllib.parse, json, io, tarfile, hashlib, base64
packages = [('primeng','22.1.4'),('@primeuix/themes','3.1.0'),('@primeuix/styled','1.2.0'),('@primeuix/styles','3.1.0'),('@primeuix/motion','1.0.0'),('@primeuix/utils','0.8.3'),('@primeicons/angular','8.0.2'),('@primeicons/core','8.0.2'),('@primeui/license-manager','1.1.0')]
seen = set()
for name,version in packages:
    url = 'https://registry.npmjs.org/' + urllib.parse.quote(name, safe='') + '/' + version
    with urllib.request.urlopen(url, timeout=45) as response:
        manifest = json.load(response)
    with urllib.request.urlopen(manifest['dist']['tarball'], timeout=60) as response:
        archive = response.read()
    actual = 'sha512-' + base64.b64encode(hashlib.sha512(archive).digest()).decode()
    assert actual == manifest['dist']['integrity'], 'Registry integrity mismatch: ' + name
    licenses = []
    with tarfile.open(fileobj=io.BytesIO(archive), mode='r:gz') as tar:
        for member in tar.getmembers():
            if member.isfile() and member.name.lower() in {'package/license.md','package/license','package/license.txt'}:
                raw = tar.extractfile(member).read()
                digest = hashlib.sha256(raw).hexdigest()
                item = {'path':member.name, 'sha256':digest, 'bytes':len(raw)}
                if digest not in seen:
                    item['text'] = raw.decode('utf-8')
                    seen.add(digest)
                else:
                    item['text'] = 'Identical license text already printed in this run'
                licenses.append(item)
    print(json.dumps({'package':name,'version':version,'registry':url,'license':manifest.get('license'),'integrityVerified':True,'licenses':licenses}, ensure_ascii=False, indent=2))
'@
~~~

Résultat : **exit 0, intégrité vérifiée sur les neuf archives**, LICENSE PrimeUI, clé valide exigée et renvoi aux conditions Community/Commercial. SHA-256 des textes dans [la matrice](../docs/DEPENDANCES.md). Le texte de branche GitHub MIT/LTS est différent et n'est pas retenu comme licence de PrimeNG 22.1.4. Conditions générales/addenda et leurs différences avec les résumés consultés ; aucune éligibilité WorldTech déclarée, aucune licence acquise.

## 4. Contrôle statique de l'inventaire des routes

Commande exécutée après correction d'un premier essai `py -B -c` ayant échoué avec `SyntaxError` à cause des guillemets transmis par PowerShell. La relance utilise l'entrée standard, sans changer les sources :

~~~powershell
@'
import json, pathlib, re, collections
root=pathlib.Path('../backend')
d=json.loads((root/'openapi/maintenance-api.json').read_text(encoding='utf-8'))
methods={'get','post','put','patch','delete','head','options','trace'}
expected={(m,p) for p,item in d['paths'].items() for m in item if m in methods}
found=set()
for file in (root/'src/main/java').rglob('*.java'):
    text=file.read_text(encoding='utf-8')
    if '@RestController' not in text or '@RestControllerAdvice' in text: continue
    constants=dict(re.findall(r'public static final String\s+(\w+)\s*=\s*"([^"]+)"', text))
    for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping\(([^\n]*?)\)', text):
        method=match[1].lower(); args=match[2]
        if args.startswith('path = '): args=args.split(',',1)[0].split('=',1)[1].strip()
        args=args.strip('{} ')
        for expr in args.split(','):
            parts=expr.strip().split('+')
            path=''.join(part.strip()[1:-1] if part.strip().startswith('"') else constants.get(part.strip(), 'UNRESOLVED') for part in parts)
            if path.startswith('/api/') or path == '/health': found.add((method,path))
found.update({('post','/api/web/v1/auth/login'),('post','/api/web/v1/auth/logout'),('get','/oauth2/authorization/google'),('get','/login/oauth2/code/google')})
missing=sorted(expected-found); extra=sorted(found-expected)
counts=collections.Counter('web' if p.startswith('/api/web/') else 'mobile' if p.startswith('/api/mobile/') else 'other' for m,p in expected)
print(json.dumps({'scope':'static_source_mapping_only_not_http','documentedOperations':len(expected),'sourceOperationsIncludingSecurityFilters':len(found),'missingInSources':missing,'undocumentedInOpenApi':extra,'counts':dict(counts)},ensure_ascii=False,indent=2))
assert not missing and not extra
'@ | py -B -
~~~

Résultat : exit 0, 133 opérations de chaque côté, aucune manquante/supplémentaire ; web 109 / mobile 21 / autres 3. Le script est limité aux formes d'annotations présentes ; ce n'est pas un analyseur Java générique. Les quatre routes de filtres sont ajoutées après lecture de leur configuration, pas supposées à partir du seul OpenAPI.

Autre contrôle Python en lecture seule exécuté : parsing JSON, résolution des `$ref` locaux, comptage des schémas et examen de la sécurité des opérations. Résultat : 117 chemins / 83 schémas, 711 références résolues, aucune référence cassée, sécurité explicite partout, CSRF déclaré pour toutes les mutations web ; 132 operationId absents. Empreinte du contrat : `b5f122cc7ac464b544b1ee0f98b4e8ae379bf6521af2156e8b0ab6f73db1f1c5`.

Ces contrôles ne valident ni toutes les réponses contre JSON Schema, ni les filtres SQL, ni l'authentification à l'exécution. Aucun appel HTTP même à `/health`, aucun Maven/Docker/SMTP lancé, aucun compte ou secret utilisé.

## 5. Préservation et contrôle final

Baseline backend calculée avant nos modifications documentaires puis contrôlée en fin de lot : 268 fichiers dans `src`, `docs`, `openapi`, `scripts`, `.github`, `infra`, `.mvn`, plus AGENTS/POM/README-dev/wrappers. Agrégat SHA-256 des chemins et empreintes :

`93531b45dde0d391a9c2303b5046f3db34a6b724010facafbdda5f8048b4b32f`.

Les sorties volatiles `target` et journaux JVM ne font pas partie de cette baseline ; ils ne sont pas modifiés par l'audit. Le contrôle final ci-dessous conserve explicitement le résultat négatif du vérificateur d'archive tout en vérifiant le périmètre des écarts autorisés :

~~~powershell
@'
import pathlib, hashlib, json, runpy
root=pathlib.Path('.')
manifest=root/'manifest.sha256.json'
assert hashlib.sha256(manifest.read_bytes()).hexdigest() == '5ec373d5b0b368a18bb3d6a0214b3cbdc685509285dcb395d7b90db796e8da66'
baseline=json.loads(manifest.read_text(encoding='utf-8'))['files']
edited={
    'README.md','SUIVI_DES_LOTS.md','docs/DEPENDANCES.md','docs/ARCHITECTURE.md',
    'docs/DEVELOPPEMENT.md','docs/AUTHENTIFICATION.md','docs/API_CONTRATS.md',
    'docs/ALIGNEMENT_BACKEND.md','docs/UX_ECRANS.md','docs/ARBITRAGES.md',
    'docs/adr/ADR001_APPLICATION_ET_ROUTAGE.md','docs/adr/ADR002_UI_ET_ETAT.md',
    'docs/adr/ADR003_SESSION_ET_API.md','docs/adr/ADR004_PREUVES_ET_LIVRAISON.md'
}
added={'docs/comptes-rendus/FE00_AUDIT_ET_DECISIONS.md','docs/CONTRAT_BACKEND_RELEVE_FE00.md','docs/PLAN_FE01.md','verification/FE00_COMMANDES_ET_CONTROLES.md'}
actual={p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file()}
missing=set(baseline)-actual
changed={name for name,item in baseline.items() if name in actual and hashlib.sha256((root/name).read_bytes()).hexdigest() != item['sha256']}
new=actual-set(baseline)-{'manifest.sha256.json'}
assert not missing, missing
assert changed == edited, changed ^ edited
assert new == added, new ^ added
assert not any(pathlib.Path(name).suffix != '.md' for name in changed|new)
result=runpy.run_path('scripts/verify_pack.py')['verify']()
allowed={'Manifest inventory differs from pack'}|{prefix+name for name in edited for prefix in ('Size differs: ','SHA256 differs: ')}
assert not result['ok']
assert set(result['errors']) <= allowed, set(result['errors'])-allowed
backend=pathlib.Path('../backend')
folders=['src','docs','openapi','scripts','.github','infra','.mvn']
files=sorted(set([p for folder in folders for p in (backend/folder).rglob('*') if p.is_file()]+[backend/p for p in ['AGENTS.md','pom.xml','README-dev.md','mvnw','mvnw.cmd']]),key=lambda p:p.relative_to(backend).as_posix())
h=hashlib.sha256()
for p in files:
    h.update(p.relative_to(backend).as_posix().encode('utf-8')+b'\0'+hashlib.sha256(p.read_bytes()).digest())
assert len(files)==268
assert h.hexdigest()=='93531b45dde0d391a9c2303b5046f3db34a6b724010facafbdda5f8048b4b32f'
print(json.dumps({'scope':'FE00_documentation_changes_and_preservation_only','archiveVerifierOk':result['ok'],'archiveErrors':result['errors'],'files':len(actual),'changed':sorted(changed),'added':sorted(new),'missing':sorted(missing),'unchangedBaselineFiles':len(baseline)-len(changed),'backendFilesUnchanged':len(files),'backendAggregate':h.hexdigest(),'otherPackErrors':[]},ensure_ascii=False,indent=2))
'@ | py -B -
~~~

Résultats finaux exécutés :

| Contrôle | Résultat |
|---|---|
| `py -B scripts/verify_pack.py` relancé sans option de contournement | **FAIL, exit 1** : exactement 29 erreurs attendues, un inventaire différent et taille/SHA-256 des 14 Markdown adaptés. Aucun autre échec. 77 liens locaux sur 55 Markdown, JSON, 44 contrastes et 23 sources unslop contrôlés |
| Script de préservation ci-dessus | **Exit 0**, `archiveVerifierOk:false` maintenu ; 91 fichiers, 14 Markdown modifiés, 4 Markdown ajoutés, aucun fichier supprimé, 72 fichiers du manifeste inchangés et manifeste lui-même intact |
| Backend | 268 fichiers de baseline identiques, agrégat inchangé |
| Instructions, tiers et modèles | AGENTS, scripts, templates, notices/provenance et 23 fichiers unslop inchangés ; aucune installation ou source applicative ajoutée |
| Git final frontend et backend | Deux contrôles exit 1 « not a git repository », comme initialement ; aucun état clean, commit ou push annoncé |

Les 14 modifications et 4 ajouts sont exactement les ensembles `edited`/`added` du script. Le résultat d'archive reste négatif : le contrôle de périmètre documentaire n'est pas présenté comme une nouvelle validation de l'archive d'origine.

## 6. Contrôles non exécutés et passage suivant

- Pas de création Angular, installation globale, résolution complète/lock npm, build/lint/typecheck applicatif, Vitest ou Playwright.
- Pas de capture ni d'audit visuel/WCAG de composants réels ; contrastes des tokens et initialiseur VM seulement.
- Pas de simulation de parcours métier, recette Spring Boot, Google réel, test de charge, CI distante, scan de vulnérabilités ou déploiement.
- Pas de modification des scripts du pack, du manifeste, des templates, d'AGENTS, des notices/provenance tierces ou des sources backend pour faire passer un contrôle.

Prochaine étape autorisable : [FE01](../docs/PLAN_FE01.md), après préparation de Node compatible. PrimeUI reste suspendu à la licence de l'organisation, sans bloquer les fondations indépendantes. Le lot n'est pas lancé par ce rapport.
