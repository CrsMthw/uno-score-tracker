# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.5] - 2026-06-28

_Initial public release — a complete UNO score tracker and pocket rulebook for every official variant._

### Added
- **Config-driven scoring engine** — One engine scores every variant; all point values, targets, and
  special rules live in a single bundled config that feeds both the calculator and the in-app rulebook,
  so the reference and the maths can never drift apart. Covered by a unit-test suite of worked examples
  for every variant.
- **Eight official variants + reskins** — UNO (Original), Flip, Liar's, No Mercy, All Wild, Attack,
  Triple Play, and Party, with themed/licensed decks treated as Original-compatible.
- **Variant-correct rules** — Flip side-dependent scoring (score the ending side), No Mercy 1000 target
  with a +250-per-knockout bonus and 25-card elimination, All Wild's number-less palette, Attack's own
  table, and Liar's joint winners.
- **Two hand-entry modes** — A fast manual total, or card-by-card by tapping the variant's own palette;
  the palette adapts per variant, and Flip swaps to the ending side's cards.
- **Three scoring readings** — Winner-takes (Mode A), lowest-total (Mode B), and the elimination
  "Challenge Game", chosen per game.
- **The Table** — A circular live board with a slow wavy ring that travels in the direction of play,
  the turn pointer, active color, per-player hand size, and No Mercy eliminations; toggles to a flat
  Standings list with points-to-target progress.
- **Teams / partners** — Score to a team instead of an individual.
- **Editable round history with full recompute** — Fix any past round and every total, elimination,
  and the final result recalculate via a single replay of the recorded rounds.
- **Custom score limit** per game.
- **Resume + guaranteed winner screen** — In-progress games survive an app kill, and crossing the
  target always reaches the winner screen, re-surfacing it on next launch if needed.
- **Abandon game** — Ending a game where no one has scored discards it with no winner recorded.
- **Players, leaderboard, and game history/detail** — A saved roster, lifetime win-% stats, and a
  read-only per-game breakdown with a round-by-round table.
- **Biometric-protected deletes** — Removing a game, wiping all history, or deleting a player requires
  fingerprint / device PIN.
- **In-app rulebook** — A booklet per variant (quick facts, jump-to-scoring, collapsible sections, the
  card glossary and scoring table from the same config), reworded from the official Mattel sheets.
- **Material 3 Expressive UI** — Flexible app bars, a floating toolbar, expressive shapes and motion,
  dynamic color, optional AMOLED black, semantic haptics, and edge-to-edge transparent bars.
- **Foldable two-pane** — On the Galaxy Z Fold 6 and any wide screen, the active game shows the Table
  and Standings side by side, and the rulebook shows the variant list and reader together.

[1.0.5]: https://github.com/CrsMthw/uno-score-tracker/releases/tag/v1.0.5
