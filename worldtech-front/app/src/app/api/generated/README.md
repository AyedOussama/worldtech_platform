# Contrats TypeScript générés — FE03

`types.gen.ts` et `index.ts` sont produits par **@hey-api/openapi-ts 0.97.0 (MIT)** depuis le contrat réel OpenAPI 3.1.0 / API 0.6.0 du backend voisin. `provenance.json` porte son empreinte ; `app/api-contracts/maintenance-api.json` est une copie exacte, pas un Swagger inventé ou simplifié.

Depuis `app/` : `npm run api:generate` pour régénérer après revue du contrat, `npm run api:verify` pour comparer sans modifier les sorties. Quand le backend voisin est présent, toute dérive du snapshot est refusée ; le snapshot vérifié permet une CI frontend isolée. Les noms des opérations sans operationId sont déterminés par méthode/chemin, sans modifier la source.

Le transport Angular adapté aux endpoints d'identité est dans `core/auth/identity-api.ts`, hors de ce dossier. Les modèles readOnly/writeOnly sont distincts ; login/reset utilisent leurs types d'entrée avec mot de passe. Null/chaînes décimales restent tels que décrits. **Les int64 métier générés en number ne sont pas qualifiés pour des versions longues** : l'identité n'en utilise pas ; conserver les ETag opaques et qualifier ce mapping avant les lots métier.

Ne jamais éditer les fichiers générés manuellement. Ils sont compilés strictement, exclus du lint de style manuel ; aucun runtime fetch/Bearer mobile n'est ajouté par ce générateur de types.
