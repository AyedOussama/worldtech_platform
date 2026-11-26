# Carte des modules et contrats interdomaines

27 octobre 2026 — conception initiale. Les noms ci-dessous sont ceux du cahier, pas des packages déjà générés. Un seul module Maven ; aucun rôle supplémentaire. Voir ADR [001](../adr/001-monolithe-modulaire-mvc.md), [002](../adr/002-postgresql-postgis.md), [003](../adr/003-identite-applicative.md).

## Règles de dépendance

```text
bootstrap -> configuration et assemblage de tous les adaptateurs
feature.api -> feature.application -> feature.domain + ports
feature.infrastructure -> ports de son domaine
feature.application -> autreFeature.publicapi, seulement selon la carte
shared -> Java/technique minimal, jamais un domaine métier
```

`publicapi` contient interfaces et valeurs neutres, pas des entités JPA, types HTTP ou repositories. Les contrôleurs ne court-circuitent pas les cas d'usage. Aucun domaine ne dépend de `bootstrap` ; aucun cycle interdomaines n'est autorisé. Ne pas généraliser `shared` en répertoire de règles métier communes à tous.

## Propriété et capacités publiques envisagées

| Module `features.*` | Propriétaire de | Dépendances métier autorisées, via `publicapi` | Capacités à préciser dans son lot |
|---|---|---|---|
| `identity` | Compte interne, credentials, sessions, MFA, identités externes | Aucune | Identité et statut de sécurité actuels, révocation ; jamais accès générique à ses tables |
| `clients` | Compte client, membres, invitations et dossier client | `identity` | Appartenance actuelle au compte et admission |
| `technicians` | Dossier professionnel, compétences déclarées/vérifiées, mobilité, admission | `identity` | Éligibilité courante et compétences vérifiées |
| `assets` | Sites, équipements, contacts privés, habilitations de site | `clients` ; principal `identity.publicapi.IdentitySession` | Accès au site, cohérence compte/site/équipement, projection privée autorisée |
| `contracts` | Contrats, révisions de couverture, calendriers/SLA | `clients`, `assets` | Couverture et règles versionnées applicables |
| `tickets` | Besoin client, échanges publics/internes, proposition financière client, résolution | `clients`, `assets`, `contracts` | Contexte qualifié et chronologie publique filtrée |
| `missions` | Visite/créneau, publication, point public/privé, révisions financières admin | `tickets`, `assets`, `contracts`, `technicians` | Mission éligible ; verrou et transition d'allocation ; révision courante |
| `applications` | Candidatures et affectations/réservations/confirmations | `missions`, `technicians` | Affectation confirmée actuelle ; sélection/confirmation atomiques |
| `documents` | Métadonnées d'objets, upload, quarantaine, visibilité et scan | Aucune dépendance directe au parent métier | Gestion d'un document sous contrôle du port d'autorisation du parent |
| `interventions` | Exécution, temps, versions de rapport, attestation, revue et PDF | `missions`, `applications`, `documents` | Version figée du rapport et attestation rattachée |
| `notifications` | Outbox technique, livraisons, notifications et reprises | Aucune dépendance directe aux domaines producteurs | Enregistrement transactionnel d'événement puis livraison différée |
| `audit` | Événements d'audit à accès restreint | Aucune | Enregistrement append-only d'une action/version, sans secret |

Les producteurs peuvent appeler les capacités publiques `audit` et `notifications` quand leur lot introduit l'effet correspondant. L'identité de l'acteur est fournie par le contexte serveur contrôlé, jamais prise d'un champ de commande publique.

## Trois frontières à préserver

1. **Clients/sites** : `clients` possède l'appartenance au compte ; `assets` possède les habilitations de ses sites et interroge l'appartenance. `clients` n'importe pas le repository des sites pour gérer ses membres. Toute suppression/révocation impactant des sites passe par un contrat explicite au lot 05.
2. **Mission/affectation** : `applications` orchestre la réservation dans une transaction avec une capacité publique de `missions`, qui verrouille et revalide la mission. La candidature et l'affectation restent dans `applications`. `missions` n'appelle pas le repository d'affectations pour contourner cette frontière. Aucun appel externe sous verrou.
3. **Document/parent métier** : `documents` définit un port d'autorisation du parent ; un adaptateur assemblé dans `bootstrap` utilise les `publicapi` du parent. Le parent peut ainsi utiliser `documents.publicapi` sans cycle. Même principe pour une projection de notification nécessitant une revalidation, ou le résumé de travaux utile à la clôture d'un ticket. Ces ports sont implémentés quand leur lot arrive, pas comme autorisations toujours vraies.

Une dépendance nouvelle exige d'actualiser cette carte et les tests. Il ne s'agit pas de créer maintenant toutes ces interfaces ni toutes les tables.

## Contrats communs déjà cadrés

- `user_id` UUID stable, exactement trois rôles ; périmètres de sites ≠ rôles.
- `api/web/v1` et `api/mobile/v1` : authentifications distinctes, cas d'usage partagés.
- DTO par acteur ; aucun transfert implicite de rôle, propriétaire, approbation, montant publié ou affectation depuis une requête publique.
- Ticket → zéro à plusieurs missions ; une mission préventive peut relever directement d'un contrat/site. Mission terminée ≠ ticket automatiquement résolu.
- Proposition client, publication admin et snapshot accepté distincts ; aucun paiement/facture/marge inventé.
- Idempotence liée à acteur/opération/clé/empreinte ; autorisation rejouée avant restitution. Conflit métier 409, précondition de version périmée 412.
- Versions soumises/attestées immuables ; brouillons modifiables avec contrôle de version, `Clock` et UTC.

## Répartition proposée pour trois développeurs

| Référent | Responsabilité | Revue croisée et livraison |
|---|---|---|
| A | Identité, infrastructure, CI, audit/outbox de base | B relit isolation et migrations ; C relit mobile/refresh/MFA |
| B | Clients, parc, contrats, tickets | C relit contraintes métier ; A relit droits et secrets |
| C | Missions, candidatures, interventions, intégration documents | A relit concurrence/sécurité ; B relit contrats et rapports |

Les contrats OpenAPI, les cas de refus et la recette bout en bout sont partagés avec Angular/Flutter. Cette répartition humaine n'est pas une instruction de délégation à des agents. Les dépendances et les tranches parallélisables figurent dans le [plan](../plan-lots.md).

## Mise en œuvre du lot 04

`clients` possède l'invitation et le dossier client ; `technicians` possède le profil déclaré et son admission. Le compte client/membres/sites reste au lot 05. Les deux modules utilisent `identity.publicapi` pour les credentials préparés hors verrou, la vérification de session/rôle/email/MFA/statut et la transition d'admission ; aucun SQL intermodule sur les tables d'identité.

`IdentitySession` est le contrat Java du principal authentifié, jamais un DTO public. `RegistrationAccounts` et `AdmissionIdentity` sont des capacités dédiées, pas un repository générique. Les notifications et audits restent via leurs interfaces publiques existantes. `shared.idempotency.CommandReceipts` est technique : aucune règle de profil, aucun corps sensible stocké ; le cas d'usage autorise/verrouille avant de consulter une réception.

L'identité ne dépend pas des implémentations clients/techniciens pour l'inscription Google. Son port `GoogleAdmission` est relié par `bootstrap/identity/GoogleAdmissionAdapter` à `clients.publicapi.GoogleClientAdmission` et `technicians.publicapi.GoogleTechnicianAdmission`. Ces capacités internes exigent une création d'identité explicitement autorisée ; aucune route n'accepte un user_id ou un rôle public pour les invoquer. Les règles ArchUnit contrôlent toujours les dépendances et l'absence de cycles.

## Mise en œuvre du lot 05

`clients` crée comptes et adhésions depuis les dossiers validés, via `identity.publicapi.BusinessIdentity` pour le statut/session/rôle courant. `clients.publicapi.ClientAccountAccess` transmet à `assets` une portée transactionnelle après contrôle et verrou du compte/adhésion. Le principal `IdentitySession` n'est jamais lié depuis un corps HTTP.

`assets` filtre dans ses propres requêtes les sites actuellement accordés avant détail, pagination et count ; aucune lecture SQL de `client_member` ou des tables d'identité par assets. Les FK composites vers comptes/membres sont des garanties structurelles de migration, pas un accès repository intermodule. Le retrait d'un membre conserve son UUID d'adhésion historique ; une nouvelle adhésion en reçoit un autre, empêchant la résurrection des droits sans dépendance clients → assets.

Audit technique via le port existant ; événements métier de version/portée append-only dans le module propriétaire. Aucun contrat/ticket/mission ou module documentaire ajouté pour anticiper les lots suivants. [Contrat et limites](../lot-05-comptes-parc.md).

## Mise en œuvre du lot 06

`assets.publicapi.AssetAccess` expose une autorisation transactionnelle sur **un site actif**, son fuseau et le contrôle d'un équipement de ce même site. Les cas d'usage contrats/tickets l'utilisent après les droits d'identité/compte du lot 05 ; leurs listes CLIENT sont rattachées à ce site avant SQL, pagination et total. Pas de SQL intermodule ou de liste non bornée de tous les sites autorisés.

`contracts` possède les révisions, couvertures et calendriers normalisés, les objectifs SLA et le calcul borné des secondes de service. `contracts.publicapi.ContractCoverage` valide la révision choisie pour le contexte temporel d'un ticket déjà autorisé ; aucune décision implicite de « meilleur contrat ». Les types HTTP/entités/repositories ne traversent pas cette frontière. Le principal `identity.publicapi.IdentitySession` reste fourni par le serveur, pas par une commande.

`tickets` possède besoin, priorité demandée/retenue, propositions CLIENT, cycle et chronologies ; ni visite ni conditions financières ADMIN. Notes internes et messages publics sont physiquement distincts, y compris leurs curseurs. `shared.idempotency.CommandReceipts` conserve les résultats techniques minimaux, sans contenu métier libre ; le résultat historique ne remplace pas la lecture courante. Extension nullable compatible avec les réceptions d'admission existantes.

Producteurs → `notifications.publicapi.BusinessEvents` pour métadonnées d'outbox atomiques, et `audit.publicapi.SecurityAudit` pour la trace corrélée. Aucun nouveau worker de livraison/notification complète au lot 06 ; la revalidation des destinataires/reprises reste au lot 11. Aucun cycle vers les producteurs ni modèle générique de finance/SLA ajouté à `shared`. [Contrat du lot 06](../lot-06-contrats-tickets.md).

## Mise en œuvre du lot 07

`missions` possède mission, compétences et révisions financières. Il appelle `assets.publicapi.AssetAccess` pour un site actif, `tickets.publicapi.TicketContext` pour un ticket qualifié et `contracts.publicapi.ContractCoverage` pour une révision applicable ; aucune lecture SQL de leurs tables ni autorisation fournie par un DTO. La rémunération courante est lue dans le même verrou que la mission.

La découverte mobile utilise `identity.publicapi.ActiveTechnician`, distinct du simple statut ACTIVE : session MOBILE, email vérifié, rôle TECHNICIAN exclusif. `JdbcMissionDiscovery` borne la requête en SQL (publiée, rayon, compétences, fenêtre), trie distance/identifiant et pagine par keyset ; aucun chargement complet ni compteur global. Le point public est un `geography(Point,4326)` séparé du point privé du site.

Producteurs → mêmes ports d'audit/outbox ; événements de mission métadonnées seulement. Aucun candidat, affectation ou détail privé de mission n'est ajouté avant les lots 08/10. [Contrat du lot 07](../lot-07-missions.md).
