# FE01 — Socle Angular et outils gratuits

Date : 10 novembre 2026. Lot lancé par l'utilisateur après FE00, avec la contrainte explicite « always free tools ». **Statut : socle vérifié localement**, limites d'intégration et alertes d'outillage conservées ci-dessous. FE02 non commencé.

## Résultat livré

- Application `worldtech-web` créée dans **`app/`**, pack préservé en racine et backend voisin non modifié.
- Angular 22.2.5, CLI/build 22.2.6, standalone/CSR/zoneless, TS et templates stricts ; composants OnPush, état de vue Signals et transport RxJS.
- **Angular Material/CDK 22.2.6 MIT**, en remplacement de PrimeNG/PrimeUIX conformément à la demande utilisateur. Aucune clé de licence, achat, abonnement, police distante ou composant payant ; [ADR005](../adr/ADR005_OUTILS_LIBRES.md), instructions et design system rapprochés.
- Node **26.13.0 LTS** / npm **11.20.0** de la nouvelle installation utilisateur dans Program Files. Le terminal initial pointait toujours vers Node 24.13.0 de nvm ; PATH corrigé **seulement dans les processus de commandes**, pas globalement. La cible Node 24 documentaire FE00 est remplacée par ce runtime compatible.
- Lock npm réel, engines stricts, scripts reproductibles et whitelist versionnée de quatre scripts natifs examinés. Pas de contournement `--force`/`--legacy-peer-deps`.

## Navigation et frontières

Routes publiques de connexion **non encore implémentée**, session indisponible, refus et 404. Aucun formulaire qui recueille un mot de passe sans authentification réelle. Entrées lazy Client/Admin fermées par défaut avec `canMatch` ; absence d'identité, faux rôle local et lien direct n'accordent aucun accès. Les gardes servent à la navigation, pas à l'autorisation serveur.

Configuration publique chargée/validée avant bootstrap, API relative exacte `/api/web/v1`, aucune URL de repli ; erreurs de démarrage accessibles sans révéler la valeur rejetée. Google/télémétrie et mode démo runtime refusés tant que non livrés. HttpClient et erreurs issus de ProblemDetail réel : réseau, refus, conflit/version, champ, Retry-After ; pas de Bearer, retry de commande, redirection ou fallback simulé.

Proxy dev `/api/**` vers `127.0.0.1:8080`, sans réécriture, préparé mais pas validé contre Spring Boot dans ce lot. La session, le CSRF JSON et le MFA restent à intégrer en FE03, sans prétendre que les routes privées sont utilisables.

Exemple `/dev` : champ/bouton Material et validation locale uniquement. Remplacement de routes **au build de développement** ; pas de drapeau public activable en production. Thèmes clair/sombre/système et fondations clavier disponibles ; il ne s'agit pas du design system ni d'un écran métier FE02.

## Fichiers importants

- `app/package.json`, `package-lock.json`, `.npmrc`, `.node-version`, `.nvmrc`, `angular.json` et configurations TypeScript/ESLint.
- `app/src/app/core/config`, `core/auth`, `core/http`, `core/theme`, routes/features/layout/shared ciblés ; `api/generated/README.md` sans faux client.
- `app/public/runtime-config.json`, `theme-init.js`, notices gratuites ; aucun logo inventé, favicon Angular généré retiré.
- `app/e2e`, configurations Playwright, scripts licences/build/serveur de smoke local.
- `.github/workflows/frontend-ci.yml` : version Node exacte, actions officielles épinglées par SHA, droits lecture, aucun changement de CI backend ; pas de run distant attesté sans dépôt Git/remote.
- [README de travail](../../app/README.md), ADR002/005, dépendances, architecture, design system et AGENTS actualisés sans remplacer les autres consignes.

## Commandes réellement exécutées

Commandes npm depuis `app/`, avec `$env:PATH = 'C:\Program Files\nodejs;' + $env:PATH` et `NG_CLI_ANALYTICS=false` pour le CLI :

| Commande | Résultat observé |
|---|---|
| `node --version`, `npm --version`, `nvm list`, recherche des installations | Ancien PATH 24.13.0/11.14.1 ; nouvelle installation vérifiée explicitement : 26.13.0/11.20.0 |
| `npx --yes --package=@angular/cli@22.2.6 ng new --help` | CLI et options vérifiés |
| `npx --yes --package=@angular/cli@22.2.6 ng new worldtech-web --directory app --routing --style scss --standalone --strict --ssr=false --zoneless --test-runner=vitest --prefix=wt --package-manager=npm --skip-git --skip-install --ai-config=none --defaults --interactive=false` depuis la racine | Application générée dans app, pas de Git ni installation globale |
| `npm install` puis `npm ci` | Deux succès, 390 paquets installés ; audit complet remonte 10 alertes modérées d'outillage, non masquées |
| `npm ls --depth=0` | 22 dépendances directes aux versions fixées, aucun conflit indiqué |
| `npm install-scripts ls` | Quatre scripts initiaux examinés, puis whitelist exacte ; après ci : aucun script non revu |
| `npm run check:licenses` | Licences du lock contrôlées, zéro paquet PrimeUI ; dix expressions de licences libres identifiées, plateformes optionnelles comprises |
| `npm run lint` | Premier échec pour une échappée regex inutile, corrigée ; relance réussie sans désactiver la règle |
| `npm run typecheck` | Premier échec de signature de garde dans le test, corrigé par un test de navigation réelle ; relance réussie sur app/templates/tests/outils |
| `npm run test` | 34 tests réussis, 4 fichiers, runner CLI/Vitest, sans backend |
| `npm run build` | Premier build production réussi, 257,38 kB bruts initiaux / 69,16 kB de transfert **estimé par le CLI**, pas un score de performance navigateur |
| `npx playwright install chromium` | Chromium 158 / build 1254 et auxiliaires téléchargés dans le cache local Playwright |
| `npm audit --json` | Exit 1 : dix alertes modérées issues de minimatch, chaîne de lint ; zéro critique/élevée dans ce relevé |
| `npm audit --omit=dev --json` | Exit 0, aucun avis renvoyé pour le périmètre production |
| `npm run check` après corrections finales | Exit 0 : licences, lint, types/templates, 34 unitaires, build, notices et contrôle de 11 scripts production ; aucun catalogue dev inclus |
| `npm run test:e2e` | Premier passage : 11/12, défaut réel du lien d'évitement corrigé sans retirer l'assertion ; dernière relance : **12/12** |
| `npm run test:e2e:production` | Exit 0 : rebuild optimisé + vérification + **7/7** tests ; relance directe `npx --no-install playwright test --config playwright.production.config.ts` : 7/7 |
| Contrôle Python des liens Markdown propres au workspace | Exit 0 avant clôture, aucun lien cassé ; dépendances/vendor/sorties générées exclus de ce contrôle documentaire |
| Contrôle SHA-256 du pack et baseline backend | Manifeste historique intact ; aucun fichier original supprimé, seuls 19 Markdown adaptés depuis l'archive initiale (FE00 et FE01 cumulés). Templates/prompts/scripts/vendor inchangés ; 23 sources unslop et 268 fichiers backend de baseline identiques |
| `git status --short --branch` initial/final | Exit 1 « not a git repository », aucun init/commit/push ou CI distante annoncé |

Les licences des archives exactes Material/CDK ont aussi été lues en mémoire et contrôlées par SHA-512 ; conditions MIT et empreintes dans [DEPENDANCES](../DEPENDANCES.md). Les lectures et contrôles ne constituent pas un avis juridique.

## Recette finale et limites

**Chaîne finale réussie : 34 tests unitaires + 12 tests navigateur développement + 7 sur build production**, sans test ignoré ou retry masquant un échec. Les suites couvrent configuration invalide/absente, retour interne, espaces privés fermés, faux rôle local ignoré, erreurs HTTP sans rejeu, thème système/persisté/stockage bloqué et exclusion du catalogue en production.

Défaut clavier trouvé puis corrigé : avec `<base href="/">`, un href fragment simple revenait à la racine. Le lien d'évitement annule cette navigation, donne le focus au contenu et garde l'URL ; le test vérifie maintenant focus **et** maintien du lien direct.

Captures synthétiques 1440×900 et 390×844, clair et sombre, générées et lues : aucune fuite horizontale observée, labels et focus visibles. Ce contrôle porte sur l'exemple technique FE01, pas sur un écran métier ou une conformité WCAG complète. [Preuves et captures](../../verification/fe01/RESULTATS.md).

Dernier build : **257,60 kB bruts initiaux**, transfert **estimé CLI 69,28 kB**, dont JS initial 246,44 kB ; ce ne sont pas des mesures Core Web Vitals. Les notices runtime sont copiées dans le répertoire statique, pas seulement laissées à côté du dossier publié.

Les tests frontend/mocks HTTP vérifient le comportement de l'interface, **pas Spring Security ni les transactions backend**. Aucun compte créé, email envoyé, migration, serveur Spring Boot, API métier ou Google réel exécuté. Pas de déploiement, Lighthouse, preuve WCAG complète, Firefox/WebKit, mobile réel, revue humaine indépendante ou CI distante.

L'audit npm complet n'est pas vert : minimatch `<11.0.1` peut subir une amplification de motifs glob. Les outils de lint déclarent une autre ligne majeure ; ne pas appliquer un override majeur ou rétrograder TS-eslint vers une version incompatible avec TS 6 uniquement pour masquer l'avis. Suivi des mises à jour compatibles requis ; l'audit production sans avis ne certifie pas l'absence de vulnérabilité.

Le vérificateur de l'archive d'origine attend notamment absence de node_modules/application : il n'est pas utilisé comme test du workspace Angular et n'est pas modifié. Le manifeste historique et les sources unslop/templates/prompts/scripts historiques ont été contrôlés séparément et sont inchangés. Le bilan FE00 demeure une preuve historique, non réécrite comme build ou recette FE01.

## Prochain prérequis

Lancement explicite de **FE02** seulement après lecture de ce bilan. Logo/charte validés à fournir pour la validation visuelle ; Material/CDK remplace les API PrimeNG des prompts. Les identités synthétiques, proxy/HTTPS et session/CSRF/MFA réels seront les prérequis de FE03, pas une raison d'ouvrir le socle avec un faux utilisateur.
