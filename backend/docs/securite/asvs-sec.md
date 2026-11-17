# Correspondance initiale OWASP ASVS 5.0.0 / SEC-xx

27 octobre 2026 — **mapping initial et partiel, aucune certification**. Référence stable : [release officielle `v5.0.0_release`](https://github.com/OWASP/ASVS/releases/tag/v5.0.0_release), [export JSON consulté](https://github.com/OWASP/ASVS/releases/download/v5.0.0_release/OWASP_Application_Security_Verification_Standard_5.0.0_en.flat.json) : 345 exigences. Ne pas utiliser les identifiants ASVS 4.x pour ce référentiel.

Les identifiants ci-dessous ont été lus dans cet export. `L1`/`L2` désigne le niveau minimal de l'exigence ASVS, **pas un niveau atteint**. L'objectif L2 inclut les exigences applicables de L1. Les SEC sont internes au projet ; une SEC peut couvrir plusieurs exigences et une exigence plusieurs SEC.

## État et preuve

- **Prévu/conçu** : ADR, matrice et plan présents ; aucune route métier, authentification ou contrainte SQL implémentée.
- **Implémenté** : documentation/sonde du lot 00, socle HTTP du lot 01, infrastructure du lot 02, puis identité locale web/mobile, MFA/récupération et Google optionnel isolé au lot 03.
- **Vérifié** : [lot 00](../preuves/lot-00-verifications.md), [lot 01](../preuves/lot-01-verifications.md), puis SQL/PostGIS réels, ACL Windows, persistance, one-shot et démarrage runtime au [lot 02](../preuves/lot-02-verifications.md). La CI distante reste non exécutée ; consulter cette preuve pour les scans et leurs limites.
- **Vérifié au lot 03** : [172 tests Java + 6 Python](../preuves/lot-03-verifications.md), y compris session/CSRF, MFA/reset, rotation/rejeu mobile, double OIDC signé, contrat et contrôles ciblés des secrets. Ne vaut pas conformité de toutes les exigences associées.
- **Non vérifié** : couverture complète SEC/ASVS, futurs droits multi-comptes métier, Google réel, concurrence métier, TLS/staging, restauration, revue indépendante et pentest. Les constats critiques/élevés et le patch Temurin du lot 02 restent ouverts.

## Table initiale de correspondance

Chaque ligne reste **partiellement ou entièrement à implémenter/vérifier** : le lot 01 introduit des premiers contrôles SEC-01/06/14/17/19, sans achever ces exigences. Les références sont un recouvrement initial, pas une déclaration de couverture exhaustive de chaque exigence citée. [Détail des contrôles et limites du socle](../lot-01-socle.md).

| SEC | Exigences ASVS 5.0.0 et objet du rapprochement | Lots de réalisation / preuves attendues |
|---|---|---|
| SEC-01 | `V8.1.1` L1 : règles fonction/données documentées ; `V8.2.1` L1 : permissions explicites ; `V8.3.1` L1 : contrôle sur couche de confiance | 01 puis chaque domaine : refus par défaut, profils positifs/négatifs, aucune fonction privilégiée pré-MFA |
| SEC-02 | `V8.2.2` L1 : accès aux objets ; `V8.4.1` L2 : séparation des tenants | 05–12 : permutations compte/site/UUID sur listes, totaux, exports et documents ; DB/effets finaux |
| SEC-03 | `V8.1.2` L2 : règles par champ ; `V8.2.3` L2 : accès par champ ; `V2.2.1` L1 : validation positive | 03–08 : champs rôle/approbation/propriétaire/prix privilégiés refusés selon le contrat, DB finale contrôlée |
| SEC-04 | `V11.4.2` L2 : stockage des mots de passe avec KDF coûteuse et paramètres adaptés | 03 : paramètres, Unicode, coût, charge bornée, rehash ; E00-04 = compatibilité seulement |
| SEC-05 | `V2.4.1` L2 : anti-automatisation ; `V6.3.1` L1 : brute force ; `V15.3.4` L2 : IP provenant de sources de confiance | 03 puis routes coûteuses/13 : limites distribuées, compte absent/existant, X-Forwarded-For falsifié, refus 429 |
| SEC-06 | `V7.2.4` L1 : renouvellement de session ; `V7.3.1`/`V7.3.2` L2 : inactivité/absolu ; `V3.3.4` L2 : HttpOnly ; `V3.5.1` L1 : anti-CSRF | 03 : fixation, cookie effectif, CSRF login/logout/mutations, expiration et révocation |
| SEC-07 | `V7.2.1`/`V7.2.3` L1 : validation serveur/entropie des tokens de référence ; `V7.4.1` L1 : fin de session | 03 : opaque local, cookie refusé, refresh atomique et rejeu ; règles spécifiques de rotation issues du contrat interne |
| SEC-08 | `V7.4.2` L1 : fin des sessions à désactivation ; `V8.2.1`/`V8.2.2` L1 : permissions actuelles | 03/04 puis chaque domaine : ancienne session/access encore non expiré et rejeu après perte de droit |
| SEC-09 | `V6.4.1` L1 : activation initiale aléatoire/expirante ; `V6.4.3` L2 : reset sans bypass MFA ; `V7.4.3` L2 : terminaison des autres sessions | 03/04 : token lié au but, expiré/double/concurrent, GET de préchargement sans effet ; règle de révocation projet plus stricte |
| SEC-10 | `V6.3.3`/`V6.3.4` L2 : force d'authentification et chemins cohérents ; `V6.4.4` L2 : récupération du facteur ; `V6.5.1` L2 : usage unique TOTP/codes | 03 : MFA sur tous les chemins ADMIN, récupération, rejeu ; **écart L2 sur les autres rôles à examiner ci-dessous** |
| SEC-11 | `V6.8.1`/`V6.8.2` L2 : namespace d'identité et assertions signées ; `V10.1.2` L2 : transaction ; `V10.5.1`/`V10.5.3` L2 : nonce/issuer OIDC | 03, option Google : signature/issuer/audience/nonce/state, liaison réauthentifiée sans fusion email ; doubles + fournisseur de test |
| SEC-12 | `V10.1.2` L2 : secrets liés à session/transaction ; `V14.2.1` L1 : absence de secrets dans URL ; `V16.2.5` L2 : traces sans données sensibles | 03, option Google mobile : code/verifier/destination/TTL/rejeu ; point de vigilance du code de remise en lien ci-dessous |
| SEC-13 | `V5.2.1`/`V5.2.2` L1 : taille/type réel ; `V5.3.2` L1 : chemins contrôlés ; `V5.4.3` L2 : antivirus ; `V8.2.2` L1 : accès objet | 09/10 : quarantaine fail-closed, faux MIME, parent hors périmètre, upload incomplet, URL temporaire |
| SEC-14 | `V1.2.1`/`V1.2.4` L1 : encodage contextuel/requêtes paramétrées ; `V2.2.1`/`V2.2.2` L1 : validation côté confiance | 01 puis chaque domaine : erreurs, longueurs/tris, injection, XSS, SSRF/traversal et rendu PDF ; compléter le mapping spécifique au lot |
| SEC-15 | `V8.1.2`/`V8.2.3` L2 : restrictions de lecture par champ | 07 : inspection réponses réseau/DTO, texte public, point/distance publics ; aucun contact ou détail privé |
| SEC-16 | `V2.3.3` L2 : atomicité métier ; `V2.3.4` L2 : réservation sans double attribution | 06–08/10 : contraintes DB, transactions concurrentes, révision/snapshot acceptés, idempotence et effets finaux |
| SEC-17 | `V13.3.1`/`V13.3.2` L2 : gestion et moindre privilège des secrets ; `V16.2.5` L2 : limitation des logs sensibles | 01/02 puis tous : secret scan, droits runtime, clé MFA externe, aucune route métier sur credentials ; E00-01/03 = préparation seulement |
| SEC-18 | `V12.1.1`/`V12.2.1` L1 : TLS ; `V12.3.2` L2 : certificats validés ; `V3.4.2` L1 : CORS ; `V4.1.3` L2 : headers intermédiaires non falsifiables | 02/03/14 : configuration effective staging, cookies/CORS/proxy/en-têtes, DB non publique |
| SEC-19 | `V15.2.1` L1 : délais de maintenance/remédiation ; `V15.2.3` L2 : fonctionnalités production nécessaires seulement | 01/02/14 : politique de remédiation, résolution réelle, scans/SBOM/provenance ; E00-02 = sources, pas analyse de vulnérabilités |
| SEC-20 | `V16.2.1` L2 : métadonnées de traces ; `V16.3.2` L2 : refus d'autorisation journalisés ; `V16.4.3` L2 : collecte/détection séparées ; `V7.4.5` L2 : révocation administrative | 03/11/13/14 : audit, alertes et révocation testés ; sauvegarde/restauration = exigence opérationnelle additionnelle, pas équivalence ASVS complète |

## Écarts et applicabilité à ne pas masquer

1. **MFA** : `V6.3.3` L2 vise l'accès à l'application avec plusieurs facteurs/mécanismes, ou une justification documentée et des mesures compensatoires. Le produit exige le MFA pour ADMIN seulement. Ce n'est pas une couverture complète de cette exigence pour CLIENT/TECHNICIAN. Examiner et faire arbitrer le risque avant de revendiquer L2 ; ne pas étendre silencieusement les parcours pendant le lot 01.
2. **Secrets en liens** : le contrat prévoit un code de remise mobile en lien ainsi que des liens d'invitation/action. Un code à usage unique reste sensible même sans access/refresh token. `V14.2.1` n'est donc pas réputée satisfaite par simple changement de vocabulaire : revue du transport et des traces, absence de tiers/Referer/cache, PKCE et consommation atomique, puis justification/écart résiduel documenté.
3. **OAuth** : le backend est client OIDC pour Google, pas serveur OAuth générique. Les règles de refresh mobile s'inspirent des protections standard mais ne rendent pas automatiquement applicables toutes les exigences ASVS `V10.4` d'un authorization server. Leur applicabilité sera justifiée explicitement ; aucune architecture d'identité remplacée n'est restaurée.
4. **Couverture** : seuls des liens initiaux sont créés. Passer l'inventaire entier de 345 exigences en revue, retenir les exigences L1/L2 applicables et justifier chaque exclusion, avec propriétaire et date. GraphQL, WebSocket, WebRTC ou fournisseur OIDC ne sont pas ajoutés pour remplir cet inventaire.
5. **Mobile** : cette table ne couvre pas le stockage local, la purge ou la protection de l'app Flutter. Complément [OWASP MASVS](https://mas.owasp.org/MASVS/) à établir lors de la tranche mobile/hors ligne.
6. **Exploitation** : secrets et privilèges locaux sont vérifiés au lot 02, pas leur rotation en production. Scans et écarts : preuve de 02. TLS/staging, restauration et surveillance restent non vérifiés ; un simple redémarrage n'est pas une restauration. Les ACL Windows ont été contrôlées effectivement hors OneDrive, pas déduites du contrôle du pack sous Linux.

## Mise à jour par lot

Créer une preuve selon le [modèle](modele-preuve.md), avec un identifiant ASVS exact et sa version. Pour chaque exigence : menace, contrat/contrôle, tests positifs et négatifs, résultat réel, limites, revue et décision d'applicabilité. Un test non exécuté reste non exécuté, pas « conforme par conception ». Revue humaine, pentest indépendant et retests avant production selon `SECURITE.md`.
