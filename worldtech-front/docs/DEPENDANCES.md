# Versions, dépendances et licences — FE00 à FE03

Relevé initial FE00 du **10 novembre 2026**, registre effectivement configuré et accessible : `https://registry.npmjs.org/`. Au moment de FE00, rien n'était installé ; « disponible » signifiait métadonnées accessibles, pas résolution ou build. L'état réellement installé en FE01 est décrit dans la décision actuelle ci-dessous et `app/package-lock.json` ; la matrice plus bas conserve le relevé historique.

## Socle FE01 — outils gratuits uniquement

Cette décision utilisateur remplace les cibles runtime/UI du relevé historique ci-dessous. Le socle dans `app/` utilise **Node 26.13.0 LTS Lithium**, fourni par la nouvelle installation `C:\Program Files\nodejs`, **npm 11.20.0**, Angular **22.2.5**, CLI/build **22.2.6**, **Material/CDK 22.2.6 MIT**, TypeScript **6.0.3** et RxJS **7.8.2**. Lockfile réel : `app/package-lock.json`. Pas de clé, abonnement, siège payant ou critère de taille d'entreprise.

- Les peers Material acceptent Angular `^22.0.0 || ^23.0.0`, CDK **exactement 22.2.6**, RxJS `^6.5.3 || ^7.4.0`. Le besoin au socle est réel : bouton/lien des pages d'état et champ/bouton de l'exemple de développement.
- Licences des **deux archives exactes** lues et SHA-512 vérifié contre le registre : MIT, même LICENSE SHA-256 `adb40d7d256e33c3c20a67869de175e7ca249afee92f587ccc48b5bec0ac24e4`. Attribution Google et permission de redistribution conservées par les notices de l'application/build.
- Node 26 est explicitement accepté par la matrice officielle Angular 22 ; `https://nodejs.org/dist/index.json` confirme 26.13.0 LTS et npm 11.20.0. Aucun changement global de version effectué par l'agent ; l'ancien Node 24.13.0 de nvm reste prioritaire dans le PATH initial.
- Tests/lint : Vitest 5.1.0, jsdom 31.0.0, Playwright Test 1.65.0, angular-eslint 22.6.0, ESLint 10.14.0, typescript-eslint 8.72.2, `@eslint/js` 10.0.1, types Node 26.6.6 ; licences MIT/Apache-2.0 et compatibilité conformes au registre. tslib 2.9.1 : 0BSD.
- **PrimeNG 22, PrimeUIX, PrimeIcons et license-manager exclus**, y compris transitivement. Pas de rétrogradation Angular/PrimeNG pour contourner ce choix. Lucide et la police libre restent différés jusqu'à utilisation.
- `npm run check:licenses` contrôle les métadonnées du lock, refuse ces fournisseurs et les licences non revues. L'inventaire inclut les variantes natives optionnelles non installées sur cette machine. Licences libres transitives : MIT, ISC, Apache-2.0, BSD, 0BSD, BlueOak, CC BY 4.0 et MPL-2.0 ; respecter attribution/conditions, pas assimiler gratuit à sans obligations.
- `npm audit` initial : dix alertes modérées liées à minimatch dans la chaîne de lint ; `npm audit --omit=dev` : zéro avis renvoyé au contrôle FE01. Pas de `--force`, rétrogradation TypeScript incompatible ou override majeur non qualifié pour masquer ce constat. Cela ne certifie pas l'absence de vulnérabilité.

Sources supplémentaires consultées le 10/11/2026 : `https://registry.npmjs.org/@angular/material/22.2.6`, `https://registry.npmjs.org/@angular/cdk/22.2.6`, leurs dist.tarball/LICENSE, `https://raw.githubusercontent.com/angular/components/main/guides/theming.md`, registre des versions exactes citées, index Node. Les URLs taggées GitHub 22.2.6 consultées ont renvoyé 404 ; elles ne sont pas utilisées comme preuve de licence. L'API de thème est aussi vérifiée sur le paquet installé et à la compilation.

Décision complète et conséquences FE02 : [ADR005 — outils gratuits](adr/ADR005_OUTILS_LIBRES.md).

## Ajouts FE02 — versions exactes et gratuits commercialement

Archives exactes lues en mémoire le 10/11/2026, SHA-512 vérifié contre `dist.integrity`, puis installation réelle dans `app/` sans autre changement de version directe.

| Dépendance | Besoin et compatibilité | Licence et obligations | SHA-256 du LICENSE |
|---|---|---|---|
| `@fontsource/ibm-plex-sans` **5.3.0** | Police de l'interface, sans peer Angular. Trois WOFF2 Latin 400/500/600 uniquement, `font-display: swap`, accents/Œ/œ/euro présents | **OFL-1.1**, usage commercial permis sans abonnement ; fichiers non modifiés, copyright IBM et licence redistribués, pas de vente de la police seule | `d0283623ef57e722fd0eb688a8041589670c608ab780cd3612d06ba6f153d3fd` |
| `@lucide/angular` **1.71.0** | SVG ciblés navigation/actions, peers Angular core/common `>=17`, tslib existant compatible | **ISC**, plus **MIT Feather** pour les icônes dérivées ; les deux notices et la liste des icônes sont redistribuées intactes | `b495047bd93a9b06913511076f504daba17d5bbeb3e0650f3bb53a4220329c57` |
| `@axe-core/playwright` **4.14.0**, dev uniquement | Tests d'accessibilité Playwright ; Node `>=22.12`, playwright-core `>=1`, compatibles Node 26 / Playwright 1.65 | **MPL-2.0** ; moteur libre sans axe DevTools PRO ni service payant. Aucune modification de ses sources, outil non livré au navigateur | `812e9d96e900a093ae4d1d3f22c5f82f568a0a0461c3007a99d00573d41c5461` |

Le lock résout aussi `axe-core` sous MPL-2.0 ; pas de suite UI supplémentaire. Le contrôle licences admet OFL-1.1 revue, refuse également `primeicons` non scopé et vérifie versions/empreintes des trois nouveaux LICENSE ; une mise à jour exige une nouvelle revue. `angular.json` copie les deux notices runtime depuis les paquets exacts dans les actifs publiés ; `verify:production` vérifie leur présence.

Sources : métadonnées et archives npm des versions exactes ; https://lucide.dev/guide/angular/getting-started ; https://fontsource.org/fonts/ibm-plex-sans/install ; https://github.com/dequelabs/axe-core-npm/tree/develop/packages/playwright ; API Sass et types effectivement installés de Material/Lucide. Aucun fournisseur payant, clé, limite de sièges ou achat implicite.

## Ajout FE03 — générateur de types OpenAPI

**`@hey-api/openapi-ts@0.97.0` MIT**, développement uniquement. Besoin : types OpenAPI 3.1 / API 0.6.0, sans ajouter un transport fetch/Bearer au runtime Angular. Plugins limités à `@hey-api/typescript` ; compatible Node 26 / TypeScript 6, sorties compilées avec Angular 22. Licence de l'archive exacte lue en mémoire, intégrité SHA-512 vérifiée, `LICENSE.md` SHA-256 **`7354457f471fda3d2ef7ae7b7dc758c98a57054875ccc1e211b950646b03bf10`**. Version/empreinte contrôlées par `check:licenses`, sorties non modifiées manuellement.

Le relevé npm FE03 rapporte **11 alertes modérées d'outillage, zéro élevée/critique** et aucun avis renvoyé pour le runtime. Dix alertes concernent la chaîne minimatch/lint déjà suivie ; la onzième, [GHSA-hhx9-57xq-r5rw](https://github.com/advisories/GHSA-hhx9-57xq-r5rw), concerne le template runtime `buildClientParams`. **Ce template n'est pas généré ni livré ici** : seuls `types.gen.ts` et `index.ts` sont produits depuis le JSON local revu. Cela limite l'exposition sans supprimer l'alerte.

La version 0.99.0 essayée durant qualification introduisait des alertes élevées YAML et a été écartée. La correction **0.97.3** du template a aussi été examinée : archive/SHA-512/MIT contrôlés (même empreinte de licence), installation **isolée sans scripts**, audit renvoyant **1 élevée + 3 modérées** via son nouveau graphe `js-yaml`. Elle n'a pas été installée dans l'application. Conservation de 0.97.0 en types-only, pas d'override de dépendance ou de `--force` pour cacher ces résultats. Requalifier une version corrigée avant d'ajouter un SDK runtime, d'accepter un contrat distant non fiable ou de mettre à jour l'outil. Rapports complets dans `verification/fe03/npm-audit*.json`.

Sources : versions exactes du registre npm et leurs archives, avis GitHub ci-dessus, génération réellement exécutée et compilée. L'installation verrouillée est aussi rejouée par `npm ci` dans un dossier temporaire, sans remplacer les modules du serveur utilisateur. Aucun abonnement/service commercial ajouté.

La recette Spring réutilise **les outils existants du backend** : JDK/Maven, Testcontainers (MIT) et l'image PostGIS déjà verrouillée par digest. Aucun logiciel Desktop payant n'est exigé ni installé ; un moteur compatible Testcontainers peut être libre (Moby/Docker Engine ou Podman, selon sa configuration). Docker Desktop n'est pas une dépendance du projet et sa licence ne doit pas être supposée gratuite pour toute entreprise. Respecter les licences PostgreSQL/PostGIS et des composants de l'image ; aucune image modifiée ou redistribuée par FE03.

## Matrice FE00 historique — cibles runtime/UI remplacées par la décision FE01

Les URL R des sources ci-dessous s'appliquent à toutes les lignes ; les dates sont celles de la consultation FE00, pas une promesse de support illimité.

| Élément | Pack / local | Version cible disponible | Compatibilité relevée | Licence / décision | Source |
|---|---|---|---|---|---|
| Angular framework, compiler, compiler-cli, forms/router/common/platform-browser | Majeur 22 proposé ; aucun installé | 22.2.5 | Node `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` dans le paquet ; compiler exact 22.2.5 ; RxJS `^6.5.3 \|\| ^7.4.0` | MIT ; retenu | A1/A2/R |
| CLI / `@angular/build` | Pas de CLI projet | 22.2.6 | Même ligne 22.2 ; build : Angular `^22.0.0`, TS `>=6.0 <6.1`, Vitest `^4.0.8 \|\| ^5.0.0` | MIT ; retenu, patch CLI distinct normal | A1/R |
| Node | Branche 24 proposée ; **24.13.0 installé** | **24.22.2**, LTS Krypton, publié 27/10/2026 | Matrice officielle Angular 22 : `^22.22.3 \|\| ^24.15.0 \|\| ^26.0.0` ; cible branche 24 | MIT et notices des composants embarqués ; Node local trop ancien, préparer la cible avant CLI | N1/N2/N3 |
| Gestionnaire npm | pnpm proposé, aucun lock ; npm 11.14.1 installé | **11.19.1** livré avec Node 24.22.2 | npm : Node `^20.17.0 \|\| >=22.9.0` ; CLI accepte npm `>=8` | Artistic-2.0 ; un seul gestionnaire, `packageManager` en FE01 | N2/R |
| TypeScript | Plage 6.0 proposée | **6.0.3** | compiler-cli/build `>=6.0 <6.1` ; **latest 7.0.2 écarté** | Apache-2.0 ; retenu dans la plage Angular | A2/R |
| RxJS | Branche 7 proposée | **7.8.2** | Intersection Angular `^7.4.0` et PrimeNG `^7.8.1` | Apache-2.0 ; retenu | R |
| `primeng` | 22.1.4 repérée | **22.1.4** | Angular core/common/forms/router/platform-browser et CDK `^22.1.0`, RxJS `^6.0.0 \|\| ^7.8.1` | **PrimeUI, pas MIT** ; intégration conditionnelle | P1/P2/R/T |
| `@angular/cdk` | Si nécessaire | **22.2.6** | Angular `^22.0.0 \|\| ^23.0.0`, RxJS `^7.4.0` ; satisfait PrimeNG `^22.1.0` | MIT ; requis si PrimeNG adopté, pas Material en seconde suite | R |
| `@primeuix/themes` | Version à vérifier | **3.1.0** | Dépend de `@primeuix/styled ^1.2.0`, pas de peer Angular déclaré | PrimeUI ; preset sur base Aura standard 16 px si droits validés, FE02 | P3/R/T |
| PrimeUIX transitive | Non figée | styled **1.2.0**, styles **3.1.0**, utils **0.8.3**, motion **1.0.0** | PrimeNG demande respectivement `^1.2.0`, `^3.1.0`, `^0.8.3`, `^1.0.0` ; themes et styles partagent styled | PrimeUI ; même réserve, ne pas supposer ces auxiliaires MIT | R/T |
| PrimeIcons / vérification licence transitives | Non précisées | `@primeicons/angular` **8.0.2**, core **8.0.2**, `@primeui/license-manager` **1.1.0** | Angular icons peer `>=21` ; PrimeNG demande icons `^8.0.0`, license-manager `^1.0.0` | PrimeUI ; ne pas contourner le mécanisme. Pas de seconde famille d'icônes choisie pour l'UI applicative | R/T |
| Lucide | `@lucide/angular` proposé | **1.71.0** | Angular core/common `>=17.0.0` ; standalone/Signals/zoneless documentés | ISC ; imports ciblés. Ancien `lucide-angular@1.0.0` écarté : peers limités à Angular 13–21 | L1/R |
| Formulaires | Reactive Forms typés | `@angular/forms` **22.2.5** | Même framework ; mises à jour programmatiques à notifier en zoneless | MIT ; pas de nouvelle bibliothèque de formulaire | A3/R |
| Vitest | Intégré au CLI proposé | **5.1.0** | Build accepte `^5.0.0` ; Vitest Vite `^6.4 \|\| ^7 \|\| ^8`, compatible Vite **8.3.5** déclaré par build ; Node `^22.12 \|\| ^24 \|\| >=26` | MIT ; intégration runner à prouver en FE01 | A4/V1/R |
| jsdom | Non figé | **31.0.0** | Node `^22.22.2 \|\| ^24.15.0 \|\| >=26.0.0` ; canvas optionnel, ne pas l'ajouter sans besoin | MIT ; DOM unitaire, pas preuve navigateur | A4/R |
| Playwright Test | Proposé | **1.65.0** | Node `>=20` déclaré, documentation OS/Node actuelle à respecter ; dépend exactement de playwright 1.65.0 | Apache-2.0 ; navigateurs assortis à installer en FE01 | W1/R |
| angular-eslint | Non choisi | **22.6.0** | devkit `>=22 <23`, ESLint `^9 \|\| ^10`, typescript-eslint `^8` | MIT ; flat config Angular/templates | E1/R |
| ESLint / typescript-eslint | Non choisis | **10.14.0** / **8.72.2** | TS-eslint : TS `>=4.8.4 <6.1.0`, ESLint `^8.57 \|\| ^9 \|\| ^10` ; ESLint Node `^20.19 \|\| ^22.13 \|\| >=24` | MIT ; cohérents avec TS 6.0.3 et Node cible | R |
| Types Node si requis | Absents | `@types/node` **24.19.2** | Branche runtime choisie, acceptée par le peer Vitest `>=24` | MIT ; outils seulement, pas de types Node globaux dans les composants navigateur | R |
| IBM Plex Sans | Proposée, aucun fichier | Candidat `@fontsource/ibm-plex-sans` **5.3.0** | Pas de peer Angular ; sous-ensemble latin/accents, 400/500/600 à contrôler en FE02 | OFL-1.1 ; conserver licence/nom réservé Plex, auto-hébergement | F1/R |
| Générateur OpenAPI | À choisir | **Différé, version non vérifiée** | Exiger Angular 22, TS 6, OpenAPI 3.1/nullable et noms stables malgré operationId manquants | Licence de l'outil exact à vérifier avant intégration FE03 | [Relevé backend](CONTRAT_BACKEND_RELEVE_FE00.md) |

Angular 22 est en support actif jusqu'à juin 2027, LTS jusqu'à juin 2028 selon A1. Angular 21 est LTS jusqu'à juin 2027 ; Angular 20 approche sa fin LTS du 28/11/2026 : aucune raison de partir sur ces lignes pour contourner le Node local. Ne pas choisir Node 25 EOL ou Node 27 alpha. Les peers des paquets autorisent parfois `>=26` plus large que la matrice officielle ; notre décision reste **Node 24**.

## Licence exacte PrimeUI — réserve avant installation

Neuf archives publiées ont été téléchargées **en mémoire seulement**, leur SHA-512 comparé à `dist.integrity`, et leur `package/LICENSE.md` lu, sans installation ni exécution de code fournisseur.

| Archive publiée | SHA-256 de LICENSE.md | Résultat |
|---|---|---|
| `primeng@22.1.4` | `e1a9c04fb0cb13763f191c63078a9a56ff5fca60ccf5aeb85f5d851cf4f46c9d` | PrimeUI, clé valide exigée ; ni MIT ni une simple édition LTS |
| themes 3.1.0, styled 1.2.0, styles 3.1.0, motion 1.0.0, utils 0.8.3, @primeicons/angular et core 8.0.2, license-manager 1.1.0 | `c401d571a85d4b3c98f35f1a3fc331ecd8526df6ed73953908be791426461cbb` | Même texte PrimeUI entre ces huit archives |

**Différence résolue au niveau du paquet :** le LICENSE de la branche GitHub PrimeNG consultée parle encore de Community MIT/LTS ; il n'est pas celui de l'archive 22.1.4. Le résumé embarqué mentionne par ailleurs associations/open source plus largement que les conditions web. Les conditions générales et l'addendum applicables doivent être examinés par le responsable de l'organisation, pas remplacés par ce résumé ou par la taille supposée de l'équipe.

L'addendum Community et les conditions générales consultés (P2) demandent notamment :

- moins de **5 développeurs dans l'organisation**, pas seulement dans ce projet ; moins de **10 employés** ; revenu annuel ou budget associatif **<1 M USD** ; financement externe selon l'addendum **≤3 M USD** et agrégation des entités contrôlées pour revenu/financement ; exclusions des organismes publics/établissements publics d'enseignement ;
- la page de résumé est plus stricte sur certains points (financement « <3 M », exclusion plus large des établissements). En cas de seuil ou statut concerné, obtenir clarification écrite plutôt que conclure à l'éligibilité ;
- au maximum quatre sièges de développement Community, clé valable douze mois, renouvellement annuel, grâce de trente jours ; avis de licence possible en production si clé absente/invalide/expirée ;
- vérification hors ligne selon l'éditeur, sans télémétrie de licence ; ne pas masquer les avis, désactiver le mécanisme ou redistribuer une clé ;
- Commercial per-developer : droits perpétuels sur versions reçues pendant une année de mises à jour ; chaque développeur concerné, prestataires inclus, doit être couvert. CI/agents automatisés ne consomment pas de siège ; les humains responsables restent concernés ;
- redistribution de composants pour que des tiers développent eux-mêmes : régime OEM séparé. Aucun besoin OEM n'est établi par ce portail.

**État WorldTech : non déterminé.** Entité juridique, périmètre de groupe, effectifs, revenus/budget/financements et licence existante non fournis. Aucun achat, inscription, attestation ou récupération de clé effectué. Ce relevé n'est pas un avis juridique.

**Travaux autorisés indépendants :** FE01 Angular sans PrimeUI, configuration publique, navigation fermée, HttpClient, gestion d'erreurs, tests/CI. Attendre les droits avant ajout des paquets/imports UI ; point limite FE02. Si l'organisation ne retient pas PrimeUI, présenter les conséquences d'une alternative et réauditer ses versions/licences, sans changement silencieux de suite ni gel sur une ancienne branche pour éluder le sujet.

## Sources consultées le 10/11/2026

- **A1** : https://angular.dev/reference/releases
- **A2** : https://angular.dev/reference/versions
- **A3** : https://angular.dev/guide/zoneless
- **A4** : https://angular.dev/guide/testing
- **N1** : https://nodejs.org/en/about/previous-releases
- **N2** : https://nodejs.org/dist/index.json et https://nodejs.org/download/release/v24.22.2/SHASUMS256.txt (distributions référencées, aucun binaire installé)
- **N3** : https://raw.githubusercontent.com/nodejs/node/v24.22.2/LICENSE
- **P1** : https://primeng.dev/installation
- **P2** : https://primeui.dev/pricing ; https://primeui.dev/licenses/community ; https://primeui.dev/eula/community ; https://primeui.dev/licenses/commercial ; https://primeui.dev/eula/commercial-per-developer
- **P3** : https://primeng.dev/theming/styled
- **L1** : https://lucide.dev/guide/angular
- **V1** : https://vitest.dev/guide/
- **W1** : https://playwright.dev/docs/intro
- **E1** : https://github.com/angular-eslint/angular-eslint
- **F1** : https://raw.githubusercontent.com/IBM/plex/master/LICENSE.txt
- **R** : métadonnées `https://registry.npmjs.org/<nom-encodé>/<version>` et tags des paquets de la matrice ; `npm view` exécuté également pour build/PrimeNG/npm. Exemples : https://registry.npmjs.org/@angular/build/22.2.6 ; https://registry.npmjs.org/typescript/6.0.3 ; https://registry.npmjs.org/primeng/22.1.4
- **T** : archives `dist.tarball` des neuf versions UI précises, contrôle décrit dans [les commandes FE00](../verification/FE00_COMMANDES_ET_CONTROLES.md).

## Verrou et dépendances différées

FE01 résout effectivement et conserve le lock npm, sans RC/nightly, `--force` ou `--legacy-peer-deps`. Un lockfile et un build restent indispensables même si les plages se recoupent dans cette matrice. Ne pas prétendre qu'une version publiée ou une licence permissive garantit absence de vulnérabilités ou comportement zoneless correct.

Agenda, carte, graphiques, éditeur riche, PDF viewer, MSW et outils de génération restent différés jusqu'au besoin qualifié, compatibilité et licence vérifiées. PDF côté backend ; un téléchargement autorisé peut suffire au frontend. Aucun paquet PRO, template commercial ou police n'est ajouté par FE00.
