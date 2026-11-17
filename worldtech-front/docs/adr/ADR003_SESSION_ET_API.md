# ADR003 — Session web et contrat API
Statut : rapproché des sources réelles en FE00 le 10/11/2026 ; intégration HTTP/navigateur non vérifiée.

## Décision
API web par session serveur Spring Security et CSRF. Origine commune et chemins relatifs privilégiés. Mobile Bearer opaque séparé.
MFA administrateur avant toute autorisation privilégiée, y compris avec Google.

## Conséquences
Angular ne manipule pas de refresh token applicatif. Coordination nécessaire sur CSRF, cookies, OAuth et proxy.
Le backend reste responsable des autorisations de chaque ressource, recherche, compteur et fichier.

## Contrat
OpenAPI réel, client typé régénérable, ProblemDetail, montants décimaux en chaînes, versions et idempotence explicites.
Les endpoints indicatifs du pack ne remplacent pas l'inspection du code.

## Contrat présent et conséquences
`../backend/openapi/maintenance-api.json` : OpenAPI 3.1.0 / API 0.6.0, 117 chemins, 133 opérations, 83 schémas. [Relevé et empreinte FE00](../CONTRAT_BACKEND_RELEVE_FE00.md).

CSRF par JSON `GET /api/web/v1/auth/csrf`, stockage mémoire et envoi de `X-CSRF-TOKEN` sur mutations web autorisées ; pas de cookie XSRF lisible. Rotation après login/MFA/réauthentification. `/me` ne contient que userId/status/roles/emailVerified et refuse ADMIN pré-MFA. Ne pas inventer de capacités, ni un statut privilégié depuis un 403 ; celui-ci peut être le même ACCESS_DENIED pour CSRF et autorisation. Aucun retry POST générique.

Proxy de même origine cible : navigateur localhost:4200, backend 127.0.0.1:8080 ; à tester. Google désactivé. Génération différée jusqu'à qualification Angular 22/TS 6/OpenAPI 3.1 et noms stables (132 operationId manquants), sans modification manuelle d'un fichier généré ou faux OpenAPI.
