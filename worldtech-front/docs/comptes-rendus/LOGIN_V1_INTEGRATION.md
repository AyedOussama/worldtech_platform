# WorldTech Login V1 — intégration de la référence fournie

12 novembre 2026. Demande utilisateur : exécuter `Codex_Prompt_WorldTech_Angular_Login.md` avec `WorldTech_Login_Assets_V1/`. Périmètre : **la connexion existante `/connexion` uniquement**, pas le dashboard, l'onboarding FE04 ou le backend. Intégration effectuée ; recette finale en cours.

## Source et audit

- Les 36 fichiers du pack extrait sont présents. README, manifeste, démo HTML, tokens et actifs ont été examinés avant modification ; le prompt et le pack d'entrée restent inchangés.
- La référence complète est fournie dans le dernier retour utilisateur, mais pas comme fichier `worldtech-login-reference.png` dans le dossier. Elle complète la démo et le montage gauche pour la revue de composition ; aucune de ces captures ni la planche d'assets n'est publiée comme fond de page. Aucun score de différence pixel automatique contre la pièce jointe n'est revendiqué.
- Projet conservé : Angular 22.2.5 standalone/zoneless, Material/CDK 22.2.6, SCSS, Reactive Forms et session Spring/CSRF. Aucune dépendance, police, seconde application ou bibliothèque d'animation ajoutée. IBM Plex Sans auto-hébergée est conservée ; l'import Google Fonts de la démo n'est pas repris.
- La route de connexion sort seulement de l'enveloppe publique générique afin d'obtenir le plein écran 61/39 ; son URL reste identique. Les autres pages gardent leur layout et leurs styles.

## Direction et intégration

Référence de méthode : **unslop par Damir Sokolovsky (@heydamir), CC BY 4.0**, sources tierces inchangées. La direction visuelle explicite du prompt utilisateur prime sur les préférences génériques du guide : ses dégradés bleu/cyan, badges et logos sont reproduits, pas remplacés par un formulaire neutre.

- **Produit** : accès client/administrateur au portail des interventions IT.
- **Caractère** : technique, incarné, contrasté.
- **Univers** : technicien à la tablette, racks de datacenter et réseau connecté.
- **Moments** : photographie immersive à gauche ; véritable formulaire dans une carte lumineuse à droite.
- **Signature** : logos WorldTech fournis, accent cyan du titre et repère lumineux sous l'éditorial.

Espacements, couleurs, rédaction, typographie, disposition, détails et mouvement sont traités localement : colonnes 61/39 dès 1280 px, promotion réduite sur tablette, formulaire prioritaire sur téléphone, logos clair/sombre, champs nommés et boutons tactiles. La carte et la scène sont deux petits composants de présentation ; le formulaire et les événements restent dans `LoginPage`. Le thème partagé n'est pas remplacé.

## Correction après le retour utilisateur

- Défaut reproduit à **1280 × 720** avec indisponibilité simulée : hauteur document/scène **880 → 907 → 1 029 px**, à l'affichage, au blur puis à la validation. Le fond était recalculé selon une grille pilotée par le contenu droit.
- Grille désormais de hauteur `100svh`, ligne `minmax(0, 1fr)` ; scène positionnée dans son propre cadre, sans minimum de 800 px. Les messages et la saisie ne peuvent plus changer ses dimensions.
- Sélecteur et réservation d'espace supprimés de `/connexion` seulement. Le service de thème existant reste initialisé sans contrôle visible ; les préférences et les sélecteurs des autres pages restent conservés.
- Carte dimensionnée par la hauteur disponible, densité adaptée à la hauteur et à la largeur. Une seule zone de feedback affiche validation liée aux champs, indisponibilité ou refus, sans empiler des messages redondants. La synthèse partagée reçoit une variante compacte **optionnelle**, sans changement du rendu par défaut des autres formulaires.
- Titre gauche, césures, sous-lignage, badges, taille des logos, bande de bénéfices et cadrage du technicien rapprochés du montage. La photographie conserve son ratio ; aucun personnage étiré ni montage contenant des textes n'est utilisé.
- Les contrôles de recette vérifient aussi l'absence de défilement **vertical et interne** aux tailles normales retenues et l'identité pixel de la scène pendant les interactions, pas seulement l'absence de débordement horizontal.
- En très faible hauteur ou avec texte fortement agrandi, un défilement **interne de secours** garde les champs/actions accessibles au lieu de les couper. La page et la scène restent bornées à l'écran. Le bouton Google déjà inactif est remplacé par sa mention d'indisponibilité à moins de 661 px de haut ; aucune action opérationnelle n'est retirée.

Les badges et bénéfices sont du HTML avec SVG, pas une capture d'écran. Les mentions internationales et l'inscription technicien de la démo sont écartées : « Couverture nationale », « partout en France » et consigne de contacter son administrateur. Aucun contact, email ou endpoint d'activation n'est inventé.

## Assets et droits

Déploiement dans `app/public/assets/worldtech/login/` : **20 copies exactes et une carte dérivée**, soit 21 fichiers. Inventaire détaillé et empreintes dans [ASSETS.json](../../verification/login-v1/ASSETS.json).

- Deux logos : `branding/worldtech_logo_blanc.png`, `worldtech_logo_sombre.png`.
- Deux photos : `photos/photo_technicien_datacenter.webp`, `photo_salle_serveurs.webp` ; la seconde fournit le fond/fallback, la première le technicien, avec cadrage et fondu CSS.
- Graphiques : `arcs_connexion.svg`, `vagues_fond_droite.svg` et `carte_mondiale_transparente.webp`.
- Icônes fournies : mission, réseaux, systèmes, support, bolt, globe, layers, mail, lock, eye, eye_off, arrow_right, security, google_g.
- La carte PNG 1800 × 647 de **1 219 251 octets** est convertie hors runtime en WebP transparent **900 × 324 / 156 350 octets**, soit environ 87 % de moins. Le PNG source n'est pas modifié. Aucun base64 n'est livré dans l'application.
- Non publiés : previews, démo, tokens globaux de la démo, photo bureau, personnage détouré approximatif, carte opaque alternative, favicon/symbole et icônes inutilisées. Le favicon commun et l'ancien logo utilisateur restent intacts.

**Droits non certifiés pour la production** : le manifeste fourni qualifie ces illustrations de travail à valider. Cette intégration locale n'invente ni licence MIT des images ni autorisation de marque Google ; les droits/qualité des sources doivent être confirmés avant diffusion de production. Aucun outil payant ou service d'images n'a été introduit.

## État fonctionnel

| Fonction | État |
|---|---|
| Email/mot de passe | Vrai `SessionService` conservé, CSRF/session/MFA/navigation par identité serveur ; dépend du backend pour l'usage normal |
| Visibilité du mot de passe / erreurs / Entrée | Vrais contrôles HTML/Reactive Forms, autocomplete conservé, erreurs reliées et synthèse focalisée |
| Google | Emplacement visuel demandé, bouton natif **désactivé** et libellé « Connexion Google non configurée » ; aucun handler OAuth ou URL inventée, configuration toujours `false` |
| Mot de passe oublié / email | Routes et services existants conservés ; livraison SMTP réelle non déduite d'une simulation |
| Invitation client | Pas d'écran d'activation FE04 créé ; accès sur invitation, consigne de contacter son administrateur, pas d'inscription technicien web |

## Vérification

Les résultats finaux, captures et commandes sont regroupés dans [la recette Login V1](../../verification/login-v1/RESULTATS.md). Les preuves FE01–FE03 ne sont pas réécrites : les relances de leurs suites prennent `WT_E2E_EVIDENCE_DIR=../verification/login-v1/regression-fe03`.

La revue est comparée aux sources disponibles, pas déclarée pixel-perfect : la photo est un cadrage interpolé différent de la référence, les logos raster ont des bords imparfaits et la carte un détourage approximatif. Le bouton dégradé est ajusté pour maintenir le contraste du texte ; Google et l'accès au compte diffèrent volontairement des faux parcours de la première maquette. Pas de revendication de conformité WCAG complète ou de mise en production.
