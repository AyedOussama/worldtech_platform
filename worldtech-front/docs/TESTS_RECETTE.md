# Recette et vérifications

## Niveaux de preuve
1. Unitaire : formatage, adaptation, erreurs et comportements de services.
2. Composant : interactions réelles, états de formulaire et focus.
3. Navigateur avec simulation : scénarios reproductibles de réponses HTTP.
4. Intégration Spring Boot : identité, autorisations, transitions et fichiers réels.
5. Recette visuelle et manuelle : lisibilité, téléphone, clavier et thèmes.

Les niveaux 1–3 ne remplacent pas le niveau 4. Les tests ciblent des comportements significatifs et les risques introduits ; pas de miroir de chaque méthode ou de chaque classe CSS.

FE03 : `npm run test:e2e` utilise les doubles HTTP d'identité dans `app/e2e/`, jamais importés par Angular ; `npm run test:e2e:integration` lance réellement Spring/Tomcat/PostGIS sur un environnement éphémère distinct de la base existante. Les actions email sont émises par le vrai domaine de la fixture, sans livraison SMTP. Captures réelles désactivées pour les secrets d'enrôlement ; captures/axe synthétiques sous `verification/fe03/`. Voir [les résultats FE03](../verification/fe03/RESULTATS.md), sans assimiler cette recette locale à HTTPS/Google/production.

## Jeu de recette
Deux comptes clients avec sites différents, un membre client limité à un site, un client en attente, deux techniciens actifs, un technicien en attente et un administrateur pré/post-MFA.
Données synthétiques, domaines example.test, identifiants et horaires déterministes. Aucun mot de passe ou cookie de production dans les fixtures.

## Matrice minimale
| ID | Scénario | Résultat |
|---|---|---|
| R01 | Invitation expirée ou consommée | Pas d'activation, reprise explicite |
| R02 | Client non approuvé déclare un ticket | Refus effectif, état utile |
| R03 | Client A ouvre ticket/fichier B | Aucun contenu de B, API réelle |
| R04 | Admin pré-MFA ouvre mission | Aucun privilège admin |
| R05 | Token CSRF absent | Mutation refusée |
| R06 | Incident de réseau sans équipement | Déclaration autorisée selon contrat |
| R07 | Site/équipement incompatibles | Erreur, choix et saisie préservés |
| R08 | Proposition client différente du montant publié | Valeurs distinctes et compréhensibles |
| R09 | Deux sélections ou sélection déjà périmée | Conflit explicite, pas de faux succès |
| R10 | Timeout après commande | Vérification/rejeu idempotent convenu |
| R11 | Upload terminé mais scan en cours | Pièce indisponible au téléchargement |
| R12 | Rapport corrigé | Ancienne version et attestation conservées |
| R13 | Mission terminée, ticket encore ouvert | Affichage distinct |
| R14 | Filtres puis fiche puis retour | Contexte de liste restauré |
| R15 | Noir/clair, zoom, clavier, téléphone | Actions et messages lisibles |
| R16 | Logout pendant requête | Aucune donnée privée réinjectée |
| R17 | Réponse d'ancienne recherche après la nouvelle | Résultat courant conservé |
| R18 | Créneau au changement d'heure | Fuseau et heure corrects |

## Contrôle visuel
Au moins connexion, liste de tickets, formulaire, détail mission et revue rapport en clair/sombre et largeur téléphone/ordinateur.
Captures déterministes sans données personnelles. Les images montrent le résultat ; elles ne prouvent pas l'autorisation serveur.
Tester menus, dialogues et retours de focus ; le scan axe est complété par clavier et lecteur d'écran.

## Rapport
Pour chaque contrôle : commande ou geste, environnement, résultat observé, fichier de preuve si nécessaire et limite.
Les commandes attendues sont définies au FE01 puis adaptées au dépôt. Une commande absente ou un serveur indisponible est signalé ; jamais transformé en succès.

## Parcours final
Invitation → validation → ticket → mission → candidature/confirmation mobile → intervention/attestation mobile → revue web → consultation client.
Aucun remplacement de l'étape technicien par un privilège admin non prévu.
