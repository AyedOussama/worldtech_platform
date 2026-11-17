# Prompt 14 — Livraison vérifiable et restauration

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Finalise CI/CD sans déployer automatiquement hors demande. Maven verify avec tests d'intégration réels, architecture, contrat API, secret scan, analyse dépendances/images, SBOM et artefacts immuables. Vérifie les patches actuels, digests et dérogations datées. Ne pas marquer vert un test sauté parce que Docker manque.

Valide profil production : TLS/proxy de confiance, cookie/CSRF, révocation des sessions, Google OIDC, rotation refresh mobile, MFA admin, CORS explicite, gestion privée, comptes nominatifs, rôle runtime sans DDL, migration one-shot, secrets externes, conteneur non root et limites de ressources. Aucun port DB/gestion public, MFA de test, compte par défaut ou validation OAuth désactivée en production.

Écris runbooks incident, backup/PITR, restauration DB (identités/sessions incluses)+objets+clés MFA/configuration Google et rollback applicatif compatible. Réalise une restauration sur environnement isolé et mesure RPO/RTO avant de les déclarer satisfaits. Déploiement expand-contract ; pas de rollback de schéma destructif automatique.

Exige une revue humaine du module identité et un pentest indépendant sur préproduction ; corrige et reteste les constats critiques/élevés avant lancement. Relie les résultats aux exigences SEC-xx et à ASVS sans inventer de preuve. Recette finale bout en bout : invitation/validation → incident/proposition client → publication/montant admin → candidature/sélection/confirmation → intervention/pièces/rapport/attestation → revue/historique/statistiques. Inclure les scénarios négatifs de sécurité, financiers et concurrence. Livre bilan des preuves, risques restants et décisions de lancement ; ne déploie ni n'envoie de messages réels sans demande correspondante.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
