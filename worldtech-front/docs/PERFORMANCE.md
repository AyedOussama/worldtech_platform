# Performance et stabilité visuelle

## Budgets de départ
| Mesure | Cible proposée | Mesure |
|---|---|---|
| LCP | <= 2,5 s | Laboratoire puis p75 réel par appareil |
| INP | <= 200 ms | Interactions réelles ; pas déduit du score Lighthouse |
| CLS | <= 0,1 | Chargement, pièces et actualisation |
| JS initial | <= 300 Ko compressés | Connexion et structure, périmètre documenté |
| Pagination | 20 à 50 lignes | API bornée, taille max du contrat |
| Recherche | Environ 250 ms de temporisation si adaptée | Réponse la plus récente, annulation |
| Polling | Point de départ 15–30 s si utile | Arrêt onglet caché, sans chevauchement |

Le budget JS est un objectif WorldTech, pas un seuil officiel Angular. Préciser gzip/Brotli, chunks, CSS/fonts exclus ou inclus et route mesurée.
Les valeurs réelles se trouvent dans le bilan FE13 après exécution. Le temps API et le temps de rendu sont mesurés séparément.

## Mesurer
Build de production, navigateur et version, matériel, réseau/throttling, jeu de données et nombre d'essais documentés.
Choisir connexion, liste de tickets, fiche de ticket, formulaire de déclaration et liste de missions. Examiner tâches longues, waterfall et changements de layout.
Les routes SPA nécessitent une mesure de navigation applicative en plus du chargement initial. Un bon LCP d'entrée ne prouve pas une bonne ouverture de fiche.

## Techniques
- Lazy routes, imports individuels et analyse de la taille des dépendances.
- Signals et OnPush compatibles ; éviter la recomputation coûteuse dans les templates.
- Suivre les éléments par identifiant, limiter les nœuds et préserver la sélection.
- Pagination avant chargement massif ; virtualisation seulement après besoin et vérification d'accessibilité.
- Images dimensionnées, vignettes et chargement différé hors écran.
- Annulation de recherche dépassée, teardown des subscriptions et polling.
- Cache éventuel par utilisateur/périmètre, durée, capacité et invalidation explicites.
- Ressources lourdes hors bundle initial ; aucun agenda/carte chargé sur la connexion.
- Navigation utilisable pendant l'actualisation d'une section.

## Méthode d'optimisation
Mesurer un problème, en identifier la cause, modifier le minimum utile et comparer dans les mêmes conditions.
Ne pas ajouter de cache ou réduire les textes/contrastes pour améliorer un score artificiel.
Une régression tolérée temporairement a un propriétaire, une cause et une échéance.

## Sources
Core Web Vitals : https://web.dev/articles/vitals
Angular zoneless : https://angular.dev/guide/zoneless

