# Prompt 06 — Contrats, calendriers et incidents clients

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente contrats versionnés, couverture des sites/équipements, calendriers et tickets. Le client indique problème, site, équipement optionnel, priorité demandée et proposition rémunération/frais ; ADMIN qualifie et retient une priorité. Le ticket peut se résoudre à distance ou produire plusieurs missions plus tard.

Avant de figer les champs financiers, fais trancher les seules ambiguïtés bloquantes : HT/TTC, forfait/horaire, null/obligatoire, frais forfaitaires ou plafond. Avance sur les parties indépendantes. Stocke la proposition client distinctement, versionnée, BigDecimal/numeric, devise EUR MVP et montants JSON en chaîne. N'invente pas de facturation ni de marge.

Échanges publics séparés des notes internes. Calculs temporels par Clock et fuseau du site. Prépare les événements SLA sans supposer que WAITING_CLIENT arrête toujours le compteur. Prépare les références aux pièces du lot 09 ; ne permets pas d'URL privée arbitraire.

Recette : droits par site, mauvais équipement, bornes de contrat, ticket sans équipement, priorité demandée/retenue distinctes, valeurs nulles/négatives/precision, historisation de proposition, résolution sans mission. Ne pas confondre les états du ticket et ceux d'une visite.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
