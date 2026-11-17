# Démarrer avec ce pack

## 1. Extraire le ZIP
Décompresser WorldTech_Angular_Pack_v1.zip dans un emplacement de travail. Conserver le dossier worldtech-angular-pack-v1 entier, avec ses documents et ses prompts.

## 2. Ouvrir le vrai dépôt cible
Ouvrir le dossier du projet dans votre éditeur avec Codex ou votre agent de développement.
- Si un dépôt WorldTech existe, travailler dedans et préserver ses modifications.
- Si le dépôt est vide, FE00 le constate ; FE01 crée ensuite le socle.
- Le frontend peut se trouver dans un sous-dossier d'un monorepo. Son chemin exact est une décision de FE00.
- Ne pas lancer une création Angular dans la racine d'un backend existant.

Placer une copie de ce pack, par exemple dans docs/frontend-pack. Éviter toute fusion automatique du AGENTS.md fourni avec un fichier existant : l'agent doit lire les deux, rapprocher les règles et préserver les consignes du projet. Les sources tierces restent dans vendor ; aucune installation globale de skill n'est nécessaire pour utiliser leur documentation.

## 3. Exécuter FE00
Ouvrir [prompts/00_AUDIT_ET_DECISIONS.md](prompts/00_AUDIT_ET_DECISIONS.md), copier son texte complet et l'envoyer à l'agent ayant accès au dépôt. Le prompt sait demander à l'agent de retrouver ce pack par son nom si son chemin n'est pas indiqué.

FE00 doit produire une inspection du dépôt, une matrice de compatibilité et des décisions écrites. Il ne crée pas toute l'application et ne déclare pas les autres lots terminés.

## 4. Passer à FE01 puis FE02
FE01 construit le socle à partir des décisions réelles de FE00.
FE02 construit la structure visuelle, les composants et un écran de référence.
À la fin de chaque lot, examiner le bilan de l'agent et mettre à jour SUIVI_DES_LOTS.md avant d'envoyer le prompt suivant. La décision humaine concerne le passage au lot suivant ; les tâches nécessaires au lot courant sont déjà autorisées par son lancement.

## 5. Vérifier l'intégration
FE03 nécessite le contrat réel de session, CSRF et MFA du backend. Les autres fonctionnalités peuvent être préparées avec des réponses simulées isolées, mais leur intégration reste explicitement « à vérifier » tant que l'API réelle manque.

Un écran qui fonctionne avec des données de démonstration ne valide ni les permissions Spring Security ni les transactions de sélection.

## 6. Reprendre une session interrompue
Fournir à l'agent :
- le chemin du dépôt et du pack ;
- SUIVI_DES_LOTS.md ;
- le compte rendu du dernier lot ;
- le prompt du seul lot à poursuivre.

Les travaux déjà vérifiés sont conservés. Les tests sont rejoués lorsque les changements ou une incertitude le justifient.

## Résultat attendu à chaque lot
Un résultat concret dans le dépôt, les contrôles exécutés, leurs résultats exacts et les limites restantes. Un prérequis absent est identifié précisément ; l'agent avance sur les tâches indépendantes et ne remplace pas silencieusement le backend par un faux succès.

## Premier message prêt à envoyer
~~~text
Nous travaillons sur WorldTech. Le pack worldtech-angular-pack-v1 est joint ou présent dans le dépôt. Lis son AGENTS.md et DEMARRER_ICI.md, puis exécute uniquement prompts/00_AUDIT_ET_DECISIONS.md. Préserve le travail existant et termine par les résultats observés, les fichiers créés et les prérequis du lot FE01.
~~~

