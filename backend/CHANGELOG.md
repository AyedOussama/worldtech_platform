# Historique des révisions

## Lot 06 — 02 novembre 2026

- Contrats ADMIN par révisions immuables, couverture de sites entiers/équipements sélectionnés et validité [début,fin) ; projection CLIENT pour un seul site, sans notes internes/autres sites.
- Calendriers 24/7/hebdomadaires et exceptions datées explicites, objectifs SLA sans défaut ; secondes de service et changements d'heure testés, aucun compteur universellement suspendu par WAITING_CLIENT.
- Tickets par site autorisé, équipement facultatif, priorité demandée/retenue distinctes, déclaration hors contrat et forfaits CLIENT facultatifs HT/EUR. Propositions immuables avec auteurs, BigDecimal/numeric exact et montants JSON en chaînes ; null ≠ zéro.
- Qualification/traitement/résolution ADMIN, confirmation/persistance CLIENT, annulation motivée ; aucun cycle de visite ou prix publié technicien anticipé. Notes internes séparées jusqu'aux tables, curseurs et versions publiques.
- Versions/idempotence autorisées avant rejeu, FK composites et immutabilité SQL renforcée ; audits et métadonnées d'outbox atomiques sans contenu libre. Aucun nouveau worker d'envoi ou calcul d'échéance/alerte SLA revendiqué.
- V14–V16 additives, POM et V1–V13 conservés. **293 tests Java + 6 Python**, zéro échec/erreur/skip ; OpenAPI **117 chemins/83 schémas**. [Preuves et limites](docs/preuves/lot-06-verifications.md). Aucun frontend/déploiement/migration Compose persistante ; réserves technicien/Google/production conservées.

## Lot 05 — 01 novembre 2026

- Comptes BUSINESS/INDIVIDUAL depuis dossiers APPROVED et membres CLIENT déjà validés ; aucun droit site par défaut, particulier limité au titulaire. Retrait/réadhésion sans restauration d'ancien droit.
- Sites, contacts privés et équipements ; écritures ADMIN uniquement, lecture CLIENT scoped en SQL avant pagination, filtre, total et détail. Aucun transfert intercompte/site.
- Habilitations explicites par liste de sites ou ALL, révocation dès la prochaine requête ; ETag/If-Match, PUT/PATCH documentés, archivage sans effacement et historiques append-only/audit atomique.
- Attributs JSONB v1 bornés et validés, FK composites et source APPROVED imposées aussi hors HTTP. Ports publics clients/identité sans cycle ni requête repository intermodule.
- V12–V13 additives, POM et V1–V11 inchangés. OpenAPI 92 chemins/52 schémas ; **243 tests Java + 6 Python** réussis. [Preuves et limites](docs/preuves/lot-05-verifications.md). Aucun frontend/déploiement/migration Compose persistante ; blocage documentaire technicien conservé.

## Lot 04 — 01 novembre 2026

- Invitations client ADMIN, expiration configurable, destinataire lié, empreinte seule en table et lien dans l'outbox chiffrée ; renvoi invalidant le lien précédent, acceptation atomique locale ou Google explicite.
- Dossiers client/technicien rattachés au user_id, brouillons versionnés, soumissions idempotentes, compléments/resoumissions, décisions motivées et historique append-only. Aucune activation par inscription ou vérification email.
- Approbation client uniquement sur identité PENDING vérifiée et rôle CLIENT exclusif ; cumuls de rôles refusés pour empêcher une activation technicien indirecte. Rejet révoquant les sessions ; suspension jamais levée implicitement.
- **Justificatifs technicien obligatoires** : approbation bloquée par le domaine et SQL tant que les pièces et leur contrôle privé/antivirus ne sont pas disponibles. Pas d'upload ou faux statut CLEAN.
- Google optionnel désactivé : intentions d'inscription distinctes du LOGIN, absence de fusion email/triplet et handoff mobile conservé. Reset d'un compte externe seul incapable d'ajouter un password implicitement.
- V8–V11 additives, POM/dépendances et V1–V7 inchangés ; OpenAPI courant 71 chemins/35 schémas. [Résultats réellement exécutés et réserves](docs/preuves/lot-04-verifications.md). Aucun frontend, compte/site/membre anticipé, migration du volume Compose ni déploiement.

## Lot 03 — 01 novembre 2026

- Identité locale UUID, Argon2id versionné, session web JDBC/CSRF, quotas SQL, vérification/reset et outbox mail chiffrée.
- MFA ADMIN obligatoire, TOTP anti-rejeu, recovery codes à usage unique, réauthentification récente et bootstrap hors HTTP, sans compte par défaut.
- Canal mobile technicien sans cookie, access/refresh opaques hashés, rotation atomique, révocation de famille sur rejeu et politique de réponse perdue explicitée.
- Google OAuth2 Login optionnel : state/nonce/PKCE et validateurs standards, liaison explicite sans fusion email, handoff mobile lié au verifier. Désactivé ; validation avec un double signé local seulement.
- Migrations V2–V7 additives, contrat OpenAPI 35 chemins, matrice SEC, documentation opérateur/Flutter et revue de menace. **172 tests Java + 6 Python** réussis ; ni Google réel ni production intégralement validés. [Preuves du lot 03](docs/preuves/lot-03-verifications.md).

## Lots 01/02 — 27 octobre 2026

- Socle Java 25/Boot 4.1.2 réellement compilé/testé, wrapper officiel, refus HTTP et erreurs expurgées ; voir `docs/lot-01-socle.md`.
- Secrets externes privés/ACL Windows, lock public réel, Compose local, PostgreSQL/PostGIS, migrations et droits SQL restrictifs.
- Audit technique append-only pour le runtime, tests PostGIS réels, migrateur one-shot et image backend non root sans secrets embarqués.
- CI Linux/Docker obligatoire distincte des unitaires Windows ; documentation et manifeste public actualisés, preuve initiale archivée.
- Résultats de scans et écarts, dont le patch Temurin disponible : `docs/preuves/lot-02-verifications.md`. Pas de déploiement, identité métier, certification ou approbation production.

## 1.1 — 27 octobre 2026

- Suppression de Keycloak de l'architecture active, des services Compose, images, rôles SQL et secrets générés.
- Interfaces email/mot de passe propres dans Angular et Flutter ; authentification applicative Spring Security.
- Sessions web JDBC avec CSRF ; jetons opaques mobiles révocables avec rotation refresh, sans JWT applicatif.
- Google optionnel via OAuth2 Client ; liaison vérifiée au compte métier et retour mobile par code éphémère lié à PKCE.
- Cycle explicite de vérification email, reset, MFA administrateur et bootstrap contrôlé.
- Dossier professionnel, pièces privées et validation administrative indépendants du moyen de connexion.
- Révision des quinze prompts et d'AGENTS.md ; ajout d'AUTHENTIFICATION.md, SECURITE.md, UPGRADE.md et du profil Google de référence.
- Actualisation du validateur statique et des limites déclarées. Aucun résultat de démarrage Docker, build Java ou audit n'est inventé.

Les décisions métier de la version 1.0 sont conservées : trois rôles, candidature puis sélection admin, propositions financières distinctes et protection des données par compte/site.
