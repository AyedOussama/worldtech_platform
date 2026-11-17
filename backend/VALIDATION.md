# État des vérifications — backend et pack

02 novembre 2026. La validation du pack doit être distinguée de celle du backend et de son approbation de production.

## État actuel après les lots 01–06 (recette locale)

- Compilation/tests Java 25 et démarrage Spring Boot exécutés ; migrations et privilèges testés sur PostgreSQL/PostGIS réels, pas H2.
- Digests téléchargés, Compose validé et services DB/Mailpit sains sur loopback ; neuf secrets persistants hors OneDrive, ACL Windows contrôlées.
- Migrateur one-shot (1 puis 0 migration), démarrage runtime non root sans secret migrateur et persistance après redémarrage du seul PostgreSQL démontrés.
- Résultats détaillés, versions, scans et limites : [lot 02](docs/preuves/lot-02-verifications.md). Le patch Temurin de l'image est inférieur au patch repéré au cadrage ; aucune validation production.
- Identité locale, session web/CSRF, reset concurrent, MFA/récupération et jetons mobiles vérifiés : [lot 03](docs/lot-03-identite.md). Sa clôture historique comptait **172 tests Java + 6 Python**, aucun échec/erreur/skip ; chiffres et scans datés conservés dans [sa preuve](docs/preuves/lot-03-verifications.md).
- Invitations/dossiers du lot 04 conservés, avec [leurs preuves historiques de 212 tests Java + 6 Python](docs/preuves/lot-04-verifications.md).
- Lot 05 conservé : comptes/membres/parc cloisonnés et [preuves historiques de 243 tests Java + 6 Python](docs/preuves/lot-05-verifications.md).
- Lot 06 : **138 Surefire + 155 Failsafe = 293 tests Java**, zéro échec/erreur/skip à **10:54:23 le 02/11/2026** ; six Python réussis. Contrats/calendriers immuables, tickets par site, forfaits CLIENT facultatifs HT/EUR, états/public-interne, concurrence réelle et révocation au rejeu. [Contrat](docs/lot-06-contrats-tickets.md), [preuves courantes](docs/preuves/lot-06-verifications.md). V14–V16 additives, montées depuis V7/V11/V13 ; POM et V1–V13 inchangés, volume Compose non migré.
- Approbation client disponible ; **approbation technicien bloquée sans justificatifs contrôlés**. Formulaires confirmés ; pièces exactes à préciser et chaîne privée/antivirus du lot 09 absente. Les cumuls CLIENT+TECHNICIAN ne peuvent pas contourner ce blocage par une décision client.
- OpenAPI courant 0.6.0 : **117 chemins/83 schémas**, validation indépendante et inventaire GET/POST/PUT/PATCH testés. Google/OIDC local uniquement, jamais fournisseur réel déduit du double. Aucun scan dépendances/images renouvelé.
- Non exécutés : frontends, Google réel, documents/bucket Garage, missions/affectations, calculs d'échéances/alertes/statistiques SLA, livraison complète des événements, charge cible, TLS/proxy/staging, restauration, revue indépendante et pentest. CI distante et nouvelle image lot 06 non lancées. Constats critiques/élevés et patch Temurin toujours bloquants pour la production.

Commandes : `mvnw verify` (Docker requis), `python -B -m unittest discover -s scripts/tests -v`, `python -B scripts/validate-pack.py` (PyYAML requis). Windows : `MAINT_BASH` peut désigner Git Bash pour la validation syntaxique Bash. [Procédure de démarrage](README-dev.md).

`MANIFEST.sha256` décrit les sources publiques courantes, pas `target`, des secrets ou des rapports bruts. Après revue d'une modification autorisée : `python -B scripts/public_manifest.py --write`, puis vérifier sans `--write`. Ce manifeste non signé contrôle cohérence/intégrité, pas authenticité ni sécurité. L'original 37 fichiers est archivé dans `docs/preuves/MANIFEST-pack-initial.sha256` et ne doit pas être utilisé pour valider les fichiers volontairement intégrés depuis.

## État historique du pack initial

Les sections suivantes décrivent l'environnement de préparation, **pas l'environnement de développement actuel**. Sortie d'origine : `docs/preuves/VERIFICATIONS-pack-initial.txt`.

## Contrôles du pack

Commande de référence : `python3 scripts/validate-pack.py`.

- Analyse syntaxique Python/Bash et lecture XML du POM sans résolution Maven.
- Lecture YAML avec refus des clés dupliquées et TOML de la configuration Garage générée.
- Trois services locaux, quatre images candidates, ports sur boucle locale et volume PostgreSQL 18.
- Retrait des dépendances d'identité obsolètes dans les configurations/prompts ; seules les notes historiques de reprise/changement et le contrôle négatif les nomment.
- Profil Google optionnel séparé du login local ; starter OAuth2 Client conservé, Resource Server externe supprimé.
- Génération temporaire de neuf secrets distincts, deuxième lancement sans remplacement et clé MFA décodant exactement 32 octets ; aucun secret dans le pack distribué.
- Un prompt par lot 00 à 14, référant aux décisions d'identité et exigences de sécurité actuelles.
- Rejet d'un tag latest ; aucun digest d'image inventé.
- Vérification du manifeste et de l'archive finale sans ancienne configuration active ni secret local.

Ces contrôles sont statiques ou portent sur les scripts de préparation. Ils ne démontrent pas qu'un compte soit protégé, qu'un rôle SQL fonctionne ou qu'un flux Google soit configuré correctement.

## Contrôles applicatifs non exécutés

Docker et Maven ne sont pas disponibles dans l'environnement de préparation ; le Java disponible est 17 et le projet cible 25. Aucun code métier ou module d'authentification n'est fourni dans ce pack de conception.

Non exécutés : pull/digest et démarrage des images, docker compose config, initialisation PostgreSQL réelle, migrations Flyway, compilation Maven, démarrage Spring Boot, tests HTTP et d'autorisation, connexion locale/Google, sessions/MFA/refresh/handoff, scan antivirus, concurrence métier, charge, restauration et pentest.

À exécuter dans les lots : vérifier versions effectives/privilèges, construire sur JDK 25, implémenter AUTHENTIFICATION.md, tester les exigences SEC-xx, configurer Google dans un environnement dédié, éprouver les parcours métier et réaliser les conditions de lancement de SECURITE.md. Distinguer résultats mockés et intégration réelle.

## Résultat de préparation

Une sortie 0 de validate-pack.py signifie que les fichiers de référence sont cohérents selon ces contrôles ; elle ne constitue pas une approbation de sécurité ou de production. Le résultat réellement obtenu est consigné dans le fichier VERIFICATIONS.txt joint au pack.
