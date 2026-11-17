# Cahier des charges technique — Backend de maintenance informatique

Version 1.1 · 27 octobre 2026 · Document de conception et de préparation au développement

Ce dossier définit le backend à construire pour une équipe de trois développeurs. Il constitue une base de réalisation et de recette. Il ne signifie pas que l'application est déjà implémentée, auditée ou déployée. Les choix marqués « recommandé » sont des décisions techniques proposées ; les points marqués « à arbitrer » restent des décisions produit. Les prompts du pack se suivent dans l'ordre et s'appliquent à un dépôt inspecté au préalable.

## 1. Résultat attendu et périmètre

L'entreprise administratrice, basée à Paris, organise des interventions de maintenance informatique dans toute la France. Le backend sert un portail Angular pour les clients, un backoffice Angular pour l'administration et une application Flutter pour les techniciens. Il n'y a pas d'application mobile client au lancement.

Il existe exactement trois rôles métier : ADMIN, CLIENT et TECHNICIAN. L'administrateur est également l'organisateur, le valideur et l'affecteur. Plusieurs employés de l'entreprise peuvent être administrateurs avec des comptes nominatifs ; cela ne crée pas un quatrième rôle métier.

### 1.1 Parcours de référence

1. Un administrateur invite un nouveau client par un lien personnel, expirant et à usage unique.
2. Le client complète ses coordonnées et son organisation, puis attend la validation de l'administrateur. Une demande de complément ou un refus est possible.
3. Le technicien s'inscrit, déclare ses compétences et sa zone d'intervention ; l'administrateur analyse et valide son profil.
4. Un client actif déclare un incident sur un site et, si applicable, un équipement. Il décrit le problème, joint des pièces et propose une rémunération et des frais.
5. L'administrateur qualifie la demande, peut la résoudre à distance ou créer une ou plusieurs missions. Il fixe librement les montants proposés aux techniciens, en conservant la proposition initiale du client.
6. Un technicien actif découvre les missions publiées compatibles et proches de lui, puis candidate. La candidature ne vaut pas affectation.
7. L'administrateur sélectionne un technicien. Recommandation : le technicien confirme explicitement la mission et la version de ses conditions dans un délai défini.
8. Le technicien intervient, consigne ses actions et pièces, prépare un compte rendu, puis recueille l'attestation du client.
9. L'administrateur contrôle le rapport et clôture la mission ; le ticket suit sa propre résolution. Le client consulte l'avancement et l'historique autorisés.

### 1.2 Limites de la première version

Inclus : identité et habilitations, validation des profils, comptes clients, sites, équipements, contrats et calendriers, tickets, missions, candidatures, sélection, rapports, pièces jointes, notifications, audit, recherche géographique, indicateurs opérationnels et préparation au fonctionnement intermittent de Flutter.

Exclus au démarrage : encaissement, reversement aux techniciens, facturation comptable, calcul de marge supposé, paie, optimisation automatique de tournées, suivi GPS continu, marketplace publique anonyme, microservices et moteur d'IA de dispatch. Ces fonctions nécessitent leurs propres règles et contrats d'API.

## 2. Décision base de données : pourquoi PostgreSQL ?

**Décision recommandée : PostgreSQL avec PostGIS, une seule base métier au lancement.** Le critère principal est la cohérence des relations et des changements concurrents, pas une supériorité universelle de performance.

Le modèle relie des clients, leurs membres, sites et équipements, des contrats, des incidents, des missions, des candidatures, des affectations et des rapports. Par exemple, l'affectation doit simultanément vérifier l'état de la mission, réserver son unique place active, enregistrer les conditions financières acceptées et préparer une notification. Les clés étrangères, contraintes d'unicité et transactions SQL correspondent directement à ce besoin. PostgreSQL documente ces contraintes [S1].

| Critère du projet | PostgreSQL + PostGIS | MongoDB | Conséquence pour nous |
|---|---|---|---|
| Relations entre comptes, sites, missions et rapports | Modèle relationnel et clés étrangères | Références ou documents imbriqués ; intégrité entre documents à organiser | Avantage de simplicité à PostgreSQL pour ce domaine |
| Une seule affectation active par mission | Transaction + verrou + index unique partiel | Transactions et index possibles, schéma à concevoir selon le modèle | Les deux sont capables ; SQL rend ici l'invariant très explicite |
| Recherche des missions proches | PostGIS, index spatial | GeoJSON et index 2dsphere | Les deux conviennent |
| Champs variables par type d'équipement | Colonnes stables + JSONB validé | Documents souples et validation de schéma possible | La souplesse seule ne justifie pas une seconde base |
| Statistiques et historiques croisés | Jointures et agrégations SQL | Pipelines d'agrégation, dénormalisation éventuelle | SQL est naturel pour les tableaux de bord attendus |
| Coût d'exploitation d'une équipe de 3 | Une technologie principale à maîtriser | Très viable si expertise MongoDB existante | Réutiliser une expertise réelle peut modifier la décision |

MongoDB prend bien en charge les transactions ACID multi-documents, y compris sur plusieurs collections ; prétendre l'inverse serait incorrect [S2]. Il propose aussi des requêtes et index géospatiaux [S3]. Je le privilégierais si le cœur du produit devenait une collection de documents largement autonomes, très variables, avec peu d'invariants entre documents, ou si l'entreprise disposait déjà d'une plateforme MongoDB bien exploitée. Ce n'est pas le besoin dominant décrit ici.

MySQL ou MariaDB restent des alternatives relationnelles crédibles, notamment si l'entreprise les exploite déjà. SQL Server peut également convenir dans un environnement Microsoft existant. Il faudrait alors comparer les besoins géographiques, le coût, les contraintes SQL et les compétences disponibles. Aucune de ces bases ne rend automatiquement une application plus rapide. Une comparaison sérieuse utilise les mêmes données, requêtes, index, règles de durabilité et ressources.

PostgreSQL stocke les relations, les états et les métadonnées. Les photos et PDF vont dans un stockage objet privé ; la base conserve leurs références et autorisations. JSONB sert aux attributs variables bornés et versionnés, pas à cacher tout le modèle dans une colonne.

## 3. Architecture retenue et rapport avec MVC

**Monolithe modulaire, couches inspirées de l'architecture hexagonale, API REST exposée avec Spring MVC.** Ces notions répondent à des questions différentes : le monolithe modulaire organise et déploie le système ; les couches séparent les responsabilités ; MVC traite les échanges HTTP.

Un seul artefact Spring Boot et un seul module Maven suffisent initialement. Les modules métier sont des packages contrôlés par des tests d'architecture. Ne créer des modules Maven distincts que si une difficulté concrète le justifie. Une équipe de trois personnes n'a pas besoin de supporter immédiatement des transactions distribuées et une douzaine de services.

### 3.1 Organisation

```text
fr.entreprise.maintenance
  bootstrap/                 configuration et assemblage
  shared/                    objets techniques minimaux, erreurs, Clock
  features/
    identity/
    clients/
    technicians/
    assets/
    contracts/
    tickets/
    missions/
    applications/
    interventions/
    documents/
    notifications/
    audit/
```

Dans chaque domaine :

| Package | Responsabilité | Règle |
|---|---|---|
| api | DTO, contrôleurs, contrat HTTP | Validation de forme ; pas de logique d'affectation |
| application | Cas d'usage, orchestration, autorisation métier, transaction | Dépend du domaine et des ports ; peut utiliser @Transactional |
| domain | États, invariants, décisions, objets valeur | Java sans dépendance Spring, HTTP ou JPA |
| infrastructure | Entités JPA, requêtes SQL, stockage objet, courrier | Implémente les ports ; ne décide pas des règles produit |
| publicapi | Interfaces nécessaires aux autres domaines | Contrats explicites ; pas d'accès direct à leurs repositories |

Les entités JPA restent dans l'infrastructure ; les objets du domaine sont distincts. Le mapping manuel est suffisant au début. Aucun framework de mapping supplémentaire sans besoin observé. Éviter les classes génériques BaseController/BaseService/BaseEntity qui masquent les règles ou couplent tous les modules.

Un contrôleur appelle un cas d'usage, pas un repository. Les événements et interfaces intermodules permettent la collaboration ; ils ne remplacent pas les contraintes transactionnelles. Une réservation et ses invariants restent dans une transaction métier cohérente. Les notifications passent par une outbox.

Spring MVC avec JPA/JDBC est cohérent avec les accès bloquants choisis. WebFlux n'apporte pas un gain automatique avec une base utilisée de manière bloquante. Pas de CQRS complet, Kafka, Redis, Elasticsearch, Kubernetes ou Spring Cloud par défaut. Ajouter un composant seulement après une mesure ou une exigence qui le justifie.

## 4. Versions et dépendances

Référentiel vérifié le 27/10/2026 dans les sources officielles. Revalider au début du développement : version stable ne signifie ni absence de vulnérabilité, ni compatibilité de toutes les bibliothèques tierces. Le lock d'images ne sera effectif qu'après résolution réelle des digests.

| Composant | Choix initial | Politique |
|---|---|---|
| JDK | Eclipse Temurin 25 LTS ; patch repéré 25.0.5+8 | Même majeur en local, CI et runtime ; corriger régulièrement [S5] |
| Spring Boot | 4.1.2 | Parent/BOM unique ; Java 25 dans sa plage de compatibilité [S4] |
| Spring Framework, Security, Hibernate, Jackson | Versions du BOM Boot | Ne pas les surcharger individuellement par habitude |
| Maven | 3.10.0 via Maven Wrapper | Ne pas utiliser Maven 4 RC en production [S6] |
| PostgreSQL | Branche 18 ; patch officiel repéré 18.6 | Vérifier la version réellement embarquée par l'image [S7] |
| PostGIS | Branche 3.6 ; patch repéré 3.6.4 | Image candidate postgis/postgis:18-3.6 [S8] |
| Flyway | Version gérée par Boot | starter-flyway + module PostgreSQL ; migrations SQL [S9] |
| springdoc-openapi | 3.2.0 | Compatibilité Boot 4.1 documentée ; UI non publique en production [S10] |
| Testcontainers | Version gérée par Boot, 2.0.5 dans le référentiel vérifié | Noms d'artefacts de la génération 2 ; pas de mélange avec 1.x [S9] |
| ArchUnit | 1.5.1 | Dépendance de test explicite [S11] |
| Authentification | Spring Security géré par Boot | Compte local, session web, Bearer opaque mobile, Google optionnel [S12] |
| Bouncy Castle | bcprov-jdk18on 1.86 | Fournisseur requis par Argon2PasswordEncoder ; version directe à revalider/scanner [S22] |
| Mailpit, local uniquement | 1.32.0 | Capture des courriels ; aucun envoi à des personnes par ce dossier [S13] |
| Garage, local uniquement | 2.5.0 | Stockage objet S3 de développement, mode un nœud [S14] |
| Docker Compose | Plugin Compose maintenu compatible avec la spécification utilisée | Commande docker compose ; pas de clé racine version obsolète [S15] |

Spring Boot 4 utilise des générations récentes des API et bibliothèques. Ne pas recopier des imports ou starters de tutoriels Boot 2/3 sans vérifier la documentation correspondante ; notamment Jakarta et les changements Jackson. Privilégier les starters officiels et le BOM plutôt qu'une liste de patches transitive maintenue à la main.

Ajouter les bibliothèques S3, antivirus ou PDF au lot qui en a besoin, après vérification de leur version stable, licence, compatibilité Java/Boot et maintenance. Une bibliothèque PDF doit supporter le mode d'usage retenu sans accès réseau implicite depuis du HTML fourni par le client. Le pack n'invente pas des numéros de versions pour des intégrations non encore validées.

## 5. Identité et sécurité applicative

### 5.1 Authentification applicative et interfaces propres

Décision validée : pages de connexion, récupération et inscription développées dans Angular et Flutter. Spring Security assure l'authentification côté backend. Aucun serveur d'identité séparé, aucun thème de connexion imposé. Google est un fournisseur optionnel ; la connexion locale reste utilisable sans configuration Google.

Le compte interne est app_user(user_id UUID). Ses identifiants locaux (email vérifié et password_hash Argon2id) et ses identités externes (provider, issuer, sub) sont des associations distinctes. L'email n'est pas la clé primaire d'une identité externe et ne déclenche jamais une fusion automatique. Utiliser les bibliothèques Spring, JCA/BC et les validateurs OIDC ; ne pas créer d'algorithme de mot de passe, de signature ou de serveur OAuth propriétaire.

Angular : session serveur par Spring Session JDBC, cookie HttpOnly, Secure en production, SameSite et retour Google testés, CSRF sur les mutations, y compris login/logout. Le formulaire propre transmet les identifiants à un mécanisme d'authentification Spring correctement configuré. La stratégie de session, la prévention de fixation et la persistance du SecurityContext sont conservées et testées ; un simple AuthenticationManager appelé dans un contrôleur ne suffit pas [S12, S16]. Les tables de session sont créées par Flyway, pas automatiquement en production.

Flutter : formulaire natif email/mot de passe vers l'API dédiée ; accès par jetons opaques aléatoires, courts et révocables. La base conserve les empreintes, les expirations et la famille de session. Un filtre Bearer/AuthenticationProvider local vérifie le jeton et la session à chaque requête ; il ne traite pas ces jetons comme des JWT. Le statut du compte est également contrôlé. Durées proposées : accès 5 min, inactivité refresh 7 j, session absolue 30 j. Rotation refresh atomique, détection de réutilisation et révocation de famille ; détails et scénarios de concurrence dans AUTHENTIFICATION.md.

Google web : OAuth2 Login standard Spring Security, code échangé côté serveur, contrôles OIDC/state/nonce et callbacks exacts. Aucun mot de passe Google n'est collecté par nos formulaires. Google mobile utilise un navigateur système et un retour à l'application par code éphémère à usage unique lié à un vérificateur PKCE ; aucun access/refresh token dans un lien ou les logs. La conception de cette remise de code est détaillée dans AUTHENTIFICATION.md et doit être revue et testée avant activation. L'API métier n'accepte pas directement un jeton Google comme jeton applicatif.

Chaînes de sécurité séparées : API mobile /api/mobile/v1/** Bearer opaque sans authentification par cookie ; web /api/web/v1/** session et CSRF ; endpoints OAuth navigateur isolés pour état/nonce/callback ; refus de repli. Les endpoints publics d'inscription et de login sont limités explicitement. Aucune chaîne globale permitAll ou csrf.disable pour résoudre un problème de frontend. Les services métier sont communs aux deux canaux.

Argon2id avec sel aléatoire, coût explicite et mesuré. Point de départ minimal retenu : mémoire 19 456 KiB, 2 passes, parallélisme 1 ; renforcer selon la capacité mesurée et protéger la concurrence de vérification. Pas de mot de passe en clair, de SHA-256 seul pour un mot de passe ou de fallback NoOp. SHA-256 reste adapté à l'empreinte de jetons aléatoires à forte entropie ; ce n'est pas le même usage [S23].

MFA obligatoire pour ADMIN, y compris après Google. L'état pré-MFA n'est pas une session privilégiée. Le lot identité implémente le cycle complet : enrôlement, vérification, limitation des essais, codes de récupération hashés, renouvellement et récupération sans bypass par simple email. Recommandation MVP : TOTP via bibliothèque maintenue, secret chiffré avec clé externe versionnée, prévention du rejeu ; détail dans AUTHENTIFICATION.md. Un compte nominatif est promu ADMIN par un outil d'exploitation contrôlé et audité, jamais via le formulaire public. Aucun compte administrateur ou mot de passe par défaut livré dans Git.

Vérification d'email, réinitialisation et association de Google utilisent des jetons/challenges expirants à usage unique, une vérification d'identité adaptée et des événements audités. Un reset réussi révoque les sessions et refresh. Un compte Google seul ne reçoit pas de mot de passe implicitement par un simple parcours de reset. Un technicien peut créer son accès via Google puis remplir le dossier professionnel sur nos écrans ; son profil reste en brouillon/en attente jusqu'à décision administrative.

La suspension et la perte d'habilitation sont contrôlées lors des requêtes suivantes, même si le cookie ou le jeton n'a pas expiré. La validation des profils, sites, affectations et documents reste une responsabilité du backend métier. Référentiel de vérification : SECURITE.md, avec preuves attendues et critères de mise en production. Ce document ne prouve pas que ces mécanismes sont déjà implémentés ou audités.

### 5.2 Autorisation métier

| Acteur | Autorisé | Interdit |
|---|---|---|
| Visiteur | Démarrer inscription technicien ; compléter une invitation valide | Lister clients, tickets ou missions privées |
| CLIENT en attente | Consulter/corriger son onboarding | Déclarer un incident ou accéder aux données d'un autre compte |
| CLIENT actif | Accéder aux objets de ses comptes et sites autorisés | Choisir le technicien, modifier le montant publié ou obtenir les notes internes |
| TECHNICIAN en attente | Compléter son profil | Voir/candidater aux missions disponibles |
| TECHNICIAN actif | Découverte anonymisée, candidature, accès aux missions qui lui sont affectées | Voir contacts/adresse exacte des missions non affectées ou autres candidatures |
| ADMIN | Valider profils, qualifier, publier, sélectionner, contrôler et administrer | Effacer silencieusement historique financier, attestation ou audit |
| Compte suspendu/refusé | Accès limité au statut et aux voies de correction prévues | Continuer les opérations métier protégées |

Les règles combinent rôle, état métier, compte client, site, affectation et état de la ressource. @PreAuthorize sur un rôle ne suffit pas. Appliquer le périmètre dans la requête SQL avant pagination, comptage ou agrégation ; ne pas charger toutes les lignes puis filtrer en Java. Tous les endpoints, recherches, exports et téléchargements doivent respecter cette règle. Un UUID imprévisible n'est pas une autorisation. La prévention des accès par changement d'identifiant est une exigence centrale [S18].

Une liste blanche de champs par commande évite le mass assignment. clientAccountId, approved, role, publishedRemuneration ou assignedTechnicianId ne sont jamais appliqués automatiquement depuis un DTO public. Vérifier qu'un site et son équipement appartiennent bien au même compte que le ticket.

### 5.3 Invitations, abus et secrets

Lien recommandé : jeton aléatoire de 32 octets minimum, hash stocké en base, destinataire lié à l'invitation, durée proposée 48 h configurable. Consommation atomique, usage unique, invalidation des anciens liens lors d'un renvoi. Une invitation expirée ou déjà utilisée ne recrée pas de compte. La vérification d'identité et l'approbation administrative sont deux étapes distinctes. Ne pas connecter automatiquement une identité existante à un autre compte parce qu'un email non vérifié correspond.

Limiter par IP et identité les tentatives d'inscription, renvois, uploads, recherches géographiques et exports. Politique cohérente en multi-instance, au reverse proxy ou dans un stockage partagé ; pas uniquement un compteur mémoire par JVM. Retour 429 avec Retry-After adapté. Protéger aussi les fonctions coûteuses des utilisateurs authentifiés.

Secrets par fichiers montés/gestionnaire de secrets ; jamais dans Git, image, logs, réponse HTTP ou URL de télémétrie. TLS aux frontières et sur les connexions de production selon l'infrastructure. CORS sur une liste d'origines explicites ; production web sous la même origine si possible. Ne jamais combiner credentials et origine générique. Configurer les en-têtes proxy uniquement pour les proxies de confiance.

## 6. Domaines et modèle de données

Les noms ci-dessous expriment les concepts ; les migrations détaillées sont produites progressivement. Toutes les tables importantes ont un identifiant UUID, timestamps UTC, version de concurrence si mutable et contraintes adaptées. Ne pas ajouter des colonnes d'audit par copier-coller sans définir leur signification.

| Domaine | Agrégats et relations essentielles |
|---|---|
| Identité | app_user UUID, identifiants locaux, external_identity(provider, issuer, sub), sessions et refresh hashés, MFA, vérification/reset et audit |
| Clients | client_account BUSINESS ou INDIVIDUAL, client_member, invitation, demande de validation |
| Sites et équipements | site rattaché à un compte, contacts privés, équipement rattaché au site, attributs JSONB validés |
| Techniciens | profil, compétences référencées, zone/rayon, disponibilités déclarées, décisions de validation |
| Contrats | contrat, version de couverture, sites/équipements couverts, période, calendrier et règles SLA |
| Tickets | incident, équipement optionnel, priorité demandée et priorité retenue, échanges publics/internes, propositions client |
| Missions | mission, ticket optionnel, site obligatoire, compétences, fenêtre d'intervention, publication, conditions financières versionnées |
| Candidatures | une candidature logique par couple mission/technicien, historique de décision et version des conditions vue |
| Affectations | réservation/confirmation, technicien, mission, expiration, snapshot des conditions acceptées |
| Interventions | actions, temps déclarés, brouillon et versions du rapport, attestations et revue administrative |
| Documents | objet privé, auteur, parent métier, taille, hash, type détecté, statut antivirus et visibilité |
| Exploitation | outbox, idempotency_record, notifications, audit_event, exécutions de jobs |

Un ticket peut avoir zéro à plusieurs missions ; une mission peut être préventive et rattachée à un contrat sans ticket. Une mission correspond à une visite/un créneau ; un retour sur site peut être une autre mission liée. Ne pas imposer un équipement à un incident de réseau général.

Les membres d'un client peuvent être limités à certains sites par une relation d'habilitation ; cette portée ne constitue pas un nouveau rôle métier. Un particulier utilise un compte de type INDIVIDUAL. L'adresse email d'un utilisateur ne remplace pas le compte client.

### 6.1 États et transitions

| Objet | États principaux | Conditions |
|---|---|---|
| Onboarding | DRAFT, SUBMITTED, NEEDS_INFORMATION, APPROVED, REJECTED | Décision administrative motivée et tracée ; suspension distincte après activation |
| Ticket | OPEN, QUALIFIED, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED, CANCELLED | Réouverture tracée selon délai à arbitrer ; clôture mission ≠ résolution automatique du ticket |
| Mission | DRAFT, PUBLISHED, AWAITING_CONFIRMATION, PLANNED, IN_PROGRESS, AWAITING_REVIEW, COMPLETED, CANCELLED, EXPIRED | Publication par admin ; confirmation par technicien ; clôture après revue |
| Candidature | SUBMITTED, WITHDRAWN, SELECTED, NOT_SELECTED | Raison et historique ; retrait interdit après confirmation sans procédure de désistement |
| Affectation | RESERVED, CONFIRMED, DECLINED, EXPIRED, RELEASED | Au plus une RESERVED ou CONFIRMED par mission |
| Rapport | DRAFT, SUBMITTED, ATTESTED, APPROVED, CHANGES_REQUESTED | Les versions soumises/attestées ne sont jamais réécrites |
| Document | PENDING_UPLOAD, QUARANTINED, CLEAN, REJECTED, EXPIRED | Aucun téléchargement métier avant CLEAN |

Chaque transition est une commande explicite, avec acteurs autorisés, état source, préconditions, événements et effets secondaires. Pas de PATCH générique permettant de passer arbitrairement de DRAFT à COMPLETED. Une nouvelle version du rapport après demande de correction conserve l'ancienne attestation attachée à l'ancienne version.

Expiration d'une réservation : verrouiller la mission, vérifier l'heure serveur et l'état, libérer l'affectation, décider de republier ou de revenir en brouillon selon la politique. Le job d'expiration et la confirmation prennent les mêmes verrous dans le même ordre. Confirmer à l'instant limite ne doit pas contourner le contrôle simplement parce que le job n'est pas encore passé.

### 6.2 Montants : exigence essentielle

La proposition client et le montant publié par l'administrateur sont des données différentes. Exemple : le client propose 150 EUR de rémunération et 20 EUR de frais ; l'admin publie 180 EUR et 35 EUR. Conserver les quatre valeurs et leurs auteurs. Le technicien voit 180/35, pas nécessairement la proposition initiale. Le système ne déduit pas que le client sera facturé 215 EUR, ni une marge.

Modèle recommandé : client_financial_proposal immuable/versionné et mission_financial_revision contenant rémunération publiée, frais publiés, devise, base de calcul, auteur, date et motif. L'affectation confirmée référence une révision et conserve un snapshot. Le client consulte sa proposition et les données commerciales explicitement destinées à son espace ; il ne reçoit pas les informations internes par défaut.

Décision confirmée pour la **proposition CLIENT au lot 06** : forfaits HT en EUR, rémunération et frais forfaitaires distincts, chacun facultatif même hors contrat ; null n'est jamais zéro. Ni calcul TVA, facture ou marge. BigDecimal/numeric, bornes et échelle contrôlées sans arrondi silencieux ; montants JSON en chaînes comme "180.00". Le modèle proposé initialement `numeric(12,2)` est remplacé par numeric + CHECK d'échelle, car PostgreSQL arrondirait une entrée trop précise avant un CHECK. Les conditions publiées ADMIN restent distinctes et à leur lot. [Contrat et bornes réellement livrés](docs/lot-06-contrats-tickets.md).

Avant publication, l'administrateur fixe librement les montants. Après publication, une modification crée une révision, avertit les candidats et exige qu'une sélection/confirmation se fasse sur la version courante. Après confirmation, recommandation : proposer un avenant et recueillir un nouvel accord ; tant qu'il n'est pas accepté, les conditions confirmées restent la référence. Aucune mutation silencieuse d'un accord existant.

### 6.3 Contraintes et concurrence

Exemple de garde-fous SQL à adapter à la migration réelle :

```sql
CREATE UNIQUE INDEX uq_application_mission_technician
  ON app.mission_application (mission_id, technician_id);

CREATE UNIQUE INDEX uq_assignment_active
  ON app.mission_assignment (mission_id)
  WHERE state IN ('RESERVED', 'CONFIRMED');

ALTER TABLE app.mission_financial_revision
  ADD CONSTRAINT ck_positive_money
  CHECK (remuneration >= 0 AND expenses >= 0);
```

L'unicité d'une affectation active est garantie en base ET dans le cas d'usage. La sélection verrouille d'abord la ligne mission, recontrôle le profil, la candidature, les dates et la révision financière, puis crée la réservation et son événement d'outbox dans une transaction. L'ordre de verrouillage est constant. Ne pas garder un verrou pendant un appel HTTP, un envoi mail ou une génération PDF.

Pour les modifications ordinaires : version optimiste @Version et ETag/If-Match ; une version HTTP périmée donne 412. Un conflit métier tel qu'une mission déjà réservée donne 409. Une violation SQL attendue est convertie proprement sans divulguer de SQL. Ne pas attraper une erreur de contrainte PostgreSQL puis continuer dans la même transaction déjà invalidée ; utiliser une opération SQL adaptée ou rejouer l'ensemble de la transaction dans une limite définie.

Les relations site/compte et équipement/site doivent aussi être garanties par des clés étrangères composites ou un mécanisme équivalent testé. Un simple CHECK ne peut pas valider arbitrairement des données d'une autre table [S1]. Les clés étrangères, colonnes de filtre et parcours sont indexés selon les requêtes ; ne pas indexer aveuglément chaque colonne.

## 7. API et contrats

REST JSON, commandes explicites, DTO distincts selon acteur, OpenAPI versionné dans Git. Base /api/web/v1 pour session et /api/mobile/v1 pour Bearer ; ne publier sur chaque canal que les ressources nécessaires. Une éventuelle API partenaire sera un contrat séparé.

| Ressource / commande indicative | Acteur | Attendu |
|---|---|---|
| POST /auth/login, /auth/logout ; GET /auth/csrf côté web | Utilisateur | Formulaires propres, CSRF et session serveur ; réponses JSON sans redirection d'API inattendue |
| POST /auth/refresh et /auth/logout côté mobile | Session mobile | Rotation/révocation de jetons opaques ; aucune authentification par cookie |
| /auth/email-verification, /auth/password-reset, /auth/mfa | Selon challenge | Expiration, usage unique, limites et absence de privilèges avant MFA |
| /oauth2/authorization/google et callback dédié | Navigateur | Login Google contrôlé ; association au compte existant seulement après preuve |
| POST /admin/client-invitations | ADMIN | Créer/renvoyer invitation ; résultat sans jeton brut dans les logs |
| POST /onboarding/client/submit | Invitation + identité vérifiée | Compléter puis soumettre ; ne pas activer automatiquement |
| POST /onboarding/technician/submit | Identité vérifiée | Soumettre profil ; pas de droit de candidature |
| POST /admin/onboardings/{id}/approve ou request-information ou reject | ADMIN | Transition motivée et auditée |
| GET /me | Identité authentifiée | Rôle/statut et capacités courantes, pas de jetons |
| /client/sites, /client/equipment | CLIENT actif | Gestion dans le périmètre autorisé |
| POST /client/tickets | CLIENT actif | Incident et proposition financière initiale |
| POST /admin/tickets/{id}/qualify | ADMIN | Priorité retenue, contrat/SLA, résolution à distance ou missions |
| POST /admin/missions/{id}/publish | ADMIN | Validation complète, révision financière et événement |
| GET /technician/missions/nearby | TECHNICIAN actif | Données de découverte anonymisées et bornées |
| POST /technician/missions/{id}/applications | TECHNICIAN actif | Candidature idempotente liée aux conditions vues |
| POST /admin/missions/{id}/select | ADMIN | Réservation atomique du candidat |
| POST /technician/assignments/{id}/confirm ou decline | Technicien sélectionné | Confirmation en ligne avec révision et délai |
| POST /technician/missions/{id}/start | Technicien confirmé | Transition contrôlée, heure serveur et éventuelle heure déclarée distinguées |
| /documents/upload-intents, /documents/{id}/complete | Acteur habilité sur parent | Upload privé, quota, scan avant visibilité |
| /interventions/{id}/report-drafts puis /report-versions | Technicien affecté | Brouillon mutable, version soumise immuable |
| POST /report-versions/{id}/attest | Client habilité | Attestation sur cette version précise |
| POST /admin/missions/{id}/complete | ADMIN | Revue et conditions de clôture |
| GET /client/tickets/{id}/history | CLIENT du compte/site | Événements publics seulement |
| GET /admin/statistics | ADMIN | Agrégats bornés, définitions documentées |

Les chemins sont des propositions à figer dans le lot concerné, pas une spécification OpenAPI déjà générée. Les annotations OpenAPI ne remplacent pas les tests d'autorisation.

### 7.1 Conventions

Validation Bean Validation sur les DTO et validation métier dans les cas d'usage ; bornes de longueur, nombre de pièces, pages et fenêtres de dates. Pas d'entité JPA sérialisée. Retour 201 et Location à la création, 202 pour un traitement asynchrone réellement suivi, 204 si pas de corps. 400 forme invalide, 401 identité absente/invalide, 403 opération interdite, 404 ressource absente ou masquée selon une politique cohérente, 409 conflit métier, 412 version périmée, 413 corps trop volumineux, 429 limite atteinte.

Erreurs au format application/problem+json selon ProblemDetail, avec type stable, status, title, code métier, erreurs de champs et traceId ; aucune stacktrace, SQL, secret ou donnée d'un autre compte [S19]. Le frontend traduit les codes stables, sans analyser le texte du message.

Dates techniques Instant UTC ; dates planifiées conservées avec le fuseau IANA du site et conversion explicite. Ne pas supposer que toute la France partage Europe/Paris si les territoires ultramarins sont inclus. Tester les changements d'heure. API d'intervalle [début, fin), ordre stable et Clock injecté dans les règles temporelles.

Listes paginées, taille par défaut 20, maximum proposé 100. Curseur stable pour historiques/découverte ; filtre et tri sur listes blanches. Un curseur ne remplace pas les contrôles de périmètre. Export volumineux asynchrone, accès temporaire contrôlé, pas de GET retournant toute la base.

### 7.2 Idempotence et retries

Pour soumission, candidature, sélection, confirmation, dépôt final de rapport et finalisation d'upload : Idempotency-Key bornée, liée à l'acteur authentifié et à l'opération. Stocker empreinte du corps, état, résultat autorisé et expiration. Même clé/corps : même résultat ; même clé/autre corps : 409. La réservation de la clé et l'effet métier sont cohérents transactionnellement. Réautoriser lors d'un rejeu, notamment après suspension ou réaffectation. Ne pas restituer un ancien résultat sensible à un acteur qui a perdu son droit.

Les retries automatiques sont bornés et réservés aux opérations sûres ou munies d'un mécanisme d'idempotence. Un timeout HTTP n'est pas la preuve que l'action a échoué. Les appels externes sont hors transaction SQL et ont leurs propres timeouts.

## 8. Géographie, données privées et mobilité

PostGIS geography(Point,4326), coordonnées dans l'ordre longitude/latitude, GiST et ST_DWithin en mètres pour limiter les candidats [S20]. La distance affichée au technicien avant affectation est calculée depuis une position publique approximative distincte de la position privée du site. Une distance précise vers le site caché permettrait de le retrouver par recoupement.

Départ : ville/base déclarée ou position ponctuelle donnée volontairement par le technicien. Aucun suivi continu requis. Rayon recommandé 10–200 km configurable, résultats plafonnés, filtres compétences et dates ; regroupement de marqueurs par zone/zoom si nécessaire. Une carte n'a pas vocation à recevoir toutes les missions françaises en une réponse. Distance à vol d'oiseau et durée de trajet sont deux notions différentes ; le routage routier est une intégration future ou explicitement budgétée.

Les DTO de découverte n'incluent ni adresse exacte, ni nom du contact, téléphone, instructions d'accès, documents privés ou commentaire libre qui les révèlerait. L'administrateur rédige un résumé public nettoyé. Le technicien confirmé accède au détail nécessaire à sa mission. Après réaffectation, les prochaines lectures sont refusées ; les données déjà téléchargées hors ligne ne peuvent pas être rappelées magiquement.

Un géocodeur est appelé côté serveur avec clé protégée, quotas, cache compatible avec ses conditions et résolution manuelle en cas d'ambiguïté. Son choix commercial et ses droits d'utilisation restent à valider. Pas de transmission de dossiers complets à un service de cartographie.

## 9. Documents, rapports et fonctionnement intermittent

Stockage objet privé derrière un port applicatif S3-compatible. Taille maximale proposée : 10 Mo par photo/PDF, 10 pièces par commande, quotas par compte et mission à préciser. Limite également au proxy, détection du type réel, extension contrôlée, nom généré par serveur, hash et scan antivirus. Interdire les exécutables et traiter les PDF/images comme des données non fiables. Nettoyer les métadonnées de localisation des photos quand elles ne sont pas nécessaires.

Un upload signé éventuel est limité à une clé et une durée, puis le serveur vérifie taille/hash/présence avant de mettre en quarantaine. Une réussite d'upload n'est pas une validation antivirus. Un scanner indisponible laisse l'objet en quarantaine, avec retry borné et alerte ; il ne le marque pas CLEAN. Purger les uploads abandonnés après délai défini.

Chaque téléchargement réévalue l'autorisation métier. Une URL présignée délivrée reste exploitable jusqu'à son expiration : utiliser une durée très courte ou un proxy de téléchargement si une révocation immédiate est exigée. Aucun bucket public. Ne pas accepter une URL arbitraire à télécharger côté serveur ; prévenir SSRF et restrictions réseau contournées.

Le rapport contient actions effectuées, observations, résultat, matériel, temps déclarés, pièces jointes, auteur et heures serveur. Chaque soumission crée un snapshot immuable et un hash. L'attestation associe l'identité et la qualité du signataire, le texte d'accord, la version et son empreinte, la date serveur et le contexte de recueil. Une signature dessinée seule ne démontre pas une identité vérifiée et n'est pas une signature qualifiée. Un refus ou une absence du client suit un parcours explicite de revue administrative.

PDF produit à partir de la version figée, en tâche asynchrone ; contenu et liens chargés depuis des sources contrôlées. Pas de rendu HTML libre récupérant des URL externes. Le PDF et la preuve d'attestation gardent leur lien à la même version. Une correction génère une nouvelle version ; elle n'écrase pas le rapport signé.

Flutter peut conserver les missions confirmées autorisées et des brouillons locaux chiffrés selon les possibilités de la plateforme. Le backend fournit un manifeste de ressources/version pour rafraîchir ce périmètre. Les commandes hors ligne sont rejouées individuellement avec idempotence et version attendue. Confirmation, sélection et changements financiers nécessitent une connexion. Une mission annulée/réaffectée produit un conflit explicite au rejeu ; aucun écrasement automatique.

Pour le MVP, préférer un rafraîchissement borné de l'ensemble des missions affectées et des versions à un protocole de synchronisation universel. Un simple updatedSince oublie des suppressions, pertes d'autorisation ou transactions validées dans un ordre différent des horodatages. Un vrai delta futur exigera un curseur fiable, des événements/tombstones et des règles d'expiration.

## 10. Notifications, tâches, SLA et audit

Écrire l'événement d'outbox dans la même transaction que la transition métier. Worker avec lots bornés et verrouillage SKIP LOCKED ou bail équivalent, retries exponentiels, nombre maximal, file d'échec observable et reprise administrative. Envoi au moins une fois ; dédupliquer les notifications internes par eventId/destinataire/canal. Un crash après acceptation SMTP mais avant acquittement SQL peut créer un doublon email : ne pas promettre exactement une fois.

Événements : invitation, demande de complément, décision de profil, ticket reçu, mission publiée pertinente, sélection, expiration, confirmation, changement de conditions, annulation, rapport soumis, attestation, clôture, alerte SLA. Ne pas envoyer un email à chaque animation d'interface. Courriel V1 et notifications internes ; push Flutter au lot prévu si indispensable. Pour le rafraîchissement web, polling borné au début ; SSE seulement si une mesure et un besoin le justifient.

Les jobs sont sûrs en multi-instance, idempotents et observables : expiration des liens/réservations, rappels, scan et purge, génération PDF, SLA, agrégats. Ne pas dépendre d'un unique @Scheduled non coordonné ni maintenir une transaction pendant tout un lot.

SLA : distinguer prise en compte, début d'intervention et résolution ; calendrier ouvré, jours fériés, fuseau, couverture et exceptions du contrat versionnés. Un statut WAITING_CLIENT ne suspend pas automatiquement le SLA : seule une règle du contrat le décide. Conserver les événements et pauses justifiés. Les indicateurs définissent dénominateur, fuseau, période et traitement des tickets réouverts ou annulés.

Audit append-only au niveau applicatif, accès restreint, horodatage serveur : acteur, action, objet, compte, ancienne/nouvelle valeur utile ou référence de version, motif et traceId. Auditer particulièrement approbation, suspension, prix, publication, affectation, changement de droits, attestation, clôture et exports sensibles. Ne pas copier jetons, mots de passe, pièces complètes ou commentaires secrets dans l'audit. Une table d'audit seule n'est pas une garantie d'inviolabilité contre un administrateur de base : renforcer l'export/immutabilité d'exploitation si exigé.

## 11. Performance et disponibilité

Budgets initiaux à confirmer par mesure : 1 000 techniciens, 10 000 missions dans le jeu de charge, 100 utilisateurs simultanés, p95 < 500 ms pour lectures et commandes courantes hors upload/génération PDF/services tiers. Cible d'exploitation proposée : disponibilité 99,5 % mensuelle, RPO 15 min, RTO 4 h. Ce sont des objectifs à financer et tester, pas des garanties acquises.

Mesurer temps de réponse, taux d'erreur, saturation des pools, verrous SQL, CPU/mémoire, taille de l'outbox et âge du plus vieux message, temps de scan, taux d'expiration et échecs SLA. Logs JSON avec traceId ; pas d'identifiants utilisateurs comme labels de métriques à cardinalité illimitée. OpenTelemetry/Micrometer selon la plateforme retenue, sans exporter les données personnelles du ticket.

JPA : open-in-view=false, associations LAZY, projections/DTO pour listes, transactions courtes, pas de N+1. SQL explicite ou JDBC autorisé pour spatial, verrouillages et tableaux de bord quand plus clair. Examiner EXPLAIN (ANALYZE, BUFFERS) sur des données réalistes en environnement de test, en sachant qu'ANALYZE exécute la requête. Les indexes doivent correspondre aux filtres, tris et cardinalités observées.

Hikari : point de départ 10 connexions par instance, puis ajustement mesuré ; compter instances, workers, migrations et réserve d'administration face à max_connections. Limiter requêtes longues et timeouts transactionnels. Les virtual threads sont une option après mesure, pas un moyen de contourner la capacité de la base. Les files et exécuteurs asynchrones restent bornés ; refuser ou différer proprement sous surcharge.

Pas de cache avant preuve du besoin. Un cache doit intégrer périmètre d'accès, version et invalidation ; ne pas servir une donnée privée d'un client à un autre. Les DTO de découverte sont particulièrement adaptés à une projection légère et bornée.

## 12. Environnements, Docker et migrations

Trois profils distincts : dev, test/intégration, production. Le pack contient une infrastructure de développement, pas une plateforme de production à publier sur Internet. Les ports exposés sont liés à 127.0.0.1. Le backend n'est ajouté au Compose qu'après création de son artefact et des migrations d'identité. Le profil Google est optionnel.

Services locaux : PostgreSQL/PostGIS et Mailpit ; Garage optionnel pour le lot documents. Aucun service d'identité externe n'est nécessaire pour la connexion email/mot de passe. Les secrets sont générés localement sans valeur prédéfinie. Les volumes persistent les données. Ne jamais exécuter down -v sur des données utiles sans décision explicite.

L'image officielle candidate PostGIS 18-3.6 est actuellement amd64 ; le Compose local l'indique explicitement. Sur ARM, l'émulation peut servir au développement ; valider une image native ou une offre managée adaptée avant la production. Avec ces images PostgreSQL 18, le volume est monté dans /var/lib/postgresql, et non l'ancien chemin de volume /var/lib/postgresql/data [S8].

Le script lock-images.py tire les tags candidats et résout les digests réellement disponibles. Les digests ne sont pas inventés dans ce dossier. Le verrou doit être revu, scanné et actualisé ; un digest ancien n'est pas plus sûr parce qu'il est immuable. Vérifier SELECT version(), PostGIS_Full_Version() et java -version après résolution ; un tag de branche n'atteste pas le patch demandé.

Base : superutilisateur réservé à l'initialisation, rôle migrateur propriétaire du schéma app, rôle runtime limité au DML et à l'usage des objets nécessaires. PostGIS est installé par une opération privilégiée maîtrisée, pas par le rôle applicatif. Les scripts init Docker ne s'exécutent que sur un volume neuf ; une modification exige une migration explicite.

Flyway versionné, checksum contrôlé, migration appliquée jamais réécrite. hibernate.ddl-auto=validate ; jamais update/create en production. Développement : Flyway peut utiliser ses propres identifiants. Production : étape de migration séparée, puis application avec identifiants runtime seulement et Flyway désactivé dans la JVM. Les tables Spring Session JDBC éventuelles font aussi l'objet de migrations ; pas d'auto-création incontrôlée au démarrage.

Déploiement : image backend multi-étape ou artefact CI reproductible, JRE 25, utilisateur non root, limites de ressources, système de fichiers racine en lecture seule si compatible, /tmp borné, capabilities supprimées, endpoint de gestion non exposé publiquement. Aucun Docker socket monté dans l'application. Les références Dockerfile/config du pack sont à intégrer et tester, pas des conteneurs applicatifs déjà vérifiés.

## 13. Tests et définition de terminé

| Famille | Scénarios obligatoires |
|---|---|
| Domaine | Transitions interdites, argent, confirmation expirée, suspension, conditions financières |
| Intégration SQL | PostgreSQL/PostGIS réel par Testcontainers, migrations neuves et montée de version ; pas H2 comme preuve de compatibilité |
| Sécurité | Deux comptes clients et sites, deux techniciens, ADMIN, états en attente/suspendus, Google altéré/mauvais issuer/audience, sessions/révocation/MFA, refresh concurrent, CSRF, pièces privées, compteurs/exports |
| Concurrence | Deux sélections simultanées, double candidature, confirmation contre expiration, publication contre changement de prix, rejeu de clé |
| Documents | Faux MIME, dépassement, objet absent/incomplet, scan indisponible, refus et téléchargement hors périmètre |
| Mobilité | Rejeu après succès réseau ambigu, version périmée, annulation/réaffectation entre brouillon et synchronisation |
| Contrats/SLA | Bornes de contrat, jours ouvrés, pauses autorisées, changement d'heure et fuseau du site |
| Architecture | Pas de dépendance domaine vers Spring/JPA, pas d'accès repository intermodule, pas d'entité exposée |
| Exploitation | Démarrage à froid, readiness distincte de liveness, panne fournisseur, sauvegarde/restauration, migration compatible rollback applicatif |
| Charge | Jeu réaliste et utilisateurs de périmètres différents, p95 et saturation, recherche spatiale, exports asynchrones |

Tests unitaires pour les règles, tests d'intégration pour les garanties de la base et tests HTTP pour les contrôles d'accès. Tester les résultats et les invariants, pas chaque ligne de code par miroir. Les tests de concurrence doivent utiliser des transactions réellement concurrentes et vérifier le résultat final en base.

CI : Maven Wrapper verify, compilation Java du majeur retenu, tests unitaires et d'intégration non ignorés, tests d'architecture, vérification OpenAPI, détection de secrets, analyse des dépendances et de l'image, SBOM et rapport de vulnérabilités. Une exception à une vulnérabilité est documentée avec propriétaire et échéance. Ne pas annoncer vert si Docker manque et que les tests d'intégration n'ont pas pu s'exécuter.

Chaque lot est terminé quand son contrat est documenté, migrations et autorisations présentes, scénarios nominaux et négatifs vérifiés, logs sans secrets, documentation locale actualisée et risques résiduels explicites. Revue humaine sur identité, données privées, concurrence financière et migrations sensibles.

## 14. Exploitation, sauvegarde et protection des données

Readiness : capacité réelle à servir les fonctions essentielles ; liveness : processus bloqué ou sain, sans dépendre de toutes les pannes externes au risque de boucles de redémarrage. Actuator expose le minimum ; env, configprops, heapdump et endpoints sensibles restent privés et protégés [S21]. Ne pas inclure un fournisseur mail dans la liveness.

Sauvegardes chiffrées et restaurations testées de PostgreSQL (identités et sessions incluses), stockage objet, configuration Google et clés MFA externes. PITR nécessaire si le RPO de 15 min est retenu ; un dump quotidien seul ne le remplit pas. Couvrir aussi les métadonnées/objets et leurs liens lors d'une restauration. Une copie de volume Docker n'est pas une stratégie de sauvegarde démontrée. Conserver les clés nécessaires au déchiffrement de manière indépendante.

Déploiement progressif : sauvegarde/restauration vérifiées, migration expand-contract, application compatible avec l'ancien/nouveau schéma pendant la transition, smoke tests puis surveillance. Un rollback applicatif ne doit pas exiger de supprimer des données récentes. Pas de down-migration destructive automatique.

Durées de conservation à définir par catégorie avec les responsables produit et protection des données : profils refusés, documents techniques, rapports/attestations, journaux, positions ponctuelles, invitations et données comptables si ajoutées. Distinguer archivage métier, suppression et anonymisation. Un soft delete illimité ne constitue pas une politique de conservation. Prévoir export autorisé, purge contrôlée et traitement des obligations de conservation à valider par les personnes compétentes. Aucun délai légal arbitraire n'est inventé ici.

## 15. Ordre de développement

| Lot | Résultat principal | Dépendances |
|---|---|---|
| 00 | Inspection du dépôt, ADR architecture/base/identité, versions confirmées | Aucune |
| 01 | Squelette Maven/Boot, refus par défaut, erreurs, architecture et CI initiale | 00 |
| 02 | Infrastructure locale, secrets, rôles SQL, migrations et packaging | 01 |
| 03 | Connexion locale, sessions web/CSRF, jetons opaques mobile, MFA et Google optionnel | 02 |
| 04 | Invitations et onboarding client/technicien, validation administrative | 03 |
| 05 | Comptes, sites, équipements et cloisonnement | 04 |
| 06 | Contrats/calendriers, tickets et proposition financière client | 05 |
| 07 | Missions, révisions financières admin, publication et découverte géographique | 06 |
| 08 | Candidatures, sélection, confirmation, expiration et concurrence | 07 |
| 09 | Pièces jointes privées, quarantaine et scan | 05 ; à faire avant recette bout en bout |
| 10 | Interventions, rapports, attestation, PDF et commandes hors ligne | 08 + 09 |
| 11 | Outbox et notifications complètes, reprises | Base minimale d'outbox créée dès les premiers événements |
| 12 | SLA, statistiques, jobs et conservation | 06–11 |
| 13 | Mesures de charge, index et observabilité | Parcours métier réalisés |
| 14 | Durcissement CI, déploiement, restauration et recette finale | Tous les lots |

La sécurité, les tests et l'observabilité minimale commencent au lot 01 ; les lots 13/14 les éprouvent, ils ne les ajoutent pas après coup. Le lot 11 complète un mécanisme d'outbox minimal créé dès qu'un événement transactionnel doit être envoyé. Pas d'estimation de livraison fiable avant validation du périmètre et première tranche exécutée.

Avec trois personnes : un référent identité/infrastructure, un référent clients/tickets/contrats, un référent missions/interventions, avec revue croisée systématique. Il s'agit d'une répartition d'équipe proposée, pas d'une instruction à lancer des agents en parallèle. N'envoyer à Codex qu'un lot ou une tranche vérifiable à la fois, avec AGENTS.md et les décisions actuelles.

### 15.1 Premiers gestes concrets

1. Ouvrir le dépôt cible et exécuter le prompt 00 ; ne pas écraser un projet existant.
2. Confirmer les points produit bloquant les DTO/contraintes du prochain lot, sans bloquer les lots indépendants.
3. Exécuter le lot 01 avec JDK 25 et Maven Wrapper ; obtenir un squelette qui compile et refuse les routes non autorisées.
4. Intégrer les fichiers infra du pack ; générer les secrets, résoudre les images et valider Compose. Vérifier les versions effectives.
5. Réaliser le lot 03 avant d'ouvrir les endpoints métier.
6. Livrer une tranche complète : invitation → validation client → ticket → mission → candidature → confirmation → rapport. Ajouter chaque garantie à son introduction.

## 16. Arbitrages produit restant à fixer

Ils ne remettent pas en cause le choix Spring Boot/PostgreSQL. L'agent doit isoler les éléments concernés et demander une décision seulement au moment où elle affecte un contrat ou une règle irréversible.

| Décision | Proposition de départ | À trancher avant |
|---|---|---|
| Montants HT/TTC, forfait/horaire, frais | Proposition CLIENT confirmée en 06 : forfaits HT/EUR distincts, aucune facturation déduite ; conditions ADMIN distinctes à leur lot | 06 validé côté CLIENT |
| Montants obligatoires ou facultatifs côté client | Confirmé en 06 : chacun facultatif même hors contrat, null non assimilé à zéro | 06 validé |
| Délai de confirmation technicien | Paramétrable par mission, défaut à définir | 08 |
| Remise en ligne après refus/expiration | Retour PUBLISHED si fenêtre encore valide et admin autorise | 08 |
| Modification après accord | Avenant explicite, nouvelle acceptation | 08 |
| Réaffectation après début, désistement, frais engagés | Revue admin, historique et traitement des travaux partiels | 08/10 |
| Signataire non titulaire du compte | Délégation/qualité et preuve de consentement explicites | 10 |
| Validation automatique ou administrative du rapport | Revue administrative au lancement | 10 |
| Périmètre géographique France/territoires | Métropole par défaut produit à confirmer, fuseaux modélisés | 07 |
| Conservation et SLA exacts | 06 : calendriers/objectifs par révision explicites sans défaut commercial, pauses par compteur ; jobs/échéances/statistiques et conservation restent 12/14 | 06 configuré ; 12/14 à réaliser |

## 17. Ce qu'il faut éviter

- Générer toute l'application en un seul prompt, puis chercher à ajouter l'autorisation ensuite.
- Créer un rôle organisateur séparé, une app client mobile ou une facturation non demandée.
- Fusionner ticket, mission et rapport dans une seule entité aux statuts ambigus.
- Écraser les montants client avec ceux de l'admin, ou modifier un accord confirmé sans trace.
- Considérer une session/un jeton valide ou un rôle CLIENT comme un droit sur tout objet.
- Exposer les contacts et positions exactes dans les cartes de découverte.
- Utiliser H2 comme preuve de fonctionnement PostgreSQL/PostGIS ; utiliser ddl-auto=update en production.
- Envoyer un mail dans la transaction de sélection ; promettre exactement une fois sans démonstration.
- Accumuler des composants d'infrastructure sans besoin mesuré ou conserver des secrets d'exemple.
- Confondre un document de conception, une validation syntaxique et une application effectivement testée.

## 18. Sources officielles

Les sources étayent les capacités et compatibilités ; le choix d'architecture et les règles proposées restent notre analyse du besoin.

- S1 — PostgreSQL, contraintes : https://www.postgresql.org/docs/current/ddl-constraints.html
- S2 — MongoDB, transactions : https://www.mongodb.com/docs/manual/core/transactions/
- S3 — MongoDB, géospatial : https://www.mongodb.com/docs/manual/geospatial-queries/
- S4 — Spring Boot, exigences système : https://docs.spring.io/spring-boot/system-requirements.html ; maintenance : https://github.com/spring-projects/spring-boot/wiki
- S5 — Temurin, support et versions : https://adoptium.net/support/ ; https://adoptium.net/fr/temurin/releases/
- S6 — Apache Maven, versions : https://maven.apache.org/download.cgi
- S7 — PostgreSQL, versions supportées : https://www.postgresql.org/support/versioning/
- S8 — Images officielles du projet PostGIS : https://github.com/postgis/docker-postgis
- S9 — Spring Boot, dépendances gérées : https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html
- S10 — springdoc, versions et compatibilité : https://springdoc.org/
- S11 — ArchUnit : https://github.com/TNG/ArchUnit/releases/tag/v1.5.1
- S12 — Spring Security, authentification et sessions : https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html ; https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
- S13 — Mailpit : https://github.com/axllent/mailpit/releases/tag/v1.32.0 ; https://mailpit.axllent.org/docs/install/
- S14 — Garage : https://garagehq.deuxfleurs.fr/documentation/quick-start/
- S15 — Docker Compose : https://docs.docker.com/reference/compose-file/services/
- S16 — Spring Security, CSRF : https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- S17 — Spring Security, Google/OIDC : https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html
- S18 — OWASP API Security : https://owasp.org/API-Security/editions/2023/en/0x11-t10/
- S19 — Spring MVC, ProblemDetail : https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- S20 — PostGIS, ST_DWithin : https://postgis.net/docs/ST_DWithin.html
- S21 — Spring Boot, Actuator : https://docs.spring.io/spring-boot/reference/actuator/endpoints.html

- S22 — Bouncy Castle Java : https://www.bouncycastle.org/download/bouncy-castle-java/
- S23 — OWASP, stockage des mots de passe : https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- S24 — Google OIDC : https://developers.google.com/identity/openid-connect/openid-connect
- S25 — OWASP ASVS : https://asvs.owasp.org/

## 19. Documents complémentaires de la révision 1.1

AUTHENTIFICATION.md fixe les comptes, sessions, flux Google et MFA ; SECURITE.md fixe les exigences SEC-xx et les preuves ; UPGRADE.md décrit la reprise de l'ancien pack sans destruction de données. CHANGELOG.md recense la modification. Les quinze prompts référencent ces décisions. Les objectifs de sécurité ne deviennent des résultats qu'après implémentation, tests, revue et vérification de l'environnement.
