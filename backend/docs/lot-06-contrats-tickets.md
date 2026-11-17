# Lot 06 — contrats, calendriers et tickets

02/11/2026. **Implémenté et vérifié localement : 138 Surefire + 155 Failsafe = 293 tests Java**, zéro échec/erreur/skip à 10:54:23 ; six tests Python réussis. [Preuves et limites](preuves/lot-06-verifications.md). Aucune approbation de production.

## Décisions confirmées par le responsable

- Proposition CLIENT : rémunération forfaitaire HT et frais forfaitaires HT distincts, EUR seulement. Chacun est facultatif, même hors contrat : null signifie non renseigné, jamais zéro. Ni TVA calculée, facture, paiement, prix publié technicien ou marge.
- Déclaration possible sans contrat ou hors couverture, sur un site actuellement autorisé ; équipement facultatif. ADMIN qualifie explicitement la couverture et une priorité retenue distincte de la priorité demandée.
- ADMIN seul écrit les contrats. Révisions immuables, validité [début, fin), sites entiers (équipements futurs inclus) ou équipements explicitement listés ; aucun renouvellement automatique. Révision applicable à la date de déclaration choisie explicitement à la qualification, y compris si plusieurs contrats conviennent. La référence d'un ticket n'est pas remplacée par une correction de contrat.
- CLIENT lit seulement la couverture de son site autorisé, sans autres sites ou notes internes.
- Calendrier 24/7 ou plages hebdomadaires dans le fuseau du site, exceptions datées explicites, aucun jour férié implicite. Priorités LOW/NORMAL/HIGH/CRITICAL. Objectifs facultatifs de prise en compte, début d'intervention et résolution, en minutes de service depuis la déclaration ; aucun objectif par défaut. WAITING_CLIENT ne suspend que les compteurs explicitement prévus au contrat. Événements conservés ; exploitation SLA complète au lot 12.
- CLIENT révise sa proposition seulement en OPEN ; toute version et son auteur sont conservés. ADMIN qualifie, démarre, met en attente, reprend, résout (y compris sans mission) ou annule avec motif public. CLIENT habilité confirme la résolution pour clore ou signale sa persistance pour réouvrir RESOLVED. Ni clôture automatique ni réouverture de CLOSED dans ce lot.
- Échanges publics séparés des notes internes ADMIN. Aucun upload ou URL distante anticipé avant le lot 09.

## Architecture et portée

Les nouvelles listes CLIENT sont explicitement rattachées à un compte et un site : autorisation et verrou transactionnel via les ports clients/assets, puis SQL limité à ce compte/site avant pagination, filtre, total ou détail. Pas de lecture interrepository, d'énumération de tous les sites ni de filtrage Java après pagination. Un site archivé ou une permission retirée ferme aussi l'accès à ses tickets et couvertures ; les références restent conservées.

Contrats/calendriers et tickets restent deux domaines. Calendrier et règles SLA sont figés dans chaque révision ; le fuseau du site est figé à la déclaration du ticket. Le calcul des échéances, alertes et statistiques opérationnelles n'est pas revendiqué par la seule conservation des événements.

- `assets.publicapi.AssetAccess` autorise un site actif dans la transaction courante et vérifie un équipement du même compte/site sans lire sa projection privée avant contrôle. Il réutilise `ClientAccountAccess`, le rôle/statut/email/MFA et le verrou de compte du lot 05. Aucun contexte d'autorisation n'est reçu d'un DTO ou conservé en cache.
- `contracts.publicapi.ContractCoverage` vérifie une révision explicitement choisie et son intervalle à l'instant de déclaration. `tickets` ne lit aucune table de contrats, d'identité, de membres ou d'habilitations.
- Ordre conservateur : utilisateur → compte → agrégat contrat/ticket. Pas d'appel externe sous verrou. Les lectures d'un même compte restent sérialisées avec ses changements de droits ; ce n'est pas un dimensionnement de charge.

## Contrat HTTP

[OpenAPI 0.6.0](../openapi/maintenance-api.json) : **117 chemins, 83 schémas**, lots antérieurs compris. Pas d'API mobile métier, route publique anonyme ou frontend ajouté.

### Contrats et calendriers

Préfixe ADMIN : `/api/web/v1/admin/client-accounts/{accountId}/contracts`.

| Commande | Comportement |
|---|---|
| POST collection | Crée le contrat et sa révision 0, 201/Location/ETag ; corps complet, aucune activation implicite d'un compte |
| GET collection / `/{contractId}` | Page de résumés / définition courante complète ADMIN, notes internes comprises |
| POST `/{contractId}/revisions` | Nouvelle définition immuable, If-Match du contrat courant, motif obligatoire, 201/Location/ETag |
| GET `/{contractId}/revisions` / `/{revisionId}` | Curseur de versions / détail historique ; anciennes couvertures et règles jamais réécrites |

Une définition contient libellé public (160 caractères), notes internes facultatives (2 000), motif interne (1 000), validité UTC [début, fin), couverture, calendrier et liste explicite d'objectifs SLA. Les instants sont normalisés à la microseconde avant validation ; intervalle non vide, borné à 2000–2201. Ni tarif contractuel, quota consommé, renouvellement, résiliation rétroactive ou calcul financier introduit.

Couverture : 1–100 sites du compte, chacun une seule fois. Site entier = liste d'équipements vide et équipements futurs inclus ; sélection = 1–100 équipements distincts de ce site. Un incident sans équipement n'est pas couvert par une simple sélection d'équipements. La qualification valide l'appartenance et la validité de la révision choisie, pas un « meilleur contrat » implicite. Plusieurs révisions/contrats peuvent correspondre ; il n'y a pas de priorité automatique entre eux.

Lecture CLIENT : `/api/web/v1/client/accounts/{accountId}/sites/{siteId}/contract-revisions` et `/{revisionId}`. Liste des révisions dont la validité contient `at` (heure serveur par défaut), détail historique limité à ce seul site même hors période. Aucun chargement des notes/motifs internes, autres sites ou équipements d'autres sites. La liste peut contenir plusieurs révisions valides d'un même contrat ; elle n'affirme pas qu'elles sont toutes retenues pour un ticket.

Calendrier : `mode=ALWAYS` (24/7, `weekly=[]`) ou `WEEKLY` avec 1–28 plages, maximum 4/jour, jours ISO 1–7. Minutes locales de 0 à 1440, [début, fin), sans chevauchement ; scinder une plage qui traverse minuit. Jusqu'à 100 exceptions datées uniques (années 2000–2200), remplaçant la journée entière ; `windows=[]` ferme ce jour, même en 24/7. Aucun jour férié national/régional, géocodage ou horaire par défaut.

Le calcul de durée de service intersecte les fenêtres locales avec de vrais instants UTC et scinde aux changements d'offset : une heure inexistante n'est pas comptée ; les deux occurrences d'une plage répétée le sont, sans compter les minutes hors plage qui les séparent. Durée élémentaire en secondes réelles, pas en heures de cadran ; intervalle de calcul borné à 3 660 jours. Pas d'endpoint de calcul arbitraire coûteux.

Objectifs SLA : 0–12 règles, unicité priorité/compteur, 1–525 600 minutes de service par objectif, pause en WAITING_CLIENT explicitement vraie/fausse par règle. Liste vide = aucun objectif. Les événements de déclaration/qualification/démarrage/attente/reprise/résolution/réouverture préparent le lot 12. Le démarrage du **traitement du ticket** n'est pas assimilé à un départ technicien ou au début d'une visite ; les événements d'intervention des lots futurs compléteront le compteur concerné.

### Tickets et propositions

Racines par site :

- CLIENT `/api/web/v1/client/accounts/{accountId}/sites/{siteId}/tickets` ; POST de déclaration, GET liste.
- ADMIN `/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/tickets` ; GET liste et commandes réservées.
- Les deux canaux WEB autorisés disposent du détail `/{ticketId}`, `/proposals`, `/history` et `/messages`. ADMIN seul dispose de `/internal-notes`.

Déclaration : titre (160), description en texte (4 000), priorité demandée obligatoire ; équipement et proposition facultatifs. Le compte/site provient du chemin autorisé, l'acteur et l'heure du serveur. Le corps ne choisit ni priorité retenue, contrat, état, affectation, auteur ou prix ADMIN. Photos/documents non acceptés à ce stade ; une URL de pièce ou des identifiants de documents non contrôlés ne sont pas stockés en prévision du lot 09.

Liste paginée : page 0–10 000, taille par défaut 20/max 100, tri création/UUID ; filtres `state`, `requestedPriority` et `equipmentId`, même portée SQL pour total et lignes. Un équipement actif d'un autre site/compte donne 404, pas une indication sur ses tickets. Historiques/messages/propositions : curseur numérique `after=-1` initial, maximum 100 éléments, `nextAfter` ; curseurs et permissions indépendants.

Proposition : `remuneration`/`expenses` sont des chaînes décimales facultatives. Lecture à deux décimales ; « 0.00 » ne signifie pas null. Borne 0–9 999 999 999,99 ; nombres JSON, négatifs, exposants, précision >2, structures et virgule refusés sans arrondi. Champs monnaie/base immuables explicitement stockés : EUR, HT, FIXED/FIXED. La base SQL utilise `numeric` avec contraintes de précision/échelle plutôt que `numeric(12,2)` qui arrondirait silencieusement une entrée trop précise.

POST CLIENT `/{ticketId}/proposals` remplace la proposition complète par un nouveau snapshot en OPEN, avec If-Match ; omis/null = non renseigné dans cette nouvelle version. Tout CLIENT actuellement habilité au site peut le faire, pas seulement l'auteur initial. ADMIN consulte mais ne réécrit pas cette proposition ; les révisions financières ADMIN du lot 07 resteront distinctes.

### Cycle explicite

| Acteur / suffixe POST | Source | Cible / effet |
|---|---|---|
| ADMIN `/qualify` | OPEN | QUALIFIED ; priorité retenue et motif publics ; `contractRevisionId` null = hors contrat, sinon couverture à la date de déclaration |
| ADMIN `/start` | QUALIFIED | IN_PROGRESS |
| ADMIN `/wait-client` | QUALIFIED/IN_PROGRESS | WAITING_CLIENT, motif et heure conservés ; aucune pause SLA générale déduite |
| ADMIN `/resume` | WAITING_CLIENT | IN_PROGRESS |
| ADMIN `/resolve` | QUALIFIED/IN_PROGRESS/WAITING_CLIENT | RESOLVED avec compte rendu public, y compris traitement à distance sans mission |
| CLIENT `/report-persistence` | RESOLVED | IN_PROGRESS, nouvelle entrée d'historique, ancienne résolution conservée dans le journal |
| CLIENT `/confirm-resolution` | RESOLVED | CLOSED, confirmation attribuée au CLIENT habilité |
| ADMIN `/cancel` | Tout état sauf CLOSED/CANCELLED | CANCELLED, motif public obligatoire, aucune suppression ou pénalité financière |

Toutes les transitions nécessitent If-Match et Idempotency-Key. Ni PATCH d'état arbitraire, seconde qualification ou réouverture de CLOSED/CANCELLED. La création/résolution d'une mission future ne modifie pas automatiquement le cycle du ticket.

### Échanges, idempotence et erreurs

Messages publics et notes internes : tables, routes et curseurs distincts, texte maximum 4 000. Append-only, pas d'édition/suppression. Pas d'ajout en CLOSED/CANCELLED. If-Match contrôle le cycle courant ; un ajout de message ne modifie pas la version du ticket. Une note interne ne crée ni trou de curseur public, changement de version ou événement public indiquant son existence.

Création de ticket, proposition, transitions et messages : clé 16–100 caractères `[A-Za-z0-9_-]`, liée à l'acteur/opération, empreinte canonique à préfixes de longueur, conservation technique 24 h. Même clé/contenu = même reçu minimal ; différent/expiré = 409. Périmètre et agrégat verrouillés **avant** consultation de la réception ; après révocation, archivage ou suspension, un rejeu ne révèle pas l'ancien résultat. L'expiration ne permet pas de réutiliser silencieusement la clé avant une politique de purge explicite.

Reçu `{resourceId,version,state}` : ticket/version pour déclaration/cycle/proposition, messageId/numéro pour message. Reçu historique stable, pas une promesse d'état courant ; relire le détail/ETag après une réponse ou un timeout ambigu. Les créations/révisions ordinaires de contrat utilisent la version sans mécanisme de rejeu ; ne pas les relancer automatiquement après une réponse perdue.

Session WEB courante, ACTIVE, email vérifié et rôle exclusif ; ADMIN exige preuves primaire/MFA <5 min. CSRF sur toutes les mutations ; corps bornés avant parsing à 16 KiB, DTO fermés, clés JSON dupliquées et valeurs numériques d'enum refusées. Données textuelles non exécutées comme HTML et aucune URL téléchargée côté serveur. Réponses privées/erreurs no-store ; 400 entrée/précondition absente ou mal formée, 401 authentification absente, 403 rôle/preuve, 404 hors périmètre/absent, 409 état/couverture/idempotence, 412 version périmée, 413 corps trop grand. Aucun SQL ou contenu privé dans les erreurs.

## Migrations et événements

- **V14** : contrats/révisions, couverture avec FK composites, calendriers normalisés et objectifs ; révision courante référencée par une FK différée. Pas de backfill métier.
- **V15** : tickets et FK compte/site/équipement/contrat, propositions/historiques/messages et notes append-only ; droits UPDATE limités aux colonnes de cycle/version, pas aux parents. Extension additive nullable des réceptions techniques, sans changer le résultat JSON de l'admission. Métadonnées d'outbox métier transactionnelles.
- **V16** : enfants d'une définition insérables uniquement dans la transaction créant sa révision, identifiée côté SQL et estampillée par trigger. Même INSERT tardif ne peut ajouter une couverture/plage/règle à une ancienne révision. Fonctions à privilèges invocateur et search_path fixé ; runtime ne peut ni désactiver les triggers ni mettre à jour la définition.
- Événements métier et audits atomiques ; un échec d'audit/outbox annule ticket, proposition, historique et réception, ou toute la révision contractuelle. L'outbox ne contient que références/versions/acteurs/types et dates, jamais description, montants, notes ou adresses.

L'outbox prépare les effets du lot 11 : **aucun nouveau worker de livraison, email externe, notification garantie ou alerte SLA revendiqué dans ce lot**. Les événements internes ne sont pas des notifications CLIENT. La future livraison doit revalider destinataires et portée, avec déduplication/reprises.

## Limites et suite

La recette locale globale est obtenue ; [preuves de clôture et limites](preuves/lot-06-verifications.md). Les fixtures isolent les contrats/tickets avec dossiers synthétiques APPROVED ; la suite d'admission HTTP réelle est conservée.

Pas de frontend, mission/affectation, compte rendu attesté, stockage/antivirus/upload, facturation ou prix publié technicien. Pas de rétention/purge complète, calcul d'échéance/alerte SLA, statistique, charge cible, image runtime renouvelée, CI distante, déploiement, staging ou pentest. Les futures clôtures avec missions devront vérifier les travaux actifs par un port dédié ; le présent lot n'invente pas une autorisation toujours vraie pour des missions inexistantes.

Suite possible : **07 — missions, conditions financières ADMIN et découverte géographique**, après ses seuls arbitrages nécessaires ; aucun démarrage automatique. Justificatifs technicien obligatoires et blocage du lot 09 conservés, Google toujours désactivé, risques d'infrastructure/production antérieurs non levés.
