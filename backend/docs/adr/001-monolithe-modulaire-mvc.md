# ADR 001 — Monolithe modulaire, couches et Spring MVC

- Date : 27 octobre 2026.
- Statut : décision du cahier formalisée ; socle technique introduit au lot 01, domaines métier encore à venir.
- Périmètre : un backend pour Angular client/admin et Flutter technicien, équipe de trois développeurs.
- Exigences : SEC-01, SEC-02, SEC-03, SEC-14, SEC-19.

## Contexte

Tickets, missions, affectations et rapports ont des invariants transactionnels communs. Trois développeurs doivent pouvoir travailler et relire des domaines distincts sans exploiter des services distribués. Aucun projet applicatif n'existe lors de l'inspection ; les références ne sont pas du code déjà compilé.

## Décision

1. Un module Maven et un artefact déployable `maintenance-api`. Modules logiques sous `fr.entreprise.maintenance.features`, créés progressivement, pas une collection de services vides.
2. Spring MVC pour HTTP REST JSON ; JPA/JDBC bloquants derrière des ports. MVC décrit le transport HTTP, pas l'organisation complète du métier. Pas de WebFlux ajouté au-dessus de JPA pour un gain supposé.
3. Dans chaque domaine : `api` → `application` → `domain`/ports. `infrastructure` implémente les ports ; `bootstrap` assemble. `application` peut porter les transactions Spring. `domain` reste du Java indépendant de Spring, JPA, HTTP et de la sérialisation JSON.
4. `publicapi` expose seulement des capacités interdomaines et leurs objets de transfert neutres. Pas de repository, entité JPA, contrôleur ou classe d'implémentation d'un domaine utilisé par un autre.
5. DTO de commande/réponse propres à l'acteur et au canal utile. Aucun mapping automatique d'une requête publique vers une entité persistante ; autorisation dans les cas d'usage et périmètre SQL avant pagination/compteurs.
6. Transactions courtes pour les invariants ; outbox dans la transaction pour les effets externes. Pas de mail, appel HTTP, scan ou PDF pendant un verrou SQL.
7. Erreurs `application/problem+json` avec codes stables et `traceId`, sans valeur secrète, SQL ou stacktrace. `Clock` injecté et temps techniques UTC.
8. Aucun service/controller/entity universel, bus distribué, cache partagé ou deuxième base sans besoin documenté. Les événements ne remplacent pas les contraintes atomiques.

La [carte des modules](../architecture/modules.md) fixe les sens de dépendance et les propriétaires des données.

## Alternatives

| Alternative | Capacité réelle | Pourquoi non retenue maintenant |
|---|---|---|
| Packages par couche globale | Simple pour un très petit CRUD | Dilue la propriété des règles et facilite les accès interrepositories |
| Modules Maven séparés immédiatement | Frontières de compilation plus fortes | Coût de build et de découpage sans code existant ; ArchUnit d'abord |
| Microservices | Déploiements et capacité indépendants possibles | Invariants distribués, contrats réseau et exploitation disproportionnés |
| WebFlux | Pertinent avec une chaîne réellement non bloquante | Accès SQL retenus bloquants ; aucun bénéfice mesuré ici |

## Conséquences et vérification

- Déploiement et base partagés : les frontières doivent être testées, pas simplement dessinées.
- Mapping manuel initial entre HTTP, domaine et persistance ; un peu de duplication explicite est préférable au couplage des DTO.
- Lot 01 : build JDK 25/Boot du référentiel, refus HTTP par défaut, santé minimale, erreurs, CI et premiers tests d'architecture.
- Tests ArchUnit attendus : domaine sans Spring/JPA/HTTP, absence de cycles entre domaines, aucun repository intermodule, aucun contrôleur dépendant directement d'un repository, aucune entité JPA exposée dans les signatures HTTP. Ne pas annoncer une couverture de modules encore absents.
- Le passage à plusieurs artefacts exigerait une nouvelle décision fondée sur un problème concret.

## Sources consultées le 27 octobre 2026

- [Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html).
- [Exigences système Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), consultées pour Boot 4.1.2 ; détails dans [le référentiel](../versions.md).
- Sources internes : `Cahier_backend_SpringBoot_v1.md`, §§3, 7, 10, 13 ; `AGENTS.md` ; `SECURITE.md`.

Le choix de granularité est une décision de projet, pas une recommandation universelle attribuée à Spring.

## Suivi du lot 01

Le build, la santé/refus HTTP et les erreurs techniques existent sous `bootstrap`/`shared`. Les tests ArchUnit contrôlent les classes actuelles et démontrent les violations interdites sur des fixtures isolées. Aucun module métier vide ni entité fictive n'a été créé : les règles sans domaine réel ne valent pas couverture métier. Voir [la preuve du lot 01](../preuves/lot-01-verifications.md).
