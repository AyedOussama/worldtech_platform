# Backend — documentation et état d'avancement

## État courant — missions et découverte du lot 07 vérifiées localement

Lots antérieurs conservés ; missions ADMIN publiées par révisions financières immuables et découverte mobile bornée testées sur PostgreSQL/PostGIS réel : **320 tests Java (154 Surefire + 166 Failsafe)**. Rémunération HT/EUR obligatoire, frais facultatifs distincts de zéro, point public approximatif sans adresse ni contact privé. Approbation technicien bloquée sans pièces contrôlées (09), Google désactivé. Aucun frontend, affectation, upload ou déploiement.

- [Démarrer et tester le backend](../README-dev.md).
- [Compte rendu du lot 01](lot-01-socle.md) et [preuves](preuves/lot-01-verifications.md).
- [Compte rendu du lot 02](lot-02-infrastructure.md) et [preuves, écarts et scans](preuves/lot-02-verifications.md).
- [État des tranches du lot 03](lot-03-identite.md), [contrat Flutter](contrat-flutter-auth.md), [Google et limites réelles](google-optionnel.md).
- [Preuves historiques du lot 03 : 172 tests Java + 6 Python](preuves/lot-03-verifications.md).
- [Contrat du lot 04](lot-04-onboarding.md), [preuves historiques](preuves/lot-04-verifications.md), [revue de menace](securite/onboarding.md).
- [Contrat du lot 05](lot-05-comptes-parc.md), [preuves historiques](preuves/lot-05-verifications.md).
- [Contrat et décisions du lot 06](lot-06-contrats-tickets.md), [preuves historiques](preuves/lot-06-verifications.md).
- [Contrat et décisions du lot 07](lot-07-missions.md), [preuves courantes et limites](preuves/lot-07-verifications.md).
- [Contrat OpenAPI 0.7.0](../openapi/maintenance-api.json).
- Suite possible : **08 — candidatures et affectations**, sans démarrage automatique. Dépendance documentaire technicien (09) et réserves de production toujours ouvertes : images, staging, revue et pentest.

## Cadrage historique — lot 00

27 octobre 2026. Exécution de `prompts/00_cadrage.md` dans `backend`.

**Livré à ce stade historique : décisions et préparation, pas une application.** Le pack était conservé à l'identique au lot 00. Son infrastructure a ensuite été intégrée par modifications ciblées au lot 02 ; le manifeste initial est archivé sous `preuves/`, le manifeste racine décrit désormais les sources publiques courantes. Aucun dépôt Git ou compte administrateur applicatif n'a été créé.

### Ordre de lecture du cadrage

1. [Compte rendu, inspection et commandes](lot-00-cadrage.md).
2. ADR [001 — monolithe modulaire/MVC](adr/001-monolithe-modulaire-mvc.md), [002 — PostgreSQL/PostGIS](adr/002-postgresql-postgis.md), [003 — identité applicative](adr/003-identite-applicative.md).
3. [Versions, BOM, Bouncy Castle et images candidates](versions.md).
4. [Carte des modules et dépendances](architecture/modules.md).
5. [Matrice d'autorisation et suivi de l'admission](securite/autorisations.md).
6. [Correspondance initiale ASVS/SEC](securite/asvs-sec.md) et [modèle de preuve](securite/modele-preuve.md).
7. [Plan des lots et décisions du prochain lot](plan-lots.md).
8. [Preuves réellement obtenues](preuves/lot-00-verifications.md).

### Limites constatées au lot 00

Au terme du cadrage, le prochain lot était **01 — socle**, sans DTO métier ni migration métier : aucun arbitrage produit ne le bloquait. Les questions financières, d'admission ou d'attestation seront posées au lot concerné, avant de figer ses contrats. Aucune réponse n'est supposée acquise.

Les ADR formalisent le cahier et les instructions ; ils ne valent pas revue humaine du futur code. Le mapping ASVS est initial et partiel, sans certification. La sonde Java sous `preuves/` vérifie uniquement la compatibilité Argon2 ; ce n'est pas le backend.
