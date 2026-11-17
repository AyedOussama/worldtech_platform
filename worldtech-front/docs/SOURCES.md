# Sources et vérifications
Revue documentaire de la conception : 10 novembre 2026. La résolution réelle des packages et la compatibilité du dépôt sont à effectuer au FE00 ; aucune installation Angular n'a été exécutée pour ce pack.

## Source projet
Cahier_backend_SpringBoot_v1.md, révision interne 1.1, 27 octobre 2026, relu pendant la préparation du pack. Synthèse dans ALIGNEMENT_BACKEND.md.
La lecture du cahier ne prouve pas l'état d'avancement de son implémentation.

## Documentation officielle
| Sujet | Source |
|---|---|
| Versions et support Angular | https://angular.dev/reference/releases |
| Matrice Node/TypeScript/RxJS | https://angular.dev/reference/versions |
| Zoneless et Reactive Forms | https://angular.dev/guide/zoneless |
| Formulaires réactifs | https://angular.dev/guide/forms/reactive-forms |
| Sécurité Angular | https://angular.dev/best-practices/security |
| Tests Angular | https://angular.dev/guide/testing |
| PrimeNG installation | https://primeng.dev/installation |
| PrimeNG thèmes | https://primeng.dev/theming |
| PrimeNG version repérée | https://blog.primeui.dev/blog/primeng-22-1-4/ |
| PrimeUI conditions actuelles | https://primeui.dev/pricing |
| Licence dans le dépôt PrimeNG | https://github.com/primefaces/primeng/blob/master/LICENSE.md |
| Lucide Angular | https://lucide.dev/guide/angular |
| Core Web Vitals | https://web.dev/articles/vitals |
| WCAG contraste | https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html |
| WCAG cibles | https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html |
| Spring Security CSRF | https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html |
| Spring Security OAuth Login | https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html |
| Playwright | https://playwright.dev/docs/intro |
| OpenAPI Generator Angular | https://openapi-generator.tech/docs/generators/typescript-angular |
| IBM Plex | https://github.com/IBM/plex |

Les URL Playwright, générateur et police sont fournies pour la vérification d'intégration du lot concerné ; leurs patches ne sont pas figés dans ce pack.

## Source de design incluse
unslop par Damir Sokolovsky (@heydamir), CC BY 4.0.
https://github.com/heydamir/unslop
Révision et inventaire : THIRD_PARTY_NOTICES.md et manifest.sha256.json à la racine.

## Résultats du pack
Le rapport dans verification/PACK_REPORT.md concerne fichiers, liens internes, JSON, contrats documentaires et contrastes de départ.
Aucun score Lighthouse, test Playwright, build Angular ou contrôle Spring Boot n'est présenté comme exécuté par la préparation de l'archive.

