# FE12 — SLA, indicateurs et tableaux de suivi

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Présenter des indicateurs dont l'utilisateur peut comprendre la période, la définition et les limites, avec les bons périmètres.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- BE12 et définitions des métriques/couvertures ; pas de calcul contractuel improvisé dans le navigateur.

## Lecture obligatoire
- docs/API_CONTRATS.md
- docs/DESIGN_SYSTEM.md
- docs/PERFORMANCE.md
- docs/ARBITRAGES.md

## Travail demandé
1. Relève chaque métrique disponible : unité, dénominateur, période, fuseau, traitement des annulations et réouvertures, fraîcheur et portée d'accès.
2. Construis les vues SLA de ticket avec échéances et pauses exposées. Le statut WAITING_CLIENT ne suspend pas automatiquement un engagement.
3. Affiche les compteurs et tendances utiles au travail. Une valeur absente n'est pas zéro ; une variation précise sa période de comparaison.
4. Choisis le graphique selon les données et ajoute une lecture textuelle ou tabulaire. Aucun camembert, courbe lissée ou animation décorative ne doit masquer la quantité réelle.
5. Si une bibliothèque est nécessaire, vérifie maintenance, version, licence et accessibilité, puis charge-la sur les routes concernées.
6. Les filtres période/client/site respectent les droits et le contrat serveur ; ne charge pas tous les tickets pour calculer les agrégats côté navigateur.
7. Intègre l'export réel, synchrone borné ou job 202 selon API. Le compteur et le fichier final ont le même périmètre d'autorisation.
8. Traite peu de données, série vide, API indisponible, période excessive et fuseaux/changements d'heure.

## Vérifications et critères d'acceptation
- Métriques vérifiées sur un jeu de données dont les résultats attendus sont connus.
- Dénominateur/période/contexte visibles sans sous-titres décoratifs.
- Client limité à son périmètre, y compris export et compteurs.
- Graphiques compréhensibles clavier et dans les deux thèmes.

## Livrables
- Indicateurs et suivi SLA intégrés.
- Dictionnaire des métriques et choix de représentation.
- Compte rendu FE12 avec valeurs vérifiées et limites.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Aucun taux, gain, chiffre commercial ou calcul de marge inventé pour remplir le tableau de bord.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

