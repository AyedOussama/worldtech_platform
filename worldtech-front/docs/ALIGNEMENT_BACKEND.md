# Alignement avec Spring Boot
Source : Cahier_backend_SpringBoot_v1.md, texte révision 1.1, 27 octobre 2026, consulté le 10 novembre 2026. Ce document est une synthèse frontend, pas une copie des migrations ni une certification du code backend.

## Rapprochement FE00 — 10/11/2026
Backend local identifié dans `../backend`, lots identité/admission/parc/contrats/tickets présents. Source effective : OpenAPI 3.1.0 / API 0.6.0, 117 chemins, 133 opérations, 83 schémas. [Contrat relevé, écarts et fonctions absentes](CONTRAT_BACKEND_RELEVE_FE00.md). Aucun test Spring Boot ni échange HTTP réalisé par FE00.

Décisions déjà documentées par le backend : CLIENT consulte seulement son parc ; ADMIN seul le modifie. Proposition CLIENT en deux forfaits facultatifs HT/EUR, rémunération et frais séparés, null distinct de zéro. Contrats à révisions immuables et couverture par site ; déclaration possible hors contrat. Ces décisions priment sur les questions encore ouvertes du pack ; elles ne fixent pas les futures conditions ADMIN/technicien. Missions, affectations, documents/rapports et indicateurs restent absents du contrat courant. Approbation technicien bloquée sans justificatifs contrôlés ; Google présent mais désactivé.

## Contrats structurants
- Spring Security, sessions web JDBC et CSRF ; mobile par jetons opaques séparés.
- OAuth Google facultatif et MFA ADMIN obligatoire.
- DTO et commandes explicites ; aucune mutation libre des états.
- Cloisonnement rôle + statut + compte + site + affectation + ressource.
- Pièces privées avec quarantaine et scan.
- ETag/If-Match, idempotence et ProblemDetail.
- OpenAPI versionné, chemins exacts à figer par lot.

## États métier de référence
| Objet | États du cahier backend |
|---|---|
| Onboarding | DRAFT, SUBMITTED, NEEDS_INFORMATION, APPROVED, REJECTED |
| Ticket | OPEN, QUALIFIED, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED, CANCELLED |
| Mission | DRAFT, PUBLISHED, AWAITING_CONFIRMATION, PLANNED, IN_PROGRESS, AWAITING_REVIEW, COMPLETED, CANCELLED, EXPIRED |
| Candidature | SUBMITTED, WITHDRAWN, SELECTED, NOT_SELECTED |
| Affectation | RESERVED, CONFIRMED, DECLINED, EXPIRED, RELEASED |
| Rapport | DRAFT, SUBMITTED, ATTESTED, APPROVED, CHANGES_REQUESTED |
| Document | PENDING_UPLOAD, QUARANTINED, CLEAN, REJECTED, EXPIRED |

Le contrat implémenté peut préciser ou faire évoluer ces états. Confronter les enums avant génération des composants. Un état inconnu ne doit pas être silencieusement traduit par « Terminé ».

## Droits et responsabilités
CLIENT en attente : son dossier ; CLIENT actif : données de ses comptes/sites autorisés.
TECHNICIAN en attente : dossier mobile ; actif : découverte anonymisée puis missions autorisées.
ADMIN : validation, qualification, publication, sélection, revue et clôture.
Le technicien ne confirme pas une mission à travers le backoffice admin. Une simulation Flutter de recette doit utiliser le contrat mobile réel ou une commande de test appropriée, sans donner un privilège mobile à Angular.

## Finance
Client propose ; admin publie ; technicien accepte une révision. Pas de calcul automatique de marge ou de facture.
Modification après publication : révision et notifications. Après accord : mécanisme d'avenant proposé dans le backend ; ne pas inventer son protocole.

## Rapports
Mission et ticket ont leurs clôtures propres. Le rapport soumis/attesté est immuable ; la correction conserve la version précédente.
L'attestation recueillie sur le mobile n'est pas remplacée par un bouton admin « Signer pour le client ». Une éventuelle attestation web par client nécessite le contrat et le rôle habilité.

## Correspondance des lots
FE03/BE03 : identité ; FE04/BE04 : onboarding ; FE05/BE05 : comptes et parc ; FE06/BE06 : contrats et tickets ; FE07/BE07 : missions ; FE08/BE08 : candidatures et affectations ; FE09/BE09 : pièces ; FE10/BE10 : rapports ; FE11/BE11 : notifications ; FE12/BE12 : SLA et statistiques.
La performance et le durcissement se vérifient ensuite sur ces fonctions, sans reporter leurs fondations.
