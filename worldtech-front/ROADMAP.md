# Ordre de réalisation
Les lots FE sont frontend ; les lots BE sont ceux du cahier Spring Boot. Une intégration exige les contrats et fonctions BE correspondants. Des simulations isolées permettent d'avancer, sans valider le backend.

Décision utilisateur FE01 : **outils gratuits uniquement**, Angular Material/CDK MIT remplace les mentions PrimeNG/preset des prompts historiques. Lire [ADR005](docs/adr/ADR005_OUTILS_LIBRES.md) et le design system actualisé ; aucun outil payant n'est autorisé par le lancement d'un lot.

| Lot | Prompt | Dépendances | Résultat observable |
|---|---|---|---|
| FE00 | [Audit](prompts/00_AUDIT_ET_DECISIONS.md) | Dépôt cible ou vide | Inspection et décisions |
| FE01 | [Socle](prompts/01_SOCLE_ANGULAR.md) | FE00 | Build, routes et contrôles initiaux |
| FE02 | [Design](prompts/02_DESIGN_SYSTEM_ET_SHELLS.md) | FE01 | Deux thèmes, composants, écran de référence |
| FE03 | [Identité](prompts/03_AUTH_SESSION_CSRF_MFA.md) | FE01–02, BE03 | Session, CSRF et MFA intégrés |
| FE04 | [Onboarding](prompts/04_ONBOARDING_ET_VALIDATIONS.md) | FE03, BE04 | Invitation, dossier et décisions |
| FE05 | [Parc](prompts/05_COMPTES_SITES_EQUIPEMENTS.md) | FE04, BE05 | Périmètres compte/site |
| FE06 | [Tickets](prompts/06_CONTRATS_TICKETS_MONTANTS_CLIENT.md) | FE05, BE06 | Déclaration et qualification |
| FE07 | [Missions](prompts/07_MISSIONS_PUBLICATION_CONDITIONS.md) | FE06, BE07 | Publication et révisions |
| FE08 | [Affectations](prompts/08_CANDIDATURES_AFFECTATIONS.md) | FE07, BE08 | Sélection et suivi de confirmation |
| FE09 | [Documents](prompts/09_DOCUMENTS_PRIVES_UPLOADS.md) | FE05, BE09 | Pièces privées et état de scan |
| FE10 | [Rapports](prompts/10_INTERVENTIONS_RAPPORTS_ATTESTATION.md) | FE08–09, BE10 | Revue des versions et PDF |
| FE11 | [Notifications](prompts/11_NOTIFICATIONS_ACTUALISATION.md) | FE03+, BE11 | Liens, lecture et rafraîchissement |
| FE12 | [Indicateurs](prompts/12_SLA_STATISTIQUES.md) | FE06–11, BE12 | Agrégats documentés |
| FE13 | [Performance](prompts/13_PERFORMANCE_ACCESSIBILITE.md) | Parcours réalisés, BE13 selon mesure | Profilage et corrections ciblées |
| FE14 | [Livraison](prompts/14_CI_DEPLOIEMENT_RECETTE.md) | Lots concernés, BE14 | Recette et déploiement vérifiés |

FE09 peut être avancé dès que BE09 et FE05 sont disponibles. Si les photos sont nécessaires pour la première recette de FE06, avancer FE09 avant cette recette plutôt que simuler un upload finalisé.
FE11 complète des notifications et rafraîchissements minimaux introduits avec les premiers événements.
MFA, autorisations, accessibilité et contrôles CI ne sont pas reportés à la fin.

## Première tranche exploitable
Client invité et approuvé → ticket → mission publiée → candidature et confirmation dans Flutter → réalisation mobile → revue du rapport dans Angular → suivi client.

## Passage entre lots
Le prompt du lot suivant est lancé lorsque le bilan précédent est compris et que ses dépendances utiles sont satisfaites. Une maquette vérifiée peut être terminée comme maquette, mais l'intégration du lot reste à faire si Spring Boot n'a pas encore été testé.
