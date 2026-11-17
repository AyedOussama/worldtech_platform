# Preuves FE01 — 10 novembre 2026

Périmètre : socle frontend local, **aucune recette Spring Boot**. [Compte rendu complet](../../docs/comptes-rendus/FE01_SOCLE_ANGULAR.md).

## Résultats exécutés

| Contrôle | Résultat final |
|---|---|
| Node / npm utilisés | 26.13.0 LTS / 11.20.0, nouvelle installation utilisateur, priorité PATH limitée aux shells de contrôle |
| `npm ci` | Exit 0, installation du lock réel ; aucun `--force`/`--legacy-peer-deps` |
| `npm ls --depth=0` | 22 dépendances directes aux versions fixées, aucun conflit indiqué |
| `npm run check:licenses` | 527 entrées de paquets du lock examinées (y compris variantes natives optionnelles), dix expressions de licences libres revues, zéro paquet PrimeUI |
| `npm run lint` / `npm run typecheck` | Exit 0 ; types et templates stricts, tests et configuration navigateur inclus |
| `npm run test` | **34 réussis**, 4 fichiers, zéro échec/skip |
| `npm run build` + `postbuild` | Exit 0, 257,60 kB bruts initiaux, 69,28 kB de transfert estimé CLI ; notices dans le répertoire statique |
| `npm run verify:production` | 11 fichiers JavaScript contrôlés ; catalogue dev absent, source maps absentes, configuration publique relative, notices présentes |
| `npm run test:e2e` | **12 réussis**, Chromium 158/build Playwright 1254, un worker, zéro retry/skip |
| `npm run test:e2e:production` | **7 réussis** sur build optimisé et serveur statique local, pas de backend simulant une authentification |
| `npm run check` après correction clavier | Exit 0, chaîne complète licences/lint/types/unitaires/build/production |
| `npm audit --json` | **Exit 1**, dix alertes modérées dans la chaîne de lint autour de minimatch ; aucune critique/élevée dans ce relevé, pas de succès complet d'audit annoncé |
| `npm audit --omit=dev --json` | Exit 0, aucun avis renvoyé dans le périmètre production ; pas une certification de sécurité |

Défaillances préliminaires conservées dans le bilan : regex lint, signature d'appel de garde dans le test, puis lien d'évitement navigateur revenant à la racine. Les corrections portent sur le code/test comportemental approprié, sans désactiver de règle, retirer une assertion ou ajouter un retry. Les résultats courants JSON du navigateur restent dans `app/test-results/development.json` et `production.json`, dossiers d'artefacts séparés.

Les deux rapports JSON finaux ont été relus : expected 12/7, unexpected 0, skipped 0, flaky 0. Empreinte du lock réel : `cf8086e37b3c5b997008ed7ec7e72a6848765a728c67b8b5ec11affb7022a91d`. Dernier contrôle documentaire : 97 liens locaux sur 60 Markdown propres au workspace, aucune erreur.

## Rendus effectivement examinés

Exemple de développement uniquement : champ/bouton Material, aucun client, secret, ticket ou donnée métier réelle. Captures après chargement et focus du sélecteur de thème :

- [Clair — ordinateur 1440×900](screenshots/foundation-light-1440.png)
- [Sombre — ordinateur 1440×900](screenshots/foundation-dark-1440.png)
- [Clair — téléphone 390×844](screenshots/foundation-light-390.png)
- [Sombre — téléphone 390×844](screenshots/foundation-dark-390.png)

Le test vérifie aussi l'absence de débordement horizontal. Le lien d'évitement et le retour clavier sont testés ; il ne s'agit pas d'un audit WCAG 2.2 AA complet ni d'un test sur téléphone physique. Le design system et l'écran de référence métier restent FE02.

## Préservation et provenance

- Manifeste original SHA-256 inchangé : `5ec373d5b0b368a18bb3d6a0214b3cbdc685509285dcb395d7b90db796e8da66`.
- Les 86 fichiers référencés par ce manifeste existent toujours ; seuls 19 Markdown ont été adaptés depuis l'archive, FE00 et FE01 cumulés. Aucun fichier original non Markdown modifié.
- Sous-arbres originaux `vendor`, `templates`, `scripts`, `prompts` identiques ; 23 fichiers unslop comparés à leur provenance. Le script d'archive n'est pas modifié ou présenté comme un test de l'application.
- Baseline backend : **268 fichiers inchangés**, agrégat `93531b45dde0d391a9c2303b5046f3db34a6b724010facafbdda5f8048b4b32f`.
- Archives exactes Material/CDK 22.2.6 : licence MIT lue après vérification SHA-512 ; LICENSE SHA-256 commun `adb40d7d256e33c3c20a67869de175e7ca249afee92f587ccc48b5bec0ac24e4`.
- Attribution design : unslop par Damir Sokolovsky (@heydamir), CC BY 4.0, sources inchangées.
- Git initial/final absent ; workflow préparé, aucune révision commise ou exécution distante attestée.

Non exécutés : session/CSRF/MFA réels, commandes métier, Google réel, tests Spring Boot, déploiement, Lighthouse/charge, Firefox/WebKit, appareils physiques ou revue indépendante. Les assertions HTTP unitaires utilisent le double Angular ; elles ne prouvent pas les permissions serveur.
