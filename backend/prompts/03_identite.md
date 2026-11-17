# Prompt 03 — Identité locale, sessions, MFA et Google optionnel

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente AUTHENTIFICATION.md avec des écrans Angular/Flutter propres consommant nos contrats, sans serveur de login externe. Ce lot livre le backend et ses contrats ; ne génère pas les frontends hors demande. Découpe le travail en tranches vérifiées successives : 03A comptes/credentials et session web ; 03B vérification/reset ; 03C MFA/bootstrap ADMIN ; 03D jetons opaques mobile ; 03E Google et liaison/handoff ; 03F revue et matrice d'attaques. Les dépendances et tests doivent passer avant la tranche suivante ; ne pas demander une confirmation routinière entre tranches autorisées.

Compte UUID stable, credential local distinct, external_identity(provider,issuer,sub) unique, aucune fusion par email. Argon2id explicite : au moins m=19456 KiB/t=2/p=1 pour le point de départ, sel aléatoire, Bouncy Castle maintenu et coût mesuré. Politique de mots de passe bornée, absence d'énumération et limitation des tentatives distribuée. GET /me n'expose aucun secret.

Web : filtres/mécanismes Spring, Spring Session JDBC et migrations Flyway, cookie HttpOnly/Secure adapté, CSRF sur login/logout et mutations, rotation contre fixation et SecurityContext persisté. Deux états distincts avant/après MFA ; aucune fonction ADMIN avant preuve. Respecte les durées d'inactivité et absolues. Mobile : chaîne Bearer opaque, aucune authentification par cookie ; empreintes de jetons aléatoires, famille de session, contrôle révocation/statut à chaque requête, rotation refresh atomique et réutilisation révoquant la famille. Les chaînes web, mobile et OAuth navigateur sont ordonnées et terminées par refus de repli.

Vérification/reset : but, hash, expiration et usage unique atomique ; GET sans effet irréversible. Reset révoquant les sessions sans supprimer MFA ni suspension. MFA ADMIN complet, y compris après Google : bibliothèque TOTP maintenue sélectionnée/vérifiée, secret chiffré à clé externe versionnée, anti-rejeu, limites, codes de récupération et procédure sans bypass email. Bootstrap hors API publique, aucun ADMIN/mot de passe livré par défaut. Ne pas activer des privilèges MFA_PENDING.

Google est facultatif : OAuth2 Login code standard, state/nonce/PKCE S256, signature/issuer/audience/temps vérifiés, callbacks exacts et scopes minimaux. Web : session locale. Mobile : navigateur système, handoff à usage unique <=60 s lié à un challenge PKCE et à une destination revendiquée autorisée ; échange avec verifier puis émission des jetons opaques locaux. Aucun jeton Google ou applicatif dans une URL, aucun grant_type=password. Écris la revue de menace du handoff et teste la configuration réelle en staging avant activation. Les identifiants réels Google sont externes au dépôt et ne sont pas remplacés par des valeurs fictives.

Liaison Google : réauthentification récente du compte existant et preuve du fournisseur, MFA récent pour ADMIN, refus si identité déjà liée ailleurs, pas de fusion automatique par email ; unlink sans enfermer le compte. Nouvelle identité : uniquement onboarding autorisé, jamais client actif sans invitation ni technicien approuvé. Statut métier contrôlé même si session non expirée.

Recette : toute la section 8 d'AUTHENTIFICATION.md et SEC-01 à SEC-12/SEC-17. Inclure fixation/persistance, CSRF, expiration/révocation, suspension, reset double et concurrent, TOTP rejoué, MFA contourné par Google ou récupération, refresh concurrent/perte de réponse, code handoff volé/rejoué/incorrect, fausse audience/issuer/signature/nonce/state et secret absent des logs. Utilise des doubles OIDC pour cas négatifs et distingue leur résultat de l'intégration Google réelle. Ne pas marquer le lot complet avec un sous-lot manquant ou un test non exécuté.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les tranches réalisées, les fichiers/contrats modifiés, les exigences SEC traitées, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
