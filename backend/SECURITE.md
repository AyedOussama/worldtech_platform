# Exigences et preuves de sécurité — v1.1

État initial : **conçu, non implémenté et non audité**. Le pack fournit des documents et des références, pas une application dont la résistance aurait été démontrée. Aucun outil ou choix technologique ne prouve seul l'absence de vulnérabilité.

Suivi au 02/11/2026 : socle, identité/admission/parc et contrats/tickets par site vérifiés localement ; [preuves courantes du lot 06](docs/preuves/lot-06-verifications.md). Approbation technicien toujours bloquée sans pièces contrôlées (09). Google réel, missions/documents, exploitation SLA, charge, revue indépendante et pentest non vérifiés. L'état initial ci-dessus est historique, pas une déclaration de conformité actuelle.

Référence de travail : OWASP ASVS 5.0.0, objectif niveau 2 pour web/API ; exigences mobiles complémentaires à examiner avec OWASP MASVS. L'équipe doit construire la correspondance avec les exigences applicables et justifier les exclusions. Les identifiants SEC-xx ci-dessous sont internes au projet, pas des numéros ASVS ni une attestation de conformité.

## Menaces prioritaires

Visiteur abusant inscription/reset ; client changeant un identifiant pour lire un autre compte ; technicien en attente, suspendu ou non affecté ; vol/rejeu d'une session ou d'un code ; liaison Google détournée ; fichier malveillant ; administrateur compromis ; sélection financière concurrente ; fuite de secrets, chaîne de dépendances ou mauvais déploiement. Les contrôles couvrent navigateur, mobile, backend, base, stockage et exploitation.

## Matrice de réalisation et recette

Toutes les lignes ont l'état initial « à implémenter/vérifier ». Pour chacune, le dépôt conserve une preuve datée : chemin de test, résultat CI, revue ou rapport d'audit selon la nature de l'exigence.

| ID | Exigence | Preuve minimale attendue |
|---|---|---|
| SEC-01 | Refus par défaut, rôle/statut/périmètre contrôlés serveur | Matrice visiteur/attente/actif/suspendu, deux clients et deux techniciens ; chaque route protégée testée |
| SEC-02 | Compte/site avant pagination, agrégat, export et téléchargement | Permutation d'UUID et filtres entre comptes sans données, total ni document divulgué |
| SEC-03 | Aucun rôle/approbation/prix admin par mass assignment | POST/PATCH avec champs privilégiés refusés ou ignorés selon contrat, état DB final vérifié |
| SEC-04 | Argon2id explicitement configuré et mesuré | Format et paramètres du hash, mot de passe incorrect, Unicode/longueur, migration de coût et charge bornée |
| SEC-05 | Contrôle d'abus distribué et erreurs génériques | Tentatives répétées/réparties, compte existant/absent, quotas reset/upload/recherche ; spoof X-Forwarded-For sans bypass |
| SEC-06 | Session web sûre : fixation, cookies, CSRF, logout, échéances | Rotation après login/MFA, refus mutation sans CSRF, session révoquée inutilisable, TTL absolue et inactivité |
| SEC-07 | Jetons opaques mobile et rotation refresh atomique | Jeton expiré/révoqué, cookie refusé comme auth mobile, refresh concurrent/réutilisé/inconnu et réponse perdue |
| SEC-08 | Suspension et changement de droits effectifs au prochain accès | Session et access encore non expirés ne permettent pas de conserver l'ancien droit ; aucun cache permissif |
| SEC-09 | Vérification/reset à usage unique, sans effet par GET | Token double/expiré/mauvais but, consommation concurrente, préchargement email, révocation après reset |
| SEC-10 | MFA ADMIN sur tous les chemins et récupération maîtrisée | Password/Google/API directe/reset/handoff ne contournent pas MFA ; rejeu TOTP et code de récupération refusés |
| SEC-11 | Google OIDC et liaison sûre | Mauvais issuer/audience/signature/nonce/state, email identique sans preuve, identité déjà liée et unlink sans alternative |
| SEC-12 | Retour mobile sans jeton en URL | Code de remise volé sans verifier, rejeu, expiration, substitution de transaction/destination et absence de fuite dans logs |
| SEC-13 | Documents privés et quarantaine | Hors périmètre, faux MIME, fichier trop gros, contenu malveillant, scanner indisponible, upload incomplet et URL expirée |
| SEC-14 | Données d'entrée et contenus non fiables | SQL paramétré, tris bornés, encodage sortie/XSS, tests SSRF/path traversal et rendu PDF sans accès réseau arbitraire |
| SEC-15 | Découverte sans donnée privée | DTO et réponses réseau sans contact/adresse/position précise ni texte confidentiel ; distance issue du point public |
| SEC-16 | Cohérence argent/affectation | Contraintes SQL, vraies transactions simultanées, une affectation active, révision acceptée conservée, idempotence |
| SEC-17 | Secrets et privilèges d'exploitation | Secret scan, absence de tokens dans logs/URL/images ; runtime sans DDL/superuser ; gestion privée non exposée |
| SEC-18 | TLS, cookies et proxy de confiance | Configuration effective staging, CORS exact, en-têtes/CSP compatibles, session Secure, aucun port DB public |
| SEC-19 | Dépendances et livraisons maîtrisées | Lock réel d'images, scan dépendances/images, SBOM, provenance et analyse des constats datées ; pas d'exclusion cachée |
| SEC-20 | Détection, réaction et restauration | Alertes testées, audit des changements sensibles, exercice de révocation/rotation et restauration isolée mesurée |

## Condition de fin de lot

L'agent indique : exigence SEC concernée, menace, contrôle introduit, test réellement exécuté et résultat, contrôle manuel restant, limite connue et effet utilisateur. Une analyse syntaxique valide la forme, pas les droits ni les flux réseau. Un test mocké ne valide pas un fournisseur réel. Les tests PostgreSQL/PostGIS ne sont pas remplacés par H2.

Un test négatif ne se limite pas au code HTTP : vérifier aussi absence de fuite dans le corps/headers, absence de modification indue en DB, de document rendu accessible et d'effet externe préparé. Les erreurs ne doivent révéler ni SQL/stacktrace ni existence d'un objet privé. Les scans complètent les tests métier ; ils ne détectent pas seuls tous les contournements d'affectation ou de prix.

## Conditions avant production

1. Traçabilité des exigences applicables ASVS et SEC, preuves disponibles et écarts explicités.
2. Tests unitaires, HTTP, intégration SQL, autorisation, concurrence et contrats exécutés dans l'environnement adapté ; aucun skip permanent masquant une absence de Docker.
3. Revue humaine indépendante du code d'identité, des droits, des fichiers et des migrations sensibles.
4. Pentest authentifié indépendant sur préproduction avec comptes de plusieurs rôles et périmètres ; correction et retest des constats critiques et élevés. Un faux positif est analysé et justifié, pas simplement masqué.
5. Configuration de production réellement vérifiée : MFA, cookies/CSRF, Google, secrets, réseau, accès administratifs, limites, audit et alertes.
6. Restauration et révocation/rotation testées ; responsables et runbooks d'incident connus.

L'approbation de lancement appartient aux responsables du projet sur ces preuves. Ce fichier ne crée pas une permission supplémentaire pour les modifications et tests réversibles d'un lot autorisé. Il ne permet pas non plus à un agent de déployer de sa propre initiative.

## Format du compte rendu d'un agent

```text
Comportement livré : ...
Exigences SEC traitées : ...
Contrôles implémentés : ...
Vérifications exécutées : commande / environnement / résultat ...
Vérifications non exécutées : ...
Risques et limites résiduels : ...
Prochaine tranche : ...
```

Ne pas écrire « 100 % sécurisé », « impossible à pirater » ou « tous les tests passent » sans préciser ce qui a réellement été vérifié. Après lancement, maintenance des dépendances, surveillance et réévaluation continuent.

## Sources

- ASVS : https://asvs.owasp.org/
- MASVS : https://mas.owasp.org/MASVS/
- Autorisation : https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- Fichiers : https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- Mots de passe : https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- CSRF/session : documentations Spring Security liées dans AUTHENTIFICATION.md
