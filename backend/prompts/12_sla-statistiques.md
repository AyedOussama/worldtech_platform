# Prompt 12 — Délais, tableaux de bord et tâches récurrentes

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente calculs de prise en compte/intervention/résolution selon contrats et calendriers versionnés. Clock injecté, jours non ouvrés, fuseau site et changements d'heure. Aucun arrêt automatique du compteur sur un statut si le contrat ne le prévoit pas. Événements et pauses sont auditables.

Statistiques ADMIN et historiques CLIENT : définitions écrites des indicateurs, périodes et dénominateurs, filtre de compte/site avant agrégation, bornes de requête et export asynchrone si nécessaire. Ne divulgue pas les données d'autres clients par un total ou une série.

Tâches de purge/expiration/rappel coordonnées en multi-instance, idempotentes et par lots. Applique une politique de conservation explicitement validée ; ne fixe pas un délai légal imaginaire. Une purge doit préserver les références et preuves encore nécessaires selon la décision produit.

Recette : calendrier et DST, contrat expiré, pause autorisée/interdite, réouverture et annulation, agrégats croisés entre comptes, double exécution job et reprise après erreur. Consigner les arbitrages non résolus sans les transformer en comportement caché.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
