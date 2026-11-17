# Lot 04 — invitations et dossiers d'admission

01/11/2026. **Parcours d'admission implémentés et vérifiés localement ; approbation technicien bloquée sans justificatifs contrôlés.** Vérification finale à 20:24:13 locale : **98 Surefire + 114 Failsafe = 212 tests Java**, zéro échec/erreur/skip ; six tests Python également réussis. [Preuves, scans ciblés et limites](preuves/lot-04-verifications.md). Les résultats historiques du lot 03 et les risques du lot 02 sont conservés. Aucun frontend, compte réel, migration du volume Compose ou envoi externe.

## Contrat produit confirmé

- Formulaire client : prénom, nom, téléphone, adresse, type `INDIVIDUAL`/`BUSINESS` ; raison sociale obligatoire pour BUSINESS. Dossier lié au `user_id`, pas à un email comme identifiant métier. Le compte client/membres/sites sera créé au lot 05, sans doublon anticipé.
- Formulaire technicien : prénom, nom, téléphone, compétences déclarées, ville/base et rayon de mobilité entre 10 et 200 km. Les compétences déclarées ne sont jamais présentées comme certifiées/vérifiées.
- Brouillons incomplets autorisés ; soumission complète après vérification email ; aucune activation automatique.
- **Justificatifs obligatoires pour TECHNICIAN uniquement**, confirmé par le responsable. Les types de pièces ne sont pas encore précisés et la chaîne documentaire privée/antivirus du lot 09 manque. L'approbation technicien doit donc refuser `TECHNICIAN_DOCUMENTS_REQUIRED`, sans option de contournement, upload improvisé ou fausse preuve CLEAN. La soumission et les demandes de complément restent possibles. Clients : validation sur le formulaire confirmé.
- États DRAFT → SUBMITTED → APPROVED / NEEDS_INFORMATION / REJECTED ; NEEDS_INFORMATION autorise correction et resoumission. REJECTED et APPROVED sont terminaux pour ce lot ; suspension distincte et jamais annulée par une décision.

## Choix de réalisation

Invitations 48 h par défaut configurables, 256 bits aléatoires, empreinte en table d'invitation et corps d'outbox chiffré. Renvoi ADMIN révoquant le lien précédent. Aucune consommation par GET. Aucun token dans la réponse ADMIN. Une identité existante doit se connecter et prouver son email ; aucun rapprochement ou changement de rôle automatique.

Ports explicites d'identité pour la création, la lecture courante et les transitions d'admission. Aucun module métier n'accède directement aux tables d'identité. Quotas SQL partagés réutilisés avant Argon2. Changements sous transactions courtes, auteur/motif/historique append-only, contrôle de version et idempotence des soumissions. Google reste désactivé et sa validation réelle demeure externe.

## Parcours locaux implémentés et testés localement

- ADMIN web actif, email vérifié, MFA et authentification primaire de moins de cinq minutes : créer/consulter/renvoyer une invitation ; lire les listes bornées et dossiers ; décider par commandes séparées. Aucun chemin administratif mobile.
- Invitation nouvelle : `POST /api/web/v1/onboarding/client/register` avec `{email,password,token}` crée en une transaction un accès CLIENT PENDING, un brouillon et un email de vérification. Le lien d'invitation n'authentifie pas implicitement une identité existante. Après connexion et vérification email, soumettre le formulaire.
- Identité CLIENT existante : connexion locale ou Google déjà associé, puis acceptation explicite avec `{token}`. L'email courant vérifié doit correspondre au destinataire. Un email identique ne change jamais le mot de passe, le rôle ou le propriétaire.
- Technicien : inscription locale sous le canal web ou mobile, réponse 202 identique pour une adresse nouvelle/existante. Aucune fusion ; un doublon ne modifie rien. Connexion explicite puis vérification email. Le web utilise CSRF ; le mobile utilise uniquement le Bearer opaque.
- Profil : lecture propre sans UUID choisi par l'appelant, sauvegarde par POST avec `If-Match: "<version>"`, puis `/submit` avec la même précondition et une `Idempotency-Key` de 16–100 caractères ASCII `[A-Za-z0-9_-]`. Réponse `{version,state}` sans données personnelles. Un rejeu réautorise l'acteur ; même clé/requête rend la même réception, autre version donne 409. Après 24 h une clé conservée est refusée, pas rejouée silencieusement ; utiliser une nouvelle clé après relecture.
- Seuls DRAFT et NEEDS_INFORMATION sont modifiables. Les données de chaque révision sont conservées en historique SQL append-only pour le runtime. Le propriétaire voit les actions/motifs ; ADMIN voit aussi l'auteur UUID. Les motifs ne sont pas copiés dans les logs ou le courriel de notification.
- REJECTED révoque les sessions et ferme l'accès selon la politique d'identité existante. Aucune réouverture ou levée de suspension implicite. APPROVED client rend son identité ACTIVE, mais n'ouvre aucune route ticket/site du futur lot 05/06.
- L'admission exige un rôle candidat exclusif (CLIENT ou TECHNICIAN), à l'accès, au rejeu et à la décision. Un cumul de rôles existant est refusé, pas modifié : le statut global ACTIVE ne doit pas activer un technicien par l'approbation client. Aucune politique de double admission n'est inventée.

Limites techniques : corps onboarding/admin 16 KiB ; listes 20 éléments par défaut, au plus 100 et page au plus 10 000 ; historique paginé par révision. Quotas partagés 20 tentatives/IP/opération/15 min et 5/identité/opération/15 min (clés hashées, IP du pair réel, pas `X-Forwarded-For` non fiable). Les réponses privées et d'erreur sont `no-store`. Les invitations expirent également lors de la commande, même si aucun job n'a tourné. Une réponse perdue après acceptation impose de se connecter/reprendre le dossier ; le token n'est pas réutilisable.

V8 introduit les tables et droits explicites. V9 ajoute la FK composite dossier/destinataire et une contrainte SQL refusant APPROVED pour TECHNICIAN tant que le contrôle documentaire manque. V10–V11 ajoutent et bornent le contexte d'inscription Google (prédicat total face aux NULL SQL). Les migrations V1–V7 ne sont pas réécrites. Les tests PostgreSQL sont éphémères ; le volume Compose n'est pas migré.

## Inscription Google explicite (testée avec un double OIDC local uniquement)

V10 enrichit les transactions existantes avec un contexte d'inscription immuable. Un LOGIN ordinaire inconnu reste refusé. Seuls les nouveaux endpoints dédiés peuvent inscrire, sans rôle arbitraire :

- Web `/api/web/v1/auth/google/client-registration/transactions` avec `{email,token}` et CSRF : contrôle de l'invitation à la préparation, puis du même lien, de son délai et du destinataire OIDC vérifié au callback. Compte neuf CLIENT PENDING, dossier DRAFT et consommation de l'invitation dans la même transaction. Une invitation remplacée pendant le parcours fait échouer le callback sans compte parasite.
- Web `/api/web/v1/auth/google/technician-registration/transactions` avec CSRF ; mobile `/api/mobile/v1/auth/google/technician-registration/transactions` avec `{challenge,destinationId}` : accès neuf TECHNICIAN PENDING et DRAFT. Le mobile conserve le handoff PKCE et n'authentifie pas le navigateur.

L'email OIDC est utilisé comme preuve du destinataire pour une **création explicitement autorisée**, jamais comme clé de fusion d'un compte existant. Un conflit d'email ou de triplet externe annule toute création. Le dossier reste lié au même UUID après une future connexion email/Google déjà associé.

Un accès Google seul conserve un password vide comme absence d'identifiant local, pas un mot de passe vide utilisable. Les parcours reset ne lui ajoutent pas silencieusement de password ; l'ajout explicite d'un moyen local de récupération est une fonction distincte non livrée. La dissociation reste interdite sans alternative valide. Perte de Google sur ces comptes : procédure opérateur, aucune réinitialisation email de contournement.

L'adaptateur d'assemblage `bootstrap/identity/GoogleAdmissionAdapter` relie le port d'identité aux capacités publiques clients/techniciens, sans cycle de dépendances ni accès interrepository. L'ordre des verrous client local/Google reste invitation → nouvel utilisateur/credential → dossier. Google réel et associations Android/iOS ne sont toujours pas validés ; option désactivée.
