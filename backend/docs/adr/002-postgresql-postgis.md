# ADR 002 — PostgreSQL/PostGIS plutôt qu'une seconde base

- Date : 27 octobre 2026.
- Statut : retenu et mis en œuvre au lot 02 pour l'infrastructure et l'audit technique ; modèles et invariants métier encore à leurs lots.
- Exigences : SEC-02, SEC-13, SEC-15, SEC-16, SEC-17, SEC-20.

## Contexte

Le modèle relie comptes, utilisateurs, sites, équipements, contrats, tickets, missions, candidatures, affectations et versions de rapports. Une sélection doit réserver une place unique et conserver les conditions proposées sans double affectation, même en concurrence. La découverte par proximité doit être bornée et ne pas révéler le site privé.

## Décision recommandée

Une base PostgreSQL 18 avec PostGIS 3.6 ; patches et images candidats dans [versions.md](../versions.md). SQL relationnel pour les invariants, JSONB seulement pour les attributs variables validés et bornés. Photos/PDF dans un stockage objet privé ; références, autorisations et statut de scan en PostgreSQL.

| Besoin | PostgreSQL/PostGIS | MongoDB | Motif du choix ici |
|---|---|---|---|
| Relations et appartenance compte/site | FK, dont composites, et contraintes | Références ou imbrication ; intégrité à concevoir | Invariants relationnels directement exprimables |
| Sélection/confirmation atomique | Transaction, verrou mission, index unique partiel | Transactions ACID multi-documents et index disponibles | SQL rend l'invariant explicite avec les compétences retenues |
| Missions proches | `geography(Point,4326)`, GiST, `ST_DWithin` en mètres | GeoJSON, index `2dsphere` et requêtes de proximité | Les deux conviennent ; inutile de doubler la base |
| Rapports croisés, SLA, historique | Jointures/agrégations et révisions | Agrégation et dénormalisation possibles | Modèle relationnel naturel pour ces parcours |
| Données variables | JSONB avec schéma applicatif | Modèle documentaire souple | Pas le besoin dominant du produit |

MongoDB n'est dépourvu **ni de transactions ni de géospatial**. Il deviendrait une alternative sérieuse si les documents autonomes dominaient ou si une plateforme MongoDB maîtrisée existait déjà. Il n'y a pas de benchmark comparatif dans ce lot.

MySQL/MariaDB et SQL Server sont également crédibles. Leur adoption demanderait de revalider requêtes spatiales, contraintes, migrations, licences/coûts et exploitation, sans présumer de performances inférieures. H2 n'est pas une substitution pour prouver les garanties de PostgreSQL/PostGIS.

## Garde-fous à implémenter aux lots concernés

- Unicité candidature `(mission_id, technician_id)` ; index unique partiel d'affectation active pour `RESERVED`/`CONFIRMED`.
- Sélection, confirmation et expiration verrouillent d'abord la même mission, puis l'affectation ; échéance vérifiée dans la commande avec l'heure serveur. Aucun simple contrôle « existe déjà » ne remplace la contrainte SQL.
- FK compte/site/équipement cohérentes, versions optimistes, traduction des conflits attendus sans SQL divulgué. Une transaction invalidée par PostgreSQL n'est pas poursuivie.
- Argent en `BigDecimal`/`numeric`, jamais en flottants. Proposition client, montants publiés et conditions confirmées restent distincts et versionnés. **Aucune base HT/TTC ni obligation de montant n'est arrêtée dans cet ADR.**
- Flyway append-only ; runtime sans DDL/superuser ; PostGIS installé par une étape privilégiée maîtrisée. Migrations séparées du runtime en production.
- Point public approximatif distinct du point privé ; filtrage spatial sur le point public pour la découverte, sans distance permettant de retrouver le site caché.
- Tests Testcontainers PostgreSQL/PostGIS : migrations neuves/évolutions, droits SQL, appartenance et vraies transactions concurrentes ; sauvegarde/restauration avant production.

## Conséquences

Une seule technologie de données opérationnelle au départ, mais migrations, sauvegardes, index et capacité restent à exploiter. Le tag `postgis/postgis:18-3.6` est un candidat amd64 : sa présence au registre ne prouve ni le patch exécuté ni la validité des privilèges SQL. Avec PostgreSQL 18, le volume prévu est `/var/lib/postgresql`.

## Sources consultées le 27 octobre 2026

- [PostgreSQL 18 — contraintes](https://www.postgresql.org/docs/18/ddl-constraints.html).
- [MongoDB — transactions](https://www.mongodb.com/docs/manual/core/transactions/) et [géospatial](https://www.mongodb.com/docs/manual/geospatial-queries/).
- [PostGIS — ST_DWithin](https://postgis.net/docs/ST_DWithin.html).
- [Images du projet PostGIS](https://github.com/postgis/docker-postgis).
- Sources internes : cahier backend §§2, 6, 8, 12 ; cahier produit v0.3 §§4, 7 ; `SECURITE.md`.

## Réalisation du lot 02

PostgreSQL **18.6** / PostGIS **3.6.4**, image `linux/amd64` téléchargée par digest. Volume `/var/lib/postgresql`, données `/var/lib/postgresql/18/docker`. Le bootstrap privilégié installe PostGIS, crée les rôles et le schéma ; il n'est pas exécuté par la JVM.

`maint_migrator` possède `app`, crée V1 et l'historique Flyway. `maint_app` n'a ni superuser, ni création DB/rôle/temporaire/schéma/table ; il reçoit seulement `SELECT, INSERT` sur `audit_event`. Aucun grant par défaut sur les futures tables, séquences ou fonctions. L'audit est append-only **pour le runtime**, pas une protection contre le propriétaire SQL ou un superuser.

V1 ne contient pas de table utilisateur/session/métier et ne conserve pas de contenu libre ou secret. `actor_user_id` est nullable et sans FK tant que l'identité n'existe pas ; une migration ultérieure devra ajouter la référence sans réécrire V1. Le mapping JPA est immuable et Hibernate valide le schéma sans DDL.

Développement : Flyway possède sa connexion migrateur distincte du pool runtime. Production : même JAR, entrée CLI one-shot sans Spring/web, puis profil `prod` sans identifiants migrateur et Flyway désactivé. Le CLI refuse les URLs avec credentials, paramètres TLS alternatifs ou dupliqués ; `verify-full` obligatoire hors mode `dev` explicite. Connexion TLS réelle de production non testée.

Évolution : nouvelles migrations append-only, droits explicites et tests SQL négatifs à chaque lot. Appliquer les évolutions compatibles avant de démarrer la nouvelle JVM ; retour applicatif uniquement si le schéma reste compatible. Pas de migration descendante destructive, `clean`, `repair` ou `down -v` automatisé. Sauvegarde/restauration isolée à démontrer avant production.

[Tests, persistance et limites effectives](../preuves/lot-02-verifications.md). Aucun de ces contrôles ne démontre encore les invariants multi-comptes ou la concurrence d'affectation.
