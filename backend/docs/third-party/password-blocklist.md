# Liste locale de mots de passe courants

Source publique : SecLists `Passwords/Common-Credentials/10k-most-common.txt`, commit **913b327317496d062bcc7cace524aaad8a693be2** (08/09/2026), consulté le 01/11/2026.

- [Fichier source figé](https://github.com/danielmiessler/SecLists/blob/913b327317496d062bcc7cace524aaad8a693be2/Passwords/Common-Credentials/10k-most-common.txt).
- Copie brute : `src/main/resources/identity/common-passwords.txt`, SHA-256 `68782d6a4a19a4768d5f15dd66bd534e7a33055cc755411e33f16d18c50fdcce`.
- Licence MIT conservée : `SecLists-LICENSE.txt`, SHA-256 `3dbdc93d5f8829de0941744841730a09c106d0732e5ae0e98ca1d77be7ded66c`.

Ces chaînes sont des données publiques de contrôle, pas des comptes ou secrets du projet. Le contrôle local, insensible à la casse, s'applique à la création/changement du mot de passe, sans envoyer le mot de passe ou son empreinte à un service tiers. La liste est chargée une fois et ne remplace pas un inventaire exhaustif de tous les mots de passe compromis. Mise à jour à revoir explicitement et tester ; pas de téléchargement réseau pendant le login.

Politique validée par le responsable le 01/11/2026 : 15–128 points de code Unicode, espaces conservés, aucune troncature/normalisation silencieuse. Les séquences UTF-16 isolées sont rejetées pour éviter une conversion UTF-8 ambiguë.
