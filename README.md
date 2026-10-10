# Mathong

Mahjong Solitaire for Android, built with Kotlin, Jetpack Compose and Material 3.

## Features
- 22 layouts, from the 32-tile Pocket to the 144-tile Turtle and 140-tile Temple
- Guaranteed-solvable deals and shuffles
- Smart look-ahead hints, undo/redo, shuffle, auto-shuffle, auto-finish, matching-tile assist
- Daily Challenge (same deal for everyone each day) with streak calendar
- Combo scoring, time bonus, star ratings, per-layout best times
- Achievements and statistics with a pace chart
- Landscape layout with navigation rail
- Material You dynamic color, light/dark mode, tile styles, board backgrounds, corner labels
- Synthesized sound effects, haptics, pinch-to-zoom, auto-save/resume

## Building
```
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # signed release APK
./gradlew :app:testDebugUnitTest    # engine tests + screenshot rendering
./gradlew :app:recordPaparazziDebug # refresh screenshot snapshots
```

## Releases
`.github/workflows/build.yml` builds and tests every push. On the default branch, if the tag in
`version.properties` (`VERSION_TAG`) does not exist yet, the workflow creates that tag and a GitHub
release with the APKs attached. Bump `VERSION_CODE`, `VERSION_NAME` and `VERSION_TAG` to publish a new release.

### Signing
Release builds are signed with `keystore/mathong-release.jks` (bundled, so CI builds can be
installed over each other). For a private key, add the repository secrets `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`; the workflow uses them automatically.
