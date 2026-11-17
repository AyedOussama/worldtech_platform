# FE03 — Connexion, session, CSRF, MFA et Google conditionnel

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Intégrer l'identité web Spring Security sans convertir Angular au protocole mobile ni ouvrir l'administration avant MFA.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE03 et contrat réel de session/CSRF/MFA ; l'absence d'un endpoint se signale précisément.

## Lecture obligatoire
- docs/AUTHENTIFICATION.md
- docs/API_CONTRATS.md
- docs/SECURITE.md
- docs/TESTS_RECETTE.md

## Travail demandé
1. Relève les requêtes et réponses réelles de connexion, logout, contexte CSRF, utilisateur courant, vérification email, reset et MFA. Prépare ou régénère le client typé depuis le contrat réel.
2. Implémente les écrans personnalisés de connexion et récupération, avec labels, autocomplete et erreurs accessibles. Évite d'énumérer les comptes via les messages de récupération.
3. Modélise initialisation, anonyme, MFA requis, onboarding, actif et suspendu selon le contrat. Un problème réseau ne devient pas silencieusement une session anonyme.
4. Configure la session par cookie, l'origine/proxy et la protection CSRF. Distingue le cookie de session HttpOnly du jeton CSRF lisible si ce mécanisme est retenu. Vérifie le renouvellement après login/logout et les jetons masqués/différés selon Spring.
5. Implémente le cycle MFA web exposé, y compris parcours d'enrôlement et récupération réellement prévus. Ne fabrique pas de privilège après un simple succès du premier formulaire.
6. Affiche Google uniquement s'il est configuré ; démarre le flux backend, contrôle la destination de retour et recharge la session. L'API métier n'accepte pas directement les jetons Google.
7. Coordonne les 401 sans cascades de redirection, traite 403 et 429 distinctement, invalide les caches et requêtes privées au logout/changement d'identité.
8. Garde les chemins de retour internes. Ne stocke ni session, ni mot de passe, ni jeton mobile dans localStorage et n'ajoute aucun refresh token web improvisé.
9. Si l'API manque, livre les composants et adaptateurs simulés isolés, mais laisse l'intégration et ses preuves explicitement ouvertes.

## Vérifications et critères d'acceptation
- Connexion/logout réels avec cookies et CSRF, token absent/altéré refusé.
- Appel direct admin refusé avant MFA ; autorisé seulement après preuve serveur.
- Expiration, refus/suspension et concurrence de plusieurs 401 correctement affichés.
- Google activé/désactivé, retour sûr et absence de jetons dans URL/logs.

## Livrables
- Pages d'identité, service de session, gardes et intégration HTTP.
- Tests ciblés et preuves contre backend réel ou statut non exécuté explicite.
- Compte rendu FE03 et contrats d'identité documentés.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucune désactivation de CSRF ou de Spring Security pour résoudre une difficulté frontend ; aucune réécriture du backend sans besoin identifié et périmètre autorisé.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

