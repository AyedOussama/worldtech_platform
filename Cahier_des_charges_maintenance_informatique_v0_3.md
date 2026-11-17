# Cahier des charges pour la maintenance informatique et les interventions 

_Portail client et backoffice avec application mobile technicien_ 

Version 0.3  •  27 octobre 2026  •  Trois rôles et stack retenue pour une équipe de trois personnes 

Nous proposons un outil de maintenance informatique permettant aux clients de déclarer un incident, à l’entreprise de qualifier et d’affecter le travail, et aux techniciens de réaliser puis documenter leurs interventions. Les contrats, sites et équipements donnent le contexte nécessaire au traitement des demandes. 

Le produit associe la gestion des demandes et la gestion des interventions sur le terrain. Sa réussite dépend de la fiabilité du suivi, des délais contractuels, de la qualité des rapports et de la séparation des données de chaque client. 

### **1 Périmètre et décisions proposées** 

|**Sujet**|**Proposition de cadrage**|
|---|---|
|Acteurs|Entreprise de maintenance, clients entreprises ou particuliers et techniciens.|
|Client|Invitation, dossier rempli par le client, validation administrative et portail web<br>responsive.|
|Technicien|Application Android et iOS pour rechercher des missions proches, candidater et<br>réaliser les interventions attribuées.|
|Administrateur|Backoffice web pour clients, parc, contrats, affectations et suivi.|
|Admission|Validation des clients invités et des techniciens inscrits avant accès aux fonctions<br>opérationnelles.|
|Organisation|Entreprise basée à Paris, couverture nationale, candidatures puis sélection et<br>affectation par son administrateur.|



### **2 Stack retenue** 

Flutter est retenu pour le mobile technicien, Angular pour le portail client et le backoffice, Java avec Spring Boot pour l’API et PostgreSQL pour les données. L’équipe comprend trois personnes. Aucun mobile client n’est prévu au lancement ; le portail web doit être utilisable sur téléphone. 

L’administrateur représente l’entreprise et assume l’organisation, la qualification, la publication, la sélection et l’affectation. Aucun rôle métier séparé d’organisateur ou d’affecteur n’est créé. Les comparaisons techniques suivantes expliquent les compromis ; les versions et intégrations critiques restent à valider. 

Le budget doit être réévalué après le cadrage du portail client, du hors connexion, des contrats et des délais. L’estimation de la version précédente ne constitue pas un engagement pour ce périmètre. 

Maintenance informatique  |  Version 0.3  |  1 

## **3 Acteurs et canaux de service** 

|**Rôle**|**Accès et actions**|
|---|---|
|Administrateur|Représente l’entreprise, invite et valide les clients, valide les techniciens, gère les<br>contrats, qualifie, publie, sélectionne, affecte et contrôle.|
|Client|Complète son dossier depuis une invitation puis, après validation, déclare et suit les<br>incidents de ses sites et consulte ses rapports.|
|Technicien|Soumet son dossier, consulte les missions autorisées proches de lui, candidate et<br>réalise ses interventions attribuées.|



### **Un compte client peut avoir plusieurs utilisateurs** 

Une entreprise cliente peut posséder plusieurs sites et contacts. Ces contacts restent des utilisateurs du rôle Client, avec des périmètres de sites distincts si nécessaire. Un particulier possède un compte simplifié. Plusieurs personnes de votre entreprise peuvent disposer du rôle Administrateur ; chacune utilise un compte nominatif et ses actions sont tracées. 

Un client ne doit jamais voir les tickets, documents, coordonnées ou équipements d’un autre client. Cette isolation doit exister dans l’API, les recherches, les téléchargements et les exports ; masquer un menu ne suffit pas. 

### **Invitation et validation du client** 

L’administrateur prépare une invitation liée à un destinataire. Le client ouvre le lien, confirme son email, renseigne ses coordonnées et soumet son dossier. L’administrateur accepte, demande un complément ou refuse. Avant validation, l’utilisateur accède seulement à son inscription et à son suivi, sans créer de ticket opérationnel. 

Les états du dossier client sont invité, à compléter, en attente de validation, complément demandé, actif et refusé. Le lien utilise un jeton imprévisible, à durée limitée et à usage unique, lié au destinataire ; un renvoi peut invalider le précédent. La confirmation de l’email et la validation métier restent distinctes. Le client peut signer sur le téléphone du technicien. 

### **Admission des techniciens** 

Le technicien s’inscrit, renseigne ses compétences et sa zone de mobilité puis soumet son dossier. L’administrateur accepte, refuse ou demande un complément. Seuls les profils actifs et habilités accèdent aux missions éligibles. Les compétences déclarées et vérifiées restent distinctes ; une suspension bloque les nouvelles candidatures. 

Le statut du technicien et ses droits sont vérifiés côté serveur lors des actions sensibles. Les interventions déjà affectées à un technicien suspendu sont reprises par un administrateur, avec un historique de réaffectation. 

Maintenance informatique  |  Version 0.3  |  2 

## **4 Tickets et interventions et cycle de traitement** 

### **Deux objets métier distincts** 

Un ticket représente le besoin du client : une panne, une anomalie ou une demande de service. Une intervention représente une action planifiée pour traiter ce besoin. Un ticket peut être résolu à distance et ne générer aucun déplacement, ou nécessiter plusieurs passages et plusieurs techniciens. 

Exemple : le client signale un accès réseau indisponible. L’administrateur effectue un diagnostic à distance, programme une première visite, puis une seconde après réception d’une pièce. Le ticket reste unique et conserve l’ensemble des actions et rapports. 

|**Objet**|**Cycle proposé**|
|---|---|
|Ticket|Nouveau, qualifié, en cours, en attente, résolu puis clos. Annulation et réouverture<br>sont tracées.|
|Intervention|À préparer, publiée, à confirmer, planifiée, en cours puis terminée. Les refus,<br>expirations et annulations sont tracés.|
|Rapport|Brouillon, soumis, validé ou correction demandée ; les versions sont conservées.|
|Attestation client|À recueillir, recueillie, refusée ou signataire absent ; état distinct du résultat<br>technique.|



### **Règles de traitement** 

- Le client déclare un impact et une urgence. L’administrateur qualifie la priorité selon le contrat et les conséquences réelles. 

- Le ticket client est qualifié avant publication d’une mission ; sa déclaration ne diffuse pas automatiquement ses données au réseau. 

- Les techniciens actifs et éligibles peuvent candidater. L’administrateur sélectionne un profil, puis le technicien confirme selon le délai proposé. 

- Une seule candidature par technicien et mission et une seule affectation active sont autorisées ; les opérations concurrentes sont contrôlées en base. 

- La fin d’une intervention ne ferme pas automatiquement le ticket si le problème persiste ou si un autre passage est nécessaire. 

- Les changements d’état précisent l’auteur, la date, le motif et les éventuelles actions attendues. 

- Une demande de correction du rapport ne doit pas effacer les informations déjà soumises. 

### **Maintenance préventive** 

Une visite préventive peut être créée à partir d’un contrat et de sa périodicité sans attendre un incident client. Une intervention corrective référence son ticket ; une intervention préventive peut référencer directement le contrat et le site. Les séries récurrentes avancées restent un lot à préciser. 

La politique de clôture doit indiquer qui accepte la résolution, le délai de retour éventuel du client et les conditions de réouverture. Ces règles sont configurées selon le service retenu, pas déduites du seul bouton utilisé par le technicien. 

Maintenance informatique  |  Version 0.3  |  3 

## **5 Fonctions du portail client** 

Le portail doit permettre de signaler un problème et d’en comprendre le traitement sans imposer au client une expertise informatique. Il doit être utilisable depuis un ordinateur et un téléphone. 

|**Fonction**|**Besoin et règle de gestion**|
|---|---|
|Déclaration|Choisir le site, l’équipement si connu, la catégorie et décrire le problème.|
|Pièces jointes|Ajouter une photo ou une capture avec taille, format et accès contrôlés.|
|Impact et urgence|Expliquer les personnes ou services touchés ; la priorité finale est qualifiée par<br>l’entreprise.|
|Suivi|Afficher le statut, les prochaines étapes, les demandes de complément et les rendez-<br>vous.|
|Échanges|Répondre à l’administrateur sur le ticket ; distinguer les messages client des notes<br>internes.|
|Historique|Rechercher les incidents et consulter les rapports autorisés par site ou équipement.|
|Résolution|Confirmer le rétablissement ou signaler la persistance du problème selon le processus<br>convenu.|



### **Modèle minimal du parc** 

Chaque site contient une adresse, un fuseau, des contacts et des consignes d’accès. Chaque équipement peut posséder un identifiant interne, une catégorie, une marque, un modèle, un numéro de série, un emplacement et un état. Les champs réellement requis doivent être choisis selon les interventions pratiquées. 

Le client doit pouvoir déclarer un incident même si le matériel n’est pas encore référencé. Une saisie libre permet de créer le ticket ; l’administrateur complète ensuite le parc. Une donnée manquante ne doit pas empêcher de signaler une panne urgente. 

### **Accès aux informations** 

Le portail expose une chronologie lisible, sans afficher les discussions internes, les tarifs d’achat ou les documents réservés aux techniciens. Les utilisateurs d’un même client ne voient que les sites et périmètres qui leur sont accordés. 

Les notifications contiennent un résumé et un lien vers le ticket. L’ouverture du lien exige un accès autorisé et recharge le statut courant. Une notification ancienne ne doit pas permettre d’agir sur un ticket déjà annulé ou une intervention réaffectée. 

### **Points à confirmer** 

Déterminer si les clients consultent leur contrat et leur parc en lecture seule ou s’ils peuvent les modifier. La déclaration par email, les clients sans contrat et les demandes hors périmètre contractuel doivent faire l’objet de règles explicites. 

Maintenance informatique  |  Version 0.3  |  4 

## **6 Fonctions mobiles du technicien** 

|**Fonction**|**Comportement attendu**|
|---|---|
|Missions et planning|Explorer les missions autorisées en liste et sur carte, candidater, suivre les décisions et<br>consulter ses interventions attribuées.|
|Préparation|Accéder aux consignes, au matériel concerné, aux pièces utiles et au contact autorisé.|
|Déplacement|Ouvrir la navigation ; la carte complète la liste et le planning.|
|Exécution|Renseigner les actions, tests, temps passés, difficultés et éléments restant à traiter.|
|Preuves|Ajouter des photos et commentaires dans les limites autorisées.|
|Attestation|Présenter le compte rendu au signataire client ou indiquer le motif de son absence ou<br>refus.|
|Fin de visite|Terminer l’intervention et transmettre un rapport ; le ticket suit sa règle de résolution<br>propre.|



### **Hors connexion** 

Le technicien doit pouvoir retrouver les interventions nécessaires déjà téléchargées et préparer un compte rendu sans réseau. La base locale conserve des données limitées, avec une protection et une purge adaptées. Les photos et opérations en attente portent un indicateur visible de synchronisation. 

La synchronisation utilise des identifiants stables et des opérations idempotentes. Après une fermeture de l’application ou une perte de réseau, la reprise ne doit pas créer de rapport ou de pièce en double. Une intervention réaffectée ou annulée pendant la déconnexion produit un conflit explicite à traiter, sans écrasement silencieux. 

Les heures de réalisation déclarées, de capture locale et de réception serveur restent distinctes. Les opérations possibles hors connexion sont définies une par une ; Flutter ou React Native ne fournit pas automatiquement ces règles. Flutter documente plusieurs stratégies de lecture, écriture et synchronisation hors connexion. [S1] 

### **Compte rendu et attestation** 

Le rapport contient le ticket et la visite concernés, les opérations réalisées, les résultats, les réserves et les pièces retenues. L’attestation du client est rattachée à une version précise du rapport avec le nom du signataire, sa qualité et la date. Une correction après attestation produit une nouvelle version et déclenche la procédure d’acceptation prévue. 

Le niveau de preuve attendu doit être choisi avant de sélectionner un composant de signature. La capture d’un tracé au doigt ne vérifie pas à elle seule l’identité d’un signataire. Le rapport final peut être généré côté serveur après synchronisation et conservé avec son historique. 

Maintenance informatique  |  Version 0.3  |  5 

## **7 Administration et contrats de maintenance** 

Le backoffice centralise les clients, les sites, le parc, les contrats, les tickets, les interventions et les rapports. Le tableau de bord doit rendre visibles les incidents sans responsable, les délais à risque, les conflits de planning et les rapports en attente. 

### **Contrat et couverture** 

|**Donnée**|**Décision à formaliser**|
|---|---|
|Validité|Client, dates de début et de fin et règles de renouvellement.|
|Couverture|Sites, équipements, prestations et exclusions concernées.|
|Horaires|Jours et heures de service, fuseau et calendrier applicable.|
|Engagements|Délais de prise en charge, de rétablissement ou de résolution selon le contrat.|
|Attentes|Motifs d’attente et effet éventuel sur chaque compteur de délai.|
|Consommation|Visites ou heures incluses si le contrat prévoit un quota.|
|Hors contrat|Règle de devis ou d’autorisation avant facturation supplémentaire.|



### **Calcul des délais** 

Les engagements de niveau de service, souvent appelés SLA, ne se réduisent pas à une date limite. Le calcul peut dépendre des heures ouvrées, des jours fériés, de la priorité et des pauses autorisées. Un changement de statut « en attente » ne suspend pas tous les engagements automatiquement. 

Le contrat et les règles applicables au ticket sont versionnés. Les corrections administratives ne doivent pas modifier rétroactivement les mesures historiques sans justification. Un moteur de calcul simple, explicite et testé est préférable à des conditions dispersées dans les écrans. 

### **Affectation et déplacements** 

L’administrateur publie une mission après qualification, examine les candidatures et sélectionne le technicien selon les compétences, disponibilités et conditions d’intervention. La sélection réserve la place jusqu’à confirmation ; un refus ou une expiration permet une nouvelle sélection. Une affectation directe peut également être prévue avec confirmation du technicien. 

Les techniciens recherchent les missions proches d’une ville saisie, de leur point de départ ou de leur position ponctuelle. Le rayon, les dates et les compétences filtrent la liste et la carte. Avant affectation confirmée, seuls une zone approximative et les informations nécessaires à la candidature sont diffusés ; le contact client, l’adresse exacte et les pièces privées restent protégés. Les distances affichées sont indicatives. 

### **Indicateurs** 

Suivre les délais de prise en charge et de résolution, le respect des engagements, les tickets réouverts, les passages multiples et les interventions non réalisées. Les temps en attente et les annulations doivent être distingués pour interpréter les résultats. 

Maintenance informatique  |  Version 0.3  |  6 

## **8 Flutter pour les techniciens** 

Flutter et React Native peuvent couvrir les formulaires, cartes, photos, notifications et rapports de ce produit. Aucun résultat de performance comparatif n’a été mesuré sur ses écrans. Les conclusions cidessous sont des choix d’architecture et d’organisation, pas un benchmark. 

|**Critère**|**Flutter**|**React Native avec Expo**|
|---|---|---|
|Approche<br>interface|Widgets et moteur de rendu Flutter ;<br>animation intégrée au framework.|Composants natifs et écosystème React ;<br>gestes et animations via les outils retenus.|
|Compétences|Dart et maîtrise du cycle de vie mobile.|React et TypeScript ; maîtrise native toujours<br>nécessaire pour certaines intégrations.|
|Hors connexion|Base locale et synchronisation à concevoir.|Base locale et synchronisation à concevoir.|
|Bibliothèques|Contrôler plugins et compatibilité Android et<br>iOS.|Contrôler modules natifs, compatibilité React<br>Native et matrice Expo.|
|Maintenance|Canal stable et migrations planifiées.|Versions compatibles et migrations<br>planifiées.|
|Choix proposé|Référence si la capacité Flutter est<br>disponible.|Référence si la maîtrise React Native réduit<br>le risque réel du projet.|



### **Pourquoi envisager Flutter** 

Son ensemble de widgets et d’animations constitue un socle cohérent pour le planning, les formulaires d’intervention, les états hors connexion et les comptes rendus. La compilation mobile en code machine et le moteur de rendu sont décrits par la documentation officielle. [S2] Ces caractéristiques ne prouvent pas que chaque écran sera plus rapide qu’un écran React Native équivalent. 

### **Pourquoi conserver React Native dans la comparaison** 

Une équipe déjà productive en React et TypeScript peut réutiliser des pratiques et une partie des contrats de données. La nouvelle architecture React Native remplace l’ancien pont asynchrone par JSI ; une comparaison fondée uniquement sur les limites de l’ancien pont serait incomplète. [S3] 

### **Décision proposée** 

Flutter est retenu pour le mobile technicien. Le prototype doit couvrir la carte de missions, la candidature, les photos, le rapport et la reprise hors connexion. Le motion design utilise des interactions courtes et respecte les préférences d’accessibilité. Les comparaisons précédentes ne constituent pas une validation de chaque plugin. 

Le choix de bibliothèque cartographique reste séparé de celui du fournisseur, de ses conditions et de son coût. Google propose une documentation officielle pour son package Flutter. [S4] Les essais portent sur des appareils réels en version de production ou de profilage. 

Maintenance informatique  |  Version 0.3  |  7 

## **9 Angular pour les interfaces web** 

Le portail et le backoffice comportent des formulaires, des recherches, des droits, des tableaux et des parcours de traitement. Le référencement public ne constitue pas un besoin central de ces espaces authentifiés. 

### **Comparaison des interfaces web** 

|**Point étudié**|**Socle Angular**|**Socle React**|
|---|---|---|
|Organisation|Framework avec conventions et outils<br>intégrés.|Bibliothèque permettant de composer un<br>socle adapté.|
|Formulaires|Outils de formulaires réactifs fournis par<br>Angular.|Choisir et standardiser les composants et la<br>gestion des formulaires.|
|Équipe|Conventions utiles pour un développement<br>partagé.|Très cohérent si le socle et les compétences<br>existent déjà.|
|Mises à jour|Politique publiée et outils de migration.|Suivi du cœur et des bibliothèques retenues.|
|Usage pour ce<br>produit|Bon choix pour un portail et un backoffice<br>métier durable.|Bon choix avec une architecture et une<br>sélection de dépendances disciplinées.|



### **Proposition Angular** 

Angular est retenu pour les espaces web client et administrateur. Les formulaires réactifs sont documentés et testables. [S5] Angular Material et CDK constituent un socle possible pour les composants et l’accessibilité ; un agenda métier avancé doit être évalué séparément. 

La politique de support publiée par Angular et les mécanismes de migration permettent de planifier la maintenance. La durée de support doit être vérifiée pour la version choisie ; une application supportée suppose des mises à jour régulières. [S6] 

### **Compromis** 

Angular implique une maîtrise de ses conventions, de son système de composants et des mécanismes réactifs utiles. L’ajouter à une équipe qui ne le connaît pas a un coût de formation et de revue. React reste un choix professionnel et durable si les pratiques, les dépendances et les tests sont maîtrisés. 

Il n’est pas nécessaire d’ajouter un magasin d’état global complexe dès le départ. Les données métier restent dans l’API ; l’état des formulaires et de navigation est géré au niveau approprié. Le portail client et le backoffice peuvent partager des composants sans partager leurs autorisations. 

### **Critère de décision** 

Comparer un formulaire réel de déclaration et un écran de qualification avec les compétences disponibles. La rapidité du développeur, la compréhension du code par une autre personne et le coût des dépendances importent davantage qu’une affirmation générale selon laquelle un framework serait toujours plus rapide. 

Maintenance informatique  |  Version 0.3  |  8 

## **10 Spring Boot pour les services métier** 

|**Critère**|**Java avec Spring Boot**|**TypeScript avec NestJS**|
|---|---|---|
|Modélisation|Écosystème Spring pour services, sécurité,<br>données et transactions.|Modules et services structurés ; intégrations<br>de données et sécurité à choisir.|
|Bibliothèques|Gestion cohérente de nombreuses versions<br>via les dépendances gérées de Boot.|Socle modulaire ; contrôler les versions et<br>compatibilités des intégrations retenues.|
|Compétences|Java et écosystème Spring à maîtriser.|Continuité TypeScript avec une équipe React<br>ou Angular.|
|Capacité|Adapté aux API et règles métier de ce projet.|Adapté aux API et règles métier de ce projet.|
|Proposition|Référence à compétences comparables pour<br>le cœur métier durable.|Alternative forte pour une équipe experte<br>TypeScript et Node.js.|



### **Pourquoi proposer Spring Boot** 

Les contrats, droits par client, historiques et transactions justifient un socle métier structuré. Spring Boot gère un ensemble de versions de dépendances et fournit des fonctions d’exploitation ; Spring Framework documente sa gestion déclarative des transactions. [S7 et S8] Cela réduit certains choix d’intégration, sans rendre automatiquement le code correct ou sécurisé. 

NestJS propose aussi une structure modulaire, des mécanismes d’autorisation et des outils de test. [S9] La langue Java ne rend pas à elle seule l’application plus professionnelle, et aucun besoin décrit ne dépasse par nature les capacités de NestJS. Les performances doivent être mesurées avec les requêtes, médias et volumes réels. 

### **Architecture de référence** 

- Monolithe modulaire Spring Boot : clients et parc, contrats, tickets, interventions, rapports, notifications et audit. 

- PostgreSQL pour les données ; PostGIS si la recherche par proximité est retenue. 

- Stockage objet privé pour photos et rapports, avec autorisation et liens temporaires. 

- Traitements différés pour les rapports et notifications ; suivi des reprises et des erreurs. 

- API REST documentée et contrats de données versionnés ; clients Dart et TypeScript générables depuis OpenAPI. 

Retenir un JDK LTS maintenu, une ligne Spring Boot encore supportée, Maven, Spring Security, Spring Data et des migrations versionnées. Le choix exact des versions et bibliothèques est figé après un test de compatibilité. Un catalogue documentaire de versions stables ne constitue pas, à lui seul, une garantie de support actif. 

Maintenance informatique  |  Version 0.3  |  9 

## **11 Stabilité des dépendances et validation technique** 

Une stack stable résulte du choix des versions, des composants et de leur maintenance. La popularité d’un framework ne garantit pas la pérennité d’un plugin particulier. 

|**Contrôle**|**Critère de sélection**|
|---|---|
|Maintenance|Éditeur ou mainteneur identifié, versions récentes et traitement des problèmes<br>bloquants.|
|Compatibilité|Versions supportées des SDK, systèmes mobiles, runtime et bibliothèques principales.|
|Licence|Conditions compatibles avec le produit et coûts éventuels connus avant intégration.|
|Fonctions critiques|Preuve sur appareil réel pour carte, caméra, fichiers, stockage local et attestation.|
|Sécurité|Suivi des vulnérabilités et limitation des données collectées par les composants.|
|Réversibilité|Accès aux données exportables et règles métier hors des composants fournisseurs.|



### **Politique de mise à jour** 

Utiliser des versions de production supportées, des fichiers de verrouillage et une intégration continue. Planifier les mises à jour courantes et traiter les correctifs de sécurité selon leur risque. La documentation Flutter recommande le canal stable pour la production ; React Native publie aussi une politique de support et un calendrier de versions. [S10 et S11] Ces engagements ne remplacent pas la vérification des plugins. 

### **Prototype de validation à réaliser avant engagement** 

- Télécharger une intervention et l’ouvrir sans réseau après redémarrage de l’application. 

- Ajouter des photos et un rapport, interrompre l’envoi puis reprendre sans doublon. 

- Recueillir une attestation rattachée à une version et produire un rapport cohérent après synchronisation. 

- Simuler une réaffectation pendant la déconnexion et présenter le conflit au technicien. 

- Vérifier une notification, un lien vers une mission et l’interdiction d’accès entre deux clients. 

Mesurer le démarrage, le défilement, les interactions de la carte, la mémoire et les temps API sur du matériel cible. Pour la base : pagination, index et absence de requêtes répétées inutiles. Les seuils de charge et de performance sont fixés une fois les volumes connus. 

Les secrets clients et codes d’accès ne doivent pas être diffusés dans les notifications, journaux ou rapports non autorisés. Le hors connexion nécessite une politique de conservation locale, une protection adaptée et une gestion des données après révocation du compte. 

Maintenance informatique  |  Version 0.3  |  10 

## **12 Livraison et décisions de cadrage** 

### **Premier parcours à livrer** 

Inviter et valider un client ; lui permettre de déclarer un ticket ; qualifier puis publier une mission ; recevoir les candidatures ; sélectionner et confirmer un technicien ; réaliser et attester la visite ; soumettre le rapport et traiter la résolution du ticket. Ce parcours complet sert de base à la recette et au pilote. 

|**Socle de première version**|**Lots à préciser ou étendre**|
|---|---|
|Clients invités et validés et accès par site|Application mobile client éventuelle après la première<br>version.|
|Parc simple et pièces jointes|Import massif et inventaire automatique du parc.|
|Tickets et missions publiées et candidatures|Optimisation des tournées et interventions complexes.|
|Rapports et attestation|Signature avec un niveau de preuve renforcé.|
|Contrats et engagements simples validés|Moteur contractuel complexe, quotas et facturation<br>avancée.|
|Hors connexion ciblé|Synchronisation étendue à toutes les opérations.|



### **Critères de recette prioritaires** 

- Un client non validé ne crée pas de ticket ; un lien expiré ou déjà utilisé ne réactive pas une invitation. 

- Un client ne lit aucun ticket ni fichier d’un autre client, y compris via une URL directe. 

- Une double candidature ou deux sélections simultanées ne créent ni doublon ni double affectation. 

- Deux visites peuvent traiter un même ticket sans dupliquer le besoin initial. 

- Une intervention terminée peut laisser le ticket ouvert si une autre action est nécessaire. 

- Les délais respectent le calendrier et les pauses prévus par le contrat testé. 

- La reprise hors connexion ne crée pas de doublon et ne masque pas une réaffectation. 

- Le rapport attesté est identifiable ; une correction conserve la version précédente. 

### **Équipe et décisions restantes** 

L’équipe de trois personnes peut répartir les responsabilités entre mobile Flutter, web Angular et API Spring Boot. Les contrats OpenAPI, la modélisation et la recette des parcours complets sont partagés. Cette répartition est à adapter aux compétences réelles. 

Restent à préciser les justificatifs, le statut des techniciens, la rémunération et les frais affichés avant candidature, le délai de confirmation, les contrats et les volumes. Fournir des exemples de rapports et d’incidents, les contraintes de réseau, le budget et la date cible. 

À partir de ces réponses, établir le backlog, les maquettes des parcours clés, la matrice de droits et une estimation révisée. La stratégie technique doit rester compatible avec la capacité réelle de l’équipe à exploiter et maintenir le produit. 

Maintenance informatique  |  Version 0.3  |  11 

## **13 Sources techniques officielles** 

Documentation consultée le 27 octobre 2026. Les comparaisons et choix proposés relèvent de l’analyse d’architecture ; ils ne constituent pas des résultats de benchmark. 

[S1] Flutter — Fonctionnement hors connexion https://docs.flutter.dev/app-architecture/design-patterns/offline-first 

[S2] Flutter — Architecture du framework https://docs.flutter.dev/resources/architectural-overview 

[S3] React Native — Nouvelle architecture et JSI https://reactnative.dev/architecture/landing-page 

[S4] Google — Package Maps pour Flutter https://developers.google.com/maps/flutter-package/overview 

[S5] Angular — Formulaires réactifs https://angular.dev/guide/forms/reactive-forms 

[S6] Angular — Versions et politique de support https://angular.dev/reference/releases 

[S7] Spring Boot — Gestion des dépendances et systèmes de build https://docs.spring.io/spring-boot/reference/using/build-systems.html 

[S8] Spring Framework — Transactions déclaratives https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html 

[S9] NestJS — Documentation et modules https://docs.nestjs.com/ 

[S10] Flutter — Mise à jour et canal stable https://docs.flutter.dev/install/upgrade 

[S11] React Native — Calendrier et politique de support https://reactnative.dev/releases/ 

Les versions exactes, les fournisseurs et les licences des intégrations doivent être vérifiés au moment de leur sélection. Les bibliothèques tierces de planning, de signature et de stockage local ne sont pas réputées validées par la seule présence du framework dans ce document. 

Maintenance informatique  |  Version 0.3  |  12 

