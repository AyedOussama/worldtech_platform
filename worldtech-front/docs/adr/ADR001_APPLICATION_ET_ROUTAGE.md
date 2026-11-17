# ADR001 — Une application web et deux espaces
Statut : retenu après inspection FE00 du 10/11/2026 ; implémentation FE01 non commencée.

## Contexte
Équipe de trois personnes, Angular pour Client/Admin, identité commune et forte réutilisation visuelle. Flutter couvre Technicien.

## Décision
Une application standalone, routes lazy /client et /admin, composants et contrats partagés de manière explicite. Rendu navigateur au lancement.
La séparation de routes ne constitue pas une sécurité serveur ni un moyen de cacher des secrets.
Implantation : projet `worldtech-web` dans `worldtech-front/app/`, backend existant dans `../backend`. Pack/AGENTS conservés en racine ; pas de migration d'une application absente, de monorepo Nx ou de création dans le backend. Angular 22 CSR/zoneless, versions précises dans [DEPENDANCES](../DEPENDANCES.md).

## Conséquences
Moins de duplication du design et des outils. Déploiement commun, à réévaluer si deux cycles de livraison distincts deviennent nécessaires.
L'architecture par fonctionnalités permet une extraction future sans déplacer les règles métier dans les composants.

## Alternatives
Deux applications dans un workspace si une contrainte réelle l'exige. Microfrontends et SSR non justifiés par le périmètre actuel.
