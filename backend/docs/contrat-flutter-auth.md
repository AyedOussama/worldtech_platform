# Contrat Flutter — authentification locale

Périmètre initial : application technicien uniquement. Aucun compte CLIENT ou ADMIN n'obtient un accès par ce canal ; les privilèges administratifs restent sur le parcours web avec MFA. Un technicien en attente peut lire son propre statut, sans autorité métier active.

La chaîne `/api/mobile/v1/**` est sans session navigateur : ne pas envoyer de cookie, ne pas accepter une authentification par cookie et ne pas utiliser un jeton Google comme Bearer applicatif. Prévoir `Authorization: Bearer <access opaque>` pour les lectures protégées. Les valeurs brutes ne doivent apparaître ni dans les URLs, ni dans les logs/crash reports/analytics.

La réponse de connexion/rotation contient `tokenType=Bearer`, `accessToken`, `refreshToken`, `accessExpiresAt`, `refreshExpiresAt`, `familyExpiresAt` ; elle est `no-store`. L'access vaut 5 min. Conserver les secrets uniquement dans les mécanismes protégés de la plateforme, avec remplacement atomique de la paire après rotation. L'inactivité refresh est de 7 jours et la durée absolue de famille de 30 jours.

**Un seul renouvellement en vol** par famille : partager le résultat entre requêtes concurrentes dans l'application. Deux appels avec l'ancien refresh ne produisent jamais deux branches actives. Un refresh déjà consommé révoque toute sa famille, y compris l'access issu de la première rotation.

**Réponse perdue après commit :** ne pas rejouer automatiquement l'ancien refresh. Il est désormais consommé ; son rejeu impose une nouvelle connexion. Pas de délai de grâce caché ou de réponse reconstruite depuis un refresh conservé côté serveur. Sur 401 de rotation, supprimer les secrets locaux et revenir à la connexion. Un refresh aléatoire inconnu ne révoque aucune famille choisie par le client.

Logout appareil : révocation de la famille courante. Logout global/reset : révocation de toutes les sessions du compte. Suspension, retrait du rôle et changement de version de sécurité sont contrôlés à la prochaine requête, même si l'access n'a pas expiré. Le serveur ne peut pas rappeler instantanément des données déjà copiées hors ligne : minimiser le cache et le purger au prochain refus/contact.

Routes sous `/api/mobile/v1` : POST `/auth/login` `{email,password}`, POST `/auth/refresh` et `/auth/logout` `{refreshToken}`, GET `/me` par Bearer et POST `/auth/logout-all` par Bearer. Vérification/reset : mêmes DTO et suffixes que le web, mais sans cookie/CSRF ; la preuve d'action reste obligatoire à la confirmation. POST `/auth/reauthenticate` `{password}` par Bearer pour une future liaison/déliaison sensible.

Les dix tests `MobileIdentityIT` ont réussi sur Tomcat/PostGIS le 01/11/2026. Preuves consolidées dans `lot-03-identite.md`. L'intégration Flutter et le stockage sécurisé effectif sur appareil ne sont pas testés par ces tests backend.
