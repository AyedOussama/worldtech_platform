# WorldTech — Pack frontend Angular
Version 1.0.0 · 10 novembre 2026 · Portail client et backoffice administrateur

Un dossier de conception et de réalisation progressive, organisé comme le cahier backend Spring Boot. Commencer par [DEMARRER_ICI.md](DEMARRER_ICI.md), puis exécuter un seul prompt à la fois, de FE00 à FE14.

## Ce qui est livré
- Un cahier des charges frontend et les décisions d'architecture proposées.
- Quinze prompts autonomes : inspection, socle, design, identité, parcours métier, performance et livraison.
- Les exigences d'authentification, d'API, de sécurité et de recette.
- Une spécification des écrans et états d'interface, avec thèmes clair et sombre.
- Des exemples de tokens, configuration publique, initialisation du thème et routage Nginx.
- Le dépôt unslop demandé, sans son historique Git, avec attribution et révision d'origine.
- Un suivi des lots et un script d'intégrité du pack.

## Statut
Le pack est utilisé dans le dossier réel `worldtech-front`. **FE00–FE02 vérifiés localement ; identité locale FE03 intégrée à Spring**, avec réserve Google désactivé : [démarrer l'application](app/README.md), [bilan FE03](docs/comptes-rendus/FE03_IDENTITE_WEB.md), [preuves et limites](verification/fe03/RESULTATS.md). Outils gratuits uniquement : Angular Material/CDK MIT, IBM Plex OFL et Lucide ISC/MIT, sans PrimeUI ; [ADR005](docs/adr/ADR005_OUTILS_LIBRES.md). FE04–FE14 non commencés ; aucune intégration métier ou mise en production attestée.

Demande complémentaire **Login V1** du 12/11/2026 : [intégration de `/connexion`](docs/comptes-rendus/LOGIN_V1_INTEGRATION.md) avec les actifs fournis et [recette dédiée](verification/login-v1/RESULTATS.md), sans démarrer FE04.

## Documents d'entrée
1. [Démarrage](DEMARRER_ICI.md)
2. [Cahier des charges](Cahier_frontend_Angular_WorldTech_v1.md)
3. [Consignes projet](AGENTS.md)
4. [Ordre des lots](ROADMAP.md)
5. [Suivi](SUIVI_DES_LOTS.md)
6. [Premier prompt FE00](prompts/00_AUDIT_ET_DECISIONS.md)

## Organisation
| Dossier | Contenu |
|---|---|
| docs | Architecture, UX, API, identité, sécurité, performance, recette et exploitation |
| docs/adr | Décisions d'architecture proposées |
| prompts | Un prompt complet par lot et le contrat commun d'exécution |
| templates | Exemples à adapter au projet créé ou inspecté |
| scripts | Vérification de l'archive et de ses références |
| vendor/unslop | Copie du dépôt tiers explicitement demandé |
| verification | Résultats des contrôles du pack uniquement |

Les détails commerciaux, délais de confirmation, conditions financières et droits d'attestation encore ouverts sont recensés dans [ARBITRAGES.md](docs/ARBITRAGES.md). Ils ne bloquent que le lot concerné.

## Vérifier le dossier après extraction
~~~bash
python3 scripts/verify_pack.py
~~~
Sous Windows avec le lanceur Python :
~~~powershell
py scripts/verify_pack.py
~~~

Ce contrôle vise l'archive d'origine. Après FE00, le manifeste historique est volontairement conservé : les adaptations documentaires et nouveaux comptes rendus font échouer son inventaire/SHA-256 stricts. Le contrôle initial a passé ; [les preuves FE00](verification/FE00_COMMANDES_ET_CONTROLES.md) distinguent cet écart attendu des autres contrôles. Ne pas modifier le manifeste ou le script pour le cacher.

## Référentiel
Cahier_backend_SpringBoot_v1.md, révision interne 1.1 du 27 octobre 2026, consulté le 10 novembre 2026. Les chemins d'API de ce cahier sont indicatifs : les contrats réellement présents dans le dépôt priment après rapprochement documenté.

## Crédit tiers
unslop par Damir Sokolovsky (@heydamir), CC BY 4.0.
https://github.com/heydamir/unslop
La copie incluse est identifiée dans [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
