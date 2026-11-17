# Plan FE01 — Socle Angular borné

Plan initial établi le 10/11/2026 après [FE00](comptes-rendus/FE00_AUDIT_ET_DECISIONS.md), avant toute création. **FE01 a depuis été lancé par l'utilisateur** avec la contrainte « outils gratuits ». Les choix en vigueur sont Material/CDK MIT, Node 26.13.0 et npm 11.20.0 ; les réserves PrimeUI et la cible Node 24 ci-dessous décrivent le plan historique, pas les prérequis actuels. Voir [ADR005](adr/ADR005_OUTILS_LIBRES.md) et `app/README.md` pour l'exécution réelle. Aucun passage automatique à FE02.

## Préconditions et chemin

- Réinspecter les instructions/fichiers et Git ; préserver toute nouvelle modification.
- Répertoire de création : `worldtech-front/app/`, projet `worldtech-web`. Le dossier est absent au FE00 ; s'il existe désormais, l'inspecter au lieu de l'écraser.
- Fournir Node **24.22.2 LTS**, npm **11.19.1** ; `node --version` doit satisfaire `^24.15.0`. Le Node local 24.13.0 ne permet pas de lancer le CLI retenu. Pas d'upgrade global effectué ou implicitement autorisé par FE00 ; l'opérateur peut préparer sa version avec nvm-windows, puis vérifier les versions dans le shell utilisé.
- Registre `https://registry.npmjs.org/`, accès npm et binaires Chromium de Playwright. Windows 11 x64 local satisfait la cible OS documentaire ; navigateur non téléchargé/testé en FE00.
- Licence PrimeUI toujours ouverte : **ne pas installer/intégrer PrimeNG, PrimeUIX ou PrimeIcons en attendant**. Les tâches sans cette dépendance restent réalisables. Ne pas choisir une autre suite ni rétrograder Angular silencieusement.
- API réelle identifiée dans `../backend/openapi/maintenance-api.json` ; serveur non requis pour le socle fermé, requis pour la future recette FE03.

## 1. Créer sans écraser le pack

Commande prévue depuis `worldtech-front`, après validation des versions et absence de conflit de chemin :

~~~powershell
npx --yes --package=@angular/cli@22.2.6 ng new worldtech-web --directory app --routing --style scss --standalone --strict --ssr=false --package-manager=npm --skip-git --skip-install --defaults
~~~

Vérifier les options du CLI effectivement disponible avant exécution. `--skip-install` permet de rapprocher le manifeste généré de la matrice avant de résoudre ; `--skip-git` ne choisit pas de politique de dépôt à la place de l'utilisateur. Pas de CLI global, Nx, microfrontend, SSR ou store global.

Dans `app/package.json`, aligner les paquets framework/compiler-cli sur **22.2.5**, CLI/build sur **22.2.6**, TypeScript **6.0.3**, RxJS **7.8.2**, `packageManager: "npm@11.19.1"`. Les familles sont cohérentes malgré le patch CLI distinct. Conserver les auxiliaires nécessaires générés par ce CLI après vérification, pas une liste de versions devinée.

Tests/lint cibles vérifiés au registre : Vitest **5.1.0**, jsdom **31.0.0**, Playwright Test **1.65.0**, angular-eslint **22.6.0**, ESLint **10.14.0**, typescript-eslint **8.72.2**, types Node **24.19.2** si nécessaires à la configuration. Qualifier l'intégration du runner CLI, pas une deuxième chaîne Vite indépendante. Lucide **@lucide/angular 1.71.0** seulement si une icône est effectivement utilisée ; pas l'ancien `lucide-angular`.

Résoudre réellement, produire `package-lock.json`, vérifier `npm ls`, puis refaire `npm ci` et les contrôles. Aucun `--force`/`--legacy-peer-deps` ou désactivation d'engine check pour contourner Node. Ajouter une référence Node de projet et documenter l'installation reproductible. Les versions devront être réexaminées si le registre a évolué depuis FE00.

## 2. Configurer les fondations minimales

Arborescence cible sous `app/src/` :

~~~text
app/
  core/config/        configuration publique et validation
  core/http/          transport et ProblemDetail réel
  core/auth/          frontière de session, refus par défaut
  layouts/           structure minimale, pas de dashboard métier
  shared/ui/         seulement les présentations réellement nécessaires
  features/auth/     route publique d'attente de connexion, sans faux login
  features/client/   point d'entrée lazy fermé
  features/admin/    point d'entrée lazy fermé
  api/generated/     réservé, pas de faux client ni fichier généré manuellement
styles/              fondations CSS/SCSS
~~~

Ne pas remplir les dossiers de classes vides ou d'un service HTTP universel. Standalone, TS/templates stricts ; zoneless par défaut Angular 22, sans ajouter `zone.js`. Formulaires et mises à jour asynchrones testés dès leur introduction.

## 3. Navigation et session fermées

- Routes publiques minimales, écran 404, refus et indisponibilité ; lien de retour interne validé.
- `/client` et `/admin` en chargement différé ; pas d'utilisateur par défaut, de menu donnant ADMIN ou de garde qui accepte un rôle stocké localement.
- Avant FE03 : session non établie = aucun contenu privé. Distinguer état anonyme et service de session indisponible. Une session de démonstration ne devient jamais une authentification.
- Catalogue `/dev` minimal pour FE02, sans écran ticket métier. Route/modules de démonstration exclus **à la compilation production**, pas un simple booléen activable dans la configuration publique.

## 4. Configuration et HTTP

- Charger et valider la configuration publique avant bootstrap, avec erreur accessible si absente/invalide ; base API relative `/api/web/v1`, nom WorldTech, version de build ; Google/télémétrie désactivés. Pas d'URL de production de repli ou de secret.
- HttpClient dans un service de frontière ; représenter ProblemDetail et `fieldErrors` du [relevé réel](CONTRAT_BACKEND_RELEVE_FE00.md). Aucun appel métier dans les composants visuels.
- Préparer le proxy de développement `/api` vers `http://127.0.0.1:8080`, sans retirer `/api`. Documenter son résultat observé et son absence éventuelle de serveur ; ne pas inventer un succès d'intégration.
- Ne pas activer de Bearer web, refresh mobile ou stockage du CSRF. L'intégration du token JSON, des rotations et du MFA appartient à FE03 ; le mécanisme cookie/header Angular par défaut ne constitue pas une solution vérifiée.
- Pas de retry générique POST ou 403. Une erreur API n'active jamais les fixtures en production.

## 5. Scripts et CI à réaliser

Toutes ces commandes seront exécutées avec `app/` comme cwd, après création effective des scripts :

| Commande prévue | Mise en œuvre / preuve attendue |
|---|---|
| `npm ci` | Installation à partir du lock réel, sans modification du lock |
| `npm run start` | `ng serve` avec proxy déclaré et port/origine documentés |
| `npm run lint` | ESLint Angular et templates, flat config, aucun ignore global pour obtenir du vert |
| `npm run typecheck` | Vérification TS **et templates**, par exemple `ngc -p tsconfig.app.json --noEmit` à qualifier sur le socle |
| `npm run test` | `ng test --watch=false`, builder `@angular/build:unit-test`, tests comportementaux |
| `npm run test:e2e` | Playwright contre serveur frontend local piloté par `webServer`, pas une API simulée présentée comme Spring Boot |
| `npm run build` | `ng build --configuration production`, routes et budgets vérifiés |

Workflow futur distinct `.github/workflows/frontend-ci.yml` en racine du frontend, cwd `app`, versions fixées, `npm ci`, lint/typecheck/unit/build et smoke navigateur. Pas de changement de la CI backend. En l'absence actuelle de Git/remote, le fichier peut être préparé mais aucune exécution distante ne sera annoncée. Prévoir un worker Playwright local si les ressources l'exigent, sans désactiver des scénarios.

## 6. Tests d'acceptation utiles

1. Configuration absente/invalide/externe : pas de bootstrap privé ni appel vers un serveur de repli.
2. Accès direct `/client` ou `/admin` sans identité : fermé, pas de données privées ni flash privilégié ; indisponibilité distincte d'un logout.
3. URL inconnue et retour interne : 404 utilisable, destination externe refusée.
4. Build production : `/dev` et fixtures non accessibles et non activables par un changement runtime.
5. Échec HTTP : erreur explicite, pas de fallback métier ni retry de commande implicite.
6. Catalogue/socle : vérification clavier et rendus représentatifs clair/sombre, desktop/téléphone lorsque ces fondations sont effectivement livrées ; ne pas confondre test de tokens et capture navigateur.

## Livrables et arrêt

Socle compilable, lockfile réel, scripts et CI initiale, README de développement dans `app/`, rapport FE01 et suivi séparant local/simulé/API réelle/visuel. **Pas de tickets, missions, rapports, authentification complète ou design system FE02.**

Prérequis différés : licence PrimeUI avant ses premiers imports (FE02 au plus tard), actifs de marque avant validation visuelle FE02, backend démarrable et identités synthétiques contrôlées avant FE03. Ne créer ni compte ADMIN, ni secret, ni transaction métier pour terminer FE01.
