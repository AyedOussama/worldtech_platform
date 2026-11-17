# WorldTech — Instructions frontend
Périmètre : création et maintenance du portail Angular Client et du backoffice Administrateur. Ces consignes accompagnent un pack ; avant de les appliquer dans un dépôt, lire également ses instructions existantes et celles de l'utilisateur.

## Méthode
1. Inspecter les fichiers et l'état Git avant modification ; utiliser rg pour les recherches.
2. Lire le prompt du lot courant, ses documents et le compte rendu du lot précédent.
3. Exécuter les actions nécessaires au lot autorisé, puis s'arrêter avant d'entamer le suivant.
4. Préserver les modifications de l'utilisateur ; ne pas écraser un projet, sa configuration ou ses données.
5. Produire des changements limités et compréhensibles. Ne pas lancer d'agents supplémentaires sans demande explicite.
6. Résoudre les choix réversibles avec les informations disponibles. Demander une décision seulement lorsqu'une information produit manquante affecte réellement la tâche.
7. Ne pas annoncer un build, un audit ou une intégration réussis sans les avoir exécutés.

## Invariants métier
- Marque WorldTech ; trois rôles ADMIN, CLIENT, TECHNICIAN. Aucun rôle organisateur séparé.
- Angular : Client et Administrateur. Flutter : Technicien. Le portail client est responsive.
- Invitation et validation administrative du client ; validation des justificatifs du technicien.
- Ticket, mission, affectation, intervention et rapport sont des concepts distincts.
- Une candidature ne constitue pas une affectation ; la confirmation technicien reste dans Flutter.
- Montants proposés par le client, publiés par l'admin et acceptés par le technicien restent distincts et versionnés.
- Rapport attesté immuable ; correction par nouvelle version.
- Pas d'encaissement, paie, marge automatique, GPS continu ou application mobile client ajoutés au périmètre.

## Identité et API
- Le web utilise la session serveur Spring Security et CSRF. Pas de JWT web ni de refresh mobile dans Angular.
- MFA administrateur obligatoire, Google inclus ; une session pré-MFA n'est pas privilégiée.
- L'API contrôle rôle, statut, compte, site, affectation et état de la ressource.
- Les gardes de route n'apportent pas une autorisation serveur.
- Contrat OpenAPI réel avant génération. Les exemples de chemins du pack ne sont pas des endpoints certifiés.
- Préserver montants décimaux, version de concurrence, idempotence et fuseau.
- Pas de secret ni de données métier sensibles dans localStorage, console ou captures non anonymisées.

## Architecture
- Application standalone, routes par espace, chargement à la demande.
- Modules fonctionnels lisibles ; pas de gros service générique ni d'appel HTTP dans les composants visuels.
- État local avant état partagé ; RxJS pour les flux, Signals pour l'état lu par la vue.
- Le code généré depuis OpenAPI ne se modifie pas manuellement.
- Introduire une bibliothèque uniquement avec besoin, compatibilité et licence vérifiés.
- Pas de contournement de dépendances par --force ou --legacy-peer-deps.

## Contrainte utilisateur — outils gratuits (FE01)
- Uniquement des outils et dépendances gratuits, utilisables commercialement sans abonnement, siège payant ou clé commerciale obligatoire. Les obligations des licences libres restent à respecter.
- Angular Material/CDK (MIT) remplace le candidat PrimeNG du pack. Ne pas ajouter PrimeNG 22, PrimeUIX, PrimeIcons, composant PRO ou template payant, ni demander une éligibilité Community pour contourner ce choix.
- Les mentions PrimeNG/preset/darkModeSelector des prompts d'origine sont à traduire vers les API publiques Material (`mat.theme`, overrides, `color-scheme`), sans ajouter une seconde suite UI. Voir docs/adr/ADR005_OUTILS_LIBRES.md.
- Vérifier et verrouiller la licence de chaque nouvel outil ; une version future ou un changement de modèle commercial nécessite une nouvelle revue, pas un achat implicite.

## Design et rédaction
- Respecter docs/DESIGN_SYSTEM.md et docs/UX_ECRANS.md.
- Conserver le logo réellement validé. Palette de ce pack : proposition jusqu'au rapprochement avec les actifs de marque.
- Une action principale identifiable, libellés précis, colonnes alignées, densité adaptée.
- Supprimer le texte redondant ; conserver libellés, contraintes utiles et erreurs accessibles.
- Thèmes clair/sombre/système et clavier dès les premiers composants.
- Lire vendor/unslop/skills/unslop/SKILL.md avant une passe de design. Respecter son attribution.
- Lancer un lot autorise sa réalisation dans la direction WorldTech décrite ; ne pas imposer une reconfirmation pour chaque détail réversible. Une refonte hors périmètre doit être distinguée du travail courant.

## Vérification
- Tests ciblés sur les comportements et risques, pas de tests qui recopient l'implémentation.
- Cas de refus, erreur réseau, version périmée et double action dès l'introduction de ces fonctions.
- Rendus représentatifs clair/sombre, ordinateur/téléphone et clavier.
- Les preuves simulées et celles obtenues contre Spring Boot sont identifiées séparément.
- Ne pas modifier ce pack ou un fichier tiers pour masquer l'échec d'une vérification.
- Résumé final du lot : résultat, fichiers, commandes exécutées, résultats, limites et prochain prérequis.
