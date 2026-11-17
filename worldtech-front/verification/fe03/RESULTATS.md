# FE03 — Résultats et preuves

10/11/2026, Windows, Node 26.13.0 / npm 11.20.0. **Identité locale vérifiée, réserve Google activé. FE04 non commencé.** [Compte rendu et fichiers](../../docs/comptes-rendus/FE03_IDENTITE_WEB.md).

## Commandes et résultats finaux

Commandes npm depuis `app/`, PATH du shell vers `C:\Program Files\nodejs`, `NG_CLI_ANALYTICS=false` pour Angular.

| Contrôle réellement exécuté | Résultat |
|---|---|
| `git status --short` | Exit 1 attendu : pas de dépôt Git, aucune CI distante/commit |
| `npm ls --depth=0` | Versions directes retenues, aucun conflit indiqué |
| `npm ci --prefix <dossier temporaire opencode>` | Exit 0, 435 paquets installés ; modules du serveur utilisateur conservés |
| `npm run check:licenses` | Exit 0, zéro PrimeUI, licences libres du lock et empreinte du générateur validées |
| `npm run api:generate`, `npm run api:verify` | Exit 0, deux fichiers générés, OpenAPI 3.1 / API 0.6.0, source inchangée |
| `npm run check` | Exit 0 : licences, génération, lint, types/templates/tests/outils, **77 unitaires**, build, contrôles production |
| `npm run test:e2e` | **57/57**, 0 échec/skip/flaky, 1 worker, aucun retry |
| `npm run test:e2e:integration` | **5/5**, 0 échec/skip/flaky, vrai Spring et base isolée |
| `npm run test:e2e:production` | **9/9**, 0 échec/skip/flaky, build optimisé et serveur statique de smoke test |
| `npm audit --json` | Exit 1 : **11 modérées, 0 élevée/critique**, outils seulement ; [rapport](npm-audit.json) |
| `npm audit --omit=dev --json` | Exit 0 : aucun avis renvoyé ; [rapport](npm-audit-production.json) |
| Audit isolé du générateur 0.97.3 | Exit 1 : 1 élevée + 3 modérées, alternative non retenue ; [rapport](npm-audit-generator-0973-rejected.json) |

Les JSON Playwright originaux sont locaux dans `app/test-results/development.json`, `spring.json` et `production.json`. Le [résumé durable](CONTROLES.json) ne conserve ni identifiants de session ni secrets de recette. Build initial **311,77 kB brut / 85,27 kB estimé CLI** ; ce n'est pas une mesure du transfert ou des performances navigateur.

## Ce qui est réellement prouvé

| Garantie / scénario | Niveau de preuve |
|---|---|
| Login/logout client, cookie HttpOnly/SameSite Lax illisible par JavaScript | **Spring réel**, navigateur/proxy 4311 → 18083 |
| CSRF absent/altéré refusé 403, token antérieur rejeté après login | **Spring réel**, pas de désactivation/rejeu |
| `/me` et endpoint admin refusés avant MFA, endpoint admin 200 après preuve | **Spring réel**, puis rechargement de l'espace admin |
| Enrôlement, code de récupération consommé une fois, réauthentification, renouvellement des codes | **Spring réel** |
| Sixième opération MFA limitée, `Retry-After` et message distinct | **Spring réel**, limites backend conservées |
| Remplacement du facteur révoquant les sessions, nouvelle connexion + récupération | **Spring réel**, compte distinct pour respecter la limite de cinq opérations par utilisateur |
| Reset explicite, token consommé une fois et sessions déjà actives révoquées | **Spring réel**, autre contexte navigateur contrôlé par `/me` |
| Confirmation email à usage unique, accès client toujours fermé avant approbation | **Spring réel**, route client directe 403 après confirmation |
| Génération de liens email | Vrai port de domaine dans la fixture ; **pas de livraison SMTP** |
| 401 simultanés, réponses tardives, mutation privée attendant CSRF au logout | **Unitaires avec doubles HTTP**, pas des données métier réelles |
| Réseau ambigu, 409, 403, 429, double envoi et saisies conservées | **Navigateur simulé** et unitaires ; 429 aussi réel |
| Suspension/refus/technicien, faux rôle local et navigation inter-onglets/retour arrière | **Simulé/unitaire** ; attente d'approbation aussi réelle |
| Capture avant runtime-config, confirmation jamais en GET, nouveau fragment sur page déjà ouverte | **Simulé**, complété par reset/email réels |
| Google désactivé et configuration `true` refusée | **Unitaire + navigateur** ; Google activé/fournisseur **non exécuté** |
| Exclusion des aperçus/fixtures du bundle production et API non transformée en HTML | **Build réel + smoke test**, pas de déploiement HTTPS |

La fixture n'est pas un backend HTTP de démonstration : vrai Spring/Tomcat, image PostGIS verrouillée par le backend, base nouvelle et jetable. Pas d'accès à la base personnelle sur 5432. SMTP/Google désactivés. Les secrets temporaires passent par stdin/environnement des workers ; aucune backdoor HTTP ou fichier de mots de passe. Captures/traces/vidéos/snapshots DOM d'échec sont désactivés sur cette suite réelle.

## Visuel, clavier et accessibilité

**28 scans axe identité + 23 non-régressions = 51**, zéro violation détectée sur les tags WCAG utilisés. **58 occurrences `incomplete`**, pas nécessairement 58 règles uniques, restent dans les JSON pour revue humaine. Pas de suppression de règle ou d'exclusion pour rendre le résultat vert.

**28 captures d'identité + 24 non-régressions**, toutes avec données synthétiques/doubles HTTP, pas avec les secrets Spring réels. Exemples :

| Écran | Ordinateur clair | Ordinateur sombre | Téléphone clair | Téléphone sombre |
|---|---|---|---|---|
| Connexion | [1440](screenshots/login-light-1440.png) | [1440](screenshots/login-dark-1440.png) | [390](screenshots/login-light-390.png) | [390](screenshots/login-dark-390.png) |
| Reset | [1440](screenshots/reset-light-1440.png) | [1440](screenshots/reset-dark-1440.png) | [390](screenshots/reset-light-390.png) | [390](screenshots/reset-dark-390.png) |
| MFA | [1440](screenshots/mfa-light-1440.png) | [1440](screenshots/mfa-dark-1440.png) | [390](screenshots/mfa-light-390.png) | [390](screenshots/mfa-dark-390.png) |
| Enrôlement | [1440](screenshots/enrollment-light-1440.png) | [1440](screenshots/enrollment-dark-1440.png) | [390](screenshots/enrollment-light-390.png) | [390](screenshots/enrollment-dark-390.png) |
| Codes fictifs | [1440](screenshots/recovery-codes-light-1440.png) | [1440](screenshots/recovery-codes-dark-1440.png) | [390](screenshots/recovery-codes-light-390.png) | [390](screenshots/recovery-codes-dark-390.png) |
| Accès admin | [1440](screenshots/admin-access-light-1440.png) | [1440](screenshots/admin-access-dark-1440.png) | [390](screenshots/admin-access-light-390.png) | [390](screenshots/admin-access-dark-390.png) |

Rendus relus : connexion clair desktop/sombre mobile, reset sombre mobile, MFA clair mobile, enrôlement desktop et codes/accès admin mobile. Titres, alignement, contrôles, contraste et retour à la ligne sont cohérents ; pas de débordement horizontal détecté dans la matrice. Pas de logo redessiné ni d'illustration ajoutée. Référence de finition : unslop par Damir Sokolovsky (@heydamir), CC BY 4.0.

Les tests clavier couvrent synthèse d'erreurs/focus, envoi par Entrée, menu/drawer/dialogue et retour de focus FE02. Les non-régressions conservent le texte racine à 200 % et le reflow à 320 px ; ce n'est pas le zoom natif du navigateur. Lecteur d'écran, marque officiellement validée, téléphones physiques et autres moteurs ne sont pas certifiés.

## Échecs préliminaires corrigés

- Des formulaires mono-contrôle sans `FormGroup` provoquaient une navigation GET native : liaison réactive ajoutée aux demandes de lien, codes MFA et mot de passe de réauthentification. Les assertions réelles/simulées restent en place.
- Chemin de fixture backend initial mal résolu depuis `app/`, corrigé vers `../../backend` ; aucun faux succès d'intégration annoncé avant exécution.
- L'enchaînement initial dépassait la limite MFA réelle : scénario explicite 429 ajouté, remplacement testé sur une autre identité de recette. **Aucun effacement du compteur, horloge avancée ou assouplissement de limite**.
- Un nouveau fragment sur un document déjà ouvert n'exécutait pas `main.ts`, puis le Router pouvait le réintroduire après nettoyage : écoute du lien et réconciliation de fin de navigation, avec nouveau test et confirmation toujours explicite.
- Une saisie de test arrivait avant le changement de phase d'enrôlement : synchronisation sur son titre visible, sans attente arbitraire ni retrait d'assertion. Les derniers passages complets sont tous réussis, pas un cumul de succès partiels.

## Préservation et limites

- Baseline frontend : **243 fichiers**, deux scaffolds d'attente retirés intentionnellement, aucune autre suppression ; **114 fichiers protégés** comparés sans différence. Sources vendor vérifiées séparément contre le manifeste historique ; prompts/templates/scripts historiques, preuves FE01/FE02 et PNG utilisateur préservés.
- Contrôle documentaire final : **64 Markdown, 162 liens locaux**, aucun lien cassé. Les **24 fichiers vendor** du manifeste sont identiques ; le manifeste et le vérificateur historiques ne sont pas modifiés pour masquer les écarts applicatifs attendus.
- Aucun patch backend ; compilation dans ses sorties Maven et fixture séparée. SHA-256 OpenAPI inchangé. L'empreinte agrégée historique backend n'est pas revendiquée comme redémontrée ; le contrôle chronologique du périmètre source ne relève aucune modification postérieure à la baseline frontend.
- Serveur utilisateur 4200 et PostgreSQL 5432 préservés ; serveurs de recette distincts et arrêtés en fin de suite. Pas de Git, CI distante ou déploiement attestés.
- Relevé final des ports : seuls 4200 et 5432 restent à l'écoute parmi les ports contrôlés ; aucun serveur de recette résiduel ni backend permanent sur 8080. L'utilisation normale exige donc de démarrer/configurer le backend séparément.
- Lock final SHA-256 : `a4e959eb9c4f9882d852c2e35df23d6602155c28ea0ddfb9d47d30d8a457d5c4`.
- Audit : le template runtime vulnérable du générateur types-only n'est pas livré ; alertes outillage suivies, pas de certification d'absence de risque. [Décision détaillée](../../docs/DEPENDANCES.md).
- **Non exécutés** : Google activé, SMTP, HTTPS/cookies Secure et CSP de production, expiration chronométrée de 30 min, suspension réelle en cours d'utilisation, isolation des futures données métier et conformité WCAG complète.

**Suite : FE04 seulement sur lancement explicite**, avec revue des contrats d'admission/validation BE04 et environnement de recette. Aucun lot métier n'est commencé par ce bilan.
