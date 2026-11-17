# Matrice des autorisations et suivi de l'admission

Conception du 27 octobre 2026, suivi du 02 novembre 2026. Identité/admission/parc conservés ; contrats/tickets du lot 06 implémentés, recette locale décrite dans [les preuves courantes](../preuves/lot-06-verifications.md). Lots suivants non livrés. Exigences SEC-01/02/03 et SEC-06 à SEC-16 ; ADMIN/CLIENT/TECHNICIAN seulement. Attente, suspension et pré-MFA restent des états.

## Principes communs

- Refus par défaut ; autorisation effective = rôle autorisé **et** identité/niveau MFA valides **et** statut actuel **et** périmètre **et** état de ressource compatible.
- Le backend détermine l'acteur et le propriétaire depuis son contexte, pas depuis un `userId`, rôle ou propriétaire fourni par le demandeur.
- Les listes, totaux, recherches, exports et documents sont filtrés par compte/site/affectation **avant** pagination ou agrégation. Pas de chargement global puis filtrage Java.
- Aucun droit durable déduit d'un ancien cookie, refresh, rôle en cache ou résultat d'idempotence. Recontrôle au téléchargement et au rejeu ; accès refusé si le stockage d'autorisation est indisponible.
- CLIENT/ADMIN : canal web Angular ; TECHNICIAN : parcours Flutter initial. N'exposer sur chaque canal que les opérations réellement nécessaires, sans dupliquer les règles métier.

## Matrice fonctionnelle

| Opération | Autorisé, sous conditions serveur | Refus / limites | Lot |
|---|---|---|---|
| Santé minimale de processus | Visiteur, `GET /health` intentionnel | Aucun détail DB, environnement ou configuration ; aucune autre méthode ouverte | 01 |
| Amorçage identité/invitation | Visiteur sur routes expressément publiques ; challenge/jeton valide selon le but | Pas de liste de comptes ni de promotion ; GET de lien sans consommation irréversible | 03/04 |
| Challenge MFA | Identité pré-MFA liée à son challenge, essais bornés | Pas d'autorité ADMIN effective ; seulement challenge/enrôlement autorisé, logout et récupération adaptée | 03 |
| `/me`, sécurité de son compte | Identité authentifiée selon niveau autorisé ; opérations sensibles après réauthentification | Jamais de secrets dans la réponse ; pré-MFA reste limité à son parcours | 03 |
| Compléter/soumettre dossier | CLIENT invité ou TECHNICIAN, sur son propre dossier dans un état autorisé | Attente ≠ activation ; aucune habilitation métier accordée par Google | 04 |
| Inviter, approuver, demander complément, refuser | ADMIN actif, email vérifié, preuves primaire/MFA <5 min ; décision motivée et auditée | Aucun DTO public ne fixe l'approbation ; aucune levée de suspension ; approbation technicien bloquée sans justificatifs contrôlés | 04 |
| Lire comptes/sites/parc | ADMIN avec MFA ; CLIENT actif, membre actuel du compte et habilité sur le site | Aucun autre client, contact privé ou site non autorisé | 05 |
| Modifier parc/contrat | ADMIN actif/vérifié, preuves primaire/MFA <5 min ; contrats par nouvelles révisions immuables | CLIENT lit seulement son périmètre ; aucun transfert intercompte/site ni mutation d'une ancienne couverture | 05/06 |
| Créer/suivre ticket, échanges publics | CLIENT actif du compte/site ; site et équipement cohérents | Pas de notes internes, autres sites ou montant publié admin ; parc non référencé n'interdit pas par principe l'incident | 06 |
| Qualifier ticket, créer/publier mission, fixer conditions | ADMIN avec MFA ; état et version valides | Aucune publication par CLIENT/TECHNICIAN ni déduction de facturation | 06/07 |
| Découverte de missions | TECHNICIAN actif, habilité, éligible ; publication/filtres/rayon et pagination bornés | Ni attente/suspension, ni coordonnées/adresse/point privé, ni fichiers privés | 07 |
| Candidater/retirer candidature | TECHNICIAN actif pour lui-même, mission et conditions courantes ; retrait selon état | Pas de candidature pour un tiers, doublon ou autres candidatures visibles ; retrait après confirmation hors procédure interdit | 08 |
| Sélectionner, réaffecter | ADMIN avec MFA ; verrou mission, état/candidature/éligibilité revalidés | Jamais deux affectations actives ; historique conservé ; pas d'appel externe sous verrou | 08 |
| Confirmer/refuser réservation | TECHNICIAN sélectionné actif ; réservation non expirée et révision attendue | Candidature ou sélection seules ne donnent pas le détail privé ; pas de confirmation hors ligne | 08 |
| Détail privé d'une mission | ADMIN avec MFA ; TECHNICIAN actif actuellement affecté **et confirmé** ; CLIENT via projection de son compte/site | Pas de contact/adresse exacte pour un candidat ou simple sélectionné ; refus au prochain accès après réaffectation | 07/08 |
| Upload/téléchargement | Acteur autorisé sur le parent et visibilité du document ; téléchargement seulement après `CLEAN` | Propriété de l'upload seule insuffisante ; quarantaine si scan absent/indisponible ; jamais de bucket public | 09 |
| Exécuter, brouillon, soumettre rapport | TECHNICIAN actif confirmé et actuellement affecté ; état/version/idempotence valides | Ancienne affectation, annulation ou suspension produisent un refus/conflit ; aucune réécriture du rapport soumis | 10 |
| Attester une version | Parcours de signataire habilité à préciser au lot 10 ; version/hash exacts | Un tracé au doigt ou le téléphone du technicien ne prouve pas une identité client ; délégation non supposée | 10 |
| Valider/corriger rapport, clôturer mission | ADMIN avec MFA ; version, revue et motif | Pas d'effacement d'attestation ; nouvelle version pour correction ; ticket non automatiquement clos | 10 |
| Notifications, statistiques, audit | Destinataire autorisé pour sa notification ; statistiques/audit ADMIN avec MFA et projection restreinte | Lien ancien sans droit actuel inutilisable ; pas de données secrètes, pas de compteur intercompte | 11/12 |

## Restrictions par champ / acteur

| Donnée | CLIENT actif dans son périmètre | TECHNICIAN | ADMIN avec MFA |
|---|---|---|---|
| Rôle, approbation, propriétaire, affectation | Pas d'écriture par DTO public | Pas d'écriture par DTO public | Commandes dédiées et audit ; bootstrap ADMIN hors API publique |
| Proposition financière client | Lecture/historique dans son site ; nouvelle version uniquement en OPEN | Non diffusée par défaut | Lecture seule ; conditions publiées au lot 07 distinctes, sans écraser la proposition CLIENT |
| Montants admin publiés / accord accepté | Seulement données commerciales expressément destinées à son espace | Révision publiée nécessaire à la candidature, puis version proposée/acceptée | Révisions motivées ; pas de mutation silencieuse de l'accord |
| Adresse/contact/consignes du site | Sites autorisés | Seulement affectation confirmée et besoin de mission | Accès professionnel contrôlé |
| Notes internes | Non | Non, sauf projection explicitement destinée à sa mission | Accès contrôlé ; aucune diffusion implicite |
| Rapports/pièces | Visibilité publique client + compte/site autorisés | Parent actuellement autorisé et visibilité technicien | Selon parent/visibilité ; scan exigé aussi pour ADMIN |
| Hashs de credentials, tokens, secrets TOTP/clés | Jamais via route métier | Jamais via route métier | Jamais via route métier ; cas d'usage de sécurité dédiés sans export brut |

Un compte en attente accède à son parcours d'inscription, pas aux opérations actives. Dans l'identité livrée, SUSPENDED/REJECTED ne peuvent pas s'authentifier ; le rejet d'admission révoque les sessions, sans réouverture automatique. Seul NEEDS_INFORMATION permet la correction et la resoumission du dossier. Les futures procédures de suspension/réaffectation ne donnent pas implicitement le droit de terminer hors ligne.

## Autorisations effectivement livrées au lot 04

| Parcours | Preuve et portée | Refus vérifiés |
|---|---|---|
| Inscription client locale | Token d'invitation non consommé/non révoqué/non expiré et email destinataire ; nouvel accès CLIENT PENDING + DRAFT ; CSRF web | Mauvais destinataire, token remplacé/expiré/rejoué, identité existante non authentifiée : aucune fusion ni compte parasite |
| Acceptation par identité existante | Session WEB courante, rôle CLIENT sans autre rôle, email vérifié destinataire | Autre client, technicien, ADMIN ou cumul CLIENT+TECHNICIAN ; pas de changement de rôle |
| Inscription technicien | Local web/CSRF ou mobile sans cookie ; rôle fixé TECHNICIAN et statut PENDING | Aucun champ role/owner/approved/CLEAN ; doublon local 202 sans modifier le compte existant |
| Inscription Google explicite | Contexte distinct du LOGIN, email OIDC vérifié ; invitation pour client ; handoff S256 pour mobile | LOGIN inconnu, email/triplet déjà présent, mauvais email/lien remplacé ; pas d'activation Google. Tests avec double local uniquement, option désactivée |
| Brouillon/historique propre | Session du bon canal, rôle candidat exclusif, PENDING/ACTIVE courant ; seul DRAFT/NEEDS_INFORMATION est modifiable | Autre UUID/propriétaire, rôle incompatible, suspension/refus/révocation ; aucun accès par cookie sur mobile |
| Soumission/rejeu | Même portée + email vérifié + formulaire complet ; version attendue, clé liée à acteur/opération/requête | Rejeu après changement de rôle/révocation refusé ; même clé/autre version 409, version périmée 412 |
| Liste/détail/historique ADMIN | WEB, actif, email vérifié, authentification primaire et MFA <5 min ; listes bornées avant restitution | CLIENT/TECHNICIAN, ADMIN pré-MFA ou preuve ancienne, mobile ; aucune liste/compteur globale candidat |
| Décision | Même preuve ADMIN ; dossier SUBMITTED, version attendue, cible PENDING vérifiée et rôle candidat exclusif | Auto-approbation, suspension, décisions concurrentes, motif absent ; cumul CLIENT+TECHNICIAN refusé sans modification du statut global |
| Approbation technicien | **Aucun succès autorisé** tant que pièces et contrôles documentaires manquent | 409 `TECHNICIAN_DOCUMENTS_REQUIRED`, aussi contrainte SQL ; compétences déclarées insuffisantes |

Le schéma d'identité préexistant autorise plusieurs lignes de rôle ; le lot 04 n'invente pas une politique de double admission. Il refuse les cumuls chez les candidats, à l'accès **et** avant décision. Ainsi l'approbation client ne rend pas un technicien actif par le statut global ACTIVE. Aucun nettoyage/conversion de rôle n'est appliqué aux données existantes. Une éventuelle double activité exige un contrat d'habilitations distinct et testé.

## Politique de refus et preuve attendue

### Comptes et parc effectivement livrés au lot 05

| Opération | Portée autorisée | Refus / contrôle |
|---|---|---|
| Création compte | ADMIN récent/MFA, dossier APPROVED et CLIENT actif/vérifié exclusif | Aucun DTO type/titulaire/statut ; FK composite source validée ; aucun site implicitement accordé |
| Rattachement/retrait membre | ADMIN, compte cible ; CLIENT déjà validé, actif, vérifié ; INDIVIDUAL limité au titulaire | Pas de création/association par email ; retrait effectif session inchangée, nouvelle adhésion sans ancien droit |
| Lecture compte | CLIENT actif/vérifié exclusif et adhésion courante ; ADMIN récent/MFA | Filtre SQL avant liste/total/détail, 404 pour autre compte |
| Sites/contacts/équipements | Adhésion + site explicitement accordé ou ALL courant ; ADMIN dans compte demandé | Portée SQL avant pagination/count/projection ; site caché en filtre = 404, catégorie sans résultat scoped = zéro |
| Modifications parc/droits | ADMIN seulement, CSRF, version attendue ; relations de même compte | CLIENT/TECHNICIAN/attente/pré-MFA refusés sur POST/PUT/PATCH ; DTO fermés ; 412 version périmée |
| Archivage | ADMIN avec version ; données/références conservées | Pas de suppression définitive ; site archivé masque ses enfants, aucune cascade |
| Export/document/découverte | Aucun nouvel endpoint dans ce lot | Contacts/consignes/coordonnées non exposés aux techniciens ni dans une projection publique |

Le manque de permission ne donne pas un total global. Le privilège de site appartient à une adhésion, pas à un email ; connaissance de l'UUID, ancienne session ou ancien ETag insuffisants. Les champs privés ne sont ni copiés dans les audits ni restitués dans les refus.

### Contrats et tickets du lot 06

| Opération | Autorisé | Refus / contrôle |
|---|---|---|
| Contrat/révisions/calendrier/SLA | ADMIN WEB récent/MFA, compte autorisé, couverture de sites/équipements cohérents | CLIENT/TECHNICIAN/attente/pré-MFA ; pas de réécriture d'une révision ni ajout SQL tardif à ses enfants |
| Lecture contractuelle CLIENT | Compte/site actif actuellement accordé ; projection de ce seul site | Aucune note/motif interne, autre site ou équipement d'un autre site ; SQL avant pagination/total |
| Déclaration de ticket | CLIENT actif vérifié exclusif ; site autorisé et équipement cohérent si fourni | Aucun choix d'auteur, compte parent, priorité retenue, état, contrat ou prix ADMIN ; hors contrat et sans montant acceptés |
| Proposition CLIENT | Tout CLIENT courant habilité au site, OPEN et version attendue | ADMIN ne la réécrit pas ; pas de modification après qualification ; null distinct de zéro, chaînes exactes HT/EUR |
| Qualification/traitement/résolution/annulation | ADMIN récent/MFA, état et version, motif public | Aucune résolution/clôture déduite d'une visite ; aucune facture ou SLA implicite |
| Confirmation/persistance | CLIENT courant habilité, RESOLVED et version attendue | Pas d'action ADMIN au nom du CLIENT ; CLOSED/CANCELLED non réouvrables dans ce lot |
| Échanges publics | CLIENT du site et ADMIN récent/MFA ; ticket non terminal | Table/curseur séparés des notes internes ; aucune visibilité choisie par un champ du corps |
| Notes internes | ADMIN récent/MFA seulement | Aucun contenu, compteur, trou de curseur ou changement de version publique révélant l'ajout d'une note |
| Rejeu technique | Même acteur/opération/clé/contenu, accès courant réautorisé et agrégat verrouillé avant réception | Droits retirés, site archivé ou suspension refusent aussi le rejeu ; résultat minimal historique, pas un nouveau droit |

Le refus de couverture est générique (`CONTRACT_NOT_APPLICABLE`) aussi bien pour une révision inconnue, étrangère, hors période ou inadaptée à l'équipement. Aucun endpoint d'upload, pièce distante, mission/affectation ou découverte n'est ouvert par cette tranche. Les événements d'outbox sont des métadonnées, pas des notifications publiquement consultables.

- 401 JSON pour identité absente/invalide sur une route protégée ; aucune page HTML de login ni redirection d'API inattendue.
- 403 pour rôle, niveau MFA, statut ou CSRF insuffisant. Pour une lecture d'objet par un acteur authentifié du bon type mais hors périmètre, 404 masqué identique à l'absence, sans révéler l'existence de l'objet.
- Les listes autorisées ne contiennent que le périmètre courant, y compris les totaux. Les erreurs ne révèlent pas les identifiants ou valeurs d'un autre compte.
- Chaque commande refusée doit être vérifiée aussi en base et dans l'outbox/stockage : pas seulement son code HTTP. Aucun effet externe indu après refus.
- Jeux de recette : clients A/B, au moins deux sites dont un non accordé, techniciens T1/T2, ADMIN pré-MFA/MFA, attente/suspendu/refusé, acteur réaffecté. Tester aussi téléchargement, export, compteur et rejeu avec les mêmes permutations.

Cette matrice donne une **limite conservatrice**, pas une validation des habilitations futures. Le parc CLIENT est confirmé en lecture seule ; liste exacte des justificatifs et signataire restent à arbitrer. Les justificatifs technicien sont obligatoires et ne peuvent pas être désactivés pour contourner le lot 09.
