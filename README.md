# UNO Score Tracker

An ad-free, open source score tracker **and** pocket rulebook for UNO and all its official variants.
Built for Android with Jetpack Compose and Material 3 Expressive.

---

## Why This Exists

Most UNO score apps are a single counter that assumes you only ever play classic UNO, drown you in
ads, or get the maths wrong on the variants. UNO is not one game — Flip scores differently depending
on which side the round ended, No Mercy plays to 1000 with a +250 knockout bonus, All Wild has no
number cards, Attack has no draw cards at all. This app handles **every official variant** from one
config-driven engine, ships the rules for each in a clean in-app booklet, and works fully offline with
no ads, no tracking, and no analytics.

---

## Features

### Every official variant, one engine
- **8 mechanically-distinct variants** — UNO (Original), Flip, Liar's, No Mercy, All Wild, Attack,
  Triple Play, and Party — plus "Classic-compatible" themed decks (Minecraft, Marvel, Splash…).
- **One config-driven scoring engine.** Every point value, target, and special rule lives in one data
  file that feeds both the calculator and the rulebook, so the reference and the maths can never disagree.
- **Variant-correct scoring** — Flip's side-dependent values (score the side the round ended on),
  No Mercy's 1000 target + 250-per-knockout bonus + 25-card elimination, All Wild's wild-only palette,
  Attack's own table, Liar's joint winners.
- **Verified against the rulebook** — the engine is covered by a unit-test suite of worked scoring
  examples for every variant.

### Scoring & game flow
- **Two ways to enter a hand** — a fast manual total, or **card-by-card** by tapping the variant's own
  palette (the chips you can tap change per variant; Flip swaps to the ending side's cards).
- **Three official scoring readings** — winner-takes (Mode A), lowest-total (Mode B), and the
  elimination "Challenge Game".
- **Editable round history** — fix any past round and every total, elimination, and the game result
  recompute correctly.
- **Custom score limit** — override the default target per game.
- **Resume game** — a game killed mid-play is saved and waiting on next launch.
- **Guaranteed winner screen** — crossing the target always shows the results, even if the app was
  restarted before you saw it.
- **Abandon a game** — end a game where no one has scored and it's discarded with no winner recorded.

### The Table — a live game board
- A circular **"Table"** view shows everyone around a slow **wavy ring that travels in the direction of
  play** (it reverses when you flip direction), the current turn, the active color, each player's hand
  size, and No Mercy eliminations — all optional live aids that never affect the score maths.
- Toggle to a flat **Standings** list with points-to-target progress bars.

### Players, teams, history & privacy
- **Saved player roster** — add your regular crew once, pick them every game.
- **Teams / partners** — score to a team instead of an individual.
- **Leaderboard** — lifetime win stats per player, sorted by win %.
- **Game history + detail** — browse finished games, tap for final standings and a round-by-round table.
- **Biometric-protected deletes** — deleting a game, wiping all history, or removing a player requires
  your fingerprint / device PIN, so a competitive friend can't quietly erase their losses.

### In-app rulebook
- A **pocket rulebook** for every variant: quick-facts chips, a jump-to-scoring shortcut, collapsible
  sections (object, setup, how to play, calling UNO, going out, the card glossary and scoring table),
  and a "Start a game with this variant" button. Rules are summarized in our own words from the
  official Mattel sheets, with a link to each source.

### Design & feel
- **Material 3 Expressive** — built exclusively on the latest expressive components (flexible app bars,
  a floating toolbar, expressive shapes, wavy indicators) with expressive spring motion.
- **Foldable two-pane** — on the Galaxy Z Fold 6 (and any wide screen), the active game shows the Table
  and Standings side by side, and the rulebook shows the variant list and reader together.
- **Dynamic color**, optional **AMOLED pure-black** dark theme, semantic **haptics** (with a toggle),
  and edge-to-edge transparent system bars.

---

## Supported variants

| Variant | Players | Deck | Target | Notable rules |
|---|---|---|---|---|
| UNO (Original) | 2–10 | 108/112 | 500 | The classic table |
| UNO Flip | 2–10 | 112 | 500 | Light/Dark sides; score the ending side |
| Liar's UNO | 2–6 | 112 | 500 | Bluff & challenge; joint winners |
| UNO Show 'Em No Mercy | 2–6 | 168 | 1000 | Stacking, 7-0, 25-card elimination, +250 knockout |
| UNO All Wild | 2–10 | 112 | 500 | No number cards |
| UNO Attack | 2–10 | 112 | 500 | Launcher, no draw cards, own table |
| UNO Triple Play | 2–6 | 112 | 500 | Three light-up piles |
| UNO Party | 6–16 | 224 | 500 | Big-group cards |

Themed/licensed decks (Minecraft, Marvel, Disney, sports, Splash, Mod…) use the Original table.

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose |
| Design system | Material 3 Expressive (`material3:1.5.0-alpha21`) + `graphics-shapes` |
| Navigation | Compose Navigation `2.9.8` |
| Database | Room `2.8.4` |
| Config | Bundled JSON parsed with Gson |
| Auth | `androidx.biometric` (for destructive actions) |
| Reactive state | Kotlin Flow + StateFlow |
| Architecture | MVVM (ViewModel + Repository), manual DI |
| Adaptive layout | `androidx.compose.material3.adaptive` |
| Build | AGP 9.2.1, Kotlin 2.3.21, KSP 2.3.8 |
| Min SDK | 35 (Android 15) · Target SDK 37 (Android 16) |

---

## Install

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/CrsMthw/uno-score-tracker)

Tapping this button on your Android device will open Obtainium and automatically add the repo — it'll
notify you and install new releases automatically from then on.

Or go to the [Releases](../../releases) page and download the latest `UnoScoreTracker-vX.Y.Z.apk` manually.

---

## Building

Requirements: Android Studio (latest stable), JDK 17+, Android SDK 37.

```bash
git clone https://github.com/CrsMthw/uno-score-tracker.git
cd uno-score-tracker
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## License

MIT. Do whatever you want with it.

UNO is a trademark of Mattel; this app is an unofficial, fan-made tool and is not affiliated with or
endorsed by Mattel. All rules are reworded summaries, not copies of Mattel's booklet text.

---

*Built with Claude — because keeping score for No Mercy in your head is its own kind of mercy rule.*
