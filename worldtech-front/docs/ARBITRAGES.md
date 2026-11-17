# Décisions restant à fixer
Les propositions de départ ne deviennent pas automatiquement des règles validées. Les travaux indépendants continuent.

| ID | Décision | Proposition / contrainte actuelle | Lot limite |
|---|---|---|---|
| A01 | Dépôt et chemin frontend | Résolu FE00 : projet `worldtech-web` dans `app/`, backend voisin préservé, pack en racine ; Git absent, voir A15 | FE00 |
| A02 | Versions et licence UI | Résolu FE01 par instruction « outils gratuits » : Material/CDK 22.2.6 MIT remplace PrimeNG/PrimeUIX exclus. Node 26.13.0 / npm 11.20.0 de la nouvelle installation utilisés explicitement, sans changement global du PATH ; lock réel dans app | FE01 ; licence libre à revérifier à chaque ajout/mise à jour |
| A03 | Actifs de marque et police | Ouvert : aucun logo/charte/maquette WorldTech ni police locale trouvé ; fournir les actifs validés avant validation visuelle ; tokens et fallback système permettent d'avancer | FE02 |
| A04 | Contrat de session/CSRF/MFA | Localisé : OpenAPI 0.6.0, CSRF JSON en mémoire et rotations, /me limité/refus pré-MFA. Reste recette navigateur/proxy, comptes synthétiques et parcours de reprise MFA ; aucune intégration annoncée | FE03 |
| A05 | Champs et pièces de dossier | Source backend ; aucune liste réglementaire inventée | FE04 |
| A06 | Droits de gestion du parc client | Résolu pour le contrat courant : CLIENT lecture scoped, ADMIN seul écrit ; habilitations explicites par membre/site, aucune permission par défaut. Tester le refus côté API | FE05 |
| A07 | HT/TTC, montant facultatif, forfait/horaire, frais | Résolu pour proposition CLIENT : deux forfaits HT/EUR facultatifs, frais/rémunération séparés, null ≠ zéro, pas de TVA/facture déduite. Conditions publiées/acceptées futures restent ouvertes | FE06 pour CLIENT ; FE07–08 pour ADMIN/technicien |
| A08 | Contrats et calendriers | Contrat courant trouvé : révisions immuables, couverture explicite et calendrier 24/7 ou plages/exceptions, sans jours fériés/objectifs/pauses implicites. Calcul d'échéances/indicateurs non exposé | FE06 ; métriques FE12 |
| A09 | Délais et expiration de confirmation | Paramétrables selon contrat backend | FE08 |
| A10 | Modification après accord et réaffectation | Révision / avenant explicite, pas d'écrasement | FE08–10 |
| A11 | Attestation web éventuelle et délégation | Flutter comme parcours principal ; habilitation explicite | FE10 |
| A12 | Agenda avancé / carte web | Évaluer besoin et licence, hors dépendances initiales | Lot concerné |
| A13 | Définition des indicateurs et périodes | Métriques métier backend | FE12 |
| A14 | Domaines, environnement et CSP | Configuration réelle, aucune URL supposée | FE14 |
| A15 | Politique Git et CI distante | Aucun dépôt ni remote dans frontend/backend ; ne pas init/push automatiquement. FE01 peut préparer scripts/workflow frontend séparé, mais pas prouver une CI distante sans dépôt cible | Avant première publication/CI distante |
| A16 | Génération et capacités API manquantes | Générateur Angular 22/TS 6/OpenAPI 3.1 non qualifié, 132 operationId manquants ; pas de capacités/enrôlement dans /me. Adapter ou rapprocher le besoin sans créer un faux contrat | Avant génération/identité FE03 |
| A17 | Colonnes et recherche tickets | Référence publique, actualisé, recherche/tri libre et liste globale proposés par UX mais absents API ; maquette isolée possible, intégration à adapter ou besoin serveur à arbitrer | FE02 pour preuve visuelle ; FE06 pour API |
| A18 | Avis npm dans l'outillage | Dix alertes modérées autour de minimatch dans la chaîne de lint au FE01 ; aucun avis renvoyé avec `--omit=dev`. Suivre une correction compatible des mainteneurs, sans override majeur ou downgrade TS-eslint incompatible pour masquer le résultat | À chaque mise à jour ; revoir avant CI de code non fiable et consolidation FE13–14 |

Une question ne doit pas retarder FE01 si elle ne touche que les frais de FE06. Une absence de contrat d'API empêche de déclarer l'intégration terminée, mais pas de préparer les composants isolés.

Responsables : le responsable de l'organisation valide ses droits de licence, l'identité juridique et les actifs de marque ; l'équipe backend confirme le contrat et l'environnement de recette ; l'implémentation frontend traite les détails techniques réversibles dans le lot lancé. Aucune de ces lignes n'autorise un achat ou le lancement automatique d'un lot suivant. Bilan : [FE00](comptes-rendus/FE00_AUDIT_ET_DECISIONS.md).
