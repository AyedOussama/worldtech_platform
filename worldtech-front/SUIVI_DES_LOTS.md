# Suivi des lots frontend
État au 10/11/2026 : **FE00–FE02 vérifiés ; FE03 identité locale livrée et vérifiée contre Spring réel, avec réserve Google activé**, application dans `app/`. Outils gratuits uniquement : Material/CDK MIT, IBM Plex OFL, Lucide ISC/MIT et génération MIT, pas de PrimeUI. Onze alertes modérées d'outillage et limites d'accessibilité documentées ; aucune intégration métier ou mise en production revendiquée. FE04 non commencé.

| Lot | Développement | Intégration Spring Boot | Vérification visuelle | Preuves / compte rendu |
|---|---|---|---|---|
| FE00 | Vérifié — audit documentaire uniquement | Non exécutée — contrat réel identifié, rapprochement statique seulement | Non applicable — aucun écran | [Compte rendu](docs/comptes-rendus/FE00_AUDIT_ET_DECISIONS.md), [contrôles](verification/FE00_COMMANDES_ET_CONTROLES.md) |
| FE01 | Vérifié localement — build/lint/types, 34 unitaires, 19 navigateur ; alertes outillage suivies | À préparer — aucune session/API métier intégrée | Vérifié — fondations clair/sombre, ordinateur/téléphone et clavier ; pas FE02 | [Compte rendu](docs/comptes-rendus/FE01_SOCLE_ANGULAR.md), [preuves](verification/fe01/RESULTATS.md) |
| FE02 | Vérifié localement — 45 unitaires, 31 navigateur dev, 9 production, build/lint/types/licences | Sans objet — fixtures explicites, aucun HTTP métier, identité fermée inchangée | Vérifié localement — clair/sombre, desktop/mobile, clavier, 23 analyses axe ; marque et conformité complète non validées | [Compte rendu](docs/comptes-rendus/FE02_DESIGN_SYSTEM_ET_SHELLS.md), [preuves](verification/fe02/RESULTATS.md) |
| FE03 | Vérifié — check, 77 unitaires, 57 navigateur dev, 9 production | Vérifiée — 5 scénarios Spring/PostGIS isolés ; Google activé/SMTP/HTTPS non exécutés | Vérifié localement — 28 rendus identité clair/sombre desktop/mobile, clavier et 51 scans axe avec non-régressions ; conformité complète non certifiée | [Compte rendu](docs/comptes-rendus/FE03_IDENTITE_WEB.md), [preuves](verification/fe03/RESULTATS.md) |
| FE04 | À faire | À faire | À faire | |
| FE05 | À faire | À faire | À faire | |
| FE06 | À faire | À faire | À faire | |
| FE07 | À faire | À faire | À faire | |
| FE08 | À faire | À faire | À faire | |
| FE09 | À faire | À faire | À faire | |
| FE10 | À faire | À faire | À faire | |
| FE11 | À faire | À faire | À faire | |
| FE12 | À faire | À faire | À faire | |
| FE13 | À faire | À faire | À faire | |
| FE14 | À faire | À faire | À faire | |

Statuts conseillés : À faire, En cours, Vérifié, Bloqué sur prérequis, Non applicable avec justification. Identifier explicitement toute démonstration simulée.

## Bilan FE00 — 10/11/2026
- Révision : aucun dépôt Git local (frontend et backend voisins), aucun commit/remote attesté ; état initial/final contrôlé.
- Livré : inventaire, matrice versions/licences, quatre ADR rapprochés, relevé OpenAPI réel et [plan FE01](docs/PLAN_FE01.md).
- Chemin retenu : application `worldtech-web` dans `app/`, pack en racine conservé, backend `../backend` inchangé.
- Contrôles : pack initial PASS, 9 cas de l'initialiseur de référence réussis, registre/officiels accessibles, neuf licences UI d'archives exactes contrôlées ; OpenAPI 117 chemins / 133 opérations / 83 schémas rapproché statiquement.
- Limites : pas de build, installation npm, navigateur, simulation de parcours ou intégration réelle. Manifeste historique conservé, contrôle strict final en échec attendu pour les adaptations documentaires listées, sans masquer l'écart.
- Prérequis FE01 : Node 24.22.2 compatible (local 24.13.0 insuffisant), npm cible 11.19.1. Licence PrimeUI de l'organisation à valider avant import UI (FE02 au plus tard) ; fondations sans PrimeUI possibles. Actifs de marque et identités de recette attendus pour leurs lots.
- Passage suivant : uniquement sur lancement explicite de FE01 ; aucun lot ultérieur démarré.

## Compte rendu d'un lot
### Bilan FE01 — 10/11/2026
- Résultat : socle standalone/zoneless dans `app/`, configuration publique validée, routes lazy fermées, erreurs HTTP réelles, catalogue développement exclu de production, thème et clavier.
- Contrainte « outils gratuits » appliquée : Material/CDK 22.2.6 MIT, aucun PrimeUI ; décision et AGENTS actualisés. Node 26.13.0 / npm 11.20.0 de l'installation utilisateur, sans changement global de PATH.
- Contrôles : `npm ci`, `npm ls`, `npm run check`, 34 unitaires, 12 tests navigateur dev et 7 production réussis ; quatre captures synthétiques examinées. Liens et préservation du pack/backend contrôlés séparément.
- Limites : dix alertes npm modérées dans les outils de lint ; audit production sans avis renvoyé. Aucun login, CSRF/MFA, compte, API métier, Spring Boot, CI distante ou déploiement validés. Git absent.
- Prochain prérequis : lancement explicite FE02 et actifs de marque pour validation visuelle ; Material remplace les mentions PrimeNG du prompt. Contrat/backend/identités de recette réels requis à FE03, pas de faux accès d'attente.

### Bilan FE02 — 10/11/2026
- Résultat : tokens WorldTech/Material, typographie et icônes libres, structures Client/Admin responsive, composants utiles et formulaires/liste synthétiques de référence ; `/dev/admin/tickets` et `/dev/client/incidents` uniquement en développement.
- Contrôles : installation exacte et `npm ci` isolé, `npm run check`, 45 unitaires, 31 tests navigateur dev et 9 production réussis ; 23 analyses axe sans violation détectée, résultats incomplets conservés, douze paires de contraste mesurées, 24 captures générées et rendus représentatifs examinés.
- Préservation : aucune suppression parmi 162 fichiers de baseline ; 63 fichiers protégés dont preuves FE01 et 268 fichiers backend identiques. Aucun arrêt du serveur personnel 4200 ; tests sur 4310/4173. Pas de Git/CI distante.
- Limites : fixtures pas API, contrôles locaux pas autorisations serveur ; marque non validée, résultats axe incomplets/revue humaine et zoom natif restants. Dix alertes npm modérées d'outillage, aucun avis production renvoyé ; pas de correctif forcé.
- Prochain prérequis : lancement explicite FE03, contrat réel/générateur qualifié et identité de recette session/CSRF/MFA ; aucune authentification ou intégration métier commencée dans FE02.

### Bilan FE03 — 10/11/2026
- Résultat : session cookie HttpOnly/CSRF mémoire, connexion/récupération/reset/email, MFA complet exposé (enrôlement, récupération, preuves récentes, renouvellement/remplacement), états fermés et invalidation des requêtes privées/inter-onglets ; types générés depuis le contrat réel. Aucun métier FE04 commencé.
- Contrôles : `npm run check` réussi avec 77 unitaires ; 57/57 navigateur dev, 5/5 Spring réel isolé, 9/9 production. 51 scans axe sans violation détectée, 58 occurrences incomplètes conservées, 28 captures identité + 24 non-régressions synthétiques ; revue représentative clair/sombre/ordinateur/téléphone.
- Intégration réelle : CSRF refusé absent/altéré et renouvelé, cookie HttpOnly, admin refusé avant MFA puis autorisé, codes uniques, réauthentification/renouvellement/remplacement, limite MFA réelle 429, reset avec révocation et email sans contournement de l'approbation. Aucun accès à la base existante, aucun patch backend ; serveurs de recette séparés de 4200/5432.
- Limites : Google activé non livré (callback web JSON sans retour Angular qualifié), SMTP et HTTPS de production non exécutés ; contrôles de suspension/401 simultanés avec doubles/unitaires, revue humaine d'accessibilité restante. Onze alertes modérées d'outillage, aucune élevée/critique dans le graphe retenu, aucun avis npm runtime renvoyé. Pas de Git/CI distante.
- Préservation : deux scaffolds obsolètes remplacés, 114 fichiers protégés sans différence, preuves antérieures et fichier logo utilisateur intacts. Le générateur types-only MIT est verrouillé ; pas de bibliothèque payante ni de contournement de dépendance.
- Prochain prérequis : lancement explicite FE04, revue des contrats BE04 d'invitation/dossier/validation et environnement de recette adapté. Google reste désactivé jusqu'à validation de son flux ; pas de passage automatique au lot suivant.

### Modèle pour les lots suivants
- Lot et date :
- Révision du dépôt :
- Résultat livré :
- Fichiers importants :
- Commandes réellement exécutées :
- Résultats et preuves :
- Intégration réelle / simulation :
- Limites ou décisions encore nécessaires :
- Prochain prérequis :
