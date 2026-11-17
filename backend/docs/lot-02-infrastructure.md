# Lot 02 — Infrastructure locale, migrations et privilèges

27 octobre 2026 — réalisation locale du prompt `02_infrastructure.md`, après les lots 00/01. **Pas de déploiement ni d'approbation de production.** [Preuves, commandes et limites](preuves/lot-02-verifications.md).

## Comportement livré

- PostgreSQL/PostGIS et Mailpit sur `127.0.0.1` ; images réellement téléchargées et verrouillées par digest. Garage reste optionnel et non démarré.
- Neuf secrets persistants hors dépôt/OneDrive, clé MFA encodant 32 octets, permissions effectives vérifiées et relance sans remplacement. Aucun compte applicatif créé.
- Bootstrap SQL séparé, schéma `app`, connexion Flyway `maint_migrator` distincte du pool JPA `maint_app`. Le runtime n'a ni DDL/superuser, ni accès à l'historique Flyway.
- V1 limitée à `audit_event` technique, grants `SELECT, INSERT` explicites. Pas de table identité/session/métier, d'audit modifiable ou de grant implicite sur les futures tables.
- `ddl-auto=validate`, `open-in-view=false`. En production, entrée CLI one-shot du même JAR, puis JVM web avec Flyway désactivé et seulement le secret runtime. Le migrateur exige TLS vérifié hors mode local explicite et refuse les paramètres de contournement.
- Dockerfile non root UID/GID 10001, base imposée par digest, contexte réduit au JAR. Démarrage local testé avec filesystem readonly, tmpfs, capabilities supprimées et sans credentials migrateur.
- CI adaptée : intégration PostGIS obligatoire sur Linux/Docker, job Windows unitaire/ACL identifié séparément. Pas de skip masquant une absence de Docker.

## Contrats et fichiers

`pom.xml`, `src/main/resources/application*.yaml`, `db/migration/V1__initial_audit_schema.sql`, `bootstrap/database/DatabaseMigrationRunner.java`, mapping JPA `features/audit/.../AuditEventRow.java`, fixtures/tests PostGIS réels. Infra active dans `infra/`, génération/validation privée dans `scripts/`, Dockerfile et `.dockerignore` à la racine.

Le contrat HTTP du lot 01 reste inchangé ; aucun endpoint métier/connexion/Google ajouté. `/health` reste une santé du processus, pas une readiness DB. La JVM requiert cependant une connexion SQL valide pour finir son démarrage.

Le pack initial a été inspecté et sauvegardé avant intégration, hors secrets/`target`. Son ancien manifeste est conservé dans `preuves/MANIFEST-pack-initial.sha256`. Le manifeste racine suit maintenant les sources publiques courantes ; il ne faut plus annoncer que les 37 fichiers initiaux sont tous inchangés. Les références Spring/Dockerfile du pack restent historiques et intactes.

## Preuves SEC partielles

| Exigence | Contrôle et preuve obtenue | Limite |
|---|---|---|
| SEC-17 | Secrets privés/idempotents, ACL Windows réelles, runtime SQL sans DDL/historique/réécriture d'audit ; image sans montages migrateur | Pas de gestion/rotation de secrets en production ni d'identité |
| SEC-18 | Ports Compose loopback, URL du migrateur exigeant `verify-full` hors `dev`, tests de refus des contournements | Pas de TLS réel ni de proxy/staging testés ; HTTP/DB plaintext local seulement |
| SEC-19 | Digests téléchargés, versions exécutées, dépendances gérées par BOM, CI et inventaire public cohérents ; scans partiels dans la preuve | PostGIS : 6 constats critiques/126 élevés ; Temurin plus ancien que le patch repéré ; scans Java/images incomplets. Pas d'approbation production |
| SEC-20 | Audit SQL append-only pour le runtime et persistance démontrée après redémarrage ; historique conservé | Ni audit métier complet, ni alerte, sauvegarde/restauration ou résistance au superuser |

Ces contrôles ne couvrent pas les SEC intégralement et ne valent pas certification ASVS.

## Incidents corrigés et limites

- Windows : fichiers secrets initialement CRLF, le bootstrap conservait le retour chariot dans les mots de passe. Génération corrigée en LF, bootstrap tolérant les fichiers CRLF et regression Testcontainers. Les rôles du seul volume neuf créé par ce lot ont été alignés sur les valeurs existantes, sans supprimer le volume ni régénérer les secrets.
- La slice MVC avait réactivé l'utilisateur par défaut Spring : exclusion explicite, test négatif conservé ; aucun login temporaire laissé dans le socle.
- L'image Temurin officielle disponible exécute un patch plus ancien que le repère Adoptium. Écart documenté, pas de faux digest ni de substitution silencieuse.
- Manque d'espace disque pour la base Trivy native ; une alternative en RAM a produit les rapports PostGIS/Mailpit/Garage, mais le téléchargement de la base Java a épuisé les ressources et interrompu Docker. Les services locaux ont redémarré, les autres projets sont restés arrêtés. Une vérification Maven concurrente a échoué par manque de mémoire : seules les relances complètes réussies font foi. Aucun prune/nettoyage de données étrangères.
- Les résultats de scan ne sont pas approuvés pour la production : PostGIS présente des constats critiques/élevés, Garage n'offre pas d'inventaire exploitable ; scans des binaires Java/images Temurin/backend à compléter dans un environnement adapté. Le SBOM Maven (89 composants) et son scan ont été obtenus séparément, sans constat à cette date ; les sources ont aussi été scannées pour les secrets. Ces contrôles ne remplacent pas les scans d'images incomplets. Aucune exclusion ou dérogation de production n'a été créée.

## Suite

**Lot 03 : identité applicative locale web/mobile, MFA ADMIN et récupération**, avec migrations nouvelles, pas de réécriture de V1. Google facultatif et séparé. Revue humaine, remédiation des scans/versions, TLS, restauration et pentest restent nécessaires avant production.
