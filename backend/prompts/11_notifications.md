# Prompt 11 — Outbox, notifications et reprise des échecs

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Complète l'outbox introduite avec les premiers événements. Worker par lots bornés, verrouillage SKIP LOCKED/baux, retries/backoff bornés, état d'échec exploitable, métriques d'âge et reprise administrative. Plusieurs instances ne doivent pas traiter librement le même message en même temps.

Notifications internes dédupliquées par événement/destinataire/canal ; courriels modèles sans données privées inutiles. Le transport SMTP est au moins une fois : expliquer le risque de doublon après crash entre acceptation SMTP et acquittement DB. Aucune promesse exactement une fois. Pas de mail dans une transaction de mission.

Couvre invitation, décisions, sélection/confirmation, changement financier, annulation, rapport et SLA. Respecte le périmètre du destinataire au rendu, et ne mets pas de jetons dans les logs. Configuration dev vers Mailpit seulement. Les push Flutter sont séparés si explicitement dans le périmètre ; pas d'envoi réel requis par la recette.

Recette : rollback métier ne laissant pas d'événement, double worker, crash/reprise, erreur temporaire/permanente, destinataire suspendu ou devenu non habilité, absence de fuite dans payload/logs. Démontrer que la taille du lot et la file restent bornées.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
