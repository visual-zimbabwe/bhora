# Bhora

**Bhora** is a minimalist native Android application (Kotlin + Jetpack Compose) that tracks live, upcoming, and today's finished **Premier League**, **La Liga**, and **UEFA Champions League** fixtures available on **Amazon Prime Video** and **no-add-on HBO Max / Max** in any country worldwide.

Built exclusively with free and open-source tools, styled after the **Bhora Matchday Canvas** design system ([DESIGN.md](DESIGN.md), [code.html](code.html), [screen.png](screen.png)).

---

## Features

- **Multi-Competition Tracking:** Covers English Premier League (`cid=6`), Spanish La Liga (`cid=8`), and UEFA Champions League (`cid=50`).
- **Global Base-Plan Coverage (No Paid Sports Add-Ons):**
  - **Amazon Prime Video:** Denmark, Sweden, Netherlands, Great Britain, Germany, Italy, San Marino, plus **CazéTV** in Brazil (carried inside Prime Video Brazil at no extra cost), and automatic daily discovery of newly added territories.
  - **HBO Max / Max:** Mexico, Brazil (UEFA Champions League), Belize, Costa Rica, Dominican Republic, El Salvador, Guatemala, Honduras, Nicaragua, and Panama.
  - Paid sports add-on channels (such as UK/Ireland HBO Max TNT Sports) and unrelated broadcasters containing "Max" or "Prime" are excluded.
- **Minimal Editorial Matchday UI:** Midnight navy (`#081423`) and gold (`#E9C349`) typographic canvas using bundled SIL Open Font License **EB Garamond** and **Manrope** fonts, with zero cards, shadows, icons, or rounded corners.
- **Filtering & Search:**
  - Status toggle: `ALL / LIVE ONLY`
  - Service toggle: `ALL / PRIME / MAX`
  - Borderless header search (`SEARCH CLUB`) matching club names or competitions (`EPL`, `LA LIGA`, `UCL`, `CHAMPIONS`).
- **Inline Broadcast Regions:** Tap any fixture row to smoothly expand or collapse the full list of countries and channels airing that match.
- **Live Local Timezone Sync:** Displays kickoff times in `HH:mm LOCAL` and listens to Android `ACTION_TIMEZONE_CHANGED` broadcasts and lifecycle resume events to stay synchronized automatically.
- **Offline Disk Cache & Rate-Limit Safety:** Uses a channel-first sequential discovery pipeline with atomic JSON caching and status-aware TTLs, paired with open ESPN JSON scoreboards for lightweight 60-second foreground live status updates.

---

## Tech Stack

- **Language & UI:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3
- **Networking & Parsing:** OkHttp 4.12.0, Jsoup 1.18.3
- **Concurrency & Serialization:** Kotlinx Coroutines 1.9.0, Kotlinx Serialization 1.7.3
- **Typography:** Bundled static `.ttf` fonts (`EB Garamond` Regular/Italic, `Manrope` 300/400/500/600)
- **Build & Testing:** Gradle 8.13, AGP 8.7.3, JUnit 4, Kotlinx Coroutines Test, OkHttp MockWebServer

---

## Project Structure

```text
app/src/main/java/com/bhora/app/
├── BhoraApplication.kt              # App-wide repository & HTTP client wiring
├── MainActivity.kt                  # Edge-to-edge host + live timezone receiver
├── domain/
│   ├── model/Fixture.kt             # Competition, BroadcastService, PrimeFixture models
│   └── logic/
│       ├── PrimeVideoMatcher.kt     # Global Prime Video, CazeTV (BR), and no-add-on HBO Max rules
│       ├── FixtureFilterEngine.kt   # Status, service, club/competition search & local date grouping
│       ├── FixtureTimeFormatter.kt  # Date headers & HH:mm LOCAL formatting
│       └── TeamNameNormalizer.kt    # Club name reconciliation between LiveSoccerTV and ESPN
├── data/
│   ├── remote/
│   │   ├── LiveSoccerTvClient.kt    # Throttled OkHttp HTML client + Cloudflare challenge detection
│   │   ├── LiveSoccerTvParser.kt    # Jsoup parser for schedules, channel feeds & 200-country tables
│   │   └── EspnScoreboardClient.kt  # Multi-league ESPN JSON client (eng.1, esp.1, uefa.champions)
│   ├── cache/FixtureDiskCache.kt    # Atomic JSON disk cache with schema versioning & TTLs
│   └── repository/PrimeFixturesRepository.kt
└── ui/
    ├── theme/                       # Color.kt, Type.kt, Theme.kt (Bhora Matchday Canvas tokens)
    └── matchday/                    # MatchdayUiState, MatchdayViewModel, MatchdayScreen, FixtureRow
```

---

## Building & Testing

### Prerequisites
- JDK 17+ (built and tested with JDK 21)
- Android SDK (`compileSdk = 35`, `minSdk = 26`) configured via `ANDROID_HOME` or `local.properties` (`sdk.dir=/path/to/Android/Sdk`)

### Run Unit Tests & Lint
```bash
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug
```

### Build Debug & Release APKs
```bash
./gradlew assembleDebug assembleRelease
```

APKs are written to:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### Optional Local Release Signing
By default, `assembleRelease` falls back to the debug keystore if `signing.properties` is not present. To sign release builds with your own keystore, create a gitignored `signing.properties` in the project root:

```properties
storeFile=bhora-release.jks
storePassword=your-store-password
keyAlias=bhora
keyPassword=your-key-password
```

### Install on a Connected Android Device
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Design Assets

- [DESIGN.md](DESIGN.md) — Bhora Matchday Canvas design tokens, typography, and layout principles
- [code.html](code.html) — Reference HTML/Tailwind prototype
- [screen.png](screen.png) — Reference visual mockup
