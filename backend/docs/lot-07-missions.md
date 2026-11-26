# Lot 07 — missions, conditions ADMIN et découverte géographique

18/11/2026. **Implémenté et vérifié localement : 154 Surefire + 166 Failsafe = 320 tests Java**, zéro échec/erreur/skip. [Preuves et limites](preuves/lot-07-verifications.md). Aucune approbation de production.

## Décisions confirmées par le responsable

- ADMIN crée un brouillon avec titre, résumé public, 1–10 compétences, fenêtre UTC et point public WGS84 approximatif facultativement libellé. Ticket et révision contractuelle sont facultatifs ; s'ils sont fournis, ils doivent appartenir au compte/site et être utilisables. Aucun géocodage ni adresse exacte n'est déduit.
- La publication exige une rémunération forfaitaire HT en EUR ; les frais forfaitaires HT restent facultatifs. `null` signifie non renseigné, jamais zéro. Les montants sont lus à deux décimales, sans arrondi silencieux, dans la borne 0–9 999 999 999,99. Aucune TVA, facture, paie, marge ou paiement n'est calculé.
- Chaque changement de conditions crée une révision financière numérotée, append-only. Le motif reste interne ; une ancienne révision n'est jamais réécrite ou supprimée. Un ticket lié doit être qualifié avant publication. Une révision contractuelle liée doit couvrir le site et l'équipement du ticket à l'heure de publication.
- Découverte réservée aux TECHNICIAN actifs, vérifiés, en session MOBILE et sans cumul de rôle. Origine explicite fournie par le mobile, rayon 10–200 km (défaut 50), taille de page 1–100 (défaut 20), 1–10 compétences, fenêtre et curseur facultatifs. Tri stable par distance croissante puis identifiant de mission.
- La projection mobile contient les conditions publiées et un point public approximatif, jamais le point privé du site, son adresse/contact, le compte, le ticket, le contrat, l'auteur ou le motif de révision. Aucun prix CLIENT, aucune identité d'un autre acteur.

## Architecture et portée

`missions` possède mission, compétences normalisées et révisions financières. Les cas d'usage appellent les ports existants : `assets.publicapi.AssetAccess` pour le site actif et son fuseau, `tickets.publicapi.TicketContext` pour un ticket qualifié, `contracts.publicapi.ContractCoverage` pour la couverture explicite. Aucun SQL intermodule, aucune lecture du parc privé ou des tables d'identité par le module.

L'ADMIN est déjà contrôlé par la chaîne WEB (rôle, statut, email vérifié, MFA récente). La découverte repasse par `identity.publicapi.ActiveTechnician`, qui exige une session MOBILE, un compte ACTIVE, un email vérifié et le seul rôle TECHNICIAN. Le statut global ACTIVE ne suffit pas à contourner une future admission technicien.

La découverte est une lecture transactionnelle bornée : filtre `PUBLISHED`, rayon `ST_DWithin` sur le point public, compétences normalisées et fenêtre, puis page keyset. Le total n'est pas exposé et aucune liste complète n'est chargée. Un index GiST partiel couvre les missions publiées ; un index sur la compétence normalisée évite un parcours complet avant la page.

## Contrat HTTP

[OpenAPI 0.7.0](../openapi/maintenance-api.json) : **122 chemins, 94 schémas**, lots antérieurs compris. Frontend, upload, affectation et détail privé de mission ne sont pas livrés.

### Administration

Préfixe : `/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/missions`.

| Commande | Comportement |
|---|---|
| POST collection | Crée un brouillon, 201/Location/ETag ; Idempotency-Key obligatoire ; aucun prix publié à ce stade |
| GET collection | Page de résumés du seul site, filtre `state`, page 0–10 000, taille 1–100 (défaut 20) |
| GET `/{missionId}` | Détail ADMIN, rémunération courante facultative, ETag de version |
| POST `/{missionId}/publish` | If-Match + Idempotency-Key ; rémunération obligatoire, première révision numéro 0 |
| GET `/{missionId}/financial-revisions` | Curseur `after=-1`, taille 1–100, `nextAfter` |
| POST `/{missionId}/financial-revisions` | Mission publiée seulement, If-Match + Idempotency-Key, nouvelle révision immuable |

Le corps de création contient ticket/révision contractuelle facultatifs, titre (160), résumé public (2 000), compétences (1–10 × 80), fenêtre UTC non vide et bornée 2000–2201, latitude/longitude à six décimales au plus, libellé public facultatif (200). Les textes vides ou de contrôle sont refusés. Les compétences sont dédupliquées sans tenir compte de la casse puis triées.

Les montants sont des chaînes décimales JSON, pas des nombres flottants. Une valeur omise ou `null` pour les frais reste non renseignée ; `"0.00"` est une valeur explicite. La devise et les bases `EUR`, `HT`, `FIXED` sont figées en base et rappelées en lecture.

### Découverte mobile

`GET /api/mobile/v1/technician/missions/nearby?latitude=…&longitude=…`

Paramètres : `radiusKm` (10–200, défaut 50), `skills` répété, `from`, `to`, `cursor`, `size` (1–100, défaut 20). Coordonnées inversées, hors bornes, fenêtre vide, rayon ou taille hors bornes, curseur forgé et plus de dix compétences donnent 400. Sans jeton : 401 ; TECHNICIAN non actif ou cumul de rôles : 403.

Le curseur opaque Base64URL encode `distanceMeters:missionId` et reste borné à 120 caractères. `nextCursor` n'est renvoyé que s'il reste une ligne. Les réponses sont `Cache-Control: no-store`. Les éléments exposent : identifiant, titre, résumé, compétences, fenêtre, point public et libellé, distance en mètres et conditions publiées `{revision,remuneration,expenses,currency,taxBasis,remunerationBasis,expensesBasis}`.

## Migrations, événements et privilèges

- **V17** : mission, compétences et révisions financières ; FK composites compte/site/ticket/contrat ; point public `geography(Point,4326)` ; contraintes de fenêtre, coordonnées, états, montants et échelle ; index de page par site/état et GiST partiel des missions publiées ; événements `MISSION_CREATED`, `MISSION_PUBLISHED`, `MISSION_REVISED`.
- **V18** : index `lower(skill), account_id, mission_id` pour le filtre de compétences avant pagination.
- Runtime : `SELECT,INSERT` sur les trois tables ; `UPDATE` limité à l'état, la révision financière courante, la version et les dates de mission. Aucun `DELETE`, aucune réécriture des compétences ou révisions, aucun DDL.
- Audit et outbox sont écrits dans la même transaction que la mission. L'outbox ne contient que des métadonnées ; aucun worker, email ou notification complète n'est ajouté.

## Limites et suite

Aucune affectation, candidature, détail privé de mission, pièce jointe, attestation ou facture. Les missions publiées ne sont pas encore réservables : le lot 08 doit verrouiller mission/conditions et garantir une seule affectation active. L'approbation technicien reste bloquée avant le lot 09 ; les tests de découverte activent une fixture par SQL, ce qui ne prouve pas le parcours d'admission réel.

Pas de frontend, géocodage, calcul d'itinéraire, tuilage, recherche plein texte, notification ou statistique. La recette ne prouve ni charge cible, ni disponibilité, ni revue humaine indépendante, ni déploiement. Pour la recette locale uniquement, un ADMIN de démonstration a été créé par un fixture temporaire hors API, puis le fixture a été supprimé ; aucun compte ou secret par défaut n'est livré. Google demeure désactivé.

Suite possible : **08 — candidatures et affectations**, sans démarrage automatique.
