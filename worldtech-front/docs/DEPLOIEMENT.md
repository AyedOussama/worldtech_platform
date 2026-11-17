# Déploiement du frontend
Référence proposée : fichiers statiques derrière un serveur HTTP et reverse proxy de même origine. Les domaines WorldTech définitifs ne sont pas inventés dans le pack.

## Routage
- Routes de l'application : fallback vers index.html pour les liens directs.
- API /api/web/v1 : proxy vers Spring Boot, sans fallback SPA et sans cache partagé.
- OAuth /oauth2 et callback /login/oauth2 : proxy vers le backend selon contrat exact.
- Autres endpoints /api : refus explicite si non exposés.
- Fichiers inexistants JS/CSS/images : 404, pas une réponse HTML 200.
Voir [le modèle Nginx](../templates/deploy/nginx-spa.conf.example). C'est un exemple à adapter et tester, pas un service déjà lancé.

## Cache et configuration
index.html et runtime-config.json : non périmés au déploiement, politique no-store ou revalidation adaptée.
Resources fingerprintées : cache long seulement si l'URL change avec le contenu. Ne pas appliquer immutable à toutes les images de /assets.
runtime-config.json contient uniquement des valeurs publiques. Les secrets OAuth, clés privées, SMTP et base de données restent au serveur.
Valider les types et les origines autorisées avant bootstrap. Une configuration manquante affiche une erreur explicite ; pas de repli vers une API de production inattendue.

## Build et livraison
Installation verrouillée, lint, types, tests utiles et build de production. Artefact lié au commit et à sa configuration.
Le chemin dist réel provient du projet Angular ; ne pas présumer une ancienne structure de sortie.
Construire un seul artefact promu si la stratégie de configuration runtime le permet. Documenter les variables qui exigent un rebuild.

## Sécurité d'intégration
TLS à la frontière, cookies Secure et proxy de confiance correctement configurés côté Spring.
CSP et nonces réellement compatibles avec Angular/PrimeNG ; vérification dans le navigateur cible. Les en-têtes sont définis à une frontière cohérente, sans doublons contradictoires.
Les APIs privées ne sont pas mises en cache au CDN. Les pages d'invitation et leurs jetons ne vont pas dans la télémétrie.
Ne pas rendre publics le catalogue de développement, les sources internes ou les outils de test.

## Recette du déploiement
Connexion, MFA, refus CSRF, logout, lien direct, configuration, 404 de ressource statique, appel API non transformé en HTML, callback Google si activé, téléchargement privé, deux thèmes.
Tester une nouvelle version avec un onglet ouvert sur l'ancienne. Définir une politique de chunks périmés évitant la perte de formulaire.

## Retour arrière
Conserver l'artefact précédent et sa configuration compatible. Le rollback frontend ne restaure pas une ancienne base.
Vérifier la compatibilité des contrats pendant la transition et expliquer les limites. Le plan de sauvegarde PostgreSQL et objets reste dans le dossier backend/exploitation.

