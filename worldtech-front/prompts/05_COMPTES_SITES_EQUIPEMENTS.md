# FE05 — Comptes clients, sites et équipements

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Fournir le parc nécessaire aux déclarations et à l'administration, sans fuite entre clients ni rattachements incohérents.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE05 ; portée par compte/membre/site et DTO de contacts privés.

## Lecture obligatoire
- docs/ARCHITECTURE.md
- docs/UX_ECRANS.md
- docs/API_CONTRATS.md
- docs/SECURITE.md

## Travail demandé
1. Implémente les listes et fiches compte/site/équipement à partir des DTO réels. Distingue l'identité d'un utilisateur et le compte client auquel il appartient.
2. Branche les droits par compte et site autorisé. Un membre CLIENT peut être limité à certains sites ; ne crée pas un quatrième rôle pour représenter cette portée.
3. Expose création/modification/archivage seulement lorsque le contrat et les capacités le permettent. Le client ne reçoit pas automatiquement un droit de gestion du parc.
4. Construit les formulaires avec rattachements explicites : site au bon compte, équipement au bon site. Un incident de réseau général n'exige pas artificiellement un équipement.
5. Prépare les sélecteurs paginés nécessaires à FE06, recherche serveur et états vides adaptés. Ne charge pas tout le parc national dans le navigateur.
6. Présente contacts et informations de site selon visibilité réelle ; les données privées ne doivent pas être récupérées pour être simplement cachées.
7. Gère ETag/version et modification concurrente, null/valeurs manquantes et suppression ou archivage selon commandes exposées.
8. Préserve les filtres et l'état au retour des fiches ; le téléphone conserve les informations utiles sans toutes les colonnes.

## Vérifications et critères d'acceptation
- Client A ne lit pas site/équipement/contact de B via liste ou URL directe.
- Membre limité à un site ne voit pas les autres dans les sélecteurs, compteurs ou recherche.
- Rattachement croisé site/équipement refusé réellement.
- Modification périmée : saisie préservée et résolution explicite.

## Livrables
- Parc Client/Admin, formulaires autorisés et sélecteurs.
- Contrats de portée et comportement de concurrence documentés.
- Compte rendu FE05 avec preuves de cloisonnement.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucun import massif, inventaire automatique ou nouveau rôle ne s'ajoute au lot sans besoin distinct.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

