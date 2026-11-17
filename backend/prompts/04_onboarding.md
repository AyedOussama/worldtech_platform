# Prompt 04 — Invitation client et validation des profils

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente l'invitation client, son formulaire et sa soumission, ainsi que l'inscription technicien. Seul ADMIN invite/valide/refuse/demande un complément. Les états en attente ne permettent ni ticket ni candidature. Rattache le dossier à user_id interne, que l'accès ait été créé par email ou Google. Les informations professionnelles et documents restent dans notre application. Aucun email identique ne fusionne deux identités sans preuve et confirmation.

Jeton d'invitation aléatoire fort, hash seulement en base, destinataire lié, expiration configurable, usage atomique unique ; renvoi révoquant les liens précédents. Les réponses et logs ne révèlent pas le jeton. Ajoute un mécanisme minimal outbox transactionnel pour préparer le courriel local, complété au lot 11. Les décisions ont auteur, motif et historique.

Crée les migrations, DTO selon acteur, endpoints et erreurs stables. Utilise une liste blanche de champs ; aucune inscription ne peut se donner ADMIN ou APPROVED. Rate limiting sur inscription/renvoi selon la stratégie retenue.

Recette : invitation utilisée deux fois, expirée, remplacée, concurrente, mauvaise identité ; profil incomplet, complément puis resoumission, décision par non-admin, candidat en attente/suspendu. Courriels uniquement dans Mailpit en développement. Aucun envoi à une personne réelle requis par ce lot.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
