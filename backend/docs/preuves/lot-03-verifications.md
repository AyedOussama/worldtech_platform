# Preuves du lot 03 — identité

01/11/2026, Windows/JDK Oracle 25.0.5.0.2, Boot 4.1.2, Maven Wrapper 3.10.0, Docker Desktop Linux. **Pas d'approbation de production.** État fonctionnel : [lot 03](../lot-03-identite.md). Toutes les données de test sont synthétiques.

## E03-01 — périmètre et traçabilité

Snapshot public pré-lot sous `%LOCALAPPDATA%/Temp/opencode/maintenance-lot03-20261101/baseline`, sans secrets ni artefacts. Pas de Git initialisé, commit, push, déploiement, frontend ou email externe. Le volume Compose persistant reste au lot 02 ; les tests utilisent exclusivement leurs propres conteneurs éphémères PostGIS/Mailpit et un fournisseur OIDC local.

Migrations V1–V3 antérieures conservées ; V4–V7 additives. V7 ajoute un défaut à `authenticated_at` sans réécrire V4 pour accepter aussi la forme INSERT de l'ancien login V3. Le test SQL de compatibilité ne prétend pas rejouer un ancien binaire complet. Ordre de déploiement : migrations par le migrateur séparé, puis runtime sans secret migrateur. Pas de `clean`, `repair`, reset Git, suppression de volume ou changement des conteneurs d'autres projets.

## E03-02 — commandes et résultats

Politique confirmée explicitement par le responsable : password 15–128 points de code ; web 30 min/8 h ; preuve récente 5 min ; mobile access 5 min, refresh inactivité 7 j/famille absolue 30 j ; vérification 24 h/reset 30 min.

```powershell
$env:TEMP = Join-Path $env:LOCALAPPDATA 'Temp/opencode'
$env:TMP = $env:TEMP
$env:MAVEN_OPTS = "$env:MAVEN_OPTS -Xmx384m -XX:MaxMetaspaceSize=256m"
.\mvnw.cmd -B -ntp '-DargLine=-Xmx512m -XX:MaxMetaspaceSize=384m' verify
python -B -m unittest discover -s scripts/tests -v
```

Résultat Java final du 01/11/2026 à **13:36:00 heure locale** : **87 Surefire + 85 Failsafe = 172 tests, zéro échec/erreur/skip**, `BUILD SUCCESS`. Les 85 Failsafe comprennent 17 tests SQL, 3 bootstrap, 5 socle HTTP, 19 OIDC, 9 vérification/reset/outbox, 9 MFA, 11 mobile et 12 web. Résultats des tranches précédentes : 03A 110, 03B 118, 03C 132, 03D 142 tests réussis. La vérification ciblée de 03E a ensuite été incluse dans cette vérification complète.

Sonde temporelle locale de quatre essais par cas : médiane supérieure **103 ms compte présent/mauvais password**, **124 ms absent**. Échantillon minuscule dépendant de l'environnement, pas preuve d'équivalence statistique, de résistance à l'énumération temporelle ou de p95 en production.

Les six tests Python des scripts/ACL ont réussi le 01/11 à 13:33. Deux avertissements Maven HTTP 401 de métadonnées Flyway/Redgate subsistent sans bloquer la résolution. Les avertissements de dépréciation Jackson dans les tests ne sont pas des tests ignorés.

Échecs rencontrés et corrigés : compilation incohérente après édition simultanée au premier build 03A (relance clean cohérente), cookie web initialement lu par le repli mobile (chaîne stateless séparée), fixture Google ne remplaçant pas le bon bean (override de test explicite, pas de fausse configuration réelle), et lancement OAuth rejoué produisant une exception serveur au lieu d'un refus 401 expurgé (filtre de préparation dédié). Les tests responsables sont conservés, pas supprimés pour obtenir un build vert.

## E03-03 — matrice d'attaque et preuves SEC

| Exigence | Menace / contrôle | Test effectivement exécuté avant clôture | Limite |
|---|---|---|---|
| SEC-01/03 | Refus par défaut, DTO fermés, aucune création/promotion ADMIN HTTP | `WebIdentityIT.bodyIsBoundedAndPrivilegedUnknownFieldsAreRejected`, `MfaIdentityIT.bootstrapNeverPromotesAnExistingClientAndIsNotExposedOverHttp`, tests socle | Autorisations compte/site/mission des lots métier non encore présentes |
| SEC-04 | Argon2id versionné, coût explicite, Unicode/espaces, concurrence bornée par sémaphore | `Argon2PasswordsTest`, `IdentityDomainTest` | Sonde locale seulement, pas p95 sous charge/runtime cible ; nouvelle version de coût à migrer explicitement |
| SEC-05 | Quotas SQL partagés/atomiques avant Argon2, erreurs génériques, IP non tirée de X-Forwarded-For | `WebIdentityIT.distributedAttemptLimit...`, `rateReservationsAreAtomic...`, `MfaIdentityIT.mfaAttempts...` | Quotas applicatifs ne remplacent pas la protection volumétrique réseau ; sonde temporelle non statistique |
| SEC-06 | Fixation/persistance, cookie sécurisé, CSRF login/logout/MFA, délais et révocation | `WebIdentityIT`, `MfaIdentityIT`, `IdentityCookiePolicyTest` | HTTPS/SameSite/proxy réels non testés en staging |
| SEC-07/08 | Mobile sans cookie, hash des opaques, contrôle courant et rotation stricte | Dix tests initiaux `MobileIdentityIT` : concurrence, rejeu/inconnu, réponse perdue, échéances, suspension/version/rôles et logout | Cache hors ligne Flutter et comportement sur appareil non testés |
| SEC-09 | But/destinataire/échéance/usage unique, pas d'effet GET, reset sans activation ni suppression MFA | `IdentityActionsIT`, `MfaIdentityIT.passwordResetDoesNotRemoveMfa...` | Aucun compte externe seul créé dans ce lot ; reset ne lui ajoute pas silencieusement un password |
| SEC-10 | MFA avant tout privilège ADMIN, TOTP anti-rejeu, recovery sous verrou, remplacement révoquant les sessions | Neuf tests `MfaIdentityIT`, `LibraryTotpEngineTest`, Google ADMIN et handoff ADMIN refusé dans `GoogleIdentityIT` | MFA limité à ADMIN par choix produit, écart ASVS L2 documenté ; récupération opérateur réelle non effectuée |
| SEC-11 | OIDC signé, issuer/audience/temps/nonce/state standard, pas de fusion email, LINK récent revalidé | 19 tests `GoogleIdentityIT`, `GoogleSecurityConfigurationTest` | Double OIDC signé local seulement, pas l'intégration Google réelle |
| SEC-12 | Handoff <=60 s, S256, destination fixe, consommation atomique et Bearer source pour LINK | `GoogleIdentityIT.handoff...`, `mobileLinkWaitsForBoundBearerAndVerifier...`, rejet de destination et logs | Associations Android/iOS, interception sur appareil, proxy/APM réels non vérifiés |
| SEC-17 | Secrets chiffrés/externes, audit sans contenu secret, aucun token Google persisté | `VersionedSecretCipherTest`, tests de sessions/outbox/logs, bootstrap sans arguments/console | Rotation réelle de clés, revue indépendante et scans image complets non obtenus |
| SEC-18/20 | Cookies/headers, audit et refus sans stockage d'autorisation | Cookie/CSRF/quotas/audits et tests SQL ; `MobileIdentityIT.authorizationStorageFailureRefusesAccessInsteadOfTrustingAPreviousDecision` : 503 après retrait temporaire du seul droit SELECT dans sa fixture, puis 200 après rétablissement | TLS/proxy, alertes opérationnelles, restauration et pentest restent à faire |

Les exigences SEC-02 (objets multi-comptes métier), SEC-13–16 et les nouvelles autorisations métier restent à leurs lots. Ce tableau couvre l'identité actuellement présente, pas des routes métier fictives. Les identifiants exacts ASVS et écarts restent dans [le mapping partiel](../securite/asvs-sec.md) ; aucune certification n'est déduite des tests.

## E03-04 — contrat et contrôles statiques

OpenAPI 3.1 : **35 chemins, 22 schémas**, politiques de canal/CSRF explicites, DTO sensibles sans exemples de credentials. Validation indépendante réussie avec **openapi-spec-validator 0.9.0**, installé uniquement dans un environnement Python temporaire hors dépôt. Vérification syntaxique/structurelle du contrat, pas preuve des flux réseau. Le test Java `IdentityContractTest` compare les contrôleurs/filtres au contrat ; ne pas confondre son contrôle avec une validation exhaustive de toutes les réponses JSON possibles.

`scripts/validate-pack.py` a réussi après revue et régénération explicite du manifeste public : **201 fichiers**, aucun artefact ni configuration privée. Les neuf secrets temporaires du validateur restent stables entre deux exécutions et leurs ACL sont vérifiées. Comparaison au snapshot : 94 sources ajoutées, 23 sources existantes modifiées, aucune supprimée. Les références historiques restent sous `references/`, pas des profils actifs.

## E03-05 — secrets et dépendances

Scan léger des **sources** le 01/11/2026 à 13:33 avec Trivy natif **0.76.1**, `fs --scanners secret --skip-dirs target --skip-dirs .git` : **0 constat**. Rapport JSON hors dépôt `maintenance-lot03-20261101/reports/source-secrets.trivy.json`. Le corpus public SecLists est inclus dans les sources, sans exclusion cachée. Aucun gros scan d'image ou téléchargement de base Java relancé.

Dépendances ajoutées : starters Session JDBC/mail/OAuth2 Client gérés par le BOM ; BC **1.86** ; java-otp **1.0.0**, sources officielles et licence MIT vérifiées. Les versions transitives du socle ne sont pas mises à jour individuellement.

SBOM **CycloneDX 1.6**, plugin Maven 2.9.3, généré séparément après les tests à **13:39** : **114 composants Maven**, scope test exclu. SHA-256 `1f0338260e6bce4fee5f1ce377f57a82a30f67b6d96503e65f44c7c770638d41`, copie hors dépôt `reports/backend-dependencies.cdx.json`. Deux avertissements de mots-clés de schéma conservés. Requête légère des 114 purl/versions vers l'API publique **OSV querybatch** : **aucun identifiant d'avis renvoyé** ce jour, rapport `reports/dependency-osv.json`. Ce contrôle de métadonnées dépend de la couverture et de l'actualité OSV ; aucun scan de l'OS/JRE/image ni certificat d'absence de vulnérabilités n'en découle. Le résultat de scan du lot 02 n'est pas réattribué aux nouvelles dépendances.

Les neuf secrets privés connus et la clé MFA décodée ont été cherchés sans impression de leur valeur dans toutes les sources publiques et **chaque entrée du JAR** : aucune correspondance ; fixtures de tests absentes du JAR. JAR final SHA-256 `9f6add171c339e3399d114a36947131765d2cd7a0acb8cfcd62aec83802cacea`. Contrôle ciblé distinct du scanner, pas analyse exhaustive des couches d'image.

Contrôle Docker final en lecture seule : les conteneurs `taxikiwi-*` sont toujours arrêtés. Les deux services Compose `maintenance-dev` sont observés sains ; aucun arrêt/redémarrage n'a été demandé par cette vérification. Leur historique Flyway contient **une seule migration, version 1** : la base persistante n'a pas été migrée au lot 03. Aucun conteneur de test ne subsiste à la fin.

## Restant avant activation / production

- Google réel et App/Universal Links sur appareils ; TLS/cookies/CSRF/proxy/APM du domaine cible, suppression des paramètres sensibles dans les journaux.
- Console opérateur réelle et procédure de récupération MFA hors bande, revue indépendante du code/SQL et pentest authentifié de préproduction.
- Dimensionnement Argon2/quotas et charge, surveillance/alertes et rotation/restauration réelles. Conservation/purge complète des historiques à formaliser au lot 12 ; les refresh consommés sont conservés pour la détection de rejeu.
- Nouvelle image du lot 03 non construite/démarrée dans cette recette ; ancienne preuve non root du lot 02 ne décrit pas automatiquement ce nouveau JAR. CI distante non exécutée.
- Remédiation/retests des **6 observations critiques/126 élevées PostGIS du lot 02**, patch Temurin et scans d'images incomplets. Aucun scan local ne lève ces blocages.

Le lot backend peut être intégré progressivement ; **le lot entier n'est pas déclaré intégralement validé**, en raison de la recette Google réelle et des contrôles externes absents. Aucun déploiement n'est autorisé par ce compte rendu.
