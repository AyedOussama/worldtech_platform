# Initialiser le thème
FE02 peut placer theme-init.js dans les fichiers publics, puis le charger avant les feuilles de style et le bootstrap selon le pipeline réellement retenu.
L'exécution doit être compatible avec la CSP : fichier externe ou contenu autorisé par hash/nonce. Ne pas ajouter un inline non autorisé uniquement pour éviter un flash.
L'initialisation fonctionne même si localStorage est inaccessible. Elle ne suit pas les changements du système après démarrage ; le service Angular doit s'en charger et gérer le choix utilisateur.
Le script ne traite aucune donnée métier. Sa vérification fonctionnelle isolée est disponible dans scripts/verify_theme_init.mjs.

