# Google — configuration optionnelle et activation

Google reste **désactivé par défaut** (`app.identity.google.enabled=false`). La connexion locale ne dépend pas du fournisseur. Aucun client/secret Google fictif n'est fourni comme configuration réelle ; la fixture OIDC signée est réservée aux tests.

## Configuration opérateur

Avant activation en staging, créer un client web confidentiel Google contrôlé et fournir hors dépôt :

- `MAINT_GOOGLE_WEB_CLIENT_ID` : identifiant réel du client web.
- `google_client_secret` : fichier privé monté via configtree/gestionnaire de secrets ; pas un argument CLI, une valeur frontend ou une valeur dans le YAML public.
- `MAINT_GOOGLE_REDIRECT_URI` : URI HTTPS exacte, chemin `/login/oauth2/code/google`, sans query/fragment, à enregistrer à l'identique chez Google.
- `app.identity.google.enabled=true` : activation explicite après la recette de staging.
- Pour mobile, `app.identity.google.mobile-destinations.<id>` : App Link/Universal Link HTTPS sur un domaine contrôlé. Aucun domaine par défaut et aucune URL libre reçue dans un DTO.

Les endpoints Google sont fixés dans le code, issuer `https://accounts.google.com`, scopes `openid email`, flux code serveur + PKCE S256. Pas de requête Gmail/Drive ou profil supplémentaire. Le backend ne conserve aucun ID/access/refresh token Google dans ses sessions ou tables. Les demandes transitoires (nonce/verifier) sont chiffrées, l'état stocké par empreinte et lié à une session navigateur. Le callback consomme cette transaction avant de traiter le résultat validé par Spring.

## Parcours web

1. Obtenir le cookie/token CSRF puis POST `/api/web/v1/auth/google/transactions`. Le navigateur ouvre le `launchPath` relatif reçu, utilisable une fois, transaction valable 5 min.
2. Google retourne sur le callback exact. Le backend retrouve seulement une identité externe **déjà associée** ; aucune recherche/fusion par email ni création de compte actif.
3. Succès : session locale et cookie renouvelé, réponse JSON. ADMIN reste MFA_PENDING, sans privilège avant la preuve locale TOTP/récupération.
4. Pour associer Google : depuis une session récente, POST `/api/web/v1/auth/identities/google/link` avec CSRF ; intention LINK liée au compte/session, contrôlée de nouveau après la preuve fournisseur. ADMIN exige MFA récent.
5. POST `/auth/identities/google/unlink` exige une preuve récente et un autre moyen de connexion valide. La dissociation révoque les sessions : reconnexion locale nécessaire.

Un LOGIN ordinaire inconnu échoue génériquement. Le lot 04 ajoute des transactions d'**inscription explicite** distinctes, décrites dans [le contrat d'admission](lot-04-onboarding.md) : CLIENT avec invitation et email OIDC vérifié destinataire, TECHNICIAN avec compte neuf en attente et brouillon. Aucun client actif, technicien approuvé ou ADMIN n'est créé par Google. Un email ou triplet externe déjà présent produit un conflit sans fusion.

## Parcours mobile

Flutter génère/garde un verifier PKCE (43–128 caractères), transmet le challenge S256 et l'identifiant de destination à POST `/api/mobile/v1/auth/google/transactions`. Ouvrir `launchPath` dans le navigateur système. Au succès OIDC, le navigateur reçoit un 303 vers la destination fixe avec un **code éphémère, pas un jeton**, dans le fragment, associé au transactionId. Durée maximale 60 s.

L'application retire immédiatement ce fragment et échange `{transactionId,code,verifier}` sur POST `/api/mobile/v1/auth/google/exchange`. Elle obtient les jetons opaques locaux uniquement après consommation atomique. Aucun access/refresh/ID token dans un lien ; aucun cookie mobile d'authentification. Si la réponse est perdue après commit, recommencer le login : le code consommé n'est pas réutilisable.

La liaison mobile part de POST `/auth/identities/google/link` par Bearer récemment réauthentifié. Le callback ne lie encore rien : l'échange exige **en plus** le Bearer du compte/session source et le verifier de l'application, puis répond `LINKED`, sans émettre de jeton sur la base du navigateur. La réauthentification locale est POST `/auth/reauthenticate` par Bearer. ADMIN/CLIENT ne reçoivent pas d'accès mobile dans cette version.

## Preuves et limites

[Revue de menace du handoff](securite/google-handoff.md). Les **19 tests locaux `GoogleIdentityIT` ont réussi**, inclus dans les 172 tests Java de la vérification finale : serveur OIDC éphémère, vrais JWT de test signés et JWKS local, échange code/PKCE réel, validateurs Spring standards conservés. Ces tests ne contactent pas Google. **Google réel, TLS/cookies du domaine cible et association Android/iOS ne sont pas validés** ; aucune activation de production autorisée.

Suivi lot 04 : cinq scénarios OIDC supplémentaires couvrent l'inscription explicite web/mobile, le client invité, le refus d'email différent/lien remplacé, les conflits sans fusion et l'absence de password ajouté par reset à un compte externe seul. Le handoff mobile reste lié à S256. Résultats courants dans la preuve du lot 04 ; les chiffres du paragraphe précédent sont les résultats historiques du lot 03, pas un total courant.
