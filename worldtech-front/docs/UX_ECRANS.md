# Écrans, tâches et états

## Accès
| ID | Écran | Acteur | Actions |
|---|---|---|---|
| AUTH01 | Connexion | Utilisateur web | Email/mot de passe, Google configuré, récupération |
| AUTH02 | Vérification MFA | Administrateur pré-MFA | Code, récupération prévue par le backend |
| AUTH03 | Mot de passe oublié / réinitialisation | Identité éligible | Demande générique puis saisie sur challenge valide |
| AUTH04 | Vérification email | Identité concernée | Vérifier, renvoyer selon limites serveur |
| ONB01 | Invitation client | Destinataire | Consulter l'état du lien et compléter l'identité |
| ONB02 | Dossier client | Client en onboarding | Enregistrer, soumettre, compléter après demande |
| ONB03 | En attente / décision | Client concerné | Voir le statut et l'action autorisée |

Aucun sélecteur de rôle public donnant accès à ADMIN. Pas d'inscription technicien web complète ajoutée au lancement. Les techniciens s'inscrivent et complètent leur profil dans Flutter.

## Client
| ID | Écran | Contenu / actions |
|---|---|---|
| CLI01 | Incidents | Liste, recherche, filtre, déclaration |
| CLI02 | Déclarer | Site obligatoire autorisé, équipement optionnel lié, objet, description, priorité, pièces, proposition si prévue |
| CLI03 | Détail incident | Statut, prochaines étapes, échanges publics, visites, historique |
| CLI04 | Sites | Liste et fiche dans la portée de l'utilisateur |
| CLI05 | Équipements | Rattachement et attributs autorisés |
| CLI06 | Interventions | Informations des visites accessibles |
| CLI07 | Rapports | Version, attestation accessible et téléchargement autorisé |
| CLI08 | Compte | Profil, sécurité et thème |

Les actions de création/modification de sites et équipements dépendent du contrat d'habilitation réel. La liste seule ne suppose pas un droit d'administration du parc.

## Administrateur
| ID | Écran | Contenu / actions |
|---|---|---|
| ADM01 | À traiter | Tickets, dossiers, rapports nécessitant une action |
| ADM02 | Clients | Invitation, dossier, décision et suspension autorisée |
| ADM03 | Techniciens | Profil, compétences, justificatifs, demande de complément, décision |
| ADM04 | Parc | Comptes, sites, contacts et équipements |
| ADM05 | Contrats | Versions, couverture, calendrier et règles exposées |
| ADM06 | Tickets | Qualification, priorité retenue, échanges publics et internes |
| ADM07 | Missions | Création, révision financière, publication, annulation |
| ADM08 | Candidatures | Comparaison, sélection, état de confirmation |
| ADM09 | Planning | Visites et filtres ; déplacement seulement si commande métier prévue |
| ADM10 | Rapports | Lecture, revue, demande de correction, clôture de mission |
| ADM11 | Statistiques | Indicateurs, périodes et dénominateurs |
| ADM12 | Configuration / audit | Fonctions réellement exposées et autorisées |

## Liste de tickets de référence
Rapprochement FE00 : cette liste reste une cible UX, pas une description de l'API livrée. Le contrat 0.6.0 propose des listes par compte/site, sans référence humaine de ticket, `updatedAt`, recherche textuelle ou tri libre. Les démonstrations FE02 devront être isolées ; FE06 adaptera les colonnes/filtres ou documentera un besoin backend, sans inventer ces champs. Voir [le relevé](CONTRAT_BACKEND_RELEVE_FE00.md).

En-tête : « Tickets », filtres utiles, recherche, action adaptée au rôle.
Colonnes : Référence, Objet, Site, Priorité, Statut, Actualisé. Ajouter le client seulement dans l'espace administrateur si utile ; afficher la prochaine action dans un emplacement stable.
Le lien principal porte un nom accessible. Les menus d'action ne se confondent pas avec l'ouverture de fiche. Filtres et pagination sont conservés dans l'URL lorsque pertinent.
Sur téléphone, garder objet, site, priorité et statut lisibles. Les colonnes secondaires ne sont pas simplement réduites à 10 px.

## Fiche de ticket
Référence + objet + état, prochaine action, description et pièces, site et équipement, échanges, missions et historique.
Ne pas dupliquer toutes ces sections dans une colonne latérale trop étroite. Les informations utiles à la décision sont proches de l'action.
La note interne est identifiée et absente du DTO client. L'historique client affiche seulement les événements publics.

## États obligatoires
Chargement initial, vide, aucun résultat, erreur récupérable, non autorisé, introuvable/masqué selon contrat, session expirée, actualisation, envoi et conflit.
Un skeleton reproduit une structure probable ; il ne crée pas un clignotement à chaque refresh.
Une sélection reste stable pendant le polling. Si sa ressource change d'état, une indication explique pourquoi une action disparaît.

## Mesures de recette UX
- Un client peut déclarer un incident à partir d'un site autorisé sans comprendre l'architecture du système.
- L'administrateur identifie l'objet, l'état et l'action suivante sans lire un slogan.
- Une recherche sans résultat peut être réinitialisée.
- Une erreur serveur ne détruit pas le formulaire.
- Un utilisateur clavier peut parcourir la liste, ouvrir une fiche et revenir.
- Les montants initiaux, publiés et acceptés ne sont pas ambigus.
