# Kotlin Multiplatform Solitaire

This repo demonstrates Kotlin Multiplatform card games (solitaire and FreeCell) with architecture that keeps gameplay logic reusable while platform clients stay focused on rendering and integration.

## Project Focus

- One shared module for rules, state transitions, and board interaction geometry.
- Platform code is thin, so desktop, web, Android, and iOS ship from the same gameplay core.
- Module boundaries are checked in CI; shared logic is covered by common tests.

## Quick Start

From repo root:

- Desktop: `./gradlew :clients:korge:jvmRun`
- Web dev server: `./gradlew :clients:korge:jsBrowserDevelopmentRun`
- Android (device or emulator):
  - `./gradlew :clients:korge:runAndroidDebug`
  - Install only: `./gradlew :clients:korge:installAndroidDebug`
  - Package only (AAB): `./gradlew :clients:korge:packageAndroidDebug`
  - `runAndroidDebug` expects a device or emulator already visible to `adb`, unless you use the emulator-specific KorGE tasks (and have emulators configured).
- iOS Simulator (macOS + Xcode):
  - `./gradlew :clients:korge:runIosSimulatorDebug`
  - `./gradlew :clients:korge:runIosSimulatorDebugDetached` (recommended locally)
  - Build + install only: `./gradlew :clients:korge:installIosSimulatorDebug`
  - Package only: `./gradlew :clients:korge:packageIosSimulatorDebug`

## What To Review First

If you are scanning quickly, start here:

1. [`shared/src/commonMain/kotlin/domain/`](shared/src/commonMain/kotlin/domain/) for rule logic and state transitions
2. [`shared/src/commonMain/kotlin/presentation/solitaire/`](shared/src/commonMain/kotlin/presentation/solitaire/) for intent/store mapping
3. [`shared/src/commonMain/kotlin/presentation/solitaire/geometry/`](shared/src/commonMain/kotlin/presentation/solitaire/geometry/) for pure hit/layout math
4. [`clients/korge/src/commonMain/kotlin/ui/`](clients/korge/src/commonMain/kotlin/ui/) for platform rendering and input integration

## Demo

- Live demo: [https://harrisonsoftware.dev/solitaire](https://harrisonsoftware.dev/solitaire)
- Short walkthrough: [Watch demo](https://annaharri89.github.io/images/external/KMPSolitaireDemo.mov)

![Playable V1 Solitaire running in the KorGE desktop window](docs/readme-solitaire-desktop.png)

## Repo Metrics (Current Snapshot)

Measured on **2026-04-15**.

**Summary:** About **98%** of measured app Kotlin is in `shared/commonMain` and `clients/korge/commonMain`; the rest is **105** lines in platform-specific source sets.

These two ratios answer different questions. Both use the same numerators: **1455** lines in `shared/src/commonMain/kotlin`, **3956** in `clients/korge/src/commonMain/kotlin` (**5411** combined).

**1) Cross-platform organization (Kotlin)** — how much app logic stays in `commonMain` vs platform-specific code:

- **98.10%** (`5411 / 5516` Kotlin lines): `shared/commonMain` + `clients/korge/commonMain` vs that plus all Kotlin in platform-specific source sets (`androidMain`, `iosMain`, `jsMain`, `desktopMain`, etc.). Platform-specific Kotlin today: **105** lines.
- This is the number that shows thin platform boundaries and little duplicated per-target code.

**2) Share of the tracked project (text, minus noise)** — how much of the repo (excluding docs, lockfiles, tests, and common binary assets such as PNGs) is that shared stack:

- **83.69%** (`5411 / 6466` lines): same **5411** `commonMain` Kotlin vs that plus **1055** lines of Gradle, scripts, manifests, thin platform entrypoints, and other non-`commonMain` text under the same filter. The **6466**-line total is **5411** `commonMain` Kotlin plus **1055** lines of build and wiring, not duplicate game logic.

Shared tests in `shared/src/commonTest/kotlin`: **11 files / 32 `@Test` cases** (not included in the percentages above).

**Measurement limits:**

- Coverage trend is not tracked yet in this repo.
- This project is pre-release, so there is no production bug-rate or release-velocity baseline yet.

**Post-launch measurement plan:**

After release, I plan to track these metrics to validate the architecture goals:

- Feature parity lag: time gap between the first and last platform receiving the same gameplay feature.
- Rule-change delivery time: time from merge of a shared gameplay change to all target builds passing.
- Cross-platform drift defects: count of bugs caused by inconsistent gameplay behavior between platforms.

## Design and Implementation

I designed and implemented this around one shared gameplay core (`:shared`) and thin platform rendering clients (`:clients:korge`), with CI boundary checks and shared tests to keep that split intact.

## Key Engineering Tradeoffs

- I chose strict module boundaries early, even though it adds some upfront plumbing, because it keeps platform code from leaking into shared rules.
- I kept geometry and hit-testing pure in `:shared`, which makes tests easier, but required extra mapping work in the KorGE renderer.
- I used KorGE as a rendering shell instead of putting game rules in scenes, so gameplay changes stay engine-agnostic.
- I added CI boundary scripts to prevent architecture drift, trading a little CI time for stronger long-term maintainability.

## What CI Enforces

A green local build doesn’t prove `:shared` still compiles cleanly for every target you care about. This pipeline fails PRs when boundaries slip (KorGE leaking into shared, client re-implementing domain types), when shared tests regress, or when a platform compile breaks — before merge. Smoke builds cover JVM, JS webpack, iOS Kotlin, and Android debug on Linux/macOS runners so “works on my machine” shows up as a red CI run, not a surprise for the next person.

Runs on every push and PR (`.github/workflows/ci.yml`):

- **Shared tests:** `:shared:desktopTest`, `:shared:testDebugUnitTest` (JVM desktop + Android unit tests over `commonTest`).
- **Guards:** `check-shared-no-korlibs.sh`, `check-client-boundary.sh`.
- **KorGE smoke builds:** Linux — `compileKotlinJvm`, `jsBrowserDevelopmentWebpack`; macOS — `compileKotlinIosSimulatorArm64`; separate Linux job — `assembleDebug` with Android SDK.

**Not in CI:** `:clients:korge:jsTest` needs a local Chrome/Chromium (`CHROME_BIN`); see **Browser Test Setup** below.

## Architecture Details

Dependency direction is `:clients:korge` -> `:shared` (`dependencyProject(":shared")` in `clients/korge/build.gradle.kts`).

```
:shared
├── domain/              rules, models, session, deterministic reducer, GameRenderModel projection
└── presentation/        engine-agnostic intents, store, pure geometry (hit rects, board anchors)

:clients:korge
└── src/commonMain/      KorGE scenes, assets, input, SolitaireBoardRenderer (maps shared types to KorGE views)
```

All Kotlin under `shared/src` must not reference `korlibs.*` (or other KorGE stack packages). Boundary checks run in CI — see **What CI Enforces**.

## Shared Code And Targets

`:shared` is plain Kotlin and has no KorGE dependency. Main source directories are the numbered list in **What To Review First** above.

Gradle applies Kotlin Multiplatform to `:shared`. `commonMain` is compiled for each declared target, and `clients/korge/build.gradle.kts` wires `dependencyProject(":shared")` so clients use one shared API.

`shared/build.gradle.kts` currently enables:

- Android (`androidTarget`)
- Desktop JVM (`jvm("desktop")`)
- Browser (`js` with `browser()`)
- iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`)
- tvOS (`tvosArm64`, `tvosX64`, `tvosSimulatorArm64`)

The playable app uses the KorGE Gradle plugin (aligned with the `korge` library version). JVM, JS, Android, and iOS share `commonMain` plus small platform entrypoints (`jvmMain`, `jsMain`, `androidMain`, `iosMain`, and Android manifests). This repo does not register a KorGE tvOS app target; tvOS is compiled for `:shared` only.

Why this split helps:

- One rules implementation for all targets that link `:shared`.
- Tests in `shared/src/commonTest` validate the same domain, presentation, and geometry paths across targets.
- Platform work stays focused on rendering/input and shipping, not re-implementing gameplay behavior.

## Browser Test Setup (`CHROME_BIN`)

`jsTest` needs a Chrome or Chromium binary path.

Install Chrome on macOS:

- `brew install --cask google-chrome`

Set `CHROME_BIN` in current shell:

- `export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"`

Persist in zsh:

- `echo 'export CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"' >> ~/.zshrc`
- `source ~/.zshrc`

Run browser tests:

- `./gradlew :clients:korge:jsTest`

## Roadmap

- FreeCell gameplay support: WIP
