# Cahier des charges technique — Frontend Angular WorldTech
Version 1.0.0 · 10 novembre 2026 · Dossier de conception et de réalisation progressive

## 1. Résultat attendu
Construire un portail client et un backoffice administrateur pour organiser la maintenance informatique. L'interface permet de déclarer, qualifier, organiser et suivre les interventions, puis de consulter et contrôler les rapports. Flutter reste le canal de travail des techniciens.

La qualité attendue repose sur des parcours compréhensibles, une structure visuelle cohérente, la navigation clavier, des performances mesurées et une base de code que l'équipe peut maintenir. Linear et Apple Notes sont des références de sobriété et de hiérarchie ; leurs marques, interfaces et usages ne sont pas copiés.

## 2. Périmètre
| Espace | Inclus |
|---|---|
| Accès | Connexion locale, récupération, email, Google conditionnel, MFA administrateur |
| Client | Invitation, dossier, incidents, propositions initiales, sites, équipements, interventions, documents, rapports |
| Administrateur | Validation clients/techniciens, parc, contrats, tickets, missions, candidatures, sélection, revue des rapports, statistiques |
| Commun | Notifications, profil, préférences de thème, état de session, erreurs et autorisations |
Le mobile client, la paie, le paiement, la facturation comptable, la marge supposée, le GPS continu et le dispatch IA sont hors première version.

## 3. Référentiel métier
Le cahier backend Spring Boot révision 1.1 définit les invariants. Voir [ALIGNEMENT_BACKEND.md](docs/ALIGNEMENT_BACKEND.md).
- Trois rôles : ADMIN, CLIENT, TECHNICIAN ; l'admin organise et affecte.
- Client invité, dossier soumis, validation administrative avant les opérations métier.
- Un ticket peut avoir plusieurs missions ; une mission préventive peut ne pas avoir de ticket.
- La sélection réserve une place ; la confirmation du technicien et les délais sont distincts.
- La proposition client ne devient pas automatiquement le montant publié ni le prix facturé.
- Les conditions acceptées et les rapports attestés sont versionnés ; une correction ne les écrase pas.
- Les autorisations sont vérifiées pour les lectures, pièces, recherches, compteurs et exports.

## 4. Architecture
Une application Angular standalone avec routes /client et /admin, chargées à la demande. Les composants communs, la session et les conventions HTTP sont partagés ; les pages et leurs droits sont séparés.
[ARCHITECTURE.md](docs/ARCHITECTURE.md) fixe les dépendances, responsabilités et conventions d'état.
Le rendu navigateur est la référence de départ pour les espaces authentifiés. Aucun besoin SEO ne justifie actuellement d'ajouter SSR à ces espaces.

## 5. Dépendances
Candidature de départ : Angular 22, PrimeNG 22, Node 24 compatible, TypeScript dans la plage officielle, RxJS, formulaires réactifs typés, Lucide, Vitest et Playwright.
FE00 confirme l'existence, les versions exactes, peerDependencies, licences et l'adéquation au dépôt existant. Il produit le verrou effectif à FE01. [DEPENDANCES.md](docs/DEPENDANCES.md)
Ne pas livrer un package.json faussement verrouillé depuis une simple liste documentaire. Une version déjà installée et supportée peut être conservée lorsque la migration n'apporte pas de bénéfice suffisant.

## 6. Direction visuelle
Caractère : précis, calme, opérationnel. Surfaces neutres, un accent mesuré, textes courts, grilles alignées et actions explicites.
La personnalité vient des références d'incident, de la chronologie, des créneaux et des prochaines actions. Aucune illustration décorative n'est nécessaire au travail quotidien.
Palette, police IBM Plex Sans proposée, proportions, espacements, contrastes et thèmes sont définis dans [DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md). Les actifs validés de WorldTech priment après rapprochement écrit.

## 7. Règles de rédaction et de disposition
Chaque texte identifie, aide à décider, permet d'agir ou explique un résultat.
Supprimer les slogans, sous-titres répétitifs et indications évidentes. Garder un libellé visible par champ, les unités, les différences financières, les erreurs et les contraintes qui ont une conséquence.
Préférer des titres « Tickets », « Missions », « Rapports » et des actions « Publier », « Affecter », « Déclarer un incident ».
Une information masquée visuellement ne constitue pas une protection des données.

## 8. Navigation et écrans
L'administration privilégie les éléments qui attendent une action : tickets à qualifier, dossiers à examiner, rapports en attente.
Le portail client privilégie ses incidents et leur progression. Chaque fiche possède un lien direct utilisable et le retour à la liste restaure le contexte.
L'inventaire détaillé et les droits par écran figurent dans [UX_ECRANS.md](docs/UX_ECRANS.md).
Sur téléphone, les tableaux deviennent des lignes lisibles ou des vues adaptées ; le formulaire de déclaration et la consultation restent utilisables sans réduire le texte à une taille illisible.

## 9. États d'interface
Tout écran traite chargement, vide, absence de résultat, erreur réseau, refus, session expirée et actualisation.
Toute mutation traite envoi, réussite confirmée, erreur, résultat réseau ambigu et conflit de version lorsque pertinent.
Un timeout ne prouve pas l'échec d'une commande. La saisie reste disponible pendant une erreur récupérable, avec une reprise explicite.
Les états de page et de composant sont décrits avant leur implémentation.

## 10. Identité et intégration
Session serveur par cookie HttpOnly pour le web ; CSRF sur les mutations, connexion et déconnexion comprises. API mobile Bearer distincte et hors authentification Angular.
MFA ADMIN après connexion locale ou Google ; aucune ouverture du backoffice avant sa vérification.
[AUTHENTIFICATION.md](docs/AUTHENTIFICATION.md) décrit les parcours et [API_CONTRATS.md](docs/API_CONTRATS.md) les erreurs, montants, dates, versions, uploads et traitements différés.
L'origine commune derrière un reverse proxy est privilégiée. Les chemins sont relatifs et le contrat de CSRF est testé réellement.

## 11. Sécurité et confidentialité
Le backend impose tous les périmètres ; les gardes servent à la navigation. Ni les données d'un autre client, ni les notes internes, ni les fichiers non autorisés ne sont transférés pour être cachés ensuite.
Les données de formulaire et secrets ne vont pas dans la télémétrie. Les téléchargements, journaux et captures suivent les mêmes contraintes de confidentialité.
[SECURITE.md](docs/SECURITE.md) propose une matrice de contrôles et de preuves.

## 12. Performance
Budgets initiaux proposés : LCP <= 2,5 s, INP <= 200 ms, CLS <= 0,1, JavaScript initial <= 300 Ko compressés pour connexion et structure de base.
Ce sont des objectifs, pas des mesures déjà acquises. Mesurer sur version de production, appareil/réseau documentés, puis données réelles lorsque disponibles.
Pagination serveur, ressources lourdes différées, annulation des recherches dépassées, rendu stable, cache borné et invalidation explicite.
[PERFORMANCE.md](docs/PERFORMANCE.md)

## 13. Accessibilité
Objectif de conception WCAG 2.2 AA pour les parcours livrés, avec tests automatisés et vérification manuelle. Texte courant >= 4,5:1 ; contrôles et focus perceptibles ; zoom, clavier, annonces d'erreurs et alternatives aux graphiques.
Les cibles tactiles principales visent 44 px. Cette cible produit ne doit pas être présentée comme le seuil universel WCAG AA.
Clair, sombre et système couvrent aussi les portails de menus, dialogues, notifications et sélecteurs.

## 14. Expérience développeur et agent
Organisation par fonctionnalité ; types stricts ; client OpenAPI régénérable ; composants documentés avec états utiles ; scripts cohérents ; conventions d'erreur et permissions explicites.
Un lot à la fois. L'agent inspecte, implémente, vérifie et rapporte les preuves. Le catalogue de composants est réservé au développement ; il n'expose pas de données ou outils internes en production.
[DEVELOPPEMENT.md](docs/DEVELOPPEMENT.md)

## 15. Validation
Tests unitaires ciblés, composants et navigateur ; scénarios négatifs et concurrents introduits au lot concerné.
Les parcours simulés, intégrés et visuellement vérifiés sont suivis séparément. Le pack lui-même est contrôlé par scripts/verify_pack.py ; aucun de ces contrôles ne constitue un test Angular.
[TESTS_RECETTE.md](docs/TESTS_RECETTE.md)

## 16. Exploitation
Build statique reproductible, configuration publique validée, proxy même origine, fichiers d'entrée non périmés, ressources fingerprintées et cache adaptées, gestion des liens directs, déploiement et retour à la version applicative précédente.
Le frontend ne contient ni SMTP, ni clé privée, ni accès direct PostgreSQL. [DEPLOIEMENT.md](docs/DEPLOIEMENT.md)

## 17. Ordre des lots
FE00 inspection ; FE01 socle ; FE02 design ; FE03 identité ; FE04 onboarding ; FE05 parc ; FE06 tickets/contrats ; FE07 missions ; FE08 affectations ; FE09 pièces ; FE10 rapports ; FE11 notifications ; FE12 indicateurs ; FE13 performance ; FE14 livraison.
[ROADMAP.md](ROADMAP.md) définit les dépendances. Les contrôles transverses commencent dès FE01.

## 18. Définition de terminé
Contrat documenté, comportements nominaux et négatifs utiles vérifiés, absence de fuite connue dans le périmètre testé, rendu contrôlé, code lisible, bilan exact, suivi à jour.
Une dépendance absente est un point restant ouvert, pas un succès inventé. Une limitation de sécurité ou d'intégration ne se transforme pas en simple note cachée dans la console.

## 19. Arbitrages
Les décisions commerciales, délais, pièces exigées, signataires et intégrations sont suivis dans [ARBITRAGES.md](docs/ARBITRAGES.md).
Les choix techniques réversibles sont résolus pendant le lot autorisé ; une décision manquante ne bloque pas les travaux indépendants.

## 20. Sources et références
Sources officielles, date de revue et limites dans [SOURCES.md](docs/SOURCES.md).
Référence design explicitement demandée : unslop par Damir Sokolovsky (@heydamir), CC BY 4.0, dépôt inclus et attribution conservée.

