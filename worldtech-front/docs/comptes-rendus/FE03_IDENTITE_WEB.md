# FE03 — Identité web, session, CSRF et MFA

10 novembre 2026. **Identité locale livrée et vérifiée contre Spring réel. Réserve explicite : Google activé non livré/non exécuté. FE04 non commencé.** Outils gratuits uniquement ; Material/CDK conservé, aucune seconde suite UI.

## Résultat

- Pages de connexion, demande de récupération, reset, confirmation email, challenge/récupération MFA, enrôlement manuel et gestion de sécurité. Formulaires réactifs typés, labels/autocomplete, affichage du mot de passe, synthèse d'erreurs avec focus, erreurs réseau/refus/rate limit sans effacer les saisies ni rejouer les commandes.
- Session serveur par cookie HttpOnly, CSRF JSON validé et conservé uniquement en mémoire. Acquisition concurrente unique et renouvellement après login/MFA/réauthentification ; jamais de JWT web, refresh mobile ou persistance d'identifiants.
- `/me` établit l'identité effective. Login `MFA_PENDING` n'ouvre aucun espace admin ; un 403 générique après rechargement ne permet pas d'inventer un challenge perdu. Attente de validation, email à confirmer, suspension, refus et technicien sont distincts de l'accès actif.
- Invalidation générationnelle des requêtes privées et réponses tardives, déconnexion explicite avec résultat ambigu séparé, coordination inter-onglets sans secret, retours internes bornés au rôle. Les gardes ne remplacent pas les contrôles Spring.
- Liens d'action débarrassés du fragment/query avant runtime-config/bootstrap ; gestion aussi d'un nouveau lien sur le document déjà ouvert et synchronisation avec le Router pour qu'il ne réintroduise pas le fragment. Aucun GET ne consomme une action ; le rechargement perd le secret, conservé une fois en mémoire.
- Les dix codes de récupération sont affichés une fois avec accusé de conservation hors ligne. Le remplacement MFA révoque les sessions et nécessite une nouvelle connexion ; l'email confirmé ne remplace jamais l'approbation administrative.
- Entrées Client/Admin réelles et limitées à l'identité ; aucune liste métier de démonstration substituée à l'API. Les aperçus FE02 restent exclusivement sous `/dev` en développement.

## Contrat et dépendance

Contrat réel OpenAPI 3.1 / API 0.6.0, SHA-256 `b5f122cc7ac464b544b1ee0f98b4e8ae379bf6521af2156e8b0ab6f73db1f1c5`, inchangé. Snapshot exact dans `app/api-contracts/`, provenance et types générés, transport Angular `IdentityApi` hors du dossier généré. `api:generate` / `api:verify` sont reproductibles ; `check` vérifie la génération. Sans backend voisin, la CI frontend peut utiliser le snapshot.

Générateur **`@hey-api/openapi-ts@0.97.0` MIT**, types-only, version et licence contrôlées. Modèles writeOnly/readOnly distincts ; aucune correction manuelle des sorties. Les int64 métier générés en `number` ne sont pas qualifiés pour de longues versions : conserver les ETag opaques et traiter ce point avant les lots métier.

Audit final : **11 alertes modérées d'outillage**, zéro élevée/critique dans le graphe retenu, aucun avis npm runtime renvoyé. Le template runtime du générateur concerné par la onzième alerte n'est pas produit/livré. Les versions 0.99.0 puis 0.97.3 examinées introduisaient des avis élevés YAML ; 0.97.3 a été réévaluée en installation isolée, sans scripts, sans modifier l'application. [Analyse, licences et obligations](../DEPENDANCES.md). Aucun contournement `--force`/peer deps, achat ou clé commerciale.

## Design et ergonomie

Référence : **unslop par Damir Sokolovsky (@heydamir), CC BY 4.0**, skill/références relus, sources tierces inchangées. Passe de finition dans la direction existante, pas de refonte.

| Axe | Choix appliqué |
|---|---|
| Produit / caractère | Accès aux opérations IT WorldTech ; précis, calme, opérationnel |
| Univers / moment / signature | Contrôles d'accès et étape MFA explicite ; la tâche courante et son action principale portent la hiérarchie |
| Espacement / disposition | Grille 4 px, cartes 480/640 px pour les tâches d'identité, padding 32 puis 24/20 px sur téléphone, cibles 44 px et disposition fluide |
| Couleur / icônes | Palette FE02, accent réservé à l'action, états textuels ; Lucide ciblé pour le mot de passe, aucun décor ou image ajoutés |
| Rédaction / type | IBM Plex auto-hébergée, titres de tâche, labels conservés ; consigne de code masquée lorsque la tâche devient conservation des codes |
| Détails / mouvement | Clé TOTP cachée par défaut, ancien contexte effacé, reprise explicite ; pas d'animation décorative, réduction de mouvement respectée |

La revue des rendus a notamment retiré le bouton trompeur « Retour à la connexion » des pages privées actives : l'admin dispose plutôt de l'action réelle de sécurité. Le fichier utilisateur `app/public/images/worldtech_logo.png` est préservé ; faute de statut de validation établi, le mot-symbole texte et la palette proposée restent utilisés.

## Vérifications réellement exécutées

Depuis `app/`, Node **26.13.0** / npm **11.20.0**, PATH limité aux shells, `NG_CLI_ANALYTICS=false`.

- `git status --short` : aucun dépôt Git, ni commit, push ou CI distante annoncés.
- `npm ls --depth=0`, `npm run check:licenses` : versions exactes, aucun conflit direct indiqué, zéro PrimeUI ; licence du générateur et empreintes contrôlées.
- `npm ci --prefix <temporaire opencode>` : **435 paquets installés**, sans remplacer les modules du serveur utilisateur sur 4200.
- `npm run api:generate`, `npm run api:verify`, puis **`npm run check` : exit 0**, licences/génération/lint/types/**77 unitaires**/build/exclusion production. Build initial : **311,77 kB bruts / 85,27 kB estimés CLI**, sans relever les budgets ; pas une mesure Lighthouse.
- **`npm run test:e2e` : 57/57**, zéro skip/flaky ; 31 non-régressions FE01/FE02 et 26 scénarios d'identité avec doubles HTTP.
- **`npm run test:e2e:integration` : 5/5**, vrai Spring/Tomcat et PostGIS éphémère. Cookies, CSRF absent/altéré/rotation, admin refusé avant MFA puis autorisé, enrôlement/récupération/réauthentification/renouvellement/remplacement, 429 réel, reset et email à usage unique, révocations et validation administrative non contournée.
- **`npm run test:e2e:production` : 9/9**, build optimisé, catalogue absent, refus des routes privées sans backend et pas de fallback SPA pour les erreurs API/actifs.
- Audits npm complets et production : exits **1 / 0**, résultats ci-dessus, conservés sans masquer les alertes.
- **51 analyses axe, zéro violation détectée**, dont 28 sur l'identité ; **58 occurrences de règles `incomplete`** conservées pour revue. 28 captures d'identité + 24 non-régressions, toutes synthétiques ; rendus représentatifs relus en clair/sombre, ordinateur/téléphone.

Les échecs préliminaires et leurs corrections, commandes et niveaux de preuve sont détaillés dans [les résultats FE03](../../verification/fe03/RESULTATS.md). Aucun retry/skip ajouté pour les masquer.

## Préservation et limites

Deux anciens scaffolds d'attente remplacés volontairement : `session-boundary.ts` et `connection-unavailable.ts`. Les autres fichiers de baseline restent présents ; preuves FE01/FE02, prompts, templates, scripts historiques, marque et sources tierces protégés. Aucun patch ne cible le backend ; seuls ses artefacts Maven sont compilés. Le contrôle du contrat est cryptographique ; l'ancienne empreinte agrégée globale backend n'est pas revendiquée comme redémontrée par FE03.

Les tests utilisent 4310, 4311/18083 et 4173, pas le serveur utilisateur 4200 ni la base existante 5432. Les identifiants réels de recette sont aléatoires et éphémères. Captures, traces, vidéos et snapshots DOM automatiques désactivés pour Spring ; les rapports durables ne contiennent pas ses secrets.

**Non qualifiés** : Google activé (callback web JSON sans retour Angular contracté), SMTP réel, HTTPS/cookie Secure/CSP de production, expiration chronométrée de 30 minutes, suspension réelle d'un compte en cours, revue lecteur d'écran et conformité WCAG complète, Firefox/WebKit et téléphones physiques. Expiration concurrente/refus/suspension sont testés avec doubles/unitaires ; les révocations sont aussi exercées contre Spring. Les autorisations métier futures restent à intégrer dans leurs lots.

**Prochain prérequis : lancement explicite FE04**, revue des contrats BE04 d'invitation/dossier/décision et environnement de recette adapté. Google reste désactivé tant que son retour/fournisseur n'est pas validé. Aucune continuation automatique vers FE04.
