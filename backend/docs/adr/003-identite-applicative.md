# ADR 003 — Identité applicative Spring Security, web et mobile séparés

- Date : 27 octobre 2026.
- Statut : décisions utilisateur/cahier formalisées au cadrage, identité mise en œuvre et vérifiée localement au lot 03 ; Google réel/staging et revue indépendante non vérifiés.
- Exigences principales : SEC-01 à SEC-12, SEC-17, SEC-18, SEC-20.

## Contexte et décision

Angular et Flutter possèdent leurs écrans de connexion, récupération et dossier professionnel. Le backend authentifie avec Spring Security ; **aucun serveur de connexion externe** n'est réintroduit. Google reste optionnel. Les seuls rôles métier sont `ADMIN`, `CLIENT`, `TECHNICIAN`.

Identifiant stable : `user_id` UUID interne. Identifiant local et identités externes sont des associations de ce compte. Une identité Google est unique par `(provider, issuer, sub)`, pas par email. Dossier professionnel, documents et approbation restent dans notre application. Google ne rend jamais un technicien actif ni un utilisateur administrateur.

## Canaux

| Canal | Authentification retenue | Exclusions et preuves attendues au lot 03 |
|---|---|---|
| Angular `/api/web/v1/**` | Session serveur Spring Session JDBC ; cookie HttpOnly/Secure en production ; CSRF y compris login/logout ; rotation login/MFA | Pas d'access token dans localStorage ; test fixation/persistance du SecurityContext, expiration et révocation |
| Flutter `/api/mobile/v1/**` | Access/refresh opaques locaux, aléatoires, au moins 32 octets ; empreintes seules en base ; statut et révocation à chaque requête | Aucun cookie utilisé pour authentifier, aucun JWT applicatif ou jeton Google accepté comme Bearer métier |
| Google navigateur | OAuth2 Login/OIDC standard, échange serveur, contrôles signature/issuer/audience/state/nonce, PKCE S256, callbacks et destinations exacts | Pas de mot de passe Google dans nos formulaires, pas de redirection libre, pas de fusion automatique par email |
| Google mobile facultatif | Navigateur système ; callback backend ; code de remise <= 60 s, usage unique, lié à la transaction et à un challenge S256 ; échange avec verifier | Pas d'access/refresh/ID token dans un lien ; aucune session web privilégiée accordée par accident |

Chaînes HTTP séparées avec refus de repli ; exceptions publiques explicitement listées au lot 03. Pas de `permitAll` global, ni de désactivation globale de CSRF pour faciliter le frontend. Le squelette du lot 01 n'ouvre **aucune** route d'identité anticipée.

## Mots de passe, MFA et récupération

- `Argon2PasswordEncoder` avec Bouncy Castle `bcprov-jdk18on:1.86`. Configuration explicite initiale : sel 16 octets, résultat 32 octets, Argon2id `m=19456 KiB, t=2, p=1`. Les valeurs par défaut du framework ne sont pas réputées satisfaire ce contrat. Compatibilité documentaire et sonde JDK 25 : [référentiel](../versions.md), [preuve](../preuves/lot-00-verifications.md).
- Coût à mesurer sur le runtime cible et concurrence à borner ; la sonde locale n'est pas le dimensionnement du service de connexion. Hash versionné, pas de `NoOp` ou de repli vers un hash rapide. SHA-256 est réservé aux empreintes de jetons à forte entropie.
- MFA obligatoire pour ADMIN, connexion locale **ou** Google. `MFA_PENDING` est un état technique, **pas un quatrième rôle** et jamais une autorité ADMIN effective. Enrôlement, vérification, anti-rejeu TOTP et récupération doivent être réalisés avant les fonctions privilégiées.
- TOTP via `com.eatthepath:java-otp:1.0.0`, choisi/vérifié au lot 03 ; secret chiffré JCA AES-256-GCM à clé externe versionnée. Codes de récupération à usage unique de 256 bits aléatoires et empreinte SHA-256, jamais un hash rapide de codes humains courts.
- Tokens de vérification/reset liés à un but et un destinataire, expirants, hashés et consommés atomiquement par commande ; pas de consommation irréversible par GET. Reset réussi : révocation sessions/refresh, sans suppression de suspension ni contournement du MFA.
- Association/dissociation Google : session récente réauthentifiée, intention `LINK` liée au compte, CSRF web, MFA récent pour ADMIN et autre moyen d'accès valide avant dissociation.
- Bootstrap ADMIN hors HTTP public, nominatif, contrôlé et audité ; aucun rôle choisi dans un formulaire public, aucun compte/mot de passe livré par défaut.

## Rotation mobile : compromis explicite

Refresh sous verrou de famille dans une transaction : ancien token marqué utilisé, nouveau refresh et access créés ensemble. Un refresh **connu et déjà consommé** révoque la famille ; un token inconnu ne permet pas de choisir une famille à révoquer. Flutter n'effectue qu'un renouvellement en vol.

Si la réponse est perdue après commit, rejouer l'ancien refresh impose une nouvelle connexion selon la politique stricte du cahier. Pas de tolérance cachée, de seconde branche active ou de conservation du refresh brut pour reconstruire la réponse. Tests de concurrence et de perte de réponse obligatoires.

## Autorisation et statut

Une authentification valide ne vaut pas droit métier. Rôle, niveau MFA, statut actuel, compte/site, affectation et état de ressource sont combinés côté serveur. Suspension/perte de droit prend effet au prochain accès et lors d'un rejeu ; une panne du stockage d'autorisation ne rend pas l'accès permissif. Voir [la matrice](../securite/autorisations.md).

Les durées proposées ont été confirmées explicitement par le responsable le 01/11/2026 : web 30 min/8 h, preuve récente 5 min, mobile access 5 min/refresh 7 j/famille 30 j, email 24 h/reset 30 min. TOTP choisi : `com.eatthepath:java-otp:1.0.0`, 6 chiffres ASCII/SHA1/30 s, fenêtre ±1 et consommation SQL. [Implémentation, contrat et limites](../lot-03-identite.md). Les parcours Google nécessitent toujours des tests réels en environnement dédié, en complément des doubles OIDC.

## Alternatives écartées pour ce produit

- Serveur d'identité séparé : possible techniquement, mais contraire à la décision utilisateur actuelle.
- JWT applicatifs autonomes : non retenus ; la révocation et les droits actuels sont vérifiés en base avec les sessions opaques.
- Google comme seule connexion ou comme validation professionnelle : contraire au parcours local et à l'approbation administrative.
- Cryptographie ou serveur OAuth génériques maison : hors périmètre ; utilisation des composants maintenus et revue humaine des intégrations.

## Recette future et limites du lot 00

Lot 03 : tests HTTP web/mobile, cookies/CSRF, suspension sans attendre le TTL, MFA sur tous les chemins, liaison d'identité, reset concurrent, refresh concurrent/réutilisé/inconnu, handoff volé/expiré/rejoué et absence de secrets dans les traces. Lots métier : contrôles de périmètre à chaque introduction de route.

La sonde du lot 00 ne prouve ni ces flux ni l'absence de vulnérabilité. L'objectif ASVS L2 comporte notamment un écart à examiner sur le MFA limité à ADMIN : [mapping initial](../securite/asvs-sec.md). Aucun élargissement produit du MFA n'est imposé silencieusement.

## Sources consultées le 27 octobre 2026

- Spring Security : [mots de passe](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), [session](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html).
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).
- [Bouncy Castle Java](https://www.bouncycastle.org/download/bouncy-castle-java/) ; [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- Sources internes : `AUTHENTIFICATION.md`, `AGENTS.md`, cahier backend §5 et `SECURITE.md`.
