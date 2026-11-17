# Prompt 09 — Stockage privé, quarantaine et antivirus

Révision 1.1 · Interfaces propres et authentification applicative.

Tu es développeur Java senior et expert Spring Boot. Travaille dans le dépôt fourni. Lis les instructions locales, AGENTS.md, le cahier backend, AUTHENTIFICATION.md, SECURITE.md et les ADR. Respecte le travail existant et le périmètre de ce lot. Si ses dépendances ne sont pas réalisées, constate-le avant de créer un doublon. Implémente une tranche complète et vérifiable ; ne génère pas tout le projet en une fois.

## Travail demandé

Implémente le port de stockage objet et un adaptateur S3-compatible local Garage ou le fournisseur retenu. Vérifie et verrouille la version SDK stable compatible ; ne change pas le BOM Spring arbitrairement. Le service S3 seul n'implémente aucune autorisation métier.

Flux : intention bornée → upload autorisé vers clé serveur → vérification de complétude/taille/hash → quarantaine → scan → disponibilité si CLEAN. Choisis et intègre un scanner maintenu avec son image/version vérifiée. En panne de scan, conserver la quarantaine et alerter. Aucun objet privé public. Ne pas télécharger une URL arbitraire fournie par le client.

Contrôle parent métier, rôle, compte/site/affectation à la création ET au téléchargement. MIME réel, limites, quotas, purge des uploads abandonnés et métadonnées photo. Évalue l'usage d'URL présignées courtes contre proxy si révocation immédiate exigée ; documente la fenêtre de validité résiduelle.

Recette : mauvais compte, technicien non affecté/réaffecté, faux MIME, taille excessive, upload partiel, scan négatif ou indisponible, rejeu de completion, clé étrangère au parent. Scanner et stockage ne doivent pas envoyer de données vers un service externe non prévu.


## Exigences transversales de cette révision

Respecte le compte user_id interne, la séparation session web/jetons opaques mobile et le statut métier courant. La connexion Google ne valide aucun dossier. Les données professionnelles et pièces restent dans notre application. Référence les exigences SEC-xx applicables à ce lot et joins les preuves réellement obtenues ; ne réintroduis pas d'architecture d'identité remplacée. N'affirme jamais « sécurisé » sur la seule présence de Spring Security.

## Compte rendu obligatoire

Indique le comportement livré, les fichiers/contrats modifiés, les vérifications réellement exécutées et leurs résultats, les limites ou tests non exécutés et la prochaine tranche. Une validation syntaxique n'est pas un test d'intégration. Aucun secret ni donnée personnelle réelle dans la réponse. Termine le travail réversible autorisé sans demander une confirmation routinière ; isole toute décision produit réellement bloquante.
