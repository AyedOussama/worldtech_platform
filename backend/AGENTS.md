# Instructions permanentes — agents backend

Révision 1.1 · Authentification applicative Spring Security et interfaces propres.

Ces instructions concernent le dépôt du backend de maintenance informatique dans lequel ce fichier sera intégré. Elles ne signifient pas que ce pack de préparation contient déjà une application. Les instructions explicites du responsable du projet prévalent.

## Mission et périmètre

Construire un backend Spring Boot robuste, lisible et vérifiable, lot par lot. Lire le cahier technique, AUTHENTIFICATION.md, SECURITE.md et les ADR présents avant de modifier le dépôt. Inspecter l'état Git et les instructions locales ; conserver les changements préexistants. Ne pas créer de dépendance, rôle ou fonction métier hors périmètre pour remplir un squelette.

Rôles métier uniques : ADMIN, CLIENT, TECHNICIAN. L'admin organise, valide et affecte. Angular pour client et administration, Flutter pour technicien ; pas de mobile client initial. Le technicien candidate ; l'admin sélectionne ; la confirmation technicien utilise la version des conditions proposée. Un ticket peut avoir plusieurs missions. La proposition financière client, les montants publiés par l'admin et les conditions acceptées restent distincts. Aucun prix facturé, paiement ou marge déduit sans spécification.

## Architecture

- Monolithe modulaire, un module Maven initial, packages par domaine, Spring MVC pour REST.
- Contrôleur → cas d'usage → domaine/ports ; infrastructure pour JPA, SQL et intégrations.
- Domaine indépendant de Spring/JPA/HTTP. Entités JPA non exposées ; DTO par acteur.
- Interfaces publiques explicites entre domaines ; aucun accès direct au repository d'un autre domaine.
- Pas de classe générique universelle, microservice ou nouvelle base sans problème concret documenté.
- Versions transitives gérées par le BOM Boot. Vérifier les sources officielles avant ajout/mise à jour ; pas de SNAPSHOT/RC/latest pour la production. Ne pas mettre à jour tout le socle pendant un lot métier sans raison.

## Sécurité non négociable

- Refus par défaut. Connexion email/mot de passe via Spring Security, écrans Angular/Flutter propres. Google optionnel via OAuth2/OIDC. Pas d'algorithme cryptographique maison ni de serveur OAuth inventé.
- Web : session serveur Spring Session JDBC, cookie HttpOnly/Secure, CSRF et rotation après login/MFA. Mobile : jetons opaques aléatoires, empreintes seulement en base, expiration et révocation vérifiées à chaque requête. Google mobile : navigateur système et retour par code éphémère à usage unique lié à PKCE ; aucun jeton dans les liens.
- Autorisation serveur par rôle, statut, compte, site, affectation et état. Scoper les requêtes avant pagination/compteurs/exports. Revalider sur téléchargement et rejeu.
- Aucun formulaire ne choisit ADMIN, approbation, propriétaire ou prix publié. Compte métier identifié par user_id UUID ; identité Google liée par (provider, issuer, sub) unique après vérification et, pour un compte existant, réauthentification explicite. Jamais de fusion automatique par email.
- Aucun secret, lien d'invitation brut, jeton, contenu sensible ou donnée d'un autre compte dans logs/erreurs/tests de snapshot.
- Aucun bucket public, document avant scan, adresse/contact exact dans découverte, téléchargement depuis URL arbitraire ou désactivation TLS de contournement.
- Les comptes en attente et suspendus ne disposent pas des opérations métier réservées aux comptes actifs ; Google ne valide jamais un dossier technicien.
- Argon2id via bibliothèque maintenue, configuration explicite compatible OWASP et coût mesuré ; pas de NoOp, hash rapide ni paramètres implicites supposés sûrs.
- MFA administrateur avant toute fonction privilégiée, y compris après Google ; un état MFA incomplet n'accorde aucun rôle ADMIN effectif. Récupération MFA sans contournement par simple reset email.
- Vérification email/reset : jetons forts hashés, expirants, usage unique atomique. Reset réussi révoque sessions et refresh existants. Pas de promotion à ADMIN depuis un DTO public.
- Refresh mobile : rotation atomique, détection de réutilisation, révocation de la famille ; client avec renouvellement unique en vol. Tests de concurrence et de perte de réponse obligatoires.
- Login Google web par OAuth2 Login standard : validation OIDC, state/nonce, callbacks exacts et destinations autorisées. Aucun contournement issuer/audience ni grant_type=password.
- Aucun rôle applicatif ne doit lire/modifier les tables d'identité par une route métier générique ; accès via cas d'usage dédiés.

## Cohérence et données

- PostgreSQL/PostGIS réel pour les garanties SQL ; pas H2 comme substitution aux tests d'intégration.
- Migrations Flyway append-only ; aucune réécriture d'une migration déjà appliquée. Pas de ddl-auto=update/create en production.
- Contraintes DB pour relations, unicités et montants. Index unique partiel pour l'affectation active ; verrou mission et ordre de verrouillage cohérent.
- Transactions courtes ; aucun mail/HTTP/PDF pendant un verrou SQL. Outbox pour effets externes.
- BigDecimal/numeric pour argent, devise et base explicites. Jamais double/float pour ces calculs.
- Clock injecté, temps UTC et fuseau du site explicite. Expiration vérifiée au moment de la commande, pas uniquement par un job.
- Idempotence liée à acteur/opération/clé et empreinte ; contrôle d'accès également lors d'un rejeu.
- Versions optimistes et conflits explicites. Pas d'écrasement silencieux d'un rapport attesté, d'un prix accepté ou d'un brouillon hors ligne périmé.

## Façon de travailler

1. Inspecter le dépôt, identifier le lot et les dépendances déjà présentes ; annoncer les choix significatifs.
2. Proposer un plan bref, puis réaliser une tranche complète et bornée. Ne pas s'arrêter à du pseudocode si le lot demande du code.
3. Si une règle produit manque, avancer sur la partie indépendante et poser une question ciblée pour le contrat concerné ; ne pas inventer une règle financière ou une habilitation.
4. Écrire les validations utiles : règles métier, droits négatifs, contraintes SQL et concurrence lorsque concernés. Ne pas ajouter des tests qui recopient simplement le code.
5. Exécuter les vérifications adaptées. Lire leurs résultats. Une absence de Docker ou de service externe doit être déclarée, pas convertie en succès ni cachée par un skip permanent. Référencer les exigences SEC-xx concernées et les preuves associées.
6. Relire le diff pour secrets, fuite de données, limites de requêtes et migration. Actualiser OpenAPI/ADR/documentation si le comportement change.
7. Résumer le comportement livré, les tests réellement exécutés, les tests non exécutés et la suite. Ne pas dire « sécurisé », « production ready » ou « tout passe » sans les preuves correspondantes.

Les inspections, modifications réversibles et tests du lot sont autorisés par sa demande. Ne pas inventer une demande de permission à chaque fichier. Ne pas déployer, envoyer de messages réels ou détruire des données sans instruction correspondante. Ne jamais écraser du travail existant ou effectuer un reset Git destructif pour simplifier le lot.

## Contrôle de fin de lot

Contrat explicite, périmètre respecté, erreurs ProblemDetail sans détails internes, autorisations positives et négatives, migration et rollback applicatif compatibles, logs utiles sans secrets, performances bornées, documentation à jour. Les règles de sécurité commencent dès le squelette. Les revues humaines restent nécessaires sur identité, finances, données privées et migrations sensibles.

## Preuves et mise en production

Chaque compte rendu sépare prévu, implémenté, vérifié et non vérifié. Objectif OWASP ASVS 5.0 niveau 2 à mapper exigence par exigence ; ce dossier ne vaut pas certification. Tests négatifs avec plusieurs comptes, scans et revue des changements sensibles sont obligatoires. Avant production : revue humaine, pentest indépendant sur préproduction, correction et retest des constats critiques/élevés, restauration démontrée et surveillance opérationnelle. Un résultat de scanner seul ne prouve pas l'absence de faille. Aucun agent ne peut déclarer une application inviolable.
