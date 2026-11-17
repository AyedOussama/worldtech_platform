# FE00 — Audit et décisions de départ

Date : 10 novembre 2026. Périmètre : inspection et documentation uniquement.
Statut : audit documentaire réalisé ; FE01 non commencé. Aucun compte rendu antérieur de lot frontend n'existait.

## Résultat et décisions

- Créer ultérieurement une application unique `worldtech-front/app/`, projet `worldtech-web`, sans toucher à `../backend` ni écraser le pack en racine.
- Retenir Angular standalone 22.2.5, CLI/build 22.2.6, rendu navigateur et zoneless ; routes Client/Admin différées. Pas de migration : aucune application frontend n'existe.
- Préparer Node 24.22.2 LTS et npm 11.19.1. Le Node local 24.13.0 est incompatible avec Angular 22 ; aucune modification de l'installation machine n'a été effectuée.
- Conserver PrimeNG comme choix **conditionnel** : `primeng@22.1.4` et PrimeUIX sont soumis à PrimeUI, pas à une licence MIT générale. L'organisation et ses droits ne sont pas connus. Le socle sans composants PrimeUI peut avancer ; leur installation/intégration attend la validation de licence, au plus tard avant FE02.
- Utiliser le vrai contrat `../backend/openapi/maintenance-api.json`, OpenAPI 3.1.0 / API 0.6.0. Pas de contrat fictif, de client généré ni de branchement HTTP créé en FE00.
- Conserver le logo officiel lorsqu'il sera fourni. Aucun actif WorldTech validé, police locale ou maquette n'a été trouvé dans le périmètre inspecté.

Les versions et licences sont détaillées dans [DEPENDANCES](../DEPENDANCES.md), le contrat dans [le relevé backend](../CONTRAT_BACKEND_RELEVE_FE00.md), et l'exécution suivante dans [le plan FE01](../PLAN_FE01.md).

## 1. Inventaire réel

| Élément | Constat |
|---|---|
| Dossier ouvert | `C:\Users\ayedo\OneDrive\Desktop\gestion de ticket\worldtech-front` |
| Pack | Directement en racine, sous le nom `worldtech-front` ; pas de second dossier `worldtech-angular-pack-v1` à fusionner |
| Racine initiale | 87 fichiers : documentation, 15 prompts, templates, scripts de vérification et vendor unslop |
| Instructions | `AGENTS.md` frontend lu ; aucun AGENTS supplémentaire trouvé sous ce dossier ni dans les ancêtres vérifiés. `../backend/AGENTS.md` lu pour l'inspection du backend voisin, sans le remplacer |
| Git | `git status --short --branch` échoue : aucun dépôt ici ni dans ses parents. Même résultat dans `../backend`. Pas de branche, commit, diff initial ou remote attestable |
| Préservation | Vérification initiale du manifeste : PASS, inventaire exact et SHA-256 identiques. Aucun écart au pack d'origine détecté avant nos adaptations ; cela ne prouve pas une absence d'historique utilisateur externe |
| Frontend | Ni `package.json`, lockfile, `angular.json`, `src` applicatif, builder installé, dépendance UI, configuration CI frontend ou environnement Angular |
| Gestionnaire | Aucun gestionnaire de projet imposé par un lockfile. npm fonctionne et vise `https://registry.npmjs.org/` ; choisi plutôt que d'ajouter pnpm proposé par le pack |
| Machine | Windows 11 Home x64, version 10.0.26200 ; Node 24.13.0 via nvm-windows, seul Node listé ; npm 11.14.1 ; Python 3.14.3 via `py` |
| Voisinage | `../backend`, `../.vscode` et cahier produit v0.3. Arborescence multi-projets, mais pas de monorepo Git ou workspace JS établi |
| Backend | Maven wrapper, Boot 4.1.2 / Java 25 déclarés dans le POM ; monolithe par domaines. OpenAPI, sources, migrations, tests et CI déjà présents ; aucun fichier backend modifié |
| Backend local | Configuration source : API sur `127.0.0.1:8080`, origine frontend par défaut `http://localhost:4200`, profils dev/prod. Ne pas recopier ses secrets dans Angular |
| Tests existants | Front : vérification du pack et initialiseur de thème de référence uniquement. Backend : suites Java/Python et rapports historiques ; non réexécutés dans cet audit |

Le dernier bilan backend consulté est le lot 06 du 02/11/2026 : il annonce 293 tests Java et 6 Python. Ce sont des **preuves historiques du backend**, pas des tests exécutés par FE00 ni une recette navigateur.

### Stratégie d'intégration du pack

Garder les documents, prompts, templates et `vendor/` à leur emplacement actuel. Créer seulement `app/` en FE01 ; toutes les commandes npm auront ce répertoire de travail. Les chemins `src/...` de l'architecture seront donc relatifs à `app/`.

Ne pas dupliquer ou déplacer automatiquement le pack dans `docs/frontend-pack`, ni remplacer AGENTS, ni initialiser Git en FE00. En FE01, utiliser `--skip-git` pour éviter un dépôt imbriqué choisi par le CLI. La future politique de dépôt/remote et l'activation de CI distante restent à confirmer ; cela n'empêche pas les contrôles locaux. Une éventuelle CI frontend en racine sera distincte de celle du backend, dont les chemins ne sont pas adaptés ici.

`manifest.sha256.json` reste le manifeste **historique de l'archive** : l'adaptation des documents demandée par FE00 en change nécessairement les empreintes. Ne pas le régénérer pour annoncer un faux succès. Le script d'origine, les templates et les fichiers tiers sont conservés ; le bilan des écarts intentionnels figure dans [les contrôles](../../verification/FE00_COMMANDES_ET_CONTROLES.md).

## 2. Architecture rapprochée

| Décision | État / raison |
|---|---|
| Une application standalone | Retenue : identité commune, deux espaces web et réutilisation de fondations ; pas de contrainte constatée justifiant deux builds |
| CSR, lazy `/client` et `/admin` | Retenu : espaces authentifiés, pas de besoin SEO démontré ; pas de SSR ou service worker de données privées ajouté |
| Builder application Angular CLI | Cible `@angular/build:application`, dev server et tests fournis par le CLI 22 ; configuration effective à générer/vérifier en FE01 |
| Zoneless et Reactive Forms typés | Retenus ; notifier la vue lors des mises à jour programmatiques des formulaires. Les peerDependencies ne prouvent pas le comportement des contrôles UI |
| État | Signals pour la vue ; RxJS pour HTTP/recherches annulables ; filtres partageables en URL ; pas de store global initial |
| UI | Une seule suite principale, PrimeNG conditionnel ; CDK requis par ses peers, pas de seconde suite Material ajoutée |
| Identité | Cookie serveur/CSRF JSON réel, MFA ADMIN obligatoire ; refus des routes privées tant que l'identité n'est pas établie, pas de Bearer web |
| API générée | Différée : OpenAPI 3.1 réel trouvé mais générateur Angular 22/TS 6 non encore qualifié ; 132 opérations sur 133 sans `operationId` |
| Simulation | Adaptateurs/fixtures de développement séparés, jamais de secours silencieux après une erreur API ; aucun accès de démonstration en production |

ADR001 et ADR004 retenus pour ce dépôt ; ADR002 conserve sa réserve de licence UI ; ADR003 est rapproché des sources avec intégration réelle encore non vérifiée. AGENTS existant est conservé intégralement.

## 3. Backend disponible et écarts utiles

Inspection statique : 117 chemins, 133 opérations (109 web, 21 mobile, 3 santé/OAuth), 83 schémas. Les mappings sources, complétés par les quatre routes de filtres Spring, correspondent à cet inventaire. Les 711 références locales se résolvent ; chaque opération déclare sa sécurité et chaque mutation web le CSRF. Ce n'est ni une validation exhaustive OpenAPI/JSON Schema, ni une preuve des droits à l'exécution.

Points qui changent concrètement la suite :

1. **CSRF JSON, pas cookie XSRF lisible.** `GET /api/web/v1/auth/csrf` fournit `{headerName,token}` ; le token sera gardé en mémoire et relu après login/MFA/réauthentification. L'intercepteur XSRF Angular par défaut ne suffit pas.
2. **Identité limitée.** `/me` expose exactement `userId`, `status`, `roles`, `emailVerified`, sans capacités détaillées ni indicateur d'enrôlement MFA. Un ADMIN pré-MFA y est refusé. Ne pas inventer de champs ni déduire une session pré-MFA d'un 403 ; après rechargement sans état connu, rester fermé et proposer la reprise de connexion en FE03.
3. **Refus génériques.** CSRF et autorisation peuvent produire le même `ACCESS_DENIED`. Aucun retry de mutation sur tout 403 ; ne pas afficher une cause que le serveur n'a pas établie.
4. **Parc et finances déjà décidés.** CLIENT lit son parc ; ADMIN seul le modifie. Proposition CLIENT : deux forfaits HT/EUR facultatifs et séparés, null distinct de zéro. Cela ne décide pas des conditions publiées/acceptées futures.
5. **UX de liste à rapprocher.** Tickets actuellement par compte/site, sans liste globale, recherche textuelle, tri libre, référence publique ou `updatedAt` documentés. La maquette de référence peut rester isolée ; elle ne prouve pas ces capacités API.
6. **Fonctions futures absentes.** Pas de mission/affectation/intervention/rapport, upload/antivirus, notifications utilisateur ou statistiques métier exposés. Approbation technicien bloquée en attente des contrôles documentaires. Google présent dans le code mais désactivé et non validé contre Google réel.

## 4. Direction de départ — cinq éléments

1. **Produit.** WorldTech permet aux clients de déclarer et suivre leurs incidents IT, et à l'administration d'organiser leur traitement ; l'exécution technicien reste dans Flutter.
2. **Caractère.** Précis, calme, opérationnel.
3. **Univers.** Dossiers d'incident, parc informatique, sites, chronologies et créneaux ; surfaces graphite neutres et accent bleu mesuré du pack, toujours provisoire face aux actifs de marque.
4. **Moments.** Progression du dossier et créneau/prochaine étape utiles, avec libellés et couleur sémantique ; pas d'illustration décorative ni faux graphique de performance.
5. **Signature.** Un repère discret de prochaine action, associé au statut et au responsable uniquement lorsque cette information est réellement disponible.

Aucun écran existant à redessiner : aucune passe visuelle ni capture n'est annoncée. Logo/charte, maquettes/Figma, exemples anonymisés d'incidents et de rapports manquent. Afficher ultérieurement le nom texte WorldTech en attendant le logo ne crée pas un nouveau logo. IBM Plex Sans reste proposée ; fallback système autorisé, fichiers et licence à intégrer en FE02. Clair/sombre/système, clavier, focus visible et WCAG 2.2 AA restent les objectifs, pas une conformité déjà obtenue.

Principes de relecture issus d'**unslop par Damir Sokolovsky (@heydamir), CC BY 4.0**, dont le skill a été lu et les sources sont inchangées. Voir [l'attribution](../../THIRD_PARTY_NOTICES.md).

## 5. Vérifications et limites

Commandes et scripts exacts : [FE00 — commandes et contrôles](../../verification/FE00_COMMANDES_ET_CONTROLES.md).

- Git initial/final : contrôlé, indisponible car aucun dépôt ; aucun init/commit/push effectué.
- Pack avant adaptation : PASS ; 42 liens locaux, 44 paires de contraste, 23 fichiers unslop et manifeste vérifiés.
- Initialiseur de référence : 9 cas réussis sous Node local ; cela ne rend pas Node compatible avec Angular 22 et ne teste pas un navigateur.
- Registre npm et sources officielles : accessibles ; métadonnées, peers et licences exactes relevés. Neuf archives PrimeUI lues en mémoire et intégrité SHA-512 vérifiée, sans installation/exécution de paquet.
- Contrat backend : inventaire et rapprochement statiques effectués ; aucune requête HTTP métier, aucun compte, aucune base ni secret modifiés.
- Contrôles finaux : 14 Markdown adaptés et 4 ajoutés, aucun fichier supprimé ; 72 fichiers du manifeste et le manifeste lui-même inchangés, baseline backend de 268 fichiers identique. Le vérificateur strict de l'archive sort en échec attendu avec seulement 29 écarts d'inventaire/taille/SHA-256, non masqués ; 77 liens, 44 contrastes et 23 sources unslop contrôlés sans autre erreur.
- Non exécutés : installation/résolution complète npm, build/lint/typecheck Angular, Vitest, Playwright, captures, tests Spring Boot, CI distante, scan de vulnérabilités et déploiement.

## 6. Passage à FE01

Le [plan FE01](../PLAN_FE01.md) est borné et exécutable après mise à disposition de Node compatible. La licence PrimeUI ne bloque pas le routage fermé, la configuration publique, HttpClient, les tests ou le build sans UI fournisseur. L'actif de marque et les choix des lots métier ne bloquent pas ce socle.

Seule la décision humaine de lancer FE01 autorisera ce lot. **Aucun frontend, écran métier ou achat n'a été créé par FE00.**
