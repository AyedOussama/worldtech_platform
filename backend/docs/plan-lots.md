# Plan des lots et contrat du prochain lot

27 octobre 2026 — plan de réalisation, actualisé au lot 06 le 02 novembre 2026. Source : quinze prompts existants, cahier backend §15. Un lot/tranche vérifiable à la fois ; les travaux indépendants peuvent avancer sans inventer les règles d'un autre lot.

**Lot 06 implémenté et vérifié localement ; suite possible : 07 — missions, conditions financières ADMIN et découverte géographique**, après ses arbitrages nécessaires. Approbation technicien toujours bloquée sans pièces contrôlées (09), Google désactivé. Les [preuves courantes de 06](preuves/lot-06-verifications.md) et [écarts de 02](preuves/lot-02-verifications.md) ne valent pas approbation de production. Aucun lot suivant démarré automatiquement.

## 1. Dépendances et critères de sortie

| Lot | Dépendances réelles | Résultat borné / preuve de sortie | État courant |
|---|---|---|---|
| 00 | Aucune | Inspection, trois ADR, versions/sources, carte, droits, ASVS et modèle de preuves | Documents et contrôles de préparation livrés ; limites dans le compte rendu |
| 01 Socle | 00 | Projet/wrapper JDK 25/Boot 4.1.2, refus HTTP, santé minimale, ProblemDetail, ArchUnit et CI ; `mvnw verify` réellement exécuté | Livré et vérifié localement : 50 tests Surefire + 5 Failsafe, aucun échec/skip ; CI distante non exécutée |
| 02 Infrastructure | 01 | Secrets privés, lock réel, Compose, PostgreSQL/PostGIS et rôles, premières migrations, packaging ; versions et privilèges effectifs testés | Livré/vérifié localement : neuf secrets/ACL, migrations et persistance, 22 tests d'intégration et image non root ; écarts et limites consignés, pas de validation production |
| 03 Identité | 02 | Local web/CSRF, opaque mobile, MFA ADMIN, reset/révocation et audits ; tests négatifs et concurrence ; Google facultatif isolé | Implémenté/vérifié localement : 172 Java + 6 Python ; Google réel, recette externe et revue indépendante non vérifiés |
| 04 Onboarding | Identité locale 03 + outbox minimale ; 09 pour approbation technicien | Invitation → dossier → décision admin, droits d'attente et consommation concurrente testés | Parcours locaux et Google explicites testés localement ; approbation client, complément/refus livrés ; approbation technicien bloquée sans pièces contrôlées |
| 05 Comptes/sites | 04 côté clients ; aucune approbation technicien nécessaire | Comptes/membres, parc et habilitations ; FK cohérentes et tests multi-comptes/sites | Livré localement : 243 Java + 6 Python, ADMIN seul écrit, CLIENT lit son périmètre ; V12–V13 et montée depuis V11 vérifiées |
| 06 Tickets/contrats | 05 côté clients | Contrats versionnés, calendrier, incident et proposition client ; droits et contraintes de son contrat | Livré localement : 293 Java + 6 Python ; V14–V16, forfaits CLIENT facultatifs HT/EUR, couverture explicite et cycle confirmé, droits/concurrence/immutabilité testés |
| 07 Missions/géographie | 06 | Publication, révisions admin, découverte bornée sans fuite ; tests spatiaux sur base réelle | À réaliser |
| 08 Candidatures/affectation | 07 | Sélection/confirmation/expiration atomiques ; une affectation active et conditions acceptées conservées | À réaliser |
| 09 Documents | 05 ; contrat des pièces d'admission 04 | Objets privés, autorisation par parent, quotas, quarantaine/scan ; indisponibilité du scan testée | À réaliser, parallélisable avec 06–08 ; requis avant approbation technicien |
| 10 Interventions/rapports | 08 + 09 | Exécution, versions, attestation, PDF et rejeu hors ligne ; réaffectation/version périmée explicites | À réaliser |
| 11 Notifications complètes | Événements des lots précédents | Workers/reprises/déduplication et suivi des échecs ; traitement multi-instance | À compléter, sans différer l'outbox minimale nécessaire dès 03/04 |
| 12 SLA/statistiques | 06–11 selon l'indicateur | Règles contractuelles/calendaires testées, agrégats bornés et isolation des données | À réaliser |
| 13 Performance/observabilité | Parcours métier concernés | Charge réaliste, index/SQL, alertes et seuils mesurés | À réaliser ; instrumentation minimale dès 01 |
| 14 Production/recette | Tous les lots nécessaires au pilote | CI/scans/SBOM, staging effectif, restauration mesurée, revue humaine, pentest et retests | À réaliser ; aucun déploiement autorisé par le lot 00 |

Le lot identité peut être découpé en une tranche locale complète web/mobile/MFA puis une intégration Google optionnelle. L'absence de credentials Google ne bloque pas l'identité locale ni l'onboarding ; elle reste une limite explicite de validation Google. Aucun flux Google non vérifié n'est activé pour contourner cette absence.

Le lot 11 ne doit pas être attendu pour envoyer correctement une invitation/reset : schéma d'outbox, écriture atomique et livraison locale minimale sont introduits au premier besoin. Le lot 11 complète les canaux et l'exploitation. Aucun email réel n'est envoyé dans les lots de préparation.

## 2. Contrat du lot 01 — socle (livré)

### Points produit bloquant DTO ou contraintes : **aucun**

Le lot 01 ne définit ni inscription métier, ni finance, ni affectation, ni rapport. Il n'y avait donc pas de question produit à faire trancher pour ce socle. Les questions du cahier §16 seront traitées juste avant le lot qui fige leur contrat, sans transformer une proposition en règle acceptée.

Les contrats techniques suivants bornent le lot 01 :

| Contrat | Décision de socle |
|---|---|
| Santé | `GET /health` uniquement, réponse minimale `{"status":"UP"}` de processus, sans dépendance DB ni détails d'exploitation ; ne vaut pas readiness métier |
| Routes restantes | Refus explicite par défaut, aucune inscription/login/route métier ouverte à l'avance ; pas de formulaire ou d'utilisateur/mot de passe auto-généré utilisable |
| Erreurs | `application/problem+json`, `type`, `title`, `status`, `code`, `traceId`, erreurs de champs si pertinentes ; aucun rejet de valeur sensible recopié |
| Autorisation | 401 JSON sur lecture protégée anonyme ; refus des mutations/CSRF, aucune redirection HTML de login ; pas de `csrf.disable()` global |
| Build | Un module, parent Boot 4.1.2, Java 25, distribution Maven 3.10.0 via wrapper vérifié ; dépendances BOM non surchargées |
| Structure | Packages du cahier progressivement ; domaine indépendant, entités non exposées ; pas de modèle fictif pour remplir les modules |
| Données | Aucune migration métier, aucun DTO financier/dossier/attestation, aucune base H2 substitutive |

### Tests et limites de 01

- `mvnw verify` sur JDK 25, wrapper et distribution vérifiés, CI initiale ; ne pas annoncer de succès en cas d'échec réseau.
- HTTP : seul GET de santé prévu ouvert ; chemin inconnu/protégé, mauvais verbe, Actuator sensible et documentation interactive non ouverts ; réponse de refus JSON sans détail interne.
- Erreurs : sérialisation/contrat et absence de SQL/stacktrace/secrets ; les handlers de sécurité et MVC doivent donner des erreurs cohérentes.
- Architecture : vérifier les frontières réellement présentes et la capacité des règles à détecter les dépendances interdites ; pas de couverture fictive de domaines absents.
- Pas de login, session JDBC, refresh, MFA, base réelle ou test métier revendiqué en 01. Les garanties correspondantes restent des recettes de 02/03 et des lots métier.

### Préconditions techniques, non arbitrages produit

- JDK 25 : présent ; Oracle local, Temurin prévu pour CI/runtime. Ne pas diminuer le majeur pour contourner un échec.
- Maven global : absent ; wrapper officiel 3.3.4 livré, distribution 3.10.0 contrôlée, sans installation globale.
- Réseau : téléchargement et résolution Maven réellement exécutés au lot 01 ; résultats dans les preuves, pas seulement lecture XML.
- Git : dossier non versionné ; aucun historique ou changement local à qualifier avec `git status`. Aucun `git init`, remote ou commit créé par ce lot. L'initialisation de la gestion de versions est distincte du contrat produit et ne justifie aucun écrasement.
- Avant tout fichier du lot 01, réinspecter les chemins et l'état courant : le travail peut avoir évolué depuis ce cadrage.

## 3. Contrat du lot 02 — infrastructure (livré localement)

- PostgreSQL/PostGIS et Mailpit sur boucle locale, Garage optionnel non démarré ; aucun serveur d'identité externe.
- Secrets hors OneDrive avec contrôle effectif des permissions ; lock d'images public et explicite, pas de tag flottant au build.
- V1 limitée à l'audit technique ; schéma `app`, ownership migrateur, grants explicites ; runtime sans DDL ni réécriture d'audit/historique.
- Flyway local distinct du datasource ; en production, CLI one-shot avec TLS vérifié requis et JVM web sans migrateur. Pas de `clean`/`repair` automatique.
- Tests PostGIS réels obligatoires, contrôle de persistance après redémarrage du seul conteneur DB ; image UID 10001 démarrée sans secret migrateur.
- Pas de décision produit à inventer. Identité, onboarding, documents et transactions métier restent à leurs lots.

## 4. Organisation sans blocage global

Après le socle et l'identité communs, les trois référents suivent la [carte](architecture/modules.md). Documents (09) peut avancer après 05 pendant tickets/missions (06–08). L'activation Google reste optionnelle. Les contrats frontend peuvent être préparés sur les seuls contrats approuvés, jamais en inventant des champs métier manquants.

Lorsqu'une règle manque : noter la commande/le DTO/la contrainte concernée, poser une question ciblée au responsable produit, suspendre **cette partie seulement**, puis continuer les tests, droits ou fonctions indépendants. Une mesure de performance, un email ou une maquette ne valide pas une règle financière.

## 5. Contrat et réserve du lot 04

Les formulaires minimaux et les justificatifs obligatoires pour TECHNICIAN uniquement ont été confirmés. Invitations 48 h configurables, vérification email distincte, identités PENDING et dossiers DRAFT/SUBMITTED/NEEDS_INFORMATION/APPROVED/REJECTED sont décrits dans [le contrat](lot-04-onboarding.md). Aucune table de compte/membre/site du lot 05 n'a été anticipée. Les cumuls de rôles candidats sont refusés dans cette admission : le statut global ACTIVE ne doit pas contourner une validation métier indépendante.

L'approbation TECHNICIAN reste refusée dans le domaine et en SQL, sans option de contournement. Sa clôture exige la liste des pièces, leurs règles de validité/contrôle et la chaîne privée/antivirus réelle. Cela ne bloque pas les comptes clients du lot 05, mais interdit de présenter le parcours technicien comme intégralement livré.

## 6. Contrat confirmé du lot 05

ADMIN seul gère comptes, membres, droits, sites/contacts et équipements ; CLIENT actif vérifié consulte son seul périmètre. Création explicite depuis APPROVED, type/titulaire non modifiables, particulier limité au titulaire et entreprise multi-membres CLIENT déjà validés. Aucun site par défaut ; liste explicite ou ALL accordé par ADMIN, nouvelle adhésion après retrait sans ressusciter ses anciens droits. Champs minimaux et conventions PUT/PATCH dans [le contrat](lot-05-comptes-parc.md).

À la clôture de 05, les règles financières/calendaires restaient ouvertes ; elles ont ensuite été arbitrées explicitement pour 06 ci-dessous. Documents 09 peut avancer après 05 sans attendre missions/affectations, et reste indispensable à l'approbation technicien.

## 7. Contrat confirmé du lot 06 et prochaine tranche

Proposition CLIENT : forfaits HT/EUR séparés, chacun facultatif, null non assimilé à zéro ; déclaration possible hors contrat, aucun paiement/facture/marge déduit. Contrats ADMIN par révisions immuables et couverture explicite, calendriers et objectifs SLA par révision sans défaut commercial. Pas de jour férié ou pause d'attente implicitement appliqué. Ticket par site, qualification ADMIN (priorité retenue distincte), résolution sans mission possible ; confirmation CLIENT pour CLOSED, persistance depuis RESOLVED pour réouvrir, aucune clôture automatique. [Détails et bornes](lot-06-contrats-tickets.md).

07 doit lire ses règles de publication, compétences, fenêtres et géographie avant de figer ses API. Les choix financiers de la proposition CLIENT ne définissent pas implicitement les conditions publiées aux techniciens. Périmètre métropole/territoires et point public approximatif sont à arrêter ; aucune adresse/contact/position privée dans la découverte. Ni affectation ni approbation technicien à simuler pour contourner leurs dépendances aux lots 08/09.
