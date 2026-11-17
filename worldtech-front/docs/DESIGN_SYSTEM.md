# Design system WorldTech
Statut : direction WorldTech mise en œuvre et vérifiée localement en FE02, [preuves et limites](../verification/fe02/RESULTATS.md). Palette toujours proposée : aucun logo/actif officiel n'a été trouvé. Le nom texte est conservé, sans marque inventée. Angular Material/CDK MIT remplace PrimeNG conformément à la contrainte « outils gratuits ».

FE03 : pages d'identité alignées sur cette direction, sans refonte de marque. Le fichier `app/public/images/worldtech_logo.png` est présent et préservé à l'identique ; son statut de logo officiellement validé n'est pas établi dans le lot. Le mot-symbole texte reste utilisé, sans retoucher ce PNG ni lui inventer une déclinaison.

**Exception locale demandée le 12/11/2026 — Login V1** : `/connexion` suit désormais `Codex_Prompt_WorldTech_Angular_Login.md` et les actifs `WorldTech_Login_Assets_V1/` : logos fournis, composition 61/39, photo technicien/datacenter, carte réseau, badges et bouton bleu/cyan. Styles strictement locaux, IBM Plex existante et modes clair/sombre/système conservés ; aucune modification de palette/layout des autres pages. [Intégration, droits et limites](comptes-rendus/LOGIN_V1_INTEGRATION.md).

Retour utilisateur Login V1 : aucun sélecteur de thème sur la connexion, sans supprimer ceux des autres écrans. Cadre de connexion à hauteur d'écran et scène indépendante de la taille des messages ; densité responsive sans défilement aux dimensions normales de recette. Le reflow avec texte agrandi privilégie toujours l'accès aux actions, pas leur masquage.

## Direction
- Produit : organiser des interventions IT entre clients, administration et techniciens.
- Caractère : précis, calme, opérationnel.
- Univers : dossiers d'incident, sites, équipements, horaires et rapports.
- Moments visuels : progression d'une demande et créneaux utiles, avec la couleur réservée aux informations qui la justifient.
- Signature : un repère discret de « prochaine action » relié au statut et au responsable, sans inventer de nouvelle règle métier.

Conserver le logo officiel fourni. Ne pas remplacer une marque validée parce qu'une tendance visuelle paraît plus récente.

## Couleurs de départ
| Rôle | Clair | Sombre |
|---|---|---|
| Fond | #F8F9FB | #111317 |
| Surface | #FFFFFF | #191C22 |
| Surface sélectionnée | #EDF1F7 | #262C36 |
| Texte principal | #171A20 | #F3F4F6 |
| Texte secondaire | #505967 | #B4BBC6 |
| Métadonnées | #636D7D | #929BA9 |
| Séparation décorative | #E3E6EB | #303641 |
| Limite de contrôle | #7C8799 | #738198 |
| Accent | #2457D6 | #8CADFF |
| Texte sur accent | #FFFFFF | #111317 |
| Erreur | #B42318 | #FFB4AB |
| Succès | #146C43 | #83DBAE |
| Attention | #865500 | #F4CA76 |

Les statuts utilisent un libellé explicite, pas seulement une couleur. Les contours nécessaires pour reconnaître un contrôle ont un contraste différent d'une simple séparation décorative. Le script du pack vérifie des paires de tokens ; FE02 vérifie ensuite les composants réels, y compris survol, focus et état invalide.

## Typographie et géométrie
IBM Plex Sans intégrée en FE02 via Fontsource 5.3.0 (OFL-1.1). Trois fichiers Latin WOFF2 non modifiés, graisses 400/500/600 ; pas de CDN, WOFF de repli ou autres alphabets chargés. Le sous-ensemble comprend accents français, Œ/œ, euro et espaces typographiques. `font-display: swap` et fallback `system-ui, sans-serif` conservés. Licence/copyright distribués dans `/licenses/ibm-plex-sans/LICENSE`.
Taille racine 16 px. Corps 15–16, tableau 14, métadonnées 12–13, titres 24–28. Graisses 400/500/600 ; chiffres tabulaires. Pas de réduction du zoom utilisateur.

Échelle 4/8/12/16/20/24/32/40/48/64. Exceptions optiques ponctuelles à justifier ; aucun nombre arbitraire pour corriger un alignement systémique.
Navigation latérale environ 248 px, barre supérieure 56 px, marges de page 24–32 px sur ordinateur et 16 px sur téléphone.
Contrôles environ 40 px, cibles tactiles principales 44 px. Rayons : contrôle 8, panneau 12, petit repère 4. Ajuster les rayons imbriqués selon les espacements.

## Angular Material/CDK — choix gratuit retenu
Utiliser `@angular/material` et `@angular/cdk` 22.2.6 sous MIT, avec les tokens WorldTech via les API Sass publiques `mat.theme`, `mat.theme-overrides` et overrides de composants. Importer uniquement les composants utilisés. Ne pas ajouter PrimeNG, PrimeUIX, template ou outil de thème payant. Ce choix remplace les mentions de « preset PrimeNG » des prompts d'origine ; [ADR005](adr/ADR005_OUTILS_LIBRES.md).
Préférer les points d'extension documentés ; ne pas dépendre de la structure DOM privée ni généraliser ::ng-deep et !important.
Une seule bibliothèque principale de composants et une famille d'icônes applicatives. Lucide SVG 18 px, stroke cohérent, icônes décoratives ignorées par les lecteurs d'écran ; paquet ISC utilisé depuis FE02. Aucun chargement de police d'icônes externe.

FE02 utilise `@lucide/angular` 1.71.0 : imports ciblés, SVG 18 px / trait 1,5, sans registre de toute la collection. Les notices ISC et Feather MIT sont conservées intégralement dans `/licenses/lucide-angular/LICENSE`. Les icônes décoratives sont masquées ; les boutons sans texte ont un nom accessible.

## Trois modes de thème
Clair, Sombre et Système. Préférence locale non sensible wt-theme ; classe racine wt-dark et propriété color-scheme cohérentes avec les tokens `light-dark()` de Material. Pas de darkModeSelector PrimeUI.
Initialisation avant le premier affichage. Le service Angular prend ensuite en charge le changement utilisateur et l'évolution du thème système.
Les overlays rendus dans body, les contrôles natifs via color-scheme et les graphiques utilisent le même thème. Les captures de recette couvrent les deux modes et le changement à chaud.
Le modèle theme-init.js ne remplace pas ce service complet.

## Composants minimaux
Button, Field, ErrorSummary, StatusLabel, PageHeader, FilterBar, DataTable, EmptyState, ConfirmDialog, Drawer, DocumentList, Timeline et MoneyDisplay.
Ce sont des besoins de design, pas une obligation de créer un wrapper sur chaque composant Material. Ajouter une abstraction quand elle stabilise un comportement partagé réel.

### Implémentation FE02
- `app/src/styles/_tokens.scss` porte les couleurs WorldTech, typographie et géométrie ; `styles.scss` les mappe via les mixins publics Material. Les contrôles sont à 44–48 px, le formulaire dispose d'espace pour les erreurs ; les overlays héritent des mêmes variables de racine.
- `ApplicationShell` est une structure, pas un gestionnaire de capacités : navigation 248 px, contenu fluide, drawer modal sous 960 px. L'en-tête est à 64 px plutôt que 56 pour accueillir les cibles de 44 px et leur espacement. Les liens non implémentés sont non interactifs et marqués « À venir ».
- `PageHeader`, `StatusLabel`, `ErrorSummary`, `EmptyState`, `ConfirmDialog`, `Timeline`, `MoneyDisplay` et formulaire réactif `IncidentForm` partagent les comportements réellement utilisés. Material assure champs, menus, select et dialogue ; pas de wrappers systématiques.
- La liste de référence sépare pilotage URL et présentation (`ReferenceTicketList`). À moins de 1200 px, des cartes lisibles remplacent les colonnes ; à moins de 680 px, une seule colonne. L'objet, le site, la priorité, le statut, l'actualisation et la prochaine action restent accessibles.
- Pas de DocumentList/téléchargement, calendrier ou graphique sans besoin/contrat ; pas de skeleton animé. Les montants sont formatés en chaînes décimales exactes, avec zéro distinct de non renseigné.
- `/dev/admin/tickets` et `/dev/client/incidents` sont des aperçus isolés et explicitement fictifs, pas des routes privées ouvertes. La priorité affichée est illustrative ; le formulaire distingue bien priorité demandée et retenue. Les formulaires ne publient pas de faux succès serveur.

## Rédaction et interaction
Titres directs ; une action principale. Aides uniquement si elles évitent une erreur ou expliquent une condition.
Garder les labels de champs ; ne pas utiliser le placeholder comme seul nom.
Les actions destructives ou qui engagent une affectation annoncent leur conséquence. Les erreurs de champ sont liées au contrôle ; les erreurs globales n'effacent pas la saisie.
Menu : focus initial, Échap, retour au déclencheur et défilement correctement gérés.
Mouvement court et fonctionnel, sans boucles décoratives ; respecter prefers-reduced-motion.

## Passe de qualité
Suivre les neuf passes d'unslop dans l'ordre pour un écran de référence. Une passe de style ne modifie pas les droits, l'ordre métier ou les données affichées sans raison fonctionnelle.
Référence : unslop par Damir Sokolovsky (@heydamir), CC BY 4.0. Voir [UNSLOP.md](UNSLOP.md).
