# ADR005 — Outils gratuits et Angular Material/CDK

Date : 10/11/2026. Statut : retenu en FE01 sur instruction explicite de l'utilisateur, « always free tools ».

## Décision

- Utiliser uniquement des outils et dépendances gratuits autorisant l'usage commercial, sans siège, abonnement, clé payante obligatoire ou plafond d'éligibilité d'organisation.
- **Angular Material/CDK 22.2.6 (MIT)** remplace le candidat PrimeNG/PrimeUIX identifié lors de FE00. Une seule suite de composants, pas deux.
- Pas de paquet PrimeUI même via son régime Community, d'achat, d'inscription fournisseur ou de composant PRO. Ne pas revenir à une ancienne pile Angular/PrimeNG uniquement pour contourner la licence.
- Garder Angular 22 standalone/zoneless, Reactive Forms typés, Signals pour la vue et RxJS pour les flux. Runtime réellement installé par l'utilisateur : Node 26.13.0 LTS / npm 11.20.0, compatibles, sans changement global de PATH.
- Respecter les licences libres : attribution, notices et éventuelles obligations de redistribution demeurent applicables. Pas de promesse de gratuité d'une version future ; revue avant ajout/mise à jour.

## Preuves

Archives npm exactes Material/CDK lues en mémoire et intégrité SHA-512 comparée au registre ; LICENSE MIT SHA-256 commun `adb40d7d256e33c3c20a67869de175e7ca249afee92f587ccc48b5bec0ac24e4`. Peers : Angular ^22 ou ^23, CDK exact 22.2.6, RxJS compatible 7.8.2. Sources et versions dans [DEPENDANCES](../DEPENDANCES.md), verrou dans `app/package-lock.json`.

## Conséquences

La direction WorldTech, ses couleurs proposées, les thèmes et les exigences d'accessibilité ne changent pas. `mat.theme`, `mat.theme-overrides` et overrides de composants remplacent les API de preset PrimeNG ; `.wt-dark` pilote `color-scheme`. Les contrôles restent testés en zoneless, pas supposés accessibles parce qu'ils viennent d'une bibliothèque.

Les prompts historiques citant PrimeNG se lisent avec ce remplacement explicite ; ils ne sont pas modifiés pour masquer un contrôle. FE01 livre uniquement le thème de fondation, les pages d'état et un exemple de développement exclu du build de production. La bibliothèque de composants et les écrans de référence restent FE02.

Lucide ISC et IBM Plex OFL restent possibles, sans être installés sans besoin. Les futurs agenda/carte/graphiques/visionneuses suivent la même contrainte ; une extension payante n'est pas autorisée. Le workflow CI n'exige pas de service payant, mais aucun hébergement, quota gratuit illimité ou exécution distante n'est promis.
