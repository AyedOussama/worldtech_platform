# Pack backend — révision 1.1

27 octobre 2026. Interfaces de connexion Angular/Flutter propres ; authentification applicative Spring Security ; Google facultatif. Les trois rôles et règles métier du projet sont conservés.

**État actuel : lots 00–05 conservés ; contrats, calendriers et tickets du lot 06 implémentés et vérifiés localement (293 tests Java + 6 Python).** Couverture immuable, tickets par site autorisé, proposition CLIENT facultative HT/EUR historisée, échanges publics séparés des notes internes, résolution à distance puis confirmation CLIENT. Aucun paiement/facture/prix technicien déduit. **Approbation technicien toujours bloquée sans pièces contrôlées au lot 09 ; Google désactivé.** Aucun frontend, mission ou déploiement. [Démarrage](README-dev.md), [contrat du lot 06](docs/lot-06-contrats-tickets.md), [preuves et limites](docs/preuves/lot-06-verifications.md). Risques du [lot 02](docs/preuves/lot-02-verifications.md) conservés ; aucune approbation de production.

## Contenu et ordre de lecture

1. `Cahier_backend_SpringBoot_v1.md` : cahier complet, architecture, domaines, API, données, exploitation et lots.
2. `AUTHENTIFICATION.md` : comptes locaux, sessions web, jetons opaques mobile, Google, récupération et MFA.
3. `SECURITE.md` : exigences SEC-xx, scénarios négatifs, preuves et critères avant production.
4. `AGENTS.md` : instructions permanentes à intégrer au dépôt cible après lecture des instructions existantes.
5. `prompts/00_*.md` à `14_*.md` : quinze lots progressifs, révisés pour la présente architecture.
6. `infra/`, `scripts/` et `references/` : infrastructure locale, générateurs et fichiers de référence.
7. `UPGRADE.md`, `CHANGELOG.md` et `VALIDATION.md` : reprise d'une ancienne version, changements et état exact des contrôles.

Le pack de conception initial est conservé sous `references/` et dans les documents historiques. Le POM, Dockerfile et profils Spring à la racine/dans `src/` sont désormais implémentés. Aucun formulaire frontend, compte applicatif ou mécanisme d'identité n'est livré par les lots 01/02.

## Infrastructure locale

Préconditions : Docker Engine/Desktop et Compose opérationnels, JDK 25, Python 3.11+ (Windows : 3.11.10+ avec gestion effective de `mkdir(0700)`), accès HTTPS aux registres/Maven Central. Maven est fourni par le wrapper. Seuls PostgreSQL/PostGIS et Mailpit sont lancés par défaut. Garage S3 reste optionnel, non démarré/vérifié comme stockage par ce lot.

```bash
python3 scripts/init-dev.py
python3 scripts/lock-images.py
export MAINT_LOCAL_DIR="$(python3 scripts/init-dev.py --print-local-dir)"
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml config --quiet
docker compose --env-file infra/images.lock.env -f infra/compose.dev.yaml up -d --wait db mailpit
```

Pour Garage : ajouter `--profile documents` avant `up -d`. Endpoints : PostgreSQL `127.0.0.1:5432`, Mailpit UI `http://localhost:8025` et SMTP `127.0.0.1:1025`, Garage optionnel `http://localhost:3900`. Les ports sont liés à la boucle locale ; cette configuration n'est pas un déploiement de production.

Les neuf secrets sont générés **hors dépôt et hors OneDrive** : `%LOCALAPPDATA%/maintenance-api/dev/.local` sous Windows, `${XDG_STATE_HOME:-~/.local/state}/maintenance-api/dev/.local` sous Linux. Trois mots de passe SQL, une clé MFA encodant 32 octets, cinq secrets Garage ; aucun compte administrateur applicatif ni faux secret Google. Le script conserve les fichiers existants, vérifie les ACL Windows ou les répertoires POSIX 0700 et refuse les chemins OneDrive. Les fichiers sont montables par le conteneur non root, protégés sur l'hôte par les répertoires privés. Ne jamais les copier dans le dépôt ou un prompt.

PostGIS est imposé en `linux/amd64` ; l'émulation ARM n'a pas été testée. Volume PostgreSQL 18 : `/var/lib/postgresql`, données effectives `/var/lib/postgresql/18/docker`. Les quatre digests réellement téléchargés sont dans `infra/images.lock.env`, public et destiné au versionnement. `lock-images.py` conserve ce lock sauf `--refresh` explicite. [Versions réellement exécutées](docs/versions.md#5-suivi-du-lot-02).

L'initialisation SQL s'exécute uniquement sur un volume neuf. Les migrations utilisent maint_migrator ; le runtime maint_app sans DDL reçoit les droits explicitement accordés par chaque migration. Ne pas utiliser down -v pour appliquer un changement. En production, migrations séparées de la JVM et identifiants migrateur absents du runtime.

## Identité applicative du lot 03

Web : mécanismes Spring Security, session JDBC, CSRF, cookie HttpOnly/Secure et rotation login/MFA. Mobile technicien : jetons opaques locaux révocables, sans JWT applicatif ni authentification par cookie ; rotation stricte et rejeu révoquant la famille. ADMIN : vérification email et MFA obligatoires avant tout privilège, bootstrap hors HTTP et aucun compte par défaut. Contrats : [lot 03](docs/lot-03-identite.md), [Flutter](docs/contrat-flutter-auth.md), [OpenAPI](openapi/maintenance-api.json).

Google exige une activation explicite et un vrai client Google avec callback HTTPS exact. Fournir le secret réel via un gestionnaire de secrets ou fichier privé `google_client_secret`, et `MAINT_GOOGLE_WEB_CLIENT_ID` séparément. Ne pas coller ces valeurs dans un prompt. [Configuration réelle et recette de staging requise](docs/google-optionnel.md), [revue du handoff](docs/securite/google-handoff.md). Les fichiers `references/` restent historiques, pas des profils à copier aveuglément.

Angular et Flutter gardent leurs écrans d'inscription professionnelle : coordonnées, compétences, documents, brouillon puis soumission. L'admin valide. Une identité Google n'autorise pas les missions tant que le dossier n'est pas approuvé. Le parcours Google mobile ouvre une fenêtre système pour Google ; aucun mot de passe Google n'est collecté dans nos formulaires.

Le code courant utilise `app.identity.*` et `app.mail.*`, pas le contrat historique `app.auth.*` des références. Migrations V1–V7 append-only. Le runtime requiert `runtime_password` **et** `mfa_encryption_key` ; aucune clé intégrée à l'image. La création publique de comptes et l'admission des nouvelles identités Google attendent l'onboarding/invitation du lot 04.

## Intégration et validation

Les tranches et leurs preuves sont documentées sous `docs/`. Le Dockerfile utilise le JAR produit par `mvnw verify` et une base imposée par digest ; son contexte exclut tout sauf ce JAR. Le worker mail n'est activé localement que vers Mailpit. Aucun email externe ni migration du volume Compose persistant n'a été effectué pendant le lot 03.

Rejouer les contrôles : [README-dev.md](README-dev.md) et [VALIDATION.md](VALIDATION.md). `validate-pack.py` exige PyYAML et contrôle statiquement le pack, les profils et le manifeste public ; il ne remplace ni Maven, ni Docker/SQL, ni les scans. Aucun résultat de ces contrôles ne constitue une approbation de production.
