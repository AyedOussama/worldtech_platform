# Authentification applicative — décision et contrat v1.1

Conception du 27 octobre 2026, suivi d'implémentation du 01 novembre 2026. Lire aussi le cahier et SECURITE.md. Le contrat décrit le périmètre ; l'état effectivement implémenté/vérifié et ses limites sont dans [docs/lot-03-identite.md](docs/lot-03-identite.md) et [docs/lot-04-onboarding.md](docs/lot-04-onboarding.md) pour les inscriptions/invitations explicites. Aucun audit indépendant ou validation Google réelle revendiqué. Les routes courantes sont figées dans [OpenAPI](openapi/maintenance-api.json), pas par les références historiques.

## 1. Décisions

- Interfaces email/mot de passe, récupération et dossier professionnel codées dans Angular et Flutter.
- Authentification locale avec Spring Security ; algorithmes et primitives issus de bibliothèques maintenues.
- Web : session serveur Spring Session JDBC, pas de jeton d'accès dans localStorage.
- Mobile : access et refresh opaques, aléatoires, sans format JWT applicatif ; empreintes en base et contrôle de révocation à chaque requête.
- Google optionnel via OAuth2/OIDC standard côté serveur ; les pages Google restent contrôlées par Google. La connexion locale n'en dépend pas.
- MFA ADMIN obligatoire avant toute fonction privilégiée, quel que soit le moyen de connexion initial.
- Validation professionnelle et droits d'accès décidés dans le backend métier. Google ne valide ni les justificatifs ni les compétences.

Le backend n'est pas annoncé comme un serveur OAuth générique ouvert à des partenaires. Les sessions opaques servent nos propres clients. Ne pas activer un password grant OAuth pour transmettre les mots de passe à un autre serveur. Le starter OAuth2 Client reste nécessaire à Google ; il ne remplace pas le module d'identité locale.

## 2. Données et secrets

| Objet | Données essentielles | Garanties |
|---|---|---|
| app_user | user_id UUID, statut, rôles autorisés, timestamps, version de sécurité | Identifiant stable ; aucune promotion depuis un DTO public |
| local_credential | user_id, email de connexion normalisé, preuve de vérification, hash de mot de passe versionné | Unicité et transitions atomiques ; aucun mot de passe clair |
| external_identity | user_id, provider, issuer canonique, sub | Unicité du triplet ; association après preuve, pas par email seul |
| auth_session | user_id, canal, création, échéances, révocation, niveau d'authentification | Statut et privilèges actuels revérifiés ; MFA_PENDING n'est pas ADMIN effectif |
| mobile_access_token | empreinte, session_id, expiration, révocation | 32 octets aléatoires au minimum, format borné, recherche par empreinte |
| mobile_refresh_token | empreinte, famille/session, parent, état utilisé/révoqué, dates | Rotation et détection de réutilisation atomiques |
| action_token | empreinte, but, user/invitation, expiration, consommation | Jetons non interchangeables entre reset, vérification et invitation |
| mfa_credential | user_id, type, secret TOTP chiffré, version de clé, dernier pas utilisé | Clé hors DB ; rejeu et tentatives contrôlés |
| recovery_code | empreinte, user_id, état utilisé | Usage unique ; codes bruts montrés une fois, jamais journalisés |
| oauth_transaction / mobile_handoff | intention, état/nonce, destination autorisée, challenge, expiration, consommation | Aucun redirect libre ; aucune confusion login/association |

Le hash d'un mot de passe et l'empreinte d'un jeton aléatoire ont des objectifs différents. Mot de passe : Argon2id via PasswordEncoder/DelegatingPasswordEncoder configuré. Jeton imprévisible d'au moins 256 bits : SHA-256 convient pour une empreinte non réversible à rechercher ; ne jamais réutiliser cette stratégie pour hacher un mot de passe humain.

Argon2id : au moins m=19456 KiB, t=2, p=1 pour le point de départ retenu, sel aléatoire et hash de taille adéquate. Mesurer sur le runtime cible, renforcer si possible et limiter les vérifications simultanées. Ne pas supposer que la méthode defaults d'une bibliothèque utilise automatiquement ces paramètres. Le POM de référence ajoute bcprov-jdk18on 1.86 requis par l'implémentation Spring retenue ; résolution et compatibilité restent à tester.

Politique confirmée par le responsable le 01/11/2026 : 15 à 128 points de code Unicode, espaces et gestionnaires de mots de passe autorisés, aucune troncature silencieuse, refus de mots de passe courants selon une source contrôlée locale (non exhaustive des fuites). Ne pas envoyer les mots de passe en clair à un service tiers de vérification. Limiter taille des corps et concurrence avant le calcul coûteux ; erreurs génériques de connexion/récupération sans exposer l'existence d'un compte. Mesurer aussi les écarts de comportement et de temps entre compte absent et présent.

Normaliser les emails selon une politique documentée ; ne pas supprimer arbitrairement points, suffixes + ou autres caractères en imitant un fournisseur. Conserver la valeur d'affichage distinctement. Un changement d'email exige vérification du nouveau, réauthentification récente et notification appropriée ; il ne change pas user_id.

## 3. Sessions Angular

Le mécanisme de login Spring doit invoquer la stratégie de session et sauvegarder correctement le SecurityContext. Privilégier les filtres d'authentification du framework configurés avec une réponse JSON ; une API personnalisée doit conserver les mêmes garanties. Tester réellement la fixation de session, pas uniquement la réussite de AuthenticationManager.authenticate().

Cookie de session : HttpOnly, Secure en production, Path=/, pas de Domain si le préfixe __Host- est utilisé ; SameSite=Lax avec retour OIDC par GET testé. Le nom et Secure sont différents en dev HTTP pour ne pas créer un cookie __Host- invalide. Angular est servi sous la même origine que l'API web en production. CSRF s'applique aux mutations, login et logout compris ; GET de récupération du token prévu explicitement. CORS n'est pas un remplacement de CSRF.

Expiration confirmée le 01/11/2026 : inactivité 30 min, absolue 8 h, réauthentification récente 5 min pour liaison/déliaison d'identité, changement de mot de passe, MFA et opérations administratives sensibles. Une connexion ou un passage MFA réussi renouvelle l'identifiant de session. Le logout révoque la session côté serveur puis efface le cookie. Les tables distribuées sont versionnées par Flyway ; Spring Session nettoie ses sessions expirées.

MFA_PENDING autorise uniquement challenge, logout et récupération adaptée ; pas de routes ADMIN, même si le rôle est déjà stocké sur l'utilisateur. Le niveau et la fraîcheur de l'authentification sont contrôlés côté serveur ; un champ frontend ou une session Google ne constitue pas cette preuve.

## 4. Sessions Flutter

La chaîne /api/mobile/v1/** n'authentifie jamais par cookie et ne crée pas de session web. Les seules exceptions publiques sont une liste explicite : login, amorçage d'inscription, récupération et échange d'un challenge valide. Bearer signifie ici jeton opaque local ; décoder un JWT n'est pas l'algorithme de validation retenu.

Access : 32 octets aléatoires ou plus, TTL validé 5 min. Refresh : forte entropie au moins équivalente, inactivité 7 j, durée absolue de famille 30 j. Génération via SecureRandom/bibliothèque reconnue. Stockage brut uniquement côté client dans les mécanismes sécurisés de la plateforme et lors de la réponse initiale ; aucune valeur brute dans DB, logs, analytics, URL ou rapport d'erreur. Réponses de jetons avec Cache-Control: no-store.

À chaque requête : format et longueur bornés, empreinte trouvée, expiration non atteinte, session/famille non révoquée, utilisateur et autorisation métier actuels. Le contrôle SQL peut joindre ces données en une requête indexée. Une panne du stockage d'autorisation refuse l'accès ; ne pas basculer vers une autorisation supposée en cache. Un jeton Google n'est jamais accepté comme Bearer de l'API métier.

Refresh : verrouiller la ligne/famille trouvée par l'empreinte, vérifier toutes les échéances et le statut, marquer l'ancien refresh utilisé, créer le nouveau et l'access dans une transaction. Une réutilisation d'un refresh connu et déjà consommé révoque sa famille et produit un audit ; un refresh inconnu ne doit pas permettre de révoquer une famille choisie par l'appelant.

Flutter sérialise les refresh (un seul en vol). Deux renouvellements concurrents ne produisent pas deux branches actives. Compromis explicite de cette politique stricte : si la réponse de rotation est perdue après commit, rejouer l'ancien refresh sera traité comme un rejeu et peut imposer une nouvelle connexion. Ne pas ajouter une tolérance cachée ni conserver les refresh bruts pour rejouer une réponse. Tester et expliquer ce comportement dans le contrat client.

Logout d'un appareil : révocation de sa famille. Logout global, reset réussi, suspension ou incident de sécurité : révocation de toutes les sessions concernées et contrôle des anciennes sessions au prochain accès. Les données métier déjà téléchargées hors ligne ne sont pas rappelables instantanément ; purge côté app au prochain contact et minimisation du cache.

## 5. Google web et mobile

### 5.1 Web

Le bouton Angular « Continuer avec Google » ouvre le parcours OAuth2 Login du backend. Client Google confidentiel côté serveur ; secret hors frontend. Redirect URI enregistrée exactement, scopes openid/profile/email seulement pour le besoin actuel. Pas d'accès Gmail/Drive requis.

Le framework gère l'échange du code et la validation OIDC : signature avec clés officielles, issuer, audience/client autorisé, expiration et nonce ; état de transaction lié au navigateur. Configurer/tester PKCE S256 sur le flux code. Ne pas confondre authentification Google et authentification métier. Après validation, le backend retrouve external_identity puis crée la session locale appropriée ou poursuit une inscription autorisée.

Si le fournisseur est indisponible, les connexions locales existantes restent possibles. Un nouveau compte client exige toujours son invitation ; un nouveau technicien reste en onboarding. Aucun retour Google ne crée ADMIN ou APPROVED. Le profil Google ne remplace pas les données professionnelles validées.

### 5.2 Mobile : remise d'un code à l'application

Pour éviter d'inventer une API de mot de passe Google ou de transporter des jetons dans des liens, retenir le parcours serveur suivant. Ce contrat initial a été implémenté au lot 03 et testé avec un fournisseur OIDC local ; sa recette Google réelle et les associations Android/iOS restent à obtenir.

1. Flutter génère un verifier PKCE fort et garde sa valeur localement ; il transmet seulement le challenge S256 à une transaction de login mobile. La destination de retour est un identifiant de destination préenregistrée, jamais une URL libre.
2. Le backend crée une transaction courte avec state et nonce, intention LOGIN ou LINK immuable, challenge et destination autorisée. Un lancement navigateur à usage unique engage le flux Google standard. Le navigateur système, pas une WebView collectant les identifiants, est utilisé.
3. Google revient sur un callback HTTPS du backend. Spring valide le flux et l'identité. Ce parcours mobile n'accorde pas accidentellement une session web ADMIN et ne réutilise pas une session navigateur comme preuve de liaison non consentie.
4. Le backend prépare un code de remise aléatoire à usage unique, valable 60 s au maximum, associé à l'identité, à la transaction et au challenge. Seule son empreinte est conservée. Il redirige vers un App Link/Universal Link revendiqué par l'application et strictement autorisé. Aucun access, refresh ou ID token dans le lien. Éviter journalisation et Referer de ce code ; pages de transition sans ressource tierce et cache interdit.
5. Flutter échange code + verifier sur HTTPS. Le backend contrôle l'expiration, S256 et les liens de transaction, consomme atomiquement le code puis émet ses jetons opaques. Un code volé sans verifier échoue. Un code consommé, périmé ou d'une autre transaction échoue.

Le callback Google, la session temporaire du navigateur et les routes web/mobile doivent être séparés et testés. Ce mécanisme de remise n'est pas un prétexte à créer un serveur OAuth public. Si l'équipe décide plus tard d'utiliser un SDK Google natif à la place, elle écrit un ADR, précise audience/nonce/anti-rejeu et les tests ; elle ne mélange pas les deux flux sans revue.

### 5.3 Liaison à un compte existant

Depuis une session existante récemment réauthentifiée, « Associer Google » crée une transaction LINK liée à user_id ; le callback vérifie cette intention et la preuve Google. Pour ADMIN, MFA récent également exigé. Une identité Google déjà liée à un autre compte est refusée. La correspondance d'email seule n'autorise aucune liaison et aucun transfert de rôle.

Une première connexion Google dont l'email correspond à un compte existant propose de s'authentifier à ce compte et de confirmer la liaison, sans divulgation inutile. Les identités nouvelles suivent le parcours d'onboarding autorisé. Déconnecter un fournisseur exige réauthentification et au moins un autre moyen de connexion/récupération valide ; ne pas enfermer un compte sans accès.

## 6. Vérification email, reset et MFA

Jetons d'action aléatoires 32 octets minimum, empreinte seulement, but et destinataire liés. Durées validées : vérification email 24 h ; reset 30 min. Renvoi et consommation atomiques, limites par compte/IP et invalidation selon politique. Aucune action irréversible par simple GET de lien : afficher puis confirmer par POST protégé/challenge pour résister aux préchargements des outils email.

Reset : réponse générique, token valide et nouveau mot de passe conforme avant consommation, invalidation des autres reset et des sessions/refresh, notification via outbox. Le reset n'active pas un technicien, ne retire pas une suspension et ne réinitialise pas le MFA. Pour un compte Google seul, ajout d'un mot de passe après preuve explicite de contrôle et politique de récupération ; ne pas transformer silencieusement ce compte par une route générique de reset.

MFA MVP : TOTP via une bibliothèque maintenue sélectionnée et verrouillée au lot 03, jamais calcul d'algorithme maison. Enrôlement depuis réauthentification récente, secret provisoire chiffré, confirmation d'un premier code avant activation, fenêtre temporelle bornée, consommation atomique du pas de temps pour empêcher le rejeu, limites d'essais et alertes. Les secrets TOTP doivent pouvoir être déchiffrés : chiffrement authentifié via un composant éprouvé et clé externe versionnée, avec rotation/ancienne clé de lecture. Ce n'est pas le stockage des mots de passe.

Codes de récupération forts, hashés et à usage unique ; régénération invalidant les précédents, notification et audit. Une récupération MFA administrateur n'est pas un simple reset email : codes de récupération ou procédure opérateur indépendante et documentée. Aucun opérateur ne peut promouvoir lui-même un compte par une route publique. L'outil de bootstrap ADMIN est hors API publique, contrôlé, ponctuel et audité, sans mot de passe par défaut ni rôle fourni par un formulaire.

## 7. Contrats de routes à figer au lot 03

| Canal | Routes indicatives | Exigences |
|---|---|---|
| Web | GET /api/web/v1/auth/csrf ; POST /auth/login, /auth/logout sous ce préfixe | Session/CSRF ; JSON ; cookie renouvelé après preuve |
| Mobile | POST /api/mobile/v1/auth/login, /auth/refresh, /auth/logout | Aucun cookie d'auth ; jetons opaques, quotas et révocation |
| Commun | /email-verification, /password-reset, /mfa sous un préfixe de canal explicite | Challenge limité au but ; aucun droit ADMIN avant MFA |
| Google navigateur | /oauth2/authorization/google et callback HTTPS dédié | state/nonce, PKCE, destinations fixes, intention de transaction |
| Google mobile | /api/mobile/v1/auth/google/transactions et /exchange | Handoff à usage unique, TTL <= 60 s et S256 obligatoire |
| Association | /auth/identities/google/link et unlink dans canal authentifié | Réauthentification récente ; CSRF web ; consentement ; preuve du fournisseur |
| Exploitation | Bootstrap administrateur hors HTTP public | Contrôle opérateur, audit, MFA et absence de secrets en ligne de commande |

La liste est une proposition contractuelle à finaliser avec OpenAPI au lot 03 ; aucune route n'est automatiquement exposée par ces documents ou les YAML. Les réponses MFA nécessaires au mobile doivent utiliser un challenge opaque limité au MFA et non un access token doté de privilèges. Les APIs protégées renvoient 401/403 JSON plutôt qu'une page de login HTML inattendue.

## 8. Recette obligatoire

Tests positifs et négatifs des deux canaux ; fixation et persistance de session ; login/logout sans CSRF ; durée absolue/inactivité ; suspension sans attendre l'expiration ; rôle fourni par l'appelant ; reset double/expiré/concurrent ; MFA contourné par Google/reset/mobile ; refresh concurrent/réutilisé/inconnu ; identité externe déjà liée ; token OIDC faux/mauvais audience/nonce/issuer ; callback falsifié ; handoff volé/périmé/rejoué/destinataire libre/verifier incorrect ; refus de secrets dans logs et cache.

Utiliser des doubles OIDC contrôlés pour les erreurs et un environnement Google de test séparé pour la validation de l'intégration réelle. Un test mocké seul ne prouve pas la configuration du fournisseur. Une absence de credentials de test n'autorise ni désactivation de validation ni affirmation de succès.

## Sources

- Spring Security, authentification : https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html
- Sessions : https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
- CSRF : https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- PasswordEncoder : https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- OAuth2 Login : https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html
- Google OIDC : https://developers.google.com/identity/openid-connect/openid-connect
- OAuth Security BCP : https://www.rfc-editor.org/rfc/rfc9700.html
- OAuth pour applications natives : https://www.rfc-editor.org/rfc/rfc8252.html
- OWASP Password Storage : https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
