# Prompt 07 — Publication des missions et montants administrateur

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente les missions rattachées à un site et éventuellement à un ticket/contrat. ADMIN choisit compétences, fenêtre, résumé public, position publique approximative et conditions financières publiées. La proposition client n'est jamais écrasée. Le technicien reçoit les seuls montants publiés.

Crée les révisions financières immuables ; publication valide complétude, statut du client, fenêtre et montants. Un changement de prix publié crée une révision et un événement ; les confirmations futures porteront sur la version courante. Pour un accord confirmé, prépare un avenant plutôt qu'une mutation silencieuse.

Recherche géographique PostGIS geography(Point,4326), ordre longitude/latitude, GiST, ST_DWithin en mètres, rayon/résultats bornés, filtres et pagination stable. La position privée et les contacts ne sortent jamais dans découverte ; distance calculée sur le point public pour éviter les recoupements. Un technicien en attente/suspendu ne découvre rien.

Recette : cas Paris/Lyon et limites de rayon, coordonnées inversées/invalide, filtrage de compétences/dates/états, absence de champs privés et de texte libre confidentiel. Tester EXPLAIN sur jeu réaliste et éviter N+1. La distance ne devient pas une durée routière inventée.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
