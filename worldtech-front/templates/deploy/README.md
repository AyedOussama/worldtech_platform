# Modèle de routage
nginx-spa.conf.example illustre la séparation entre API, OAuth, ressources et fallback SPA. Aucun service Nginx n'a été démarré par ce pack.
- adapter le nom upstream et le chemin réel du build ;
- vérifier les callbacks OAuth exacts ;
- si TLS est terminé avant Nginx, transmettre le schéma externe via une frontière de confiance correctement configurée ; $scheme seul vaut HTTP derrière un proxy TLS et ne doit pas être conservé aveuglément ;
- prévoir en-têtes à une frontière cohérente : les add_header de sous-location peuvent modifier l'héritage selon version/configuration Nginx ;
- définir CSP avec Angular/PrimeNG et la politique de nonces réellement supportée ;
- appliquer le cache immutable seulement à des URLs garanties fingerprintées, après vérification ; le modèle privilégie la revalidation ;
- la limite de corps d'upload et ses timeouts doivent correspondre au contrat backend.
Les vérifications nginx -t, cookies, callbacks et trafic réel appartiennent à FE14.

