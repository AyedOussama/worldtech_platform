# FE02 — Design system, thèmes et structures des espaces

## Contrat d'exécution commun
Travaille dans le dépôt réel ouvert par l'utilisateur. Retrouve le dossier worldtech-angular-pack-v1 (ou le chemin indiqué), puis lis les instructions du dépôt et le AGENTS.md du pack. Préserve les changements existants. Les chemins de lecture ci-dessous sont relatifs au pack.

Réalise uniquement le lot indiqué. Les tâches nécessaires et réversibles de ce lot sont autorisées par son lancement ; résous les détails techniques raisonnables sans arrêter le travail pour une confirmation de routine. Isole un arbitrage réellement bloquant et avance sur les tâches indépendantes. Ne commence pas le lot suivant.

WorldTech conserve trois rôles : ADMIN/CLIENT/TECHNICIAN ; Angular Client/Admin, Flutter Technicien. Le web utilise la session Spring Security et CSRF, pas les jetons mobiles. Le backend impose les droits et les invariants métier.

Les contrats, versions et exemples du pack sont à rapprocher du code réellement présent. Les données simulées sont isolées et identifiées ; une vérification simulée ne prouve pas l'intégration Spring Boot. Exécute les contrôles pertinents et indique les commandes et résultats exacts, sans tests de pure duplication.

## Objectif
Construire une identité d'interface WorldTech cohérente, avec deux thèmes et un écran de référence qui servira aux lots métier.

## Prérequis
- Lot précédent examiné et dépendances de ROADMAP.md prises en compte.
- Sans dépendance API pour les composants isolés ; l'écran de référence utilise des fixtures synthétiques explicites.

## Lecture obligatoire
- docs/DESIGN_SYSTEM.md
- docs/UX_ECRANS.md
- docs/UNSLOP.md
- templates/design/tokens.json
- templates/design/worldtech.tokens.css
- vendor/unslop/skills/unslop/SKILL.md

## Travail demandé
1. Rapproche les actifs existants, la direction FE00 et DESIGN_SYSTEM.md. Garde le logo validé et présente tout écart de palette comme décision de design, sans inventer une marque.
2. Crée le preset PrimeNG WorldTech et les tokens de surface, texte, contrôles, accent, focus et sémantique avec les API de la version réellement installée. Les fichiers templates/design sont des références à intégrer, pas un preset directement importable.
3. Intègre la police retenue après contrôle de licence et optimise les actifs. Préserve le fallback système, le zoom et les caractères français. Harmonise les icônes et les chiffres tabulaires.
4. Implémente Clair/Sombre/Système, préférence non sensible, initialisation avant affichage et suivi du thème système. Traite overlays, contrôles natifs, erreur, focus et changement de mode à chaud.
5. Construis les structures Client/Admin, navigation responsive, en-têtes et zones de contenu. Aucun menu ne simule une permission serveur ; les capacités réelles seront branchées à FE03.
6. Construis les composants réellement utiles : champs/erreurs, statuts, filtres, liste, vide, dialogue, chronologie et montants. N'enveloppe pas systématiquement chaque composant PrimeNG si cela n'ajoute aucun comportement.
7. Réalise la liste de tickets de référence et un formulaire représentatif avec fixtures déterministes. N'expose pas ces fixtures comme de vraies opérations métier en production.
8. Applique les passes d'unslop : audit localisé, direction, espacements, couleurs, icônes, rédaction, type, disposition, détails, visuels et mouvement. Conserve son attribution. Le lot autorise les corrections dans cette direction ; une refonte différente est une décision distincte.
9. Retire slogans et aides redondantes ; garde labels, unités et erreurs utiles. Vérifie les petits écrans sans réduire les données essentielles à une taille illisible.

## Vérifications et critères d'acceptation
- Rendus clair/sombre sur ordinateur et téléphone ; captures synthétiques si l'environnement le permet.
- Clavier : navigation, champ, menu, dialogue, Échap et retour de focus.
- Contrastes des composants réels, états de sélection/focus et zoom.
- Build et contrôles ciblés après intégration des dépendances UI.

## Livrables
- Thème et structures communs, catalogue développement isolé.
- Écran de référence et formulaire réutilisable.
- Bilan de design avec décisions, corrections et limites ; aucune validation métier inventée.
- Mettre à jour SUIVI_DES_LOTS.md, en séparant développement, intégration et visuel.

## Limite du lot
Une démonstration visuelle n'est pas une implémentation de liste de tickets intégrée ; celle-ci appartient à FE06.

## Compte rendu final
Résultat livré ; fichiers importants ; décisions ; commandes réellement exécutées et résultats ; preuves contre API réelle ou simulation ; limites ; prérequis du lot suivant. Un contrôle impossible est déclaré non exécuté. Ne passe pas automatiquement au prochain prompt.

