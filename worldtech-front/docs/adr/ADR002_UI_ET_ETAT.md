# ADR002 — Suite UI et état ciblé
Statut : état/formulaires retenus ; candidat PrimeNG FE00 remplacé par Material/CDK MIT en FE01 sur instruction explicite « always free tools ». Voir [ADR005](ADR005_OUTILS_LIBRES.md).

## Décision
Angular Material/CDK comme seule suite principale, tokens WorldTech via les API Sass supportées, SCSS/CSS pour la disposition. Pas de PrimeNG/PrimeUIX. Lucide ISC reste la famille d'icônes cible, différée tant qu'aucune icône n'est utilisée.
Signals pour l'état de vue, RxJS pour les flux annulables, formulaires réactifs typés. État partagé seulement lorsque plusieurs écrans en ont réellement besoin.

## Conséquences
Cohérence et composants métier disponibles. Dépendance au fournisseur à gérer par contrats visuels raisonnables ; aucun wrapper systématique sur chaque contrôle.
Les contraintes zoneless et les composants de formulaire sont testés dans la version retenue.

## Historique du rapprochement FE00 (ne vaut plus choix d'intégration)
Les plages de PrimeNG 22.1.4, CDK 22.2.6 et Angular 22.2.5 se recoupent ; PrimeUIX themes 3.1.0/styled 1.2.0 disponibles. Cela ne constitue pas une installation ou un test zoneless. L'archive publiée PrimeNG et les huit auxiliaires UI contrôlés sont sous **PrimeUI**, pas sous MIT. Éligibilité/sièges/clé de l'organisation non fournis : aucun import ou achat autorisé par cet ADR. FE01 sans UI fournisseur peut avancer ; droits à valider avant les premiers imports, FE02 au plus tard. Voir [la matrice et les licences exactes](../DEPENDANCES.md).

Famille d'icônes applicative cible : `@lucide/angular` 1.71.0, ISC, peers Angular >=17. L'ancien paquet non scopé n'accepte pas Angular 22. Les icônes internes transitives de PrimeNG n'impliquent pas l'ajout d'une seconde famille applicative.

## Alternatives
Une autre suite UI peut être étudiée si licence ou compatibilité bloque. Le remplacement doit être documenté, pas introduit silencieusement.
Un store global complexe sera réévalué si des problèmes de coordination mesurables apparaissent.
