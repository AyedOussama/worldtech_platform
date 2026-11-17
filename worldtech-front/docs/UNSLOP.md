# Utiliser unslop avec WorldTech
Le dépôt heydamir/unslop est inclus dans vendor/unslop, sans historique Git. Licence et attribution conservées. Aucun script tiers n'est exécuté automatiquement par l'extraction du ZIP.
Auteur : Damir Sokolovsky (@heydamir). Licence : CC BY 4.0.
Source : https://github.com/heydamir/unslop

## Portée
Ce pack utilise les principes de relecture visuelle. Il ne modifie pas le contenu du dépôt tiers, n'installe pas de skill globalement et ne contient pas de rendu Angular déjà audité.
La direction WorldTech figure dans DESIGN_SYSTEM.md. Les règles du produit, du backend et de l'utilisateur sont conservées.

## Processus pendant FE02
1. Lire vendor/unslop/skills/unslop/SKILL.md et les références pertinentes.
2. Examiner le composant rendu ; employer des données synthétiques.
3. Relever des problèmes localisés : espacement, alignement, texte, hiérarchie, contraste ou interaction.
4. Rapprocher la direction proposée avec les actifs WorldTech réels et le périmètre du lot autorisé.
5. Corriger par passes : espacements, couleurs, icônes, rédaction, typographie, disposition, détails, visuels, mouvement.
6. Vérifier aussi formulaires, focus, erreurs, téléphone et deux thèmes.
7. Consigner les décisions utiles ; ne pas annoncer un audit de tout le produit à partir d'un seul écran.

Une refonte hors de la direction autorisée peut nécessiter une décision ; une correction réversible comprise dans le lot ne doit pas déclencher une nouvelle autorisation systématique.

## Particularités Angular
Les exemples du dépôt sont des références visuelles. Ne pas recopier une implémentation React/Tailwind dans Angular ni remplacer les composants métier par une image.
Utiliser les tokens, le theming et les points d'extension PrimeNG réellement supportés. Conserver l'accessibilité des composants lors de la personnalisation.
Le matériel de Story Video et les scripts tiers sont disponibles comme sources, mais aucune vidéo ou image payante n'est nécessaire au pack.

## Copie locale optionnelle
Si l'utilisateur souhaite plus tard installer unslop comme skill d'un outil donné, suivre le workflow d'installation de cet outil. L'inclusion dans une archive documentaire n'effectue pas cette installation.

