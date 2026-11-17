# Lot 03 — identité applicative

01 novembre 2026. Backend uniquement ; pas de frontend généré, pas de compte réel ou d'ADMIN par défaut. Le lot est réalisé par tranches vérifiées, conformément au prompt 03. **Ne pas considérer le lot complet tant que toutes les tranches et leur recette ne sont pas obtenues.**

Les chiffres, scans et mentions de lots futurs ci-dessous décrivent la clôture historique du lot 03. L'état courant inclut désormais [les parcours d'admission du lot 04](lot-04-onboarding.md), ses inscriptions explicites et [ses résultats/limites](preuves/lot-04-verifications.md) ; les règles d'identité de cette page restent applicables.

## Politique validée

Confirmation explicite du responsable le 01/11/2026 : mots de passe 15–128 points de code, session web 30 min d'inactivité / 8 h absolues, réauthentification récente 5 min ; accès mobile 5 min, refresh 7 j d'inactivité / famille 30 j ; email 24 h, reset 30 min. Argon2id : sel 16 octets, sortie 32 octets, m=19456 KiB, t=2, p=1 ; deux calculs simultanés par JVM par défaut.

Email : login ASCII insensible à la casse, affichage conservé, pas de suppression de points ou suffixes `+`. Pas de normalisation/troncature des mots de passe. [Source contrôlée des mots de passe courants](third-party/password-blocklist.md).

## État des tranches

| Tranche | État effectif |
|---|---|
| 03A — comptes/credentials et session web | Vérifiée : 78 Surefire + 32 Failsafe, aucun échec/erreur/skip, 01/11/2026 12:30 locale |
| 03B — vérification/reset et outbox minimale | Vérifiée : 79 Surefire + 39 Failsafe, aucun échec/erreur/skip, 01/11/2026 12:41 locale |
| 03C — MFA et bootstrap ADMIN | Vérifiée : 81 Surefire + 51 Failsafe, aucun échec/erreur/skip, 01/11/2026 13:07 locale |
| 03D — jetons opaques mobile | Vérifiée : 81 Surefire + 61 Failsafe, aucun échec/erreur/skip, 01/11/2026 13:12 locale |
| 03E — Google, liaison et handoff | Implémentée et vérifiée avec 19 tests OIDC locaux signés ; Google réel/staging non exécuté, option désactivée |
| 03F — revue et matrice finale | Revue interne locale et contrat vérifiés : 87 Surefire + 85 Failsafe = 172, aucun échec/erreur/skip, 01/11/2026 13:36 locale ; recette externe/revue indépendante absentes |

## 03A : garanties déjà vérifiées

- V2 additive après V1 inchangée : comptes UUID, credentials séparés, exactement trois rôles, sessions d'authentification et tables Spring Session JDBC, compteurs partagés PostgreSQL.
- Création de comptes CLIENT/TECHNICIAN uniquement via un port interne réservé au futur onboarding après ses vérifications ; aucun endpoint public permettant de choisir un rôle/état ou de créer ADMIN.
- Login JSON par un filtre Spring, stratégie de fixation et persistance explicite du SecurityContext, cookie HttpOnly/SameSite=Lax. Nom HTTP local distinct de `__Host-maint-session` réservé au mode sécurisé.
- Lecture courante de la session, version de sécurité, statut, rôles et vérification email à chaque requête. Suspension, révocation et expiration prennent effet sans attendre un nouveau login. Aucun hash/secret dans le principal sérialisé ou `/me`.
- Rôle ADMIN stocké sans preuve MFA : seulement `MFA_PENDING`, pas d'autorité ADMIN effective, `/me`/routes métier refusées. Pas de route métier anticipée pour les autres rôles.
- Cookie web refusé comme authentification mobile ; aucune API mobile de login ouverte avant 03D.
- Login/logout protégés par CSRF ; token pré-login inutilisable après rotation. Corps d'authentification bornés, champs inconnus refusés, réponses génériques et `no-store`.
- Quotas partagés avant Argon2 : 10 essais/identité/15 min et 60/IP/minute, SQL atomique, `X-Forwarded-For` non utilisé comme preuve d'IP. Audit sans email/mot de passe/token.
- Tests HTTP sur Tomcat et PostgreSQL réels : fixation/persistance/logout, délais, suspension, version, rôle injecté, contexte sans hash, isolation de canal et réservations de quotas concurrentes.

Sonde Argon2 locale : deux encodages et trois vérifications en **517 ms au total** sur le JDK local, pas un p95 ni un dimensionnement de production. BC 1.86 et starter Session JDBC vérifiés sur Maven Central ; pas de surcharge des versions du BOM Boot.

## Contrat web courant

Préfixe `/api/web/v1` :

- `GET /auth/csrf` → token CSRF et nom d'en-tête, `no-store` ; conserver le cookie serveur.
- `POST /auth/login` JSON `{email,password}`, en-tête CSRF → statut `AUTHENTICATED`, `EMAIL_PENDING` ou `MFA_PENDING` ; cookie renouvelé. Aucun access token web.
- `GET /me` → user_id, rôles/statut courants et indicateur email vérifié ; pas de secret. Refus avant MFA pour ADMIN.
- `POST /auth/logout` avec CSRF → 204 et révocation serveur ; GET n'effectue pas le logout.
- 03B ajoute les commandes POST `/auth/email-verification/requests`, `/confirm`, `/auth/password-reset/requests`, `/confirm`, toutes avec CSRF. Demande : 202 générique ; confirmation : 204 ou erreur expurgée. Aucun GET ne consomme un token.

## 03B : garanties vérifiées

V3 ajoute des tokens d'action de 32 octets aléatoires, empreinte SHA-256 seulement, but/destinataire/échéance/consommation. Renvoi invalidant l'ancien token. Consommation sous verrou utilisateur puis token ; reset révoquant toutes les sessions, changeant la version de sécurité, sans changer rôle/statut/MFA. Argon2 exécuté avant les verrous de consommation.

Notification préparée dans la même transaction, corps chiffré AES-256-GCM par JCA, nonce aléatoire et contexte liant message/destinataire/sujet. Clés externes versionnées avec ancienne clé de lecture ; la clé locale `mfa_encryption_key` sert de clé de protection avec contextes distincts. Le worker SMTP utilise des baux/`SKIP LOCKED`, des lots/retries bornés et n'envoie pas sous verrou métier. Livraison au moins une fois : un crash après acceptation SMTP peut créer un doublon, jamais promis exactement une fois. Corps retiré après succès/expiration ; échec final observable en base.

SMTP activé uniquement dans le profil local vers Mailpit. Les tests n'envoient qu'à un conteneur Mailpit éphémère et des adresses synthétiques. Hors dev, worker désactivé par défaut. Le profil web de production doit désormais monter aussi la clé de chiffrement externe ; ne pas la placer dans l'image.

Les liens de test ciblent l'origine frontend configurée, jamais une URL reçue dans un DTO. Le token d'action est dans le fragment du lien : le futur frontend doit le retirer immédiatement et confirmer par POST, sans analytique/tiers/Referer. Les pages Angular correspondantes ne sont pas créées par ce lot backend.

Les sept tests `IdentityActionsIT` couvrent le refus GET/sans CSRF/mauvais but, double consommation, renvoi, expiration, reset concurrent à un seul gagnant, maintien d'une suspension et révocation du contexte navigateur. La livraison SMTP vers un Mailpit éphémère est obtenue et le corps chiffré est retiré après acquittement. La panne SMTP locale contrôlée prépare une reprise ; aucun email externe n'est envoyé. `VersionedSecretCipherTest` vérifie nonce frais, contexte, altération et lecture après rotation de clé.

## 03C : MFA et procédure opérateur

La bibliothèque sélectionnée est `com.eatthepath:java-otp:1.0.0`, sans dépendances, licence MIT. Sources vérifiées : Maven Central et tag officiel [`java-otp-1.0.0`](https://github.com/jchambers/java-otp/tree/java-otp-1.0.0), commit `e5e4553783d767daf59df1fea33719181be5bf4b`. Configuration : TOTP 6 chiffres ASCII, SHA1, pas 30 s, fenêtre ±1 ; dernier pas consommé sous verrou PostgreSQL. L'enrôlement chiffré est lié à une session et expire après 5 min ; le remplacement demande mot de passe et MFA récents et révoque ensuite toutes les sessions.

Le bootstrap est un point d'entrée Java distinct, jamais une route HTTP. Il crée un **nouvel email nominatif uniquement** : un compte existant n'est ni fusionné ni promu. Exécuter après migrations, sur une machine opérateur contrôlée, depuis une vraie console interactive :

```powershell
$env:MAINT_BOOTSTRAP_SECRETS_DIR = "$env:MAINT_LOCAL_DIR/secrets"
$env:MAINT_JDBC_URL = 'jdbc:postgresql://127.0.0.1:5432/maintenance'
$env:MAINT_BOOTSTRAP_ENV = 'dev' # Développement local seulement, jamais en production.
java -Dloader.main=fr.entreprise.maintenance.bootstrap.identity.AdministratorBootstrapRunner -cp target/maintenance-api.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Saisie masquée du mot de passe et confirmation opérateur. Aucun secret en argument/environnement de la commande, aucun mot de passe par défaut. Le processus charge seulement `runtime_password` et `mfa_encryption_key` depuis des fichiers privés ; pas de serveur HTTP, migration ou worker SMTP. Hors développement, l'URL doit utiliser `sslmode=verify-full` avec certificat de confiance. Référence d'audit affichée, pas de données personnelles. Vérifier ensuite l'email dans l'application et enrôler le facteur ; le rôle stocké ne suffit jamais à obtenir un privilège.

Récupération MFA : utiliser un code de récupération hors ligne après authentification primaire, puis renouveler le facteur et les codes. Sans facteur ni code : **aucune route de désactivation par email/reset**. Ouvrir un incident, contrôler l'identité hors bande et faire valider l'action par un autre responsable habilité. Suspendre/révoquer le compte compromis ; créer un nouvel accès nominatif seulement après cette validation, avec nouvel email contrôlé et audit opérateur. Ne pas reprendre un ancien compte par SQL improvisé, ni copier un secret TOTP. La reprise de propriété des données métier éventuelles exige son propre lot/runbook ; elle n'est pas automatisée par le bootstrap.

Preuves locales : neuf tests `MfaIdentityIT`, vecteur public RFC 6238 via `LibraryTotpEngineTest`, trois tests `AdministratorBootstrapIT`. Ces derniers démarrent un contexte sans HTTP/worker et exécutent le point d'entrée du JAR dans des sous-processus non interactifs : refus avant démarrage sans console ou avec arguments. Le service de bootstrap est exercé sur comptes synthétiques ; **la saisie opérateur sur une vraie console n'a pas été effectuée** et aucun compte réel n'a été créé. Le test du scheduler vérifie son activation explicite uniquement quand le mail est activé.

## 03D : jetons mobiles vérifiés

V5 ajoute access/refresh opaques (256 bits aléatoires), empreintes uniquement, famille représentée par `auth_session` canal MOBILE, parent de refresh lié à la même famille par contrainte. Index unique empêchant deux branches non consommées. Rotation sous verrou utilisateur → famille → token, sans effet réseau sous transaction. La révocation détectée sur un refresh consommé est **committée avant** le retour 401, pour ne pas l'annuler accidentellement avec une exception.

Le canal mobile initial reste technicien : CLIENT/ADMIN refusés, y compris un technicien cumulant ADMIN. Il n'existe aucun jeton privilégié administrateur mobile. Connexion/reset/vérification sans cookie, `/me` limité au compte du Bearer ; email/statut/rôles/version revérifiés. Logout appareil prouvé par refresh ; logout global par access courant, révoquant aussi les sessions web. [Contraintes du client Flutter](contrat-flutter-auth.md).

Dix tests `MobileIdentityIT` sur Tomcat/PostGIS : empreintes et absence de cookie créé, refus du refresh comme access ou du Bearer sur web, rotation/rejeu/famille inconnue, vraie concurrence, réponse perdue après commit, TTL access/inactivité/absolue de famille, suspension/version/rôle actuels, refus ADMIN/CLIENT, cookie web ignoré, reset et logout global. Aucun endpoint métier n'est ouvert artificiellement pour les tests.

## Limites

Pas de déclaration « production ready » ou ASVS conforme. Les constats d'images du lot 02 restent ouverts ; pas de scan lourd relancé. CI distante, Google réel/staging, revue indépendante et pentest non démontrés. Les conteneurs/volumes préexistants d'autres projets ne sont pas modifiés ; tests sur conteneurs éphémères uniquement jusqu'ici.

## 03E/03F : contrat et clôture locale

[Configuration Google et parcours de liaison/handoff](google-optionnel.md), [revue de menace](securite/google-handoff.md), [matrice SEC et preuves détaillées](preuves/lot-03-verifications.md). Le starter OAuth2 Client est géré par le BOM ; tous les validateurs OIDC standards sont conservés. Les seuls endpoints fournisseur de la configuration réelle sont les endpoints Google officiels ; aucun faux identifiant réel ou secret livré.

V6 ajoute le triplet externe unique et les transactions immuables LOGIN/LINK, WEB/MOBILE. Le callback n'écrit aucune liaison tant que la preuve fournisseur n'est pas validée ; mobile LINK attend en plus le Bearer source et le verifier à l'échange. Une identité inconnue est refusée sans fusion email : son admission dépend du lot 04. V7 conserve l'ancienne forme INSERT des sessions en ajoutant un défaut à la nouvelle date, sans réécrire V4.

La revue finale a aussi ajouté le refus JSON des champs dupliqués/trailing tokens, le contrôle du cookie `__Host-`, le refus sans cache permissif en cas d'échec de lecture SQL, le retrait du corps mail après cinq échecs et un nettoyage borné des compteurs/éléments temporaires expirés. Le job ne décide jamais seul d'une expiration. Les historiques de refresh consommés restent conservés pour le contrôle de rejeu ; conservation complète et purge d'archives restent à formaliser au lot 12.

À cette clôture, OpenAPI décrivait les **35 chemins** livrés et avait passé une validation indépendante OpenAPI 3.1. Le build final local a réussi : **172 tests Java**, plus **6 tests Python**. Sources : scan léger Trivy secret à zéro constat ; neuf valeurs secrètes externes et la clé décodée absentes des sources et de chaque entrée du JAR de ce lot. SBOM alors généré de 114 composants et requête de métadonnées OSV sans avis renvoyé ; ce n'est pas un scan des images ni une assurance d'absence de vulnérabilité.

**Livraison locale de l'identité utilisable, mais lot entier non déclaré intégralement validé** : Google réel/App Links, TLS/proxy de staging, console opérateur réelle, nouvelle image runtime, CI distante, revue humaine et pentest restent non vérifiés. Aucun frontend ou lot métier suivant n'a été généré automatiquement.
