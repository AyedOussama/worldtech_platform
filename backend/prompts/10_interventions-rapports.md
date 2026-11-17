# Prompt 10 — Intervention, attestation et synchronisation bornée

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente début d'intervention, actions, photos, brouillons, versions de rapport, attestation et revue ADMIN. Une version soumise/attestée est immuable ; toute correction crée une nouvelle version. L'attestation référence version/hash, signataire/qualité, consentement et heure serveur. Ne présente pas une image de signature comme une identité vérifiée ou une signature qualifiée.

Génère le PDF en asynchrone depuis un snapshot contrôlé ; vérifie le choix de bibliothèque/licence et empêche les accès réseau depuis du contenu HTML libre. L'absence/refus de signataire suit un chemin explicite avec revue administrative. Clôturer une visite ne résout pas forcément le ticket.

Expose un manifeste borné des missions affectées et versions. Les commandes de brouillon/upload/restitution hors ligne ont clé d'idempotence et version attendue. Sélection/confirmation restent en ligne. Après annulation/réaffectation, conflit explicite ; aucun écrasement automatique ni promesse de révocation de fichiers déjà copiés. Pas de pseudo-delta updatedSince sans traitement des suppressions/pertes de droit.

Recette : rapport modifié après attestation interdit, nouvelle version conservant l'ancienne preuve, signataire non habilité, fichiers encore en quarantaine, crash/replay, brouillon périmé, affectation perdue, PDF identique au snapshot. Prévoir tests de visibilité publique/interne.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
