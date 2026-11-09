# Distraction Free

App Android qui coupe trois distractions, chacune avec ses propres réglages :

- **Snapchat · Spotlight** : bloque l'onglet Spotlight.
- **Instagram · Reels** : bloque l'onglet Reels et ouvre les DM à la place.
- **Instagram · DM au lancement** : ouvre directement les messages quand tu lances Instagram.

Service d'accessibilité, Kotlin, Jetpack Compose. Le service ne lit que Snapchat et Instagram, et rien ne quitte le téléphone.

## Installer

1. Le workflow GitHub **Build APK** compile l'app à chaque push sur `main` (onglet Actions, artefact `distraction-free-debug`).
2. Installe `app-debug.apk` sur le téléphone.
3. Ouvre l'app, appuie sur **Activer le service**, puis active *Distraction Free* dans les réglages d'accessibilité.
4. Si l'option est grisée (Android 13+, app installée hors Play Store) : Infos de l'app → ⋮ → **Autoriser les paramètres restreints**, puis recommence.

## Réglages

La liste d'accueil montre les trois fonctions. L'interrupteur active ou désactive chacune séparément, et toucher la ligne ouvre ses réglages.

**Spotlight et Reels**
- **Total** : toujours bloqué.
- **Règles** : budget quotidien, limite par session, pause après une limite de session, plage horaire.
- **Strict** : retour automatique. **Souple** : écran avec « Retour » et « Continuer N min ».
- Une session se termine après 2 minutes passées hors de la page.

**DM au lancement**
- Délai : ouvre les DM seulement si Instagram était fermé depuis au moins N minutes (0 = à chaque ouverture).
- Plage horaire optionnelle : actif seulement pendant cette période.

**Réglages liés** : une modification faite sur une fonction s'applique aussi aux autres. Chaque fonction garde sa propre activation.

## Si la détection ne marche pas

Snapchat et Instagram changent leur interface à chaque mise à jour. Active **Diagnostic** sur l'écran d'accueil, ouvre l'app concernée, puis copie l'arbre de vues affiché.
Cherche la ligne de l'onglet ou de l'icône (`[sel]` quand un onglet est actif) et ajuste les libellés dans
`service/SpotlightDetector.kt` ou `service/InstagramDetector.kt`.

## Structure

- `core/` : `RuleEngine`, `DmRedirect`, `UsageTracker`, modèles. Fonctions pures, testées en JVM.
- `data/` : réglages et usage persistés, par fonction.
- `service/` : `BlockerService`, détecteurs Snapchat et Instagram, écran de blocage.
- `ui/` : liste des fonctions, écran de réglages, thème clair/sombre suivant le système.
