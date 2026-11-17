# Contrat backend relevé en FE00

Inspection du 10/11/2026, en lecture seule. Source : `../backend/openapi/maintenance-api.json` et code Java présent, pas les exemples d'endpoints du pack.

## Référence et niveau de preuve

- OpenAPI **3.1.0**, API **0.6.0** ; fichier statique, pas de Swagger UI ou endpoint public OpenAPI annoncé.
- SHA-256 : `b5f122cc7ac464b544b1ee0f98b4e8ae379bf6521af2156e8b0ab6f73db1f1c5`.
- 117 chemins, 133 opérations : 53 GET, 74 POST, 3 PUT, 3 PATCH ; 83 schémas.
- 109 opérations web, 21 mobile et 3 santé/OAuth. Les opérations mobiles ne sont pas une API de remplacement pour Angular.
- 711 références locales résolues ; sécurité explicite sur chaque opération et CSRF déclaré sur chaque mutation web.
- Inventaire rapproché des annotations des contrôleurs et des routes login/logout/OAuth des filtres : 133 opérations de chaque côté, aucun écart détecté.
- **Limite : contrôle statique**, pas test HTTP, compilation Java, validation exhaustive du standard ou recette d'autorisation. Les résultats historiques backend ne sont pas des preuves frontend.

## Identité réellement implémentée

Tous les chemins relatifs de ce tableau ont le préfixe `/api/web/v1`.

| Méthode et chemin | Entrée / réponse et conséquence Angular |
|---|---|
| GET `/auth/csrf` | Public ; crée/conserve la session anonyme, JSON `{headerName:"X-CSRF-TOKEN",token:string}`, `no-store` |
| POST `/auth/login` | JSON `{email,password}` et CSRF ; 200 `{status:"AUTHENTICATED"\|"EMAIL_PENDING"\|"MFA_PENDING"}` ; cookie renouvelé, aucun jeton web |
| GET `/me` | 200 `{userId,status,roles,emailVerified}` uniquement ; statuts PENDING/ACTIVE/SUSPENDED/REJECTED, trois rôles connus ; ADMIN pré-MFA refusé |
| POST `/auth/logout` | Cookie et CSRF ; 204, révocation et suppression du cookie ; aucun logout par GET |
| POST `/auth/email-verification/requests` | `{email}` ; 202 générique, quotas ; pas de révélation d'existence de compte |
| POST `/auth/email-verification/confirm` | `{token}` ; 204, consommation unique ; aucun GET ne consomme le challenge |
| POST `/auth/password-reset/requests` | `{email}` ; 202 générique |
| POST `/auth/password-reset/confirm` | `{token,password}` ; 204, révocation des sessions ; ni MFA ni suspension levés |
| POST `/auth/reauthenticate` | `{password}` ; 204, rotation session/CSRF ; ne remplace pas le MFA ni l'expiration absolue |
| POST `/auth/mfa/enrollment/start` | 200 `{enrollmentId,secret,uri}` ; données TOTP sensibles, mémoire uniquement, pas de log/cache |
| POST `/auth/mfa/enrollment/confirm` | `{enrollmentId,code}` ; 200 `{status,recoveryCodes}` ; statut AUTHENTICATED ou REAUTHENTICATION_REQUIRED |
| POST `/auth/mfa/challenge` | `{code}` (6 chiffres) ; 204, rotation session/CSRF |
| POST `/auth/mfa/recovery` | `{code}` (code de récupération opaque) ; 204, usage unique et rotation |
| POST `/auth/mfa/replacement/start` | Facteurs récents exigés ; prépare un remplacement, pas une désactivation |
| POST `/auth/mfa/recovery-codes/replace` | 200, dix nouveaux codes à montrer une seule fois ; preuves récentes exigées |

Sources principales : `features/identity/api/WebIdentityController.java`, `WebIdentityActionsController.java`, `WebMfaController.java`, `WebAuthenticationSessions.java` et `bootstrap/security/IdentityWebSecurityConfiguration.java`, sous `../backend/src/main/java/fr/entreprise/maintenance/`.

### Session, proxy et CSRF

- Spring Session JDBC ; cookie `MAINT_DEV_SESSION` en dev HTTP, `__Host-maint-session` en production HTTPS : HttpOnly, Secure en production, Path=/, sans Domain, SameSite=Lax.
- Inactivité 30 min, échéance absolue 8 h ; opérations ADMIN sensibles avec authentification primaire et MFA de moins de 5 min. Ces paramètres sont serveur, pas des droits reconstruits par un timer Angular.
- `HttpSessionCsrfTokenRepository`, pas `CookieCsrfTokenRepository`. Angular ne trouvera pas de cookie `XSRF-TOKEN` selon le mécanisme par défaut. Prévoir en FE03 un service mémoire et un intercepteur borné aux mutations de l'API web de même origine. Ne jamais transmettre le token à une URL externe.
- Séquence : CSRF → login → nouveau CSRF → parcours email/MFA si requis → nouveau CSRF après rotation → `/me`. Une session technique/`AUTHENTICATED` ne signifie pas compte métier actif.
- Réponses 403 génériques via `ApiProblems` : le code `ACCESS_DENIED` ne distingue pas fiablement CSRF, rôle ou preuve MFA. Pas de refresh/rejeu automatique d'un POST sur cette seule base.
- `/me` ne publie ni `capabilities`, ni `mfaEnrolled`, ni détail du challenge. Après rechargement en pré-MFA, une réponse 403 ne suffit pas à reconstituer l'état ; prévoir une reprise fermée. Évaluer le parcours en FE03, sans ouvrir l'admin ni ajouter ici un endpoint de session fictif.
- Développement prévu : origine navigateur `http://localhost:4200`, proxy `/api` vers `http://127.0.0.1:8080`, sans réécriture du chemin. Les cookies restent sur l'origine navigateur ; aucune lecture du cookie HttpOnly. Proxy à tester en FE01/03, pas exécuté ici. Production : même origine derrière reverse proxy, domaines non choisis.
- Les liens email portent les jetons d'action dans le fragment. Les retirer immédiatement de l'URL puis confirmer par POST, sans télémétrie ni tiers sur ces pages. Pas de session ou challenge dans localStorage.

### Google : présent mais conditionnel

Les commandes `/auth/google/transactions`, `/auth/google/client-registration/transactions`, `/auth/identities/google/link` et `/unlink`, ainsi que les routes Spring `/oauth2/authorization/google` et `/login/oauth2/code/google`, sont présentes dans les sources et le contrat. L'inscription technicien web existe côté backend, mais n'ajoute pas de portail technicien au périmètre Angular.

`app.identity.google.enabled=false` dans la configuration source. L'option n'est pas attestée contre Google réel ; `googleLoginEnabled=false` reste la cible frontend. Si activée plus tard : transaction POST protégée avant redirection, callback et destinations contrôlés, MFA ADMIN maintenu. Le double OIDC historique ne constitue pas une recette fournisseur.

## Commandes métier existantes

Dans les lignes suivantes, les préfixes sont complets ; `/{id}` désigne seulement le paramètre adapté à la ressource, pas un endpoint générique à inventer.

| Domaine / lot frontend | Routes réellement présentes | Limites importantes |
|---|---|---|
| Admission / FE04 | `/api/web/v1/admin/client-invitations` et `/{id}/resend` ; `/api/web/v1/onboarding/client` avec register/accept-invitation/profile/submit/history ; `/api/web/v1/admin/onboardings/clients` avec approve/request-information/reject | Invitation nominative, versions/commandes explicites ; pas de choix public ADMIN |
| Revue technicien / FE04 | `/api/web/v1/admin/onboardings/technicians` et commandes de décision | Route approve présente mais refusée sans justificatifs contrôlés du futur lot documentaire ; ne pas simuler une validation réussie |
| Comptes / FE05 | `/api/web/v1/admin/client-accounts` ; lecture CLIENT sous `/api/web/v1/client/accounts` ; membres et droits de site côté ADMIN | CLIENT en lecture seule ; aucun site automatiquement accordé |
| Sites/équipements / FE05 | Sous les comptes : `/sites`, `/sites/{siteId}`, `/equipment`, `/equipment/{equipmentId}`, `/summary` ; création/PUT/PATCH/archivage réservés ADMIN | PUT remplace les facultatifs ; PATCH préserve absents/null. Ne pas traiter PATCH comme un effacement |
| Contrats / FE06 | `/api/web/v1/admin/client-accounts/{accountId}/contracts` et révisions ; lecture CLIENT `/api/web/v1/client/accounts/{accountId}/sites/{siteId}/contract-revisions` | Révisions immuables ; projection CLIENT limitée au site, sans notes internes |
| Tickets CLIENT / FE06 | `/api/web/v1/client/accounts/{accountId}/sites/{siteId}/tickets` : POST déclaration, GET liste/détail ; propositions/messages/history ; POST confirm-resolution/report-persistence | Compte/site autorisés obligatoires ; équipement facultatif ; pas de pièces avant le lot documentaire |
| Tickets ADMIN / FE06 | `/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/tickets` : GET ; POST qualify/start/wait-client/resume/resolve/cancel sur le ticket ; messages/internal-notes | Pas de transition libre par PATCH, notes internes sur routes distinctes |

### Données et erreurs à préserver

- ProblemDetail `application/problem+json` : `type,title,status,detail,instance,code,traceId`, plus `fieldErrors:[{field,code}]` au maximum 20. `X-Trace-Id` vient du serveur ; `instance` est une URN, pas l'URL avec ses paramètres.
- Préconditions de modification : `If-Match: "<version>"` exact, 400 si absent/mal formé, 412 si périmé ; ne pas remplacer par une précondition imaginée 428. Les versions de type entier n'ont pas toutes une borne JS sûre : conserver l'ETag opaque, ne pas incrémenter ni convertir aveuglément de longues versions ; qualifier le mapping lors de la génération.
- Commandes admission/ticket concernées : `Idempotency-Key` ; ticket : 16–100 caractères `[A-Za-z0-9_-]`, même intention/contenu = même clé, conservation technique 24 h. Les commandes de contrat n'offrent pas toutes ce rejeu : pas de retry POST générique.
- Un reçu ticket `{resourceId,version,state}` est historique. Relire détail/ETag après commande ou reprise, pas assimiler le reçu à l'état courant.
- Proposition CLIENT : `remuneration` et `expenses` en chaînes décimales ou null, EUR/HT/FIXED distincts ; zéro différent de non renseigné. Pas de `parseFloat`, calcul TVA/marge ou copie vers un montant ADMIN.
- Tickets : états OPEN/QUALIFIED/IN_PROGRESS/WAITING_CLIENT/RESOLVED/CLOSED/CANCELLED ; priorité demandée distincte de retenue ; fuseau du site figé à la déclaration.
- Liste tickets : page 0-based, 20 par défaut / 100 max, filtres `state`, `requestedPriority`, `equipmentId`, ordre création/UUID. Historique/messages/propositions : `after=-1`, `nextAfter`, taille bornée. Pas de paramètre `q` ou tri libre documenté.
- DTO de résumé : UUID, titre, priorités, état, version, date de création. Pas de référence humaine de ticket ou date d'actualisation livrée ; une adaptation de la maquette doit rester identifiée, sans créer ces données serveur.

## Proposé ou absent — ne pas déclarer intégré

| Besoin | État constaté / lot limite |
|---|---|
| Capacités détaillées, statut pré-MFA/enrôlement consultable | Absent de `/me` ; adapter le parcours fermé ou rapprocher un besoin backend en FE03 |
| `operationId` stables | 132 opérations sur 133 sans identifiant ; qualification du générateur/noms déterministes et nullable OpenAPI 3.1 avant client généré en FE03 |
| Liste globale de tickets/recherche/tri libre/référence/actualisé | Proposition UX, non implémentée dans le contrat actuel ; FE02 pour la maquette isolée, FE06 pour l'intégration |
| Mission/candidature/affectation/intervention | Absents des routes métier ; prérequis des FE07–08 |
| Upload, scan, documents privés, rapport attesté/PDF | Absents ; prérequis FE09–10 ; aucun faux téléchargement |
| Notifications utilisateur, SSE, statistiques, calcul d'échéances SLA exposé | Absents ; outbox technique et calendrier ne suffisent pas ; FE11–12 |
| Déploiement et identité de recette | Aucun accès de test fourni ou créé par FE00, aucun HTTP exécuté ; préparer environnement et comptes synthétiques avant FE03 |

Génération différée : sélectionner et tester un outil compatible Angular 22/TypeScript 6/OpenAPI 3.1 et sa licence au plus tard FE03 ; conserver le contrat réel comme entrée, son empreinte et une commande reproductible. Ne pas modifier le backend ni publier une version inventée d'OpenAPI pour faire passer le générateur.
