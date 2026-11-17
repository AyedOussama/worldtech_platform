# Revue interne — invitations et admission (lot 04)

01/11/2026. Revue de conception/implémentation, pas audit indépendant ni approbation de production.

| Risque | Contrôle | Recette attendue |
|---|---|---|
| Vol/rejeu d'invitation | 256 bits, empreinte seule dans la table, destinataire lié, usage unique, TTL 48 h configurable, aucun effet GET | Ancien lien/expiry exacte/double consommation/concurrence/mauvais email ; SQL final et absence de compte parasite |
| Envoi arbitraire ou fuite | ADMIN actif/MFA récent, quotas SQL, origin frontend fixe, lien seulement dans outbox AES-GCM et Mailpit | Refus non-admin/pré-MFA/absence CSRF, scan de logs et corps/headers HTTP sans token |
| Prise d'un compte existant | Nouvel accès ou authentification explicite du compte vérifié ; pas de mise à jour par email ni de conversion de rôle | Email identique, mauvais compte, absence de modification credentials/rôles/dossier |
| Inscription Google détournée | Contexte d'inscription explicite et immuable, validateurs OIDC standards ; invitation/client email vérifié, nouvelle identité seulement | LOGIN inconnu refusé ; mauvais destinataire/lien remplacé, email/triplet existants : aucun compte créé/fusionné |
| Reset transformant un compte Google seul | Absence de password local contrôlée à l'émission, à la prévalidation et à la consommation du reset | Pas de token reset émis, password inchangé, unlink refusé sans alternative |
| Auto-approbation/mass assignment | DTO fermés, opérations distinctes, rôle fixe par parcours, aucune propriété approved/owner/userId/role/document CLEAN | Champs privilégiés, admin mobile/cookie, profil incomplet |
| Activation technicien via un dossier client | Rôle candidat exclusif à l'accès, au rejeu et à la décision ; aucun cumul activé par le statut global | CLIENT+TECHNICIAN ajouté après soumission : 403 propriétaire/rejeu, 409 décision client, statut PENDING et historique/outbox inchangés |
| Dossier ou historique d'un autre compte | Lecture propre via principal serveur, aucune route propriétaire avec UUID sélectionné ; détails/listes ADMIN seulement | Deux clients/deux techniciens, UUID permutés, refus corps/compteurs/données privées |
| Approbation technicien sans pièces | Règle produit obligatoire : refus applicatif et contrainte SQL jusqu'au contrôle documentaire | Même ADMIN ne peut pas approver ; rôle/statut/outbox/historique inchangés |
| Réactivation d'un compte suspendu | Identité/statut/version/session relus sous verrou avant toute décision ; statut cible PENDING exigé | Suspension après soumission et après authentification, refus au prochain accès/rejeu |
| Écrasement silencieux | If-Match, verrou utilisateur puis dossier, version augmentée ; snapshot historique immuable | Sauvegardes/décisions simultanées, une seule révision gagnante, 412 explicite |
| Soumission répétée après timeout | Idempotency-Key liée à acteur/opération/requête ; revalidation d'autorisation avant réception | Même clé/version même résultat, clé différente/contenu différent, concurrence et révocation |
| Effet externe sans commit | Outbox déjà disponible au lot 03, dans la même transaction ; worker SMTP hors verrou | Refus DB/outbox entraînant rollback, livraison Mailpit et effacement du corps après acquittement |
| Saturation | Quotas SQL avant Argon2, 2 calculs/JVM, corps 16 KiB, champs/listes/historiques bornés | Quotas multi-requêtes, X-Forwarded-For ignoré, entrée trop grande, pagination excessive |

La connaissance d'un UUID ne donne jamais un droit. Les seuls identifiants issus des DTO décision sont les ressources demandées, pas l'acteur, son rôle, le propriétaire ou le statut cible arbitraire. Les lectures de liste sont réservées à ADMIN ; aucune liste globale accessible aux candidats n'est filtrée après pagination.

Les compétences sont des déclarations, pas des habilitations vérifiées. La ville/base et le rayon ne constituent ni une position GPS suivie ni un géocodage démontré. Aucun document n'est accepté, téléchargé ou déclaré CLEAN par ce lot.

Points restants : pièces technicien exactes et contrôle documentaire lot 09 ; conservation des dossiers/historiques/invitations refusées lot 12 ; revue indépendante ; TLS/proxy/APM/SMTP de staging et pentest. Les constats critiques/élevés d'infrastructure précédents restent ouverts. Les tests backend ne prouvent pas le comportement d'un futur écran Angular/Flutter.
