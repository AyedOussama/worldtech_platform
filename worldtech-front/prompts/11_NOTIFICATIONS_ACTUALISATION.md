# FE11 — Notifications internes et actualisation des données

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Informer des événements utiles et rafraîchir l'interface sans bruit, perte de contexte ou fuite entre sessions.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE11 et contrats de notifications ; minimum d'actualisation introduit dans les lots précédents.

## Lecture obligatoire
- docs/API_CONTRATS.md
- docs/PERFORMANCE.md
- docs/SECURITE.md
- docs/UX_ECRANS.md

## Travail demandé
1. Implémente liste/compteur/lecture des notifications selon le périmètre utilisateur. Les contenus et liens proviennent d'événements réels, sans inventer d'envoi email depuis Angular.
2. Ouvre une destination interne autorisée ; si le droit a changé, affiche l'état approprié sans ressusciter des données mises en cache.
3. Évite de transformer chaque actualisation en toast. Une action utilisateur a un retour ; un événement de fond peut se refléter discrètement dans la liste.
4. Configure un polling borné, sans chevauchement, suspendu onglet caché et détruit au logout ou changement de compte. Reprends selon une politique documentée.
5. Déduplique les éléments selon les identifiants réellement fournis. Ne réutilise pas un compteur d'un ancien utilisateur ni un cache global non cloisonné.
6. Conserve sélection, scroll et formulaire pendant les refresh. Une modification externe importante doit être signalée sans écraser une saisie.
7. Traite 401/403/429, délais progressifs bornés et erreurs réseau ; aucune boucle infinie de reconnexion.
8. Ajoute SSE seulement si le besoin et le contrat le justifient ; vérifie alors reconnexion, authentification et reprise. Ne remplace pas le protocole sans mesure.

## Vérifications et critères d'acceptation
- Une notification répétée ne crée pas une liste de doublons.
- Onglet caché/logout : requêtes et abonnements arrêtés.
- Changement de client : aucune donnée ou compteur de la session précédente.
- État modifié en arrière-plan : action devenue invalide signalée, saisie préservée.

## Livrables
- Centre de notifications et politique d'actualisation.
- Contrôles de cycle de vie et isolation des sessions.
- Compte rendu FE11 sans promesse de livraison email exactement une fois.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Les notifications SMTP, push mobile et outbox restent des responsabilités backend/mobile.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

