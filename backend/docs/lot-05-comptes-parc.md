# Lot 05 — comptes, membres, sites et équipements

01/11/2026. **Implémenté et vérifié localement : 111 Surefire + 132 Failsafe = 243 tests Java**, zéro échec/erreur/skip à 22:26:29 ; six tests Python également réussis. [Preuves, scans ciblés et limites](preuves/lot-05-verifications.md). Aucune approbation de production.

## Décisions confirmées

- ADMIN seul crée/modifie comptes, membres, habilitations, sites, contacts et équipements. CLIENT actif consulte uniquement son périmètre. Pas d'API mobile dans ce lot.
- ADMIN crée explicitement un compte depuis un dossier client APPROVED ; type BUSINESS/INDIVIDUAL issu du dossier et immuable. Pas de création automatique de site depuis une adresse personnelle.
- INDIVIDUAL limité au titulaire ; BUSINESS accepte des CLIENT actifs, vérifiés et déjà validés, désignés par UUID interne. Pas de fusion email ni de nouveau parcours d'inscription membre.
- Aucun accès site par défaut, y compris pour le premier membre. ADMIN accorde une liste explicite ou tous les sites. Une réadhésion après retrait utilise une nouvelle adhésion et ne ressuscite pas les anciens droits.
- Site : nom, adresse et fuseau IANA obligatoires ; contacts et consignes facultatifs. Contact privé ≠ utilisateur/membre.
- Équipement : libellé/catégorie obligatoires ; référence interne, marque/modèle/série, emplacement, état descriptif et attributs complémentaires facultatifs. Attributs v1 : objet plat, au plus 20 clés ASCII et valeurs textuelles bornées. Pas de catalogue certifié, inventaire automatique ou géocodage.
- Pas de suppression définitive, transfert intercompte ou changement de site d'un équipement. Retrait des membres et archivage explicite du parc conservent les références et traces.

## Implémentation et contrat

`clients` possède comptes et adhésions ; `assets` possède sites/contacts, équipements et habilitations de sites. Accès interdomaines via capacités publiques uniquement. Les FK intertables assurent l'intégrité structurelle, sans requête repository intermodule.

Session WEB courante, statut ACTIVE, email vérifié et rôle exclusif recontrôlés. ADMIN : preuves primaire/MFA <5 minutes, comme l'admission précédente. Verrous utilisateurs triés → compte → site/équipement/habilitation ; contrôle de périmètre SQL avant pagination, compteur et projection privée. Aucun cache d'autorisation.

### API web

Préfixes : `/api/web/v1/admin/client-accounts` pour ADMIN ; `/api/web/v1/client/accounts` pour les lectures CLIENT. [OpenAPI 0.5.0](../openapi/maintenance-api.json) : 92 chemins et 52 schémas, identité/admission comprises.

- POST ADMIN `{dossierId}` crée compte et premier membre, sans changer le statut d'identité ni accorder de site. 201/Location/ETag ; même source répétée : 200, compte existant, sans rétablir un membre retiré.
- GET liste/détail : compte actuel autorisé ; pages de 20 par défaut, maximum 100, page 0–10 000, tri stable création/UUID. PATCH `/{accountId}` corrige seulement `displayName` (240 caractères).
- `/members` : liste ADMIN des adhésions actives et POST `{userId}`. Location consultable, y compris après retrait. Retrait par POST `/members/{memberId}/remove` avec version, jamais suppression d'identité ou des autres adhésions.
- `/members/{userId}/site-access` : GET/PUT ADMIN. `userId` est la cible, pas l'acteur. `{allSites:false,siteIds:[]}` révoque les sites ; `allSites:true` exige une liste vide et inclut les futurs sites actifs. Sinon au plus 100 UUID distincts du même compte. Version initiale 0 sans politique ; remplacement versionné et tracé.
- `/sites` : GET scoped ; POST ADMIN `{name,address,timeZone,contacts,accessInstructions}`. Au plus 10 contacts privés nominatifs, téléphone/email facultatifs validés ; pas de compte créé pour un contact. Fuseau IANA du JDK, sans offset libre ni choix géographique métier implicite.
- `/equipment` : GET scoped avec filtre optionnel `siteId`/`category`. Filtre de site étranger/non accordé/absent : 404. Création ADMIN sous `/sites/{siteId}/equipment` ; aucun compte ou site choisi dans le corps.
- Détails `/sites/{siteId}` et `/equipment/{equipmentId}` : GET et modifications ADMIN PUT/PATCH. **PUT remplace le profil complet** (facultatifs absents/null effacés). **PATCH préserve les absents/null**, remplace les contacts/attributs fournis ; utiliser PUT pour effacer un facultatif. Parent, propriétaire, archivage et version ne sont pas des champs de formulaire.
- POST `.../archive` archive avec précondition. Site archivé = équipements enfants masqués des lectures actives, sans suppression/cascade ; pas de restauration/transfert dans ce lot.
- `/summary` compte uniquement les sites/équipements actuellement visibles non archivés, avec la même portée SQL que les listes.

Réponses privées/erreurs `no-store`. CSRF sur POST/PUT/PATCH. Corps ADMIN 16 KiB avant parsing ; DTO fermés, clés JSON dupliquées refusées. Modifications avec `If-Match: "<version>"` : absent/mal formé 400, périmé 412 ; conflit métier 409. Un ETag ne vaut pas autorisation. Mauvais rôle/MFA : 403 ; objet absent ou hors périmètre : 404 masqué. Aucun export/téléchargement ou chemin technicien anticipé.

Attributs d'équipement v1 : objet plat, 20 clés maximum `[A-Za-z][A-Za-z0-9_-]{0,39}`, valeurs textuelles non blanches de 200 caractères, sans contrôles. Nombres, structures imbriquées et null refusés sans coercition. Aucune interprétation HTML/URL/script. Catégorie et état descriptif sont des déclarations bornées, pas un catalogue ou une habilitation certifiés.

### Données, audits et concurrence

- V12 : comptes/adhésions et événements append-only ; FK composite imposant source APPROVED, type et titulaire. INDIVIDUAL ne reçoit pas un autre titulaire, même hors HTTP. Unicité partielle des adhésions actives, aucun backfill implicite.
- V13 : sites/contacts JSONB, équipements, politiques/listes de sites et événements. FK composites compte/site/équipement et compte/adhésion/habilitation ; validation SQL des attributs en plus de l'application. Aucun DELETE runtime sur comptes/membres/parc/historiques ; seules les lignes de droits courants sont remplacées, avec snapshot historique de la portée.
- Mutations : références de version, auteur, compte et action dans les événements, plus audit technique corrélé traceId. Coordonnées/consignes/attributs libres/credentials ne sont pas copiés dans les audits/logs. Une panne d'audit annule la mutation.
- `ClientAccountAccess` autorise et verrouille compte/adhésion dans la transaction assets ; `BusinessIdentity` garde les vérifications SQL d'identité. Aucune requête repository sur les tables d'un autre domaine ; les FK sont structurelles.
- Le verrou de compte sérialise actuellement lectures et retraits/changements de droits d'un même compte. Choix conservateur de cohérence, pas mesure de capacité : charge et dimensionnement restent au lot 13.

## Limites et suite

Fixtures d'identités/dossiers synthétiques approuvés pour isoler comptes/parc ; l'admission HTTP du lot 04 reste dans la suite globale. Aucun frontend, déploiement, email externe, migration Compose persistante, contrat commercial, ticket, finance, géocodage, découverte technicien, upload ou suppression définitive. Rétention/purge et consultation des historiques métier complets restent à leurs lots.

Approbation technicien bloquée sans pièces contrôlées au lot 09 ; Google désactivé. Risques d'images du lot 02 et conditions de production non résolus. Suite possible : lot 06, après arbitrages de son contrat financier/SLA, sans démarrage automatique.
