# projet_mescontact_v3 — Gestion de contacts (TP Android V3)

Évolution du TP V2 « MesContacts » : **une seule activité + trois fragments dynamiques**,
une **RecyclerView filtrable** et l'affichage de la **position GPS** des contacts sur **Google Maps**.
Kotlin • interfaces XML • AndroidX Fragment / RecyclerView / ViewModel • Maps SDK for Android.

## Fonctionnalités
| V2 conservées | Nouveautés V3 |
|---|---|
| Liste et fiche détaillée (nom, prénom, téléphone, e-mail, photo) | `RecyclerView` + `ListAdapter`/`DiffUtil` à la place de la `ListView` |
| Modification + enregistrement, bouton Annuler | Recherche instantanée (nom, prénom, téléphone), insensible à la casse et aux accents |
| Appeler (`ACTION_DIAL`), e-mail (`ACTION_SENDTO mailto:`) | Message « Aucun contact ne correspond à … » et compteur de résultats |
| Photo depuis la galerie (Photo Picker) ou la caméra (`TakePicture` + `FileProvider`) | Latitude / longitude dans le modèle, saisie validée (−90..90, −180..180) |
| Validation des champs, gestion des annulations / applis absentes | Bouton « Voir sur la carte » → carte centrée + marqueur au nom du contact |

## Architecture
```
MainActivity  (FragmentContainerView @id/fragment_container)
 ├─ ListeContactsFragment   liste + champ de recherche          (écran racine)
 ├─ DetailContactFragment   consultation / modification / actions   replace + addToBackStack
 └─ CarteContactFragment    SupportMapFragment (fragment enfant)    replace + addToBackStack

data/ContactsViewModel  ← partagé par les 3 fragments (activityViewModels())
data/Contact            modèle (id, nom, prénom, téléphone, e-mail, photoPath, latitude?, longitude?)
util/ContactFilter      logique de filtrage (testée)
util/CoordinateValidator validation des coordonnées (testée)
util/ImageUtils         import / réduction / orientation EXIF des photos
```

**Navigation.** `MainActivity.openDetail(id)` et `openMap(...)` font
`supportFragmentManager.commit { setReorderingAllowed(true); replace(...); addToBackStack(...) }`.
Retour système et flèche de la barre d'outils dépilent la pile. Dans le détail, Retour = Annuler.

**Passage des données.**
- Liste → Détail : seul **l'identifiant** du contact est passé dans un `Bundle` (`newInstance(contactId)`).
- Détail → Carte : `Bundle` avec l'id, le nom et les coordonnées **saisies** (on peut tester une
  position avant de l'enregistrer).
- Détail → Liste : **ViewModel partagé**. `DetailContactFragment` appelle `viewModel.save(contact)` ;
  `ListeContactsFragment` observe `filteredContacts` (LiveData) et se met à jour immédiatement.
  *Justification :* la liste est une donnée partagée par plusieurs écrans et doit survivre à la
  rotation ; un ViewModel à la portée de l'activité est la source unique de vérité. La Fragment
  Result API conviendrait pour un résultat ponctuel, mais il faudrait alors dupliquer la liste
  dans chaque fragment.

**Rotation.** La liste et le filtre vivent dans le ViewModel. `MainActivity` n'ajoute le fragment
liste que si `savedInstanceState == null` (pas de doublon). Le détail garde la saisie en cours
(état des vues + `onSaveInstanceState` pour la photo temporaire) ; la carte n'ajoute son
`SupportMapFragment` qu'une fois.

## Clé Google Maps (jamais publiée)
1. Google Cloud Console → créer un projet → activer **Maps SDK for Android** → créer une clé API.
   Restreindre la clé : *Applications Android*, package `com.example.projet_mescontact_v3` + empreinte SHA-1
   (Android Studio : *Gradle → app → Tasks → android → signingReport*).
2. Ajouter dans **`local.properties`** (déjà ignoré par Git) :
   ```properties
   MAPS_API_KEY=AIza...votre_cle
   ```
3. `app/build.gradle.kts` lit la clé et l'injecte dans le manifeste (`${MAPS_API_KEY}`).
   Sans clé, l'application fonctionne et l'écran carte affiche « Clé API Google Maps non configurée ».

Voir `local.properties.example`. Ne jamais committer `local.properties`.

## Lancer le projet
1. Ouvrir le dossier `projet_mescontact_v3` dans Android Studio et synchroniser Gradle.
2. Ajouter la clé Maps (ci-dessus).
3. Lancer sur un émulateur **avec Google Play** (image « Google Play » / « Google APIs ») ou un téléphone.
4. Tests unitaires : clic droit sur `app/src/test` → *Run Tests* (filtrage + validation GPS).

## Données de démonstration
6 contacts en mémoire : Tunis, Sfax, Sousse, Paris, centre de la Tunisie, et **Karim Jlassi sans
position** (pour tester le message « coordonnées absentes »).

## Plan de tests (à illustrer par des captures dans `captures/`)
| # | Test | Résultat attendu |
|---|---|---|
| 1 | Taper « tRaBeLsI », « amira », « 20123 », « 22 987 » | Le bon contact, quelle que soit la casse |
| 2 | Taper « zzz » | Message « Aucun contact ne correspond à « zzz » » |
| 3 | Effacer (croix du champ) | Tous les contacts réapparaissent |
| 4 | Filtrer « gharbi » puis toucher la 1re ligne | La fiche de Mariem Gharbi s'ouvre (pas celle du 1er contact) |
| 5 | Modifier nom, téléphone, e-mail, photo, coordonnées → Enregistrer | Liste actualisée immédiatement |
| 6 | Modifier puis Annuler / Retour | Anciennes valeurs conservées |
| 7 | Vider le nom, e-mail « abc », latitude « 95 », longitude « x » | Erreurs affichées sous chaque champ |
| 8 | Appeler / E-mail / Galerie / Caméra, puis annuler chaque action | Toast explicite, pas de plantage |
| 9 | « Voir sur la carte » pour Amira Ben Salah | Carte centrée sur Tunis, marqueur « Amira Ben Salah » |
| 10 | « Voir sur la carte » pour Karim Jlassi | Message « n'a pas de position GPS » |
| 11 | Retour depuis la carte | Retour au détail avec la saisie intacte |
| 12 | Rotation sur chaque écran | Pas de doublon, filtre et saisie conservés |

## Questions de compréhension
1. **Activity vs Fragment.** Une Activity est un composant système déclaré dans le manifeste,
   point d'entrée avec sa propre fenêtre. Un Fragment est une portion réutilisable d'interface,
   avec son propre cycle de vie (et celui de sa vue), qui vit toujours **dans** une activité et
   est géré par son `FragmentManager`.
2. **FragmentContainerView et addToBackStack().** `FragmentContainerView` est le conteneur conçu
   pour les fragments (gère correctement animations et ordre d'affichage, contrairement à un
   `FrameLayout`). `addToBackStack()` enregistre la transaction dans la pile : le bouton Retour
   l'annule et réaffiche le fragment précédent au lieu de quitter l'activité.
3. **Adapter et ViewHolder.** L'Adapter fait le lien entre les données et la RecyclerView : il crée
   les vues de ligne (`onCreateViewHolder`) et les remplit (`onBindViewHolder`). Le ViewHolder
   garde les références des vues d'une ligne ; il est **recyclé** quand la ligne sort de l'écran,
   ce qui évite d'inflater et de chercher les vues à chaque défilement.
4. **Éviter une erreur d'indice après filtrage.** Ne jamais relier la position cliquée à la liste
   complète : on lit l'élément dans la liste **affichée** (`getItem(bindingAdapterPosition)`), on
   prend son **id**, et l'enregistrement remplace le contact par recherche d'id
   (`indexOfFirst { it.id == contact.id }`).
5. **Pourquoi l'id plutôt que la position.** La position change avec le filtre, le tri, les ajouts
   ou suppressions ; l'id est stable et unique. Il est aussi léger à mettre dans un `Bundle`,
   survit à la recréation du fragment, et le détail relit toujours la version à jour du contact
   dans le ViewModel.
6. **Ne pas exposer la clé Maps.** La clé est dans `local.properties` (listé dans `.gitignore`),
   injectée à la compilation via `manifestPlaceholders` ; le dépôt ne contient qu'un exemple
   (`local.properties.example`). En plus, la clé est **restreinte** dans Google Cloud (package
   Android + SHA-1, API Maps SDK uniquement). Si une clé a été publiée par erreur : la révoquer
   et en générer une nouvelle (la supprimer de l'historique ne suffit pas).

## Auteur
Triki Mohamed Amine – L3MDW2
