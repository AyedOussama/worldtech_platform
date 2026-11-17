# Architecture Angular
Décision rapprochée du dossier réel le 10/11/2026 : aucune application existante, backend voisin à préserver. Voir [le compte rendu FE00](comptes-rendus/FE00_AUDIT_ET_DECISIONS.md) et [le plan FE01](PLAN_FE01.md).

## Implantation retenue en FE00
Projet unique `worldtech-web` créé en FE01 dans `worldtech-front/app/`, sans déplacer le pack en racine ni toucher à `../backend`. Les chemins `src/...` ci-dessous sont relatifs à `app/`. Angular 22.2.5, CLI/build 22.2.6, builder `@angular/build:application`, CSR et zoneless. Node 26.13.0 / npm 11.20.0 effectivement utilisés ; Angular Material/CDK 22.2.6 MIT remplace le candidat PrimeNG à la demande de l'utilisateur. [Versions et licences](DEPENDANCES.md), [décision outils gratuits](adr/ADR005_OUTILS_LIBRES.md). FE00 n'avait créé aucun fichier applicatif.

## Structure
Une application Angular standalone, rendue dans le navigateur. Routes /client et /admin, groupes lazy, accès publics limités aux parcours d'identité et d'invitation. Un espace /dev de catalogue peut exister uniquement dans les configurations de développement.

| Emplacement applicatif proposé | Responsabilité |
|---|---|
| src/app/core/auth | Session, état pré-MFA, utilisateur, capacités, gardes |
| src/app/core/http | Configuration, erreurs et corrélation |
| src/app/core/config | Configuration publique validée avant bootstrap |
| src/app/layouts | Navigation des espaces et structure de page |
| src/app/shared/ui | Contrôles et présentations sans appels métier |
| src/app/shared/formatting | Dates, montants, références |
| src/app/features/auth | Connexion, MFA, récupération |
| src/app/features/onboarding | Invitation et dossier client |
| src/app/features/client | Tickets, sites, équipements, rapports |
| src/app/features/admin | Qualification, missions, validations et revue |
| src/app/api/generated | Types et clients issus du contrat OpenAPI |
| src/styles | Tokens, thème et styles de fondation |

FE01 ne matérialise que les responsabilités utiles au socle ; les dossiers métier et classes inutilisées ne sont pas créés pour remplir cette liste. `api/generated/README.md` réserve l'emplacement sans inventer de client.

## Dépendances et état
Une page appelle une façade ou un service de cas d'usage frontend, qui utilise les clients API. Les composants de présentation reçoivent les données et émettent des intentions.
Ne pas créer systématiquement cinq couches pour chaque petit écran. Un adaptateur se justifie quand DTO et affichage divergent.
Les features exposent leurs routes et contrats publics ; éviter les imports dans les détails internes d'une autre feature.

| État | Emplacement |
|---|---|
| Filtres, tri, page partageables | Paramètres d'URL validés |
| Formulaire, erreurs et envoi | Formulaire et composant propriétaire |
| Session courante | Service partagé borné |
| Données de liste / fiche | Service de feature, cache éventuel explicite |
| Thème et densité | Préférences UI non sensibles |
| Données métier persistantes | API et base serveur |

Signals pour l'état consommé dans les vues, computed pour les dérivations, RxJS pour les flux annulables. Éviter les subscriptions imbriquées et les effets qui recopient les mêmes données entre plusieurs stores. Prévoir la destruction des abonnements et timers.

## Contrats de navigation
Une fiche possède une URL propre ; les onglets importants peuvent être représentés dans l'URL. Le retour à la liste retrouve filtres et sélection.
Le redémarrage navigateur restaure la session via le serveur avant de rendre des données privées.
Les routes inconnues ont un écran 404. Les chemins de retour après login sont internes et validés.
Une route technicien ne crée pas un troisième portail web ; elle oriente, si le produit le prévoit, vers l'application mobile par un lien configuré.

## Pourquoi une application au départ
Moins de duplication de thème, tests, génération API et identité. Les routes lazy réduisent le chargement initial ; elles ne protègent pas le code client comme un secret.
Une séparation en deux applications peut être décidée plus tard pour des contraintes de déploiement, de cadence ou de propriétaire distinctes. Cette évolution ne doit pas nécessiter de déplacer les règles métier du backend.

## Choix non retenus par défaut
Pas de microfrontends, store global complexe, monorepo Nx supplémentaire, SSR, GraphQL ni service worker de données privées sans besoin démontré. Le dépôt existant et une contrainte réelle peuvent justifier une décision différente, documentée dans un ADR.
