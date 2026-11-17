# Contrats API et gestion des données

## Source de vérité
API web /api/web/v1, session et CSRF. API mobile /api/mobile/v1, Bearer opaque : ne pas l'utiliser comme substitut pour Angular.
Les chemins du cahier backend sont indicatifs. Au lot concerné, extraire l'OpenAPI réel, documenter operationId, requête, réponse, erreurs, droits, version et idempotence.
Ne pas créer un faux swagger complet pour faire croire qu'une intégration existe. Un contrat proposé reste identifié comme proposition.

**FE00, 10/11/2026 :** contrat réel trouvé dans `../backend/openapi/maintenance-api.json`, OpenAPI 3.1.0 / API 0.6.0, 117 chemins et 133 opérations. Voir [le relevé et ses limites](CONTRAT_BACKEND_RELEVE_FE00.md). L'inventaire statique correspond aux sources, sans preuve d'intégration HTTP frontend. 132 opérations sont dépourvues d'operationId ; qualifier la génération et les noms stables avant FE03, sans fabriquer un contrat ni modifier le backend dans l'audit.

## Client généré
FE03 retient **`@hey-api/openapi-ts@0.97.0` MIT**, en développement uniquement, pour les types TypeScript du contrat réel OpenAPI 3.1 / API 0.6.0. Pas de runtime fetch ou de Bearer mobile généré : `app/src/app/core/auth/identity-api.ts` est le transport Angular typé, séparé des sources générées.

Depuis `app/`, `npm run api:generate` régénère `src/app/api/generated/` ; `npm run api:verify` compare les sorties et la provenance sans les modifier. Le snapshot `api-contracts/maintenance-api.json` est une copie exacte du backend, SHA-256 `b5f122cc7ac464b544b1ee0f98b4e8ae379bf6521af2156e8b0ab6f73db1f1c5`. En présence du backend voisin, une différence avec le snapshot est refusée ; sinon le snapshot permet la CI frontend isolée. Les opérations sans operationId ont des noms déterministes méthode/chemin, sans réécriture du contrat.

Ne pas éditer les fichiers générés. Les DTO readOnly/writeOnly sont distincts : les entrées `Login` et `Reset` incluent les mots de passe, contrairement aux variantes de sortie. Null et chaînes décimales suivent le contrat. **Le mapping des int64 métier en `number` n'est pas qualifié pour les versions longues** ; l'identité n'en utilise pas. Qualifier ce point avant les lots métier, conserver les ETag opaques et ne pas introduire de calcul financier flottant.

Les types générés sont compilés strictement, mais exclus du lint de style manuel. `check` exécute aussi `api:verify`. La version et l'empreinte de licence du générateur sont verrouillées ; voir [DEPENDANCES](DEPENDANCES.md).

## Erreurs
| HTTP | Traitement |
|---|---|
| 400 / 422 si contrat | Mapper les erreurs de champ documentées |
| 401 | Réévaluer la session, une navigation coordonnée |
| 403 | Refus explicite ; code actuel ACCESS_DENIED non discriminant entre CSRF et autorisation, pas de rejeu automatique |
| 404 | Introuvable ou masqué selon politique du serveur |
| 409 | Conflit métier, état actuel et action de reprise |
| 412 | Version périmée ; préserver la saisie |
| 413 | Taille dépassée, limite et reprise adaptée |
| 429 | Limite atteinte, Retry-After |
| 5xx / réseau | Erreur récupérable ; résultat d'une mutation potentiellement ambigu |

ProblemDetail application/problem+json : utiliser code stable et fieldErrors réels, sans analyser une phrase. Le traceId peut aider au support ; il ne remplace pas un message utile. Une erreur serveur ne doit pas injecter du HTML dans une notification.

## Pagination et recherche
Pagination serveur, taille bornée, filtres et tri sur champs autorisés. Taille de départ 20, maximum selon contrat.
Annuler la recherche dépassée ; une réponse lente d'une recherche précédente ne remplace pas la plus récente.
Les compteurs et exports partagent le périmètre d'autorisation de la liste.
Lors d'un changement de compte ou logout, invalider les données et requêtes privées en cours.

## Montants
Les montants JSON sont des chaînes décimales, par exemple "180.00". Ne pas effectuer de calcul financier avec parseFloat.
Conserver devise, base de calcul et null distinct de zéro. Formater pour l'utilisateur, sans déduire HT/TTC ni prix facturé.
Exemple fictif de recette : proposition client 150.00 + 20.00, publication admin 180.00 + 35.00. Les quatre valeurs restent distinctes et le technicien confirme une révision donnée.

## Dates et engagements
Horodatages techniques UTC ; créneaux affichés dans le fuseau IANA du site.
Traiter les changements d'heure et les intervalles [début, fin). L'horloge navigateur ne décide pas de l'expiration d'une affectation.
Le backend calcule SLA, pauses et couverture ; afficher ses résultats et leur contexte.

## Concurrence et idempotence
Récupérer ETag/version et envoyer If-Match ou le mécanisme réellement prévu.
Pour une opération idempotente, réutiliser la même clé et le même contenu lors d'une reprise du même essai. Une nouvelle intention utilisateur a sa propre clé.
Désactiver le double clic améliore l'UX ; seule la garantie serveur empêche le double effet.
Pas de retry générique sur POST. Après timeout, interroger l'état ou rejouer via le mécanisme d'idempotence convenu.

## Documents et jobs
Upload intent → transfert autorisé → finalisation → quarantaine → CLEAN ou REJECTED.
Un transfert réussi ne rend pas une pièce immédiatement téléchargeable. L'application suit l'état réel.
Réponse 202 : fournir un suivi de job ou état ressource ; poll borné, annulation locale du suivi possible, traitement serveur indépendant.
Téléchargements et URLs temporaires sont renouvelés après contrôle d'autorisation, jamais traités comme identifiants publics permanents.

## Actualisation
Polling borné au départ, visible uniquement et sans chevauchement. Rafraîchir après mutation confirmée et au retour de fenêtre selon besoin.
SSE seulement avec un contrat et une justification mesurée ; pas de websocket ajouté pour chaque liste.
