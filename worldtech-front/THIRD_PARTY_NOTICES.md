# Composants et références tiers

## unslop
- Auteur : Damir Sokolovsky (@heydamir)
- Dépôt : https://github.com/heydamir/unslop
- Révision incluse : 97daa3eb365670ea3f87ed85356cbfcee18105a5
- Date de récupération : 10 novembre 2026
- Licence : Creative Commons Attribution 4.0 International, CC BY 4.0
- Copie : vendor/unslop, fichiers suivis du dépôt ; historique .git exclu.
- Modifications des sources tierces : aucune.
- Licence et README d'origine conservés.

Les documents de design WorldTech adaptent des principes de relecture d'unslop et les rapprochent du produit. Ils ne prétendent pas avoir été rédigés ni validés par l'auteur d'unslop.

## Autres bibliothèques
FE01 installe Angular et **Angular Material/CDK sous MIT**, RxJS/TypeScript/Playwright sous Apache-2.0, Vitest/ESLint et auxiliaires sous leurs licences libres, via `app/package-lock.json`. Les notices runtime figurent dans le build `3rdpartylicenses.txt` et dans `app/public/third-party-notices.txt`. Le contrôle `npm run check:licenses` inventorie aussi les licences libres des outils et paquets natifs optionnels.
PrimeNG/PrimeUIX/PrimeIcons ne sont pas installés ; leurs références d'audit n'accordent aucun droit d'utilisation et ne constituent plus un choix candidat. Chaque nouvelle dépendance respecte la contrainte utilisateur « outils gratuits », avec licences et obligations vérifiées.

## Ajouts FE02
- IBM Plex Sans, Fontsource 5.3.0 : SIL OFL-1.1, copyright IBM 2019. Trois fichiers Latin WOFF2 400/500/600 non modifiés, auto-hébergés ; notice intégrale distribuée dans `/licenses/ibm-plex-sans/LICENSE` via les actifs Angular.
- Lucide Angular 1.71.0 : ISC, copyright Lucide 2026 ; certains SVG dérivés Feather sous MIT, copyright Cole Bemis. Les deux licences et leur liste d'icônes sont conservées intégralement dans `/licenses/lucide-angular/LICENSE`.
- `@axe-core/playwright` 4.14.0 et moteur axe-core : MPL-2.0, outils de développement sans modification de sources, absents du JavaScript livré en production. Aucun produit PRO ou service distant nécessaire.
- Versions, intégrité et empreintes des licences : [DEPENDANCES](docs/DEPENDANCES.md). Les licences du runtime sont distribuées avec l'application ; les contrôles ne constituent pas un avis juridique ni une promesse sur les licences futures.

## Ajout FE03
- `@hey-api/openapi-ts` **0.97.0**, copyright Hey API, **MIT** : génération des contrats TypeScript, outil de développement uniquement. Texte `LICENSE.md` intact dans le paquet, version et SHA-256 contrôlés par `check:licenses`. Les fichiers générés portent leur en-tête d'origine ; aucune modification des sources tierces, aucun service ou clé commerciale requis. Licence et choix de version détaillés dans [DEPENDANCES](docs/DEPENDANCES.md).

## Assets fournis Login V1
- `WorldTech_Login_Assets_V1/` : ressources fournies par l'utilisateur, pas une nouvelle dépendance sous licence libre déduite. Le manifeste les qualifie d'illustrations de travail dont les droits et la qualité restent à valider avant production. Copies et dérivé WebP tracés dans [l'inventaire](verification/login-v1/ASSETS.json). Les logos WorldTech et Google n'acquièrent aucune licence MIT par cette intégration. Aucun nouvel outil payant, abonnement ou clé commerciale n'est introduit.
