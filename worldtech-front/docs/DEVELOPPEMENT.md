# Développement et travail des agents
Le pack s'intègre après inspection. Il ne contient ni node_modules, ni comptes, ni jetons, ni application Angular préconstruite.

## Commandes du projet
Les scripts du socle FE01 sont décrits dans [app/README.md](../app/README.md) : installation verrouillée, serveur de développement, lint, vérification TypeScript/templates, tests unitaires/navigateur, licences et build production.
FE03 ajoute `api:generate` / `api:verify` depuis le contrat réel et `test:e2e:integration` contre Spring/PostGIS isolés. Les suites avec doubles HTTP et les contrôles réels restent séparés ; le serveur de développement normal attend le backend sur 8080, pas une base de démonstration automatique.
Gestionnaire unique **npm** ; Node **26.13.0 LTS** et npm **11.20.0** réellement disponibles dans la nouvelle installation de l'utilisateur. Ils remplacent la cible documentaire Node 24 de FE00. `packageManager`, engines stricts, `.node-version`, `.nvmrc` et package-lock.json fixent le socle. Attention sous Windows : l'ancien Node de nvm peut précéder celui de Program Files dans PATH ; le README fournit une correction limitée au terminal. Aucun changement global de PATH/nvm ni installation globale n'est effectué.
Outils gratuits seulement : Material/CDK MIT, pas de PrimeUI ; les scripts de quatre dépendances natives ont été examinés et approuvés pour leurs versions exactes, pas avec une autorisation générale. Aucun commit possible tant qu'un dépôt Git n'est pas défini.
Ne pas recopier une configuration Angular d'une ancienne génération sans vérifier les options du CLI réellement utilisé.

## Code
Types stricts et templates stricts. Fonctions et composants nommés selon le domaine. État de session et réponses d'erreur modélisés explicitement.
Aucun any généralisé pour contourner un contrat ; unknown puis validation à la frontière si nécessaire.
Les validations de formulaire aident l'utilisateur ; le serveur revalide tout. Les règles financières et d'affectation ne migrent pas dans les composants.

## Catalogue UI
Un catalogue développement peut montrer les variantes, chargement, erreur, focus et deux thèmes. Utiliser les composants réels. Éviter de créer un framework de design parallèle au produit.
Documentation courte : quand utiliser, données, événements, accessibilité et exemple représentatif. La route n'est pas livrée en production.

## Simulation
Un adaptateur ou des handlers de développement explicites, avec fixtures déterministes et couverture des erreurs. Le choix MSW/autre est évalué si utile.
Interdire l'activation accidentelle en production. Aucun fallback silencieux d'une API échouée vers de fausses données.
Un bouton de démonstration ne prétend pas avoir envoyé un email ou généré un rapport serveur.

## Contrat d'un lot
Lire, inspecter, réaliser, vérifier, rendre compte. Les modifications nécessaires au lot courant sont autorisées par son lancement.
Un arbitrage absent est isolé ; avancer sur les tâches indépendantes. Ne pas commencer quinze lots après avoir reçu FE00.
Mise à jour du suivi avec développement, intégration et visuel séparés.

## Reprise et revue
Créer un compte rendu par lot avec contexte, décisions, fichiers importants, commandes et résultats.
Les captures utiles sont synthétiques ; pas de secret dans les logs. Revue humaine ciblée sur identité, accès client, finance, attestation et déploiement.
