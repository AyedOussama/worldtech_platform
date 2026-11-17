# Google optionnel et remise mobile — revue de conception 03E

01/11/2026. Revue interne d'implémentation, pas audit indépendant. Google doit rester désactivé tant que la configuration réelle et les App/Universal Links ne sont pas éprouvés en staging.

## Frontières et invariants

- Le fournisseur atteste uniquement `(provider=google, issuer, sub)`. Ni l'email ni son domaine ne donnent un compte, un rôle ou une validation professionnelle. Une identité inconnue ne crée pas de compte actif ; l'onboarding autorisé appartient au lot 04.
- Transaction immuable LOGIN/LINK et WEB/MOBILE, échéance courte, lancement unique et callback lié au navigateur. LINK doit être explicitement préparé depuis le compte récemment réauthentifié, avec MFA récent pour ADMIN, puis revalidé au retour. Un retour LOGIN ne devient jamais LINK implicitement.
- OAuth2 Login standard Spring, code serveur, state/nonce, PKCE S256 ; aucune suppression des contrôles signature/issuer/audience/temps. Endpoints fournisseur et callback configurés par l'opérateur, pas par les requêtes.
- La chaîne OAuth ne persiste pas un `OidcUser` privilégié ou des jetons Google dans une session métier. Seul le succès validé peut créer une session locale ; ADMIN reste MFA_PENDING. Mobile ne crée aucune session web authentifiée.
- Code de remise aléatoire de 256 bits, empreinte seule, 60 s maximum, transaction/destination/challenge S256 liés. Le code n'est ni access ni refresh. Consommation et émission locale atomiques ; pas de double émission après concurrence.

## Menaces et refus attendus

| Attaque | Contrôle attendu / conséquence |
|---|---|
| Code de remise intercepté dans le lien | Sans verifier PKCE, aucun jeton délivré ; HTTPS et App/Universal Link revendiqué indispensables |
| Application malveillante enregistrant un schéma custom | Schémas custom refusés ; destination HTTPS inscrite par identifiant et validée en staging |
| Remplacement de transaction/destination/challenge | Valeurs immuables côté serveur, liaison à la transaction et comparaison S256 |
| Rejeu ou double échange concurrent | Consommation sous verrou, un seul gagnant ; token inconnu ne modifie aucune autre transaction |
| Réponse d'échange perdue après commit | Code consommé : recommencer le login ; aucune reconstruction des jetons bruts |
| Login CSRF, state/nonce volé ou callback d'un autre navigateur | Liaison navigateur, expiration, usage unique et validateurs Spring ; aucun compte/session modifié avant la validation fournisseur |
| Fusion via email identique | Recherche par triplet externe uniquement ; liaison exige preuve locale explicite |
| Session suspendue ou mot de passe reset pendant LINK | Revalidation statut/version/session/réauthentification au callback ; refus sans liaison |
| ADMIN par Google/mobile | Session web locale pré-MFA ; canal mobile technicien exclusivement |
| Open redirect / destination libre | Origines et chemin de callback exacts, destinations préenregistrées ; aucun `redirect_uri`/returnUrl fourni par un DTO |
| Fuite via logs/cache/Referer | `no-store`, `no-referrer`, aucun contenu tiers ou jeton dans une URL ; supprimer/redacter les query strings des callbacks et codes dans le reverse proxy/APM |
| Vol d'une base | Hash des credentials aléatoires, enveloppe chiffrée des éléments temporaires sensibles ; clés externes. Cela ne protège pas une JVM compromise |

## Activation réelle encore obligatoire

Fournir les identifiants Google réels hors dépôt, enregistrer le callback HTTPS exact et les domaines contrôlés, vérifier TLS/cookies/SameSite/CSRF, state/nonce/PKCE et le retour de plusieurs comptes/rôles. Publier et vérifier les fichiers d'association Android/iOS sur le domaine de retour ; installation et interception par une autre app testées sur appareils réels. Vérifier les journaux proxy, APM et plateforme mobile, la purge du code et l'absence de cache/ressources tierces. Documenter le consentement et les erreurs sans données personnelles.

Les doubles OIDC locaux prouvent des comportements négatifs du backend, **pas** la configuration Google ou les associations d'application réelles. Aucun faux client ou secret n'est livré comme configuration réelle.
