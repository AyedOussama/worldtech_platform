# Modèle de preuve par exigence et lot

27 octobre 2026. Copier ce modèle dans `docs/preuves/lot-NN-<sujet>.md` au moment du contrôle. Référence : `SECURITE.md` et [mapping ASVS](asvs-sec.md). Ce modèle n'est lui-même **aucune preuve de réussite**.

## Identification

| Champ | À renseigner |
|---|---|
| ID preuve / lot | `ENN-xx` / numéro de lot |
| Date UTC / exécutant / relecteur | Date réelle et responsables ; pas de donnée personnelle inutile |
| Révision du code | Commit et état des changements ; si hors Git, empreintes/portée exacte |
| Exigence | SEC-xx et ASVS `v5.0.0-Vx.y.z`, niveau minimal |
| Applicabilité | Applicable / partielle / non applicable, justification et validation ; un report n'est pas une exclusion |
| Menace et effet utilisateur | Acteur, actif sensible, scénario d'abus, impact |
| Contrat et contrôle introduit | Chemins/classes/migration/config pertinents ; conditions d'autorisation |
| Environnement | OS, JDK/build, base réelle, plateforme/image par digest, configuration sans secrets |

## Séparer les états

- **Prévu** : règle et critère de recette attendus.
- **Implémenté** : mécanisme réellement présent, fichiers/révision.
- **Vérifié** : contrôle exécuté, commande, résultat observé, preuve ci-dessous.
- **Non vérifié** : dépendance manquante, contrôle non exécuté ou résultat insuffisant.

## Exécution reproductible

```text
Préconditions et données de test synthétiques : ...
Commande exacte / scénario manuel : ...
Code de sortie et date : ...
Résultat attendu : ...
Résultat observé : ...
Artefacts de preuve, chemins et SHA-256 si utile : ...
Portée : unitaire / HTTP / PostgreSQL réel / fournisseur simulé ou réel / configuration / revue ...
```

Utiliser des données fictives. Pas de mot de passe, token, lien brut, clé, document réel ou dump de table sensible. Nettoyer les traces avant conservation sans supprimer les constats ; stocker les rapports sensibles dans un espace à accès contrôlé, et seulement leur référence expurgée dans Git.

## Scénarios minimum selon le risque

| Famille | Contrôle requis |
|---|---|
| Autorisation | Bon acteur autorisé ; visiteur, mauvais rôle, pré-MFA, attente, suspension, autre compte/site/affectation refusés |
| Refus | Code HTTP **et** corps/headers sans fuite **et** aucune mutation DB, document accessible ou effet externe indu |
| Rejeu | Droits actuels au rejeu, même clé/autre corps, réponse perdue, état changé |
| Concurrence SQL | Deux transactions réellement concurrentes ; synchronisation du test, état final et contraintes constatés en PostgreSQL/PostGIS |
| Temps/version | Limites exactes avec `Clock`, expiration sans attendre le job, version périmée, fuseau/changement d'heure |
| Dépendances | Version et origine effectives, scan daté, constats/exception avec propriétaire et échéance ; pas seulement existence d'un tag |

## Conclusion limitée à la preuve

```text
Résultat : succès / échec / partiel / non exécuté.
Ce que ce résultat démontre : ...
Ce qu'il ne démontre pas : ...
Constats et risques résiduels : ...
Action restante, propriétaire et échéance : ...
Revue humaine / retest : prévu ou réellement effectué, référence ...
Décision de livraison du lot : ...
```

Ne pas convertir une absence de Docker, un test mocké, un skip ou une lecture syntaxique en intégration réussie. Un digest, un scanner ou Spring Security seul ne démontre pas la conformité. Une restauration mesurée et une revue humaine sont des preuves distinctes d'un test HTTP.
