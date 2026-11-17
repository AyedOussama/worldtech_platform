# Preuves FE02 — 10 novembre 2026

**Vérifications locales réussies, données synthétiques uniquement. Aucune intégration Spring Boot attestée.** [Compte rendu et décisions](../../docs/comptes-rendus/FE02_DESIGN_SYSTEM_ET_SHELLS.md).

## Résultats exécutés

| Contrôle | Résultat final |
|---|---|
| Runtime | Node 26.13.0 / npm 11.20.0, PATH limité aux shells ; aucune configuration globale changée |
| `npm install` | Exit 0, quatre paquets ajoutés : Fontsource, Lucide, axe Playwright et axe-core |
| `npm ci --prefix <temp opencode>` | Exit 0, 394 paquets installés depuis copie exacte package/lock/npmrc ; installation isolée pour préserver le serveur personnel sur 4200 |
| `npm run check` | **Exit 0**, licences, lint, types/templates, **45 unitaires**, build, notices et exclusion des fixtures production |
| `npm run test:e2e` | **31/31 réussis**, zéro skip/retry/flaky ; 12 régressions FE01 + 19 cas FE02 |
| `npm run test:e2e:production` puis suite directe sur dernier build | **9/9 réussis**, zéro skip/retry/flaky ; `/dev/admin/tickets` et `/dev/client/incidents` retournent la 404 en production |
| Vérification des rapports JSON | expected **31/9**, unexpected/skipped/flaky **0/0/0** dans les deux suites |
| `npm audit --json` | **Exit 1**, dix alertes modérées minimatch dans l'outillage de lint ; aucune élevée/critique renvoyée |
| `npm audit --omit=dev --json` | Exit 0, aucun avis production renvoyé ; cela ne certifie pas l'absence de vulnérabilité |
| Licences | 531 entrées de paquets du lock, onze expressions libres revues ; zéro PrimeUI/PrimeIcons, empreintes des trois nouveaux LICENSE vérifiées |
| Production | 11 scripts contrôlés ; aucun marqueur, ticket ou compte fictif FE02, pas de source map publiée, notices intégrales présentes |

Build final : **262,22 kB bruts JS+CSS initiaux**, **70,13 kB estimés en transfert par le CLI**, pas une mesure navigateur/Core Web Vitals. Polices séparées : trois WOFF2 Latin non modifiés, **71 024 octets** au total ; aucun WOFF, CDN ou alphabet supplémentaire. Les licences publiées sont identiques octet pour octet à celles des paquets installés.

Commandes npm depuis `app/`, avec `$env:PATH = 'C:\Program Files\nodejs;' + $env:PATH`, `NG_CLI_ANALYTICS=false`. Playwright utilise Chromium 158 / build 1254, un worker, ports **4310** et **4173**, sans réutilisation implicite du serveur 4200 de l'utilisateur. Le dossier d'installation de contrôle est `C:\Users\ayedo\AppData\Local\Temp\opencode\worldtech-fe02-install-201f0c77d31e4232a7368c5dc4603bdc`.

Les résultats détaillés courants sont `app/test-results/development.json` et `production.json` (sorties locales ignorées par Git). Les 19 cas FE02 exigent aussi zéro erreur JavaScript **et console error**, afin de capturer les erreurs gérées par Angular.

## Accessibilité : portée exacte

**23 rapports axe 4.14.0**, règles marquées WCAG 2 A/AA, 2.1 AA et 2.2 AA ; **zéro violation détectée** dans le périmètre testé. Rapports intégraux : [accessibility/](accessibility/). Aucun nœud exclu, aucune règle désactivée.

Cela **ne vaut pas certification WCAG**. Les résultats `incomplete` sont conservés : 138 occurrences de nœuds pour contraste, 33 pour `aria-hidden-focus`, 4 pour `aria-valid-attr-value`, répétées entre les vues. Lecture des exemples : champs vides à retester remplis, fonds/overlays dont axe ne détermine pas la superposition, contenu partiellement masqué par le scroll, sentinelles de piège de focus et références ARIA de popups. Ce ne sont pas des réussites déduites automatiquement.

Les parcours clavier couvrent lien d'évitement, menu, select, dialogue, défilement PageDown du détail, synthèse d'erreurs et liens vers champs, confirmation imbriquée, Échap et retour de focus, navigation mobile et boucle de tabulation. Les tests de focus attendent le focus initial annoncé par Material, pas une temporisation arbitraire.

Mesures sur composants rendus, six paires par thème, seuil texte 4,5:1 / contour-focus 3:1 :

| Cible | Clair | Sombre |
|---|---:|---:|
| Texte sélectionné / lien de ticket | 17,43 | 15,51 |
| Statut « attention » | 5,85 | 8,99 |
| Statut « en cours » | 5,50 | 6,33 |
| Contour du sélecteur natif | 3,63 | 4,33 |
| Anneau de focus natif réellement visible, 3 px | 6,16 | 7,75 |

Détails : [clair](accessibility/contrasts-light.json), [sombre](accessibility/contrasts-dark.json). Les états d'erreur, menus et select ouverts sont aussi analysés par axe. Une revue humaine des résultats incomplets et des lecteurs d'écran reste nécessaire ; Firefox/WebKit et appareils physiques non testés.

## Rendus synthétiques

24 captures générées, preuves FE01 non remplacées. Liste et formulaire : 1440×1000 et 390×844, deux espaces et deux thèmes ; vues d'erreur, texte 200 %, reflow et fondation complètent la collection.

| Espace | Ordinateur clair | Ordinateur sombre | Téléphone clair | Téléphone sombre |
|---|---|---|---|---|
| Administration | [Capture](screenshots/admin-light-1440.png) | [Capture](screenshots/admin-dark-1440.png) | [Capture](screenshots/admin-light-390.png) | [Capture](screenshots/admin-dark-390.png) |
| Client | [Capture](screenshots/client-light-1440.png) | [Capture](screenshots/client-dark-1440.png) | [Capture](screenshots/client-light-390.png) | [Capture](screenshots/client-dark-390.png) |

Exemples examinés : [formulaire mobile clair](screenshots/form-client-light-390.png), [sombre](screenshots/form-client-dark-390.png), [erreurs sombres](screenshots/invalid-form-dark.png), [texte à 200 %](screenshots/text-zoom-200.png), [reflow 320 px](screenshots/reflow-320.png). Les filtres s'empilent, les données essentielles ne sont pas réduites à 10 px et aucun débordement horizontal n'a été détecté dans ces cas.

Le contrôle de zoom agrandit le **texte racine à 200 %**, puis vérifie le reflow à 320 px ; il ne pilote pas le zoom natif de l'interface Chromium. Les vues ont été relues pour espacement, alignement, densité, contraste et libellés ; pas de validation humaine de marque, aucun logo officiel disponible.

## Échecs préliminaires et corrections

- Paramètre inutilisé en lint retiré ; budget CSS initial de la page dépassé, corrigé en séparant liste et pilotage au lieu d'augmenter le seuil.
- Contenu défilant du dialogue de détail non accessible au clavier : région nommée et focusable ajoutée, défilement effectif testé.
- Filtres tronqués sur téléphone, montant/devise se chevauchant : disposition intrinsèque en lignes et libellé plus court/flottant ; texte agrandi traité aussi par la grille du formulaire.
- Événements clavier envoyés avant chargement/focus initial Material : synchronisation sur titres/focus visibles, mêmes assertions conservées. Sélecteur Playwright corrigé vers le rôle et nom accessible exact « Thème ».
- Une passe fonctionnelle 31/31 a encore révélé une erreur Angular console de restauration du focus : la référence de bouton Material n'était pas un ElementRef. Lecture explicite `read: ElementRef` avec surcharge Angular correcte ; contrôle console ajouté à tous les cas FE02, puis chaîne et suite complète relancées. L'erreur TypeScript intermédiaire de surcharge a été corrigée sans cast ni désactivation ; le dernier `npm run check` est réussi.
- Les étapes initiales en échec ne sont pas présentées comme une recette réussie. Aucun retry, skip, exclusion axe, correction forcée de dépendance ou changement d'un fichier tiers pour masquer un résultat.

## Préservation, licences et limites

- Baseline avant FE02 : **162 fichiers**, aucun supprimé ; AGENTS, prompts, templates, scripts historiques, vendor et preuves/compte rendu FE01 préservés (**63 fichiers protégés** comparés).
- Dernier contrôle documentaire Python : **62 Markdown / 119 liens locaux**, aucun lien cassé. Comparaison de baseline : 22 fichiers existants modifiés, aucun supprimé ; modifications limitées à l'application, ses tests/notices et documents de suivi/design.
- Baseline backend : **268 fichiers identiques**, agrégat SHA-256 `93531b45dde0d391a9c2303b5046f3db34a6b724010facafbdda5f8048b4b32f`.
- Lock FE02 SHA-256 : `2a1f9672e0fbea8e64ac5fce0f99c634abd4f31c92ef649b0428fa2494734f3e`.
- Fontsource/LICENSE publié : `d0283623ef57e722fd0eb688a8041589670c608ab780cd3612d06ba6f153d3fd` ; Lucide/LICENSE publié : `b495047bd93a9b06913511076f504daba17d5bbeb3e0650f3bb53a4220329c57`. Textes complets OFL et ISC/MIT conservés.
- Attribution design : unslop par Damir Sokolovsky (@heydamir), CC BY 4.0 ; aucune modification du tiers.
- Git absent, ni commit/push ni CI distante ou déploiement attesté.

**Non exécutés** : identité/session/CSRF/MFA et Google réels, autorisations Spring Boot, commandes métier, client OpenAPI généré, upload/document, charge/Lighthouse, appareils physiques et revue humaine indépendante. Les fixtures ne sont pas des contrats serveur ; FE06 devra rapprocher liste, colonnes, filtres et contraintes de formulaire. Les gardes restent fermées et ne prouvent pas les droits API. FE03 attend son lancement explicite et ses prérequis d'identité réels.
