# FE02 — Design system, structures et références fictives

Date : 10 novembre 2026. Lot lancé explicitement par l'utilisateur après FE01 (« continue au next étape »). **Vérifié localement**, limites d'intégration/accessibilité ci-dessous ; FE03 non commencé. Outils gratuits uniquement, Angular Material/CDK conservé, aucun PrimeUI.

## Résultat

- Thème WorldTech clair/sombre/système mappé aux API publiques `mat.theme`, `mat.theme-overrides` et overrides de composants de Material 22.2.6. Variables communes de surface, texte, contour, accent, focus et sémantique, y compris overlays. Le service/préchargement de thème FE01 est conservé.
- IBM Plex Sans 400/500/600, trois WOFF2 Latin auto-hébergés ; fallback et `font-display: swap`, accents, Œ/œ, euro et espaces typographiques français. Lucide SVG 18 px, trait 1,5, imports ciblés ; icônes décoratives masquées, boutons nommés.
- `ApplicationShell` responsive Client/Admin : navigation 248 px, barre 64 px pour les cibles tactiles de 44 px ; drawer modal sous 960 px, Échap/piège de focus/retour au déclencheur. Aucune gestion de session ni capacité serveur simulée dans le layout.
- Composants partagés effectivement utilisés : PageHeader, StatusLabel, ErrorSummary, EmptyState, ConfirmDialog, Timeline, MoneyDisplay, ThemePicker et formulaire IncidentForm. Les contrôles/menu/dialogue Material ne sont pas enveloppés sans besoin.
- Deux références : **`/dev/admin/tickets`** et **`/dev/client/incidents`**, accessibles depuis `/dev` en développement uniquement. Filtres/recherche/pagination URL, table sur grand écran et cartes lisibles sur petit écran, statut textuel et prochaine action stable.
- Formulaire représentatif : site, priorité demandée distincte de retenue, objet, description, proposition client facultative en chaîne décimale. Erreurs liées aux contrôles, synthèse de correction avec liens et focus, valeurs conservées en simulation de réseau/refus/version périmée, double soumission bloquée pendant l'attente locale.
- Confirmation avant abandon d'une saisie modifiée ; Échap conserve la saisie dans cette confirmation. Aucun ticket n'est créé, aucun succès Spring fictif, aucun upload ou téléchargement inventé.

## Frontières et décisions

Les routes privées `/client` et `/admin` gardent **exactement la même frontière de session fermée**. Déplacer le shell public du composant racine vers les routes publiques permet des structures plein écran sans imbriquer deux en-têtes ou deux `<main>`. Les aperçus ne donnent aucun rôle ; aucun sélecteur d'identité, JWT, session de démo ou capacité serveur ajouté.

Fixtures déterministes dans `features/dev/tickets/ticket-fixtures.ts`, jamais présentées comme DTO API. Les références `DEMO-*`, actualisations, liste globale, recherche et prochaines actions sont des propositions UX **absentes du contrat 0.6.0**, à rapprocher en FE06. Les exemples de proposition client et de chronologie sont annoncés comme illustratifs ; montants client/publié/accepté non confondus, aucune marge ou paie calculée.

Les simulations sont activées par des contrôles de développement explicitement nommés. **Pas de fallback de l'API vers les fixtures**. Aucun HTTP métier n'est lancé par ces écrans. Seule la préférence de thème non sensible peut être enregistrée ; formulaire, filtres et données métier ne sont pas mis dans localStorage.

Le fichier de routes production reste vide pour le catalogue. Le contrôle de build refuse désormais aussi marqueur/reference/compte fictif FE02 ; les tests production ouvrent directement les deux nouvelles URL et vérifient une 404. Les gardes Angular ne sont toujours pas une autorisation serveur.

## Direction et passes de design

Référence : **unslop par Damir Sokolovsky (@heydamir), CC BY 4.0**, sources tierces inchangées. Skill et références signes/tokens/rendus lus avant les changements. La direction WorldTech existante est conservée, pas une refonte extérieure au lot.

| Axe | Décision/correction |
|---|---|
| Produit | Organiser les demandes d'intervention IT entre clients et administration ; technicien dans Flutter |
| Caractère | Précis, calme, opérationnel |
| Univers | Dossiers d'incident, sites, qualification et historique |
| Moments | Statuts sémantiques et chronologie utile, pas d'image décorative ou de graphique inventé |
| Signature | Prochaine action avec responsable, repère vertical discret et emplacement stable |
| Espacements | Grille de 4 px, marges 32/16, cibles 44 px ; composants distincts plutôt qu'augmentation du budget CSS |
| Couleurs | Palette du pack inchangée ; pas de dégradé, glow, teintes décoratives ou couleur seule pour un état |
| Icônes | Une famille, 18 px, trait 1,5, noms accessibles pour les actions |
| Rédaction | Titres de tâche, statut explicite, mentions fictives utiles ; pas de slogan, faux utilisateur ou fausse réussite |
| Typographie | IBM Plex Sans, 400/500/600, corps lisible et chiffres tabulaires, montants formatés sans flottants |
| Disposition | Contenu fluide, table desktop, cartes sous 1200 px, une colonne sous 680 px ; filtres/formulaire s'empilent aussi avec le texte agrandi |
| Détails | Contours de contrôle distincts des séparateurs ; libellé de montant raccourci et flottant pour éviter le chevauchement avec EUR HT |
| Visuels | Les informations d'incident portent la richesse ; aucun logo inventé, image générée ou décoration de remplissage |
| Mouvement | Pas de boucle, skeleton statique ; transitions Material courtes, réduction de mouvement respectée |

**Marque non validée** : aucun logo officiel fourni/trouvé ; le nom texte WorldTech reste en place. La palette reste une proposition jusqu'au rapprochement avec les actifs du propriétaire. Les captures ne constituent pas une validation humaine de marque.

## Dépendances et licences

Ajouts exacts : `@fontsource/ibm-plex-sans@5.3.0` OFL-1.1, `@lucide/angular@1.71.0` ISC + notices Feather MIT, `@axe-core/playwright@4.14.0` et son moteur axe-core 4.14.0 MPL-2.0 en développement. Archives/intégrités et textes contrôlés avant installation ; détails/empreintes dans [DEPENDANCES](../DEPENDANCES.md).

Les licences runtime originales sont copiées depuis les paquets par `angular.json` dans `/licenses/…/LICENSE`. Le contrôleur vérifie versions et empreintes des nouveaux LICENSE et refuse PrimeIcons non scopé en plus des fournisseurs déjà exclus. Aucun abonnement, clé ou service axe PRO utilisé. Les sources des dépendances sont inchangées.

## Fichiers importants

- `app/src/styles.scss`, `src/styles/_tokens.scss`, `_foundation.scss` : mapping Material, police, tokens et styles communs.
- `app/src/app/layouts/application-shell.*`, `layouts/public-shell.ts`, `app.ts`, `app.routes.ts` : structures et séparation des layouts ; gardes/session non modifiées.
- `app/src/app/shared/ui/` et `shared/tickets/incident-form.*` : composants et formulaire sans HTTP.
- `app/src/app/features/dev/preview-shell.ts`, `dev.routes.development.ts`, `features/dev/tickets/` : navigation et références explicitement fictives.
- `app/package.json`, `package-lock.json`, `angular.json`, notices et scripts licences/production : dépendances exactes et redistribution.
- `app/e2e/design-system.spec.ts`, tests du formulaire/montants, suites fondation/production : recette et non-régression.
- `app/playwright.config.ts` utilise **4310**, pas le 4200 de l'utilisateur ; preuves FE01 non réécrites. Les règles du pack, sources tierces et backend ne sont pas modifiés pour masquer un contrôle.

## Commandes exécutées et résultats

Depuis `app/`, Node 26.13.0 / npm 11.20.0 de Program Files, PATH limité aux shells ; `NG_CLI_ANALYTICS=false` pour le CLI.

| Commande | Résultat observé |
|---|---|
| `git status --short --branch` | Pas de dépôt Git ; aucun init, commit, push ou workflow distant annoncé |
| `npm install` après ajout exact des trois dépendances | Exit 0, quatre paquets ajoutés dont axe-core ; 395 paquets audités |
| `npm ci --prefix <dossier temporaire opencode>` | Exit 0, **394 paquets installés**, copie isolée de package/lock/npmrc pour ne pas remplacer les modules utilisés par le serveur personnel sur 4200 |
| `npm ls --depth=0` | 25 dépendances directes aux versions retenues, aucun conflit indiqué |
| `npm run check:licenses` | Métadonnées du lock et empreintes nouvelles revues ; zéro PrimeUI/PrimeIcons, onze expressions de licences libres |
| `npm run lint` | Premier passage : paramètre inutilisé corrigé ; relance/chaîne complète réussies, aucune règle désactivée |
| `npm run typecheck` | Réussi sur application/templates/tests/outils ; erreur intermédiaire de surcharge de query corrigée avec l'API installée, sans cast/contournement |
| `npm run test` | **45 tests réussis**, six fichiers ; onze nouveaux cas formulaire/montants, sans backend |
| `npm run build` | Réussi ; avertissement CSS initial corrigé en séparant pilotage et liste, sans relever de budget |
| `npm run check` | **Exit 0 final** : licences/lint/types/45 unitaires/build/notices/exclusion production ; aucun budget relevé |
| `npm run test:e2e` | **31/31 réussis à la dernière relance**, contrôles JS/console inclus pour les 19 cas FE02. Passages préliminaires et correction de focus détaillés dans les preuves, pas de retry/skip |
| `npm run test:e2e:production`, puis `npx --no-install playwright test --config playwright.production.config.ts` sur dernier build | **9/9 réussis**, dont accès directs aux aperçus fictifs refusés en production |
| `npm audit --json` | Exit 1, **dix alertes modérées** minimatch dans la chaîne de lint, aucune élevée/critique dans ce relevé |
| `npm audit --omit=dev --json` | Exit 0, aucun avis renvoyé pour les dépendances production ; pas de certification de sécurité |
| Contrôles Python de préservation, liens, actifs/licences publiés et rapports JSON | Réussis : 162 fichiers de baseline conservés, 63 protégés/268 backend inchangés, 119 liens locaux sur 62 Markdown sans erreur, trois WOFF2 et deux notices runtime exactes, rapports navigateur/axe relus |

Défauts de recette corrigés sans retirer les assertions : accès clavier au contenu défilant du détail (tabindex/nom de région), libellés trop serrés sur mobile, chevauchement montant/devise ; synchronisation des tests avec le chargement lazy et le focus initial de fin d'ouverture Material. Le test vérifie aussi le défilement effectif au clavier. Une erreur console du drawer a nécessité de lire explicitement l'ElementRef du bouton Material ; les 19 tests FE02 contrôlent maintenant aussi les erreurs capturées par Angular. Le dernier contrôle et la suite complète sont réussis après correction.

## Preuves, limites et passage suivant

**Résultats finaux : 45 unitaires + 31 navigateur développement + 9 navigateur production**, rapports JSON relus sans échec/skip/flaky. **23 analyses axe, zéro violation détectée**, résultats `incomplete` conservés et qualifiés, pas de certificat WCAG. Douze paires de contraste effectivement mesurées (texte/statuts/sélection/contour/focus) passent leurs seuils. 24 captures synthétiques générées et rendus représentatifs examinés.

Build : 262,22 kB bruts initiaux / 70,13 kB estimés CLI, plus trois WOFF2 totalisant 71 024 octets ; pas une mesure de performance navigateur. [Résultats, captures et limites](../../verification/fe02/RESULTATS.md). Lock final SHA-256 : `2a1f9672e0fbea8e64ac5fce0f99c634abd4f31c92ef649b0428fa2494734f3e`.

Les tests sont locaux et synthétiques : aucun serveur Spring, compte, commande métier, login, CSRF ou MFA réellement intégré ; aucune preuve d'autorisation API inventée. Baseline 162 fichiers sans suppression, 63 fichiers protégés et 268 fichiers backend inchangés ; le serveur personnel sur 4200 n'a pas été arrêté. Git reste absent, aucune CI distante ou mise en production attestée.

Les contrôles automatisés ne certifient pas WCAG : revue humaine/lecteur d'écran, Firefox/WebKit, téléphones physiques et zoom natif du navigateur restent à exécuter. Le test dit « zoom » agrandit **le texte racine à 200 %** et vérifie le reflow à 320 px ; il ne commande pas le zoom de l'interface Chromium. Les éléments `incomplete` d'axe restent dans les rapports pour revue.

Pas de client OpenAPI généré dans ce lot, pas d'authentification anticipée ni de liste de tickets intégrée (FE06). Les contraintes du formulaire sont représentatives, à rapprocher des DTO/validations réels avant branchement ; une validation locale n'est pas une validation serveur. Les capacités/habilitations devront fournir les vrais menus, pas convertir les items fictifs en droits.

**Prochain prérequis : lancement explicite FE03**, contrat réel relu et qualification du générateur Angular 22/TypeScript 6/OpenAPI 3.1 ; backend/session/CSRF/MFA et identités de recette volontaires disponibles. Pas de continuation automatique.
