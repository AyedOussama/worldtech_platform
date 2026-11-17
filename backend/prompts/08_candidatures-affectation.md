# Prompt 08 — Sélection atomique et confirmation des conditions

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente candidature/retrait, sélection par ADMIN, réservation et confirmation/refus par le technicien sélectionné. Une candidature n'est pas une affectation. Recontrôle statut, éligibilité, fenêtre et version financière au moment de la commande, y compris si elle a changé depuis l'affichage.

Garantis une candidature logique par couple mission/technicien et au plus une affectation RESERVED/CONFIRMED par mission via contraintes SQL. Verrouille la mission avant les lignes dépendantes selon un ordre constant. Écris réservation et outbox dans la même transaction, sans appel externe. La confirmation sauvegarde la version et le snapshot des conditions acceptées. Expiration contrôlée après verrou avec Clock, y compris avant passage du job.

Ajoute idempotence par acteur/opération/clé + empreinte et stockage transactionnel du résultat, avec réautorisation au rejeu. Pour concurrence ou violation SQL attendue, ne continue pas une transaction PostgreSQL invalidée. Les modifications après accord suivent une proposition d'avenant réacceptée.

Recette : vraies transactions concurrentes de sélection, double candidature, confirmation vs expiration, suspension/repricing entre lecture et commande, même clé même/autre corps, replay après perte de droit. Vérifier le résultat final en DB et un seul événement métier attendu. Faire préciser désistement/réaffectation/travail déjà engagé avant d'implémenter ces règles.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
