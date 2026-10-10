## Mathong v1.7 — Mahjong Solitaire

### Material tiles (new default)
- Flat, **two-tone Material 3 tiles** whose colors come from your theme — including Material You wallpaper colors
- Suit colors (red, green, blue…) are harmonized toward your theme's primary color, so they always match
- New Material iconography: two-tone dots and bamboo, a sprout for the 1 of bamboo, compass winds, rounded dragon tiles, flower / sun / leaf / snowflake bonus tiles, pill badges
- Blocked tiles fade out the Material way instead of being darkened
- Traditional and Simple faces are still available in Settings → Tile faces
- Matching two-tone launcher icon and theme-colored confetti

### Natural sounds
- All sounds rebuilt with modal synthesis: real-sounding ceramic tile ticks and clacks, a wooden knock for blocked tiles, tiles sliding when shuffling
- Soft marimba notes on each match that climb a pentatonic scale as your combo grows, and a gentle marimba arpeggio when you win

### From v1.6
### Snappier tiles
- Tiles sink the instant your finger touches them, so every tap has immediate feedback
- Tiles are drawn from a hardware cache (one recorded layer per tile) — much less work per frame
- The board no longer redraws on every timer tick, and the hint pulse only animates while a hint is showing
- Near-miss taps on small tiles pick the closest tile
- (v1.4 already removed the ~300 ms double-tap delay on every tap)
- New *Fast tile rendering* switch in Settings → Gameplay in case the cache misbehaves on your device

### From v1.5
- **Share layouts** — send any custom layout as a short `MATHONG1:` code (Editor → ⋮ → Share layout code)
- **Import layouts** — paste a code in *Choose a layout* (download icon); clipboard codes are detected automatically
- **Backup & restore** — save statistics, achievements and custom layouts to a file and restore them on a new phone (Settings → Data)

### From v1.4
- **Daily reminder** — optional notification at a time you choose, only if you haven't played today's challenge; tapping it opens the daily straight away
- **Tile inspector** — long-press any tile to see it enlarged with its name, whether it's free, and how many matches remain
- **Reset zoom** button appears while zoomed in

### Fixes
- Taps on tiles respond instantly (removed the double-tap-to-zoom delay)
- A game in progress is no longer replaced with a new deal if Android closes the app in the background

### Previously
- v1.3: interactive tutorial, simple tile faces, deal animation, TalkBack support
- v1.2: layout editor, Time Attack and Zen modes, result sharing
- v1.1: 22 layouts, auto-finish, match animation, landscape mode, pace chart
- v1: solvable deals, smart hints, daily challenge, achievements, Material You theming

Install `Mathong-<tag>.apk` (release build). It upgrades in place over earlier releases.
