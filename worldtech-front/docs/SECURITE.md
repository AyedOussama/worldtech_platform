# Sécurité frontend : exigences et preuves
Les protections du navigateur complètent celles du serveur. Aucun contrôle Angular ne garantit l'isolation des données à lui seul.

| ID | Exigence | Preuve attendue |
|---|---|---|
| WEB01 | Session serveur, pas de token web en localStorage | Inspection navigateur et trafic |
| WEB02 | CSRF sur les mutations, login/logout compris | Requête absente/altérée refusée |
| WEB03 | Aucune donnée admin avant MFA complet | Route et endpoint direct refusés |
| WEB04 | Deux clients et sites distincts | URL, recherche, compteurs, export et fichier testés |
| WEB05 | Notes internes absentes du DTO client | Réponses réseau inspectées |
| WEB06 | Affichage de texte non fiable sécurisé | Ticket avec caractères/markup, aucun script exécuté |
| WEB07 | Destinations après login limitées | URL externe refusée |
| WEB08 | Pièce privée et état CLEAN | Quarantaine et téléchargement hors périmètre refusés |
| WEB09 | Caches purgés au logout/changement de compte | Retour arrière et requêtes en cours testés |
| WEB10 | Logs et télémétrie sans secrets | Inspection des événements et captures |
| WEB11 | Configuration publique sans clés privées | Contenu construit et runtime-config inspectés |
| WEB12 | CSP compatible avec styles Angular/Material | Environnement cible, erreurs CSP examinées |

## Affichage et navigation
Utiliser les bindings Angular sûrs. Tout bypass de sanitization exige une justification localisée et une revue ; ne pas l'utiliser pour afficher une description de ticket.
Aucune URL arbitraire utilisée comme iframe ou téléchargement sans validation. Prévoir download/Content-Disposition côté serveur et révocation des object URLs créées dans le navigateur.
Limiter les informations d'erreur ; aucun SQL, stacktrace ou contenu d'un autre compte.

## Confidentialité
localStorage : thème et autres préférences non sensibles uniquement. Aucun cache persistant de tickets/rapports par défaut.
Cache HTTP des API privées et pages d'identité : configuration explicite, sans stockage partagé au CDN. Le service worker de données privées est hors socle.
Les captures de test utilisent des données synthétiques. Les outils de session replay ne capturent pas les formulaires, jetons ou pièces.
La présence d'un UUID ne constitue pas une autorisation.

## CSP
La politique est conçue avec les sources réellement utilisées. Les styles injectés par Angular/Material doivent respecter la stratégie nonce/hash/feuille externe effectivement supportée.
Ne pas déclarer une CSP stricte validée si aucun build n'a été exécuté avec elle. Ne pas ajouter unsafe-inline ou des origines génériques uniquement pour faire disparaître les erreurs.
Les URLs temporaires du stockage, fonts et connect-src sont ajoutées seulement si nécessaires et contrôlées.

## Limites de preuve
Un test navigateur avec serveur simulé vérifie l'interface, pas Spring Security. Le rapport de lot indique « simulé », « API réelle » ou « non exécuté » pour chaque garantie importante.
Une revue de pack n'est pas un audit de pénétration ni une certification ASVS.

## Preuves d'identité FE03
`core/auth/` et `webSessionInterceptor` mettent en œuvre session HttpOnly, CSRF mémoire borné à l'API web, destinations internes et invalidation des requêtes privées. Les tests couvrent réponses tardives, double action, refus/réseau/429 et changement inter-onglets. La fixture Spring isolée vérifie les cookies, le CSRF absent/altéré, l'accès admin avant/après MFA, les codes à usage unique, les révocations et l'absence de privilège après simple confirmation email. [Résultats, niveaux et réserves](../verification/fe03/RESULTATS.md).

La fixture réelle désactive **captures, traces, vidéos et snapshots DOM automatiques d'échec** pour ne pas conserver clés TOTP/codes de récupération. Les saisies sensibles de ses tests remplacent les diagnostics Playwright susceptibles d'inclure les valeurs par une erreur neutre. Les preuves visuelles utilisent uniquement des doubles HTTP et des secrets déterministes fictifs. Aucun cookie/CSRF/mot de passe de recette n'est conservé dans les rapports durables.

Google activé, livraison SMTP, cookies Secure en HTTPS, CSP de production et isolation des futures données métier ne sont pas qualifiés par FE03. L'absence d'avis npm runtime ne certifie pas la sécurité du produit ; les onze alertes modérées d'outillage restent visibles et analysées.
