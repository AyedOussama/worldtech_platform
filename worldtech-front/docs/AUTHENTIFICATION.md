# Identité web Angular / Spring Security
Référentiel rapproché le 10/11/2026 : [contrat réel relevé en FE00](CONTRAT_BACKEND_RELEVE_FE00.md), OpenAPI 3.1.0 / API 0.6.0 dans `../backend`. **Identité locale intégrée en FE03** ; les [preuves FE03](../verification/fe03/RESULTATS.md) séparent Spring réel, simulations et limites. Google reste désactivé, voir ci-dessous.

## Modèle de session
Session serveur Spring Session JDBC, cookie HttpOnly et Secure en production. Angular ne lit pas la valeur du cookie de session et ne stocke pas un JWT d'application.
`SessionStore` distingue `initializing`, `anonymous`, `mfaRequired`, `emailPending`, `onboarding`, `authenticated`, `suspended`, `rejected`, `technician`, `expired`, `blocked`, `unavailable`, `signingOut`, `logoutFailed` et `recoveryCodes`. Ces noms frontend ne sont pas les enums de l'API. Les rôles ne sont jamais chargés depuis le stockage navigateur.
`GET /api/web/v1/me` fournit exactement `userId,status,roles,emailVerified`, **pas de capacités détaillées ni d'indicateur d'enrôlement MFA**. ADMIN pré-MFA y est refusé. Au démarrage, attendre une identité établie avant de rendre le contenu privé. Ne pas déduire un état MFA d'un 403 générique ; une reprise de connexion fermée est nécessaire si le contexte de challenge a été perdu. Gérer l'indisponibilité séparément d'une session absente ; les rôles stockés ne suffisent pas à accorder un droit métier.

## Connexion locale
1. Obtenir le contexte CSRF selon la configuration serveur.
2. Soumettre le formulaire custom au mécanisme Spring.
3. Interpréter les statuts JSON réellement livrés : `AUTHENTICATED`, `EMAIL_PENDING`, `MFA_PENDING`. L'onboarding dépend ensuite du compte/dossier, pas d'un statut de login inventé.
4. Actualiser le contexte de session et CSRF si le serveur le renouvelle.
5. Naviguer vers un chemin interne autorisé.

Conserver les fonctions de gestionnaire de mots de passe : autocomplete username/current-password/new-password selon le champ. Le collage n'est pas interdit. Afficher/masquer le mot de passe avec un bouton accessible.

## CSRF : point d'intégration critique
Le backend présent utilise **HttpSessionCsrfTokenRepository**. `GET /api/web/v1/auth/csrf` renvoie `{headerName,token}` avec `headerName=X-CSRF-TOKEN`, sous la même session. Il ne livre pas un cookie XSRF lisible. L'intercepteur `webSessionInterceptor` utilise donc `CsrfContext` en mémoire, pas la convention XSRF cookie d'Angular. L'acquisition unique concurrente passe par `HttpBackend` pour éviter toute récursion ; le nom d'en-tête et le token sont validés avant emploi.
L'envoi est limité aux mutations sous `/api/web/v1`. Origines externes, URLs protocol-relative, chemins mobiles, encodés ou avec segments `.`/`..` ne reçoivent aucun secret web. Les cookies restent gérés par le navigateur en même origine, sans `Authorization` ou `withCredentials` inter-origines.
Le cookie de session reste HttpOnly : `MAINT_DEV_SESSION` en dev HTTP, `__Host-maint-session` Secure en production HTTPS. Relire le CSRF après login, MFA et réauthentification ; après logout, repartir d'un nouveau contexte anonyme si nécessaire. Ni session ni token CSRF dans localStorage.
La protection couvre connexion, déconnexion et commandes métier. Tester l'absence et l'altération du token, ainsi que son renouvellement après authentification et logout.
Un 403 n'entraîne jamais une désactivation globale de CSRF. Le code actuel `ACCESS_DENIED` peut représenter CSRF ou autorisation : ne pas prétendre distinguer ces causes sans preuve et ne pas rejouer automatiquement la mutation. Une éventuelle reprise bornée exige un contrat et un test explicites.

## MFA administrateur
MFA obligatoire, y compris après Google. Le serveur indique une session pré-MFA limitée aux challenges autorisés.
Enrôlement, code TOTP, codes de récupération, renouvellement et récupération suivent les commandes backend. Ne pas fabriquer un bypass par réinitialisation d'email.
La réussite du formulaire seule ne valide pas le MFA ; tester un accès direct à une API admin avant et après challenge.

FE03 propose un enrôlement manuel TOTP SHA-1, six chiffres / 30 secondes, clé masquée par défaut et supprimée de la vue/mémoire après confirmation, changement de session, fermeture ou cinq minutes au plus. Pas de QR transmis à un tiers ni de nouvelle dépendance de QR. Les dix codes de récupération sont présentés une fois et demandent un accusé de conservation hors ligne ; aucune copie automatique dans le presse-papiers, téléchargement ou persistance navigateur.

`/auth/securite` permet la preuve MFA récente, la réauthentification par mot de passe, le renouvellement des codes et le remplacement du facteur. Le serveur contrôle les preuves récentes ; une erreur 403 n'est pas requalifiée arbitrairement en « MFA nécessaire ». Le remplacement confirmé renvoie `REAUTHENTICATION_REQUIRED`, révoque les sessions et ramène à la connexion après conservation des nouveaux codes. Le renouvellement seul ne remplace pas le facteur.

Le serveur limite les opérations MFA à **5 par utilisateur et 20 par IP sur une fenêtre de 300 s**, enrôlement et renouvellement compris. L'interface affiche le `Retry-After` entier reçu, bloque les nouveaux envois durant cette période et ne rejoue pas automatiquement une commande.

## Google
Bouton actif uniquement si la capacité est activée dans la configuration publique et disponible côté serveur. Depuis la demande Login V1 du 12/11/2026, un emplacement visuel désactivé est affiché sur `/connexion`, explicitement nommé « Connexion Google non configurée », sans handler OAuth.
Navigation vers le début OAuth du backend ; échange de code, validation OIDC et association des comptes côté serveur.
Pas de mot de passe Google dans le formulaire ; pas de jeton OAuth en URL frontend ou localStorage.
Le callback aboutit à une destination contrôlée puis à une vérification de session. Une adresse email égale ne suffit pas à fusionner des identités.

**Réserve réelle FE03** : `googleLoginEnabled` reste strictement `false` ; `true` est refusé par la validation de configuration, pas ignoré silencieusement. Aucun parcours Google actif ni faux callback n'est livré. Le backend expose `POST /api/web/v1/auth/google/transactions` quand activé, mais son success handler web `/login/oauth2/code/google` renvoie un JSON de statut, pas un retour Angular validé. Les identifiants fournisseur/HTTPS et la recette OIDC réelle ne sont pas disponibles. Un flux backend-vers-page de retour contrôlée (ou une coordination de fenêtre de même origine explicitement contractée), puis `/me`/MFA, doit être convenu et testé avant activation. Ni jeton Google utilisé comme autorisation métier, ni protocole mobile adapté au web.

## Invitation et réinitialisation
Liens expirants, à usage unique. Les erreurs permettent une action adaptée sans révéler le compte d'un tiers.
Le backend place les tokens d'action dans le fragment du lien. `main.ts` les capture et retire fragment/query **avant le chargement de runtime-config et le bootstrap**. La page retire aussi un nouveau fragment reçu sur un document déjà ouvert. Le token opaque de 43 caractères est conservé une fois en mémoire pour la page concernée, jamais confirmé par GET. Le rechargement le perd ; l'utilisateur rouvre son email ou redemande un lien. Une seconde action reçue pendant un envoi n'est pas lancée automatiquement.
La réinitialisation conserve espaces et Unicode : 15–128 points de code, au plus 256 unités UTF-16 comme le backend, confirmation explicite et politique serveur finale. Après succès : purge de session locale et notification inter-onglets. Une confirmation email n'accorde jamais la validation administrative du client.
Un compte Google seul suit sa politique spécifique et ne reçoit pas silencieusement un mot de passe.

## Expiration et déconnexion
Centraliser les 401 de session pour éviter plusieurs redirections simultanées.
Distinguer 401, 403 et 429 ; respecter Retry-After pour les parcours limités.
Logout par la commande protégée du serveur, puis purge des caches, abonnements et vues privées.
Le retour après connexion est une route interne validée ; ne pas enregistrer d'URL porteuse de secret.

La génération locale de session invalide les requêtes privées, même lorsqu'elles attendent encore le CSRF, et rejette les résultats tardifs de login/MFA/réauthentification/`me`. Le contenu privé est masqué dès le début du logout. Un résultat réseau ambigu affiche `logoutFailed` et une reprise explicite, jamais une fausse déconnexion serveur réussie. `BroadcastChannel` transmet seulement le message `changed`, sans secret, pour fermer les autres onglets ; en cas d'indisponibilité de cette coordination optionnelle, l'API garde l'autorité.

## Contrat effectivement utilisé

Tous les POST sont protégés par le CSRF de session. Corps JSON, réponses sans cache ; détails d'erreur traduits à partir du statut/code stable, jamais injectés depuis le texte serveur.

| Méthode / chemin sous `/api/web/v1` | Entrée | Succès |
|---|---|---|
| GET `/auth/csrf` | — | 200 `{headerName,token}` |
| POST `/auth/login` | `{email,password}` | 200 `{status: AUTHENTICATED \| EMAIL_PENDING \| MFA_PENDING}` |
| GET `/me` | — | 200 `{userId,status,roles,emailVerified}` ; 403 avant MFA admin |
| POST `/auth/logout` | `{}` | 204 |
| POST `/auth/password-reset/requests`, `/auth/email-verification/requests` | `{email}` | 202 générique, aucune existence de compte révélée |
| POST `/auth/password-reset/confirm` | `{token,password}` | 204, sessions révoquées |
| POST `/auth/email-verification/confirm` | `{token}` | 204, validation administrative inchangée |
| POST `/auth/mfa/enrollment/start`, `/auth/mfa/replacement/start` | `{}` | 200 `{enrollmentId,secret,uri}` |
| POST `/auth/mfa/enrollment/confirm` | `{enrollmentId,code}` | 200 `{status,recoveryCodes}` ; AUTHENTICATED ou REAUTHENTICATION_REQUIRED |
| POST `/auth/mfa/challenge`, `/auth/mfa/recovery` | `{code}` | 204, session et CSRF renouvelés |
| POST `/auth/mfa/recovery-codes/replace` | `{}` | 200 `{status: AUTHENTICATED,recoveryCodes}` |
| POST `/auth/reauthenticate` | `{password}` | 204, session et CSRF renouvelés |

Types générés avec `@hey-api/openapi-ts@0.97.0` ; transport Angular séparé dans `IdentityApi`. Le frontend ne transforme ni les numéros longs métier ni les montants. Timeout des appels d'identité : 10 s ; aucun retry de mutation.

## Preuves FE03
Voir [résultats et limites](../verification/fe03/RESULTATS.md). La fixture isolée utilise le vrai Spring/Tomcat et un PostGIS éphémère, pas la base existante. Les scénarios visuels/erreurs utilisent des doubles HTTP exclusivement dans `e2e/`, jamais comme fallback de l'application. L'intégration Google activée, HTTPS de production et SMTP réel restent non exécutés.
