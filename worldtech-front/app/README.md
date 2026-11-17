# WorldTech — Angular / identité web FE03

Application standalone, CSR/zoneless, Angular 22.2.5 et Material/CDK 22.2.6 (MIT). **Outils gratuits uniquement** ; pas de PrimeNG/PrimeUIX, clé commerciale, police distante ou abonnement nécessaire.

## Démarrer

Node **26.13.0 LTS** / npm **11.20.0**, versions de référence dans `.node-version`, `.nvmrc`, engines et packageManager. Depuis ce dossier `app/` :

~~~powershell
node --version
npm --version
npm ci
npm start
~~~

Ouvrir `http://localhost:4200`. Le nouveau Node est installé dans `C:\Program Files\nodejs`, mais l'ancien Node 24.13.0 de nvm peut rester prioritaire dans le terminal. Pour cette session PowerShell uniquement, avant les commandes :

~~~powershell
$env:PATH = 'C:\Program Files\nodejs;' + $env:PATH
node --version
npm --version
~~~

Aucune modification globale de PATH ou nvm n'est requise par le projet. Le CLI est local, pas à installer globalement. `engine-strict=true` refuse une version incompatible au lieu de la contourner.

## État fonctionnel réel

- `/connexion` utilise l'identité locale Spring : email/mot de passe, cookie HttpOnly, CSRF JSON gardé en mémoire et confirmation serveur `/me` avant accès privé. **Un backend démarré et des comptes de recette volontairement préparés sont nécessaires** ; le frontend ne crée pas de compte et n'invente pas de session lorsque le service est indisponible.
- Login V1 : actifs fournis, deux colonnes plein écran, scène stable pendant la saisie/les erreurs et aucun sélecteur de thème sur cette page uniquement. [Compte rendu et limites](../docs/comptes-rendus/LOGIN_V1_INTEGRATION.md), [recette dédiée](../verification/login-v1/RESULTATS.md). `npm run test:e2e:login` utilise le port **4320**, avec doubles HTTP explicites, sans toucher au serveur 4200.
- `/client` et `/admin` sont lazy et fermés tant que la session n'est pas établie. L'administration exige la preuve MFA serveur ; un rôle ou JWT dans localStorage n'accorde aucun accès. Les pages d'entrée ne présentent pas de fausses données métier.
- `/auth/mot-de-passe-oublie`, `/auth/password-reset`, `/auth/email-verification` : demandes génériques, liens à usage unique confirmés par POST, secrets retirés de l'URL avant bootstrap et perdus au rechargement. Aucun envoi SMTP réel n'est revendiqué par la recette locale.
- `/auth/mfa` et `/auth/securite` : preuve TOTP/récupération, enrôlement manuel, dix codes affichés une fois, réauthentification et renouvellement/remplacement. Le remplacement révoque les sessions et impose une nouvelle connexion. Les erreurs/rate limits ne sont pas rejouées automatiquement.
- `/compte/acces` distingue attente de validation, suspension, refus et parcours technicien Flutter. L'onboarding métier reste FE04, non commencé.
- `/acces-refuse`, `/session-indisponible` et 404 sont disponibles. Les gardes ne remplacent jamais une autorisation serveur.
- `/dev` reste le catalogue local avec champ/bouton et liens vers les aperçus **FE02** : `/dev/admin/tickets` et `/dev/client/incidents`. Leurs routes/imports/fixtures sont exclus du build production par remplacement **de développement**, pas par un flag runtime.
- FE02 fournit les tokens Material WorldTech, IBM Plex Sans auto-hébergée (OFL), Lucide (ISC/MIT), structures responsive Client/Admin et composants utiles : statut, erreurs, vide, confirmation, chronologie, montants exacts et formulaire typé.
- Les aperçus sont fictifs : filtres/recherche/pagination URL locaux, simulations explicites réseau/refus/version et validation de formulaire **sans création ni HTTP**. Les références/actualisations et liste globale ne sont pas certifiées par le contrat API 0.6.0. Les menus « À venir » ne sont pas des permissions.

## Configuration et API

`public/runtime-config.json` est servi comme `/runtime-config.json` et chargé avant bootstrap, sans cache/redirect ni URL de repli. Seules les cinq clés documentées sont admises ; API exacte `/api/web/v1`, marque WorldTech, Google et télémétrie désactivés tant que non livrés. Une configuration invalide ou indisponible empêche le démarrage et affiche une reprise accessible, sans détails sensibles. Ce fichier est public, **jamais un emplacement de secrets**.

Le proxy de développement conserve `/api/**` vers `http://127.0.0.1:8080`, sans réécriture. Démarrer/configurer le backend séparément ; en son absence, la page expose une indisponibilité, pas un accès de démonstration. Aucun Bearer web, retry POST, refresh mobile ou fallback vers de fausses données. FE03 gère le CSRF JSON et sa rotation, les requêtes privées tardives, l'expiration et l'invalidation inter-onglets. Le cookie de session n'est jamais lu par Angular.

Google reste **désactivé**, y compris dans la validation de configuration : le retour OAuth web actuel est un JSON, pas un parcours Angular validé. Ne pas passer `googleLoginEnabled` à `true` avant le raccordement/recette du callback et du fournisseur. [Contrats, états et réserve Google](../docs/AUTHENTIFICATION.md).

Types depuis le vrai contrat OpenAPI 3.1 / API 0.6.0 : `npm run api:generate`, puis `npm run api:verify`. Snapshot exact dans `api-contracts/`, provenance et sorties dans `src/app/api/generated/` ; ne pas modifier les fichiers générés. Le transport Angular reste dans `core/auth/identity-api.ts`. Les int64 métier générés en `number` ne sont pas qualifiés pour de longues versions ; conserver les ETag opaques avant les lots métier.

Le script `serve-build.mjs` est un serveur **local de smoke test**, pas un déploiement ni un proxy métier ; il renvoie 502 pour `/api`, jamais une fausse réponse Spring Boot. Les configurations de cache/CSP/reverse proxy de production restent à qualifier.

## Contrôles

~~~powershell
npm run check
npx --no-install playwright install chromium
npm run test:e2e
npm run test:e2e:production
npm audit --omit=dev
~~~

`check` lance licences, conformité des types OpenAPI générés, lint, types/templates/app/tests/outils, unitaires, build production et absence des catalogues/fixtures dans les bundles. Scripts individuels : `lint`, `typecheck`, `test`, `build`, `verify:production`, `check:licenses`, `api:verify`. Aucun backend nécessaire à ces tests ; les doubles HTTP navigateur ne prouvent pas Spring Security. Playwright utilise **4310** en développement et **4173** pour le build, sans réutiliser un serveur existant. Il ne faut pas arrêter votre `npm start` personnel sur 4200 pour cette recette.

### Recette contre Spring réel, isolée

~~~powershell
npm run test:e2e:integration
~~~

Prérequis : backend voisin `../../backend` depuis `app/`, son JDK/Maven et un moteur de conteneurs compatible Testcontainers ; aucune souscription Docker Desktop imposée. La fixture existante verrouille l'image PostGIS par digest. Le script compile le backend sans changer ses sources, démarre Spring sur **18083**, le frontend sur **4311**, et une **nouvelle base éphémère** ; il refuse un port backend déjà occupé. Aucun accès à votre PostgreSQL existant sur 5432 ni à votre backend sur 8080.

Comptes synthétiques préparés uniquement dans cette base. Secrets aléatoires temporaires : pipe stdin vers Java, environnement des workers, jamais arguments CLI, captures, traces, vidéos ou fichiers d'identifiants. Les tokens de confirmation sont émis par le vrai port de domaine ; pas de test backdoor HTTP. SMTP et Google désactivés. Le processus et la base sont arrêtés à la fin de la recette ; il ne reste pas de backend permanent utilisable par `npm start`.

Captures synthétiques FE03 : `../verification/fe03/screenshots/`, rapports axe dans `../verification/fe03/accessibility/`. Preuves FE01/FE02 conservées ; leurs non-régressions vont sous FE03. Rapports JSON navigateur dans `test-results/`, non versionnés ; [preuves et limites durables](../verification/fe03/RESULTATS.md). Axe et tests clavier ne certifient pas WCAG ; Firefox/WebKit, mobile physique, lecteur d'écran et zoom natif restent à vérifier.

Audit FE03 : **11 alertes modérées d'outillage**, aucune élevée/critique dans le graphe retenu ; aucun avis renvoyé pour les dépendances production. Le générateur 0.97.0 est limité aux types : son template runtime signalé n'est pas généré/livré. Les alternatives 0.97.3/0.99 introduisaient des avis élevés YAML et n'ont pas été retenues. [Analyse et obligations](../docs/DEPENDANCES.md), sans `--force`, override majeur ou downgrade incompatible. Ce résultat n'est pas une certification de sécurité.

Les quatre scripts natifs npm autorisés sont épinglés dans `allowScripts` après lecture : esbuild 0.28.2, @parcel/watcher 2.6.0, lmdb 3.5.6 et msgpackr-extract 3.0.7. Les autres scripts restent non approuvés par défaut. Licences/attributions : `public/third-party-notices.txt`, `dist/worldtech-web/browser/3rdpartylicenses.txt` copié automatiquement par `postbuild`, et `browser/licenses/` pour les notices intégrales police/icônes. Le contrôle licences vérifie aussi les versions et SHA-256 des nouveaux LICENSE revus.

CI initiale : `../.github/workflows/frontend-ci.yml`, sans changement de la CI backend. `api:verify` sait utiliser le snapshot lorsque le backend voisin est absent. L'intégration Spring isolée reste une commande locale séparée, pas une preuve de CI distante. Aucun dépôt Git/remote créé ni déploiement attesté.
