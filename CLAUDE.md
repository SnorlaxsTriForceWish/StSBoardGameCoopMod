# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**The Co-op Board Game** is a Slay the Spire mod that adapts the physical board game into StS.

Current state in this repo:

- Single-player board game mechanics are broadly implemented.
- TogetherInSpire multiplayer integration is partially implemented.
- Multiplayer row combat, row targeting, map voting, and player-targeted card plumbing exist.
- Shared die synchronization and true simultaneous-turn resolution are not yet implemented.

## Build & Development Commands

### Building and Running

**Windows Command Line** (from `D:\STS_BG_Mod\StSBoardGameCoopMod`):

```cmd
# Package mod JAR and copy to Steam mods folder
mvnw.cmd package

# Run tests (currently no Java tests in src/test/java)
mvnw.cmd test

# Clean build artifacts
mvnw.cmd clean

# Clean + package (recommended after larger changes)
mvnw.cmd clean package
```

**Unix/Mac/Linux**:

```bash
./mvnw package
./mvnw test
./mvnw clean
./mvnw clean package
```

### Notes

- `package` builds `target/CoopBoardGame.jar` and copies it to `${steam.path}/common/SlayTheSpire/mods/CoopBoardGame.jar`.
- Maven packaging does **not** launch Slay the Spire automatically.
- Steam paths are configured in `pom.xml` (`steam.windows`, `steam.mac`, `steam.linux`).

## Current Codebase Snapshot

Snapshot from this checkout:

- `src/main/java/CoopBoardGame`: **888** Java files total
- `CoopBoardGame.java`: **1732** lines
- Major package sizes:
  - `cards/`: 322
  - `actions/`: 107
  - `powers/`: 101
  - `relics/`: 96
  - `monsters/`: 65
  - `events/`: 44
  - `potions/`: 24
  - `patches/`: 23
  - `multiplayer/`: 16
  - `targeting/`: 9

Card set counts:

- `BGRed`: 63
- `BGBlue`: 62
- `BGGreen`: 64
- `BGPurple`: 64
- `BGColorless`: 42
- `BGCurse`: 10
- `BGStatus`: 4

## Architecture Overview

### Core Pattern

- Entry point: `src/main/java/CoopBoardGame/CoopBoardGame.java`
- Main extension model: BaseMod/ModTheSpire subscribers + Spire patches
- Domain base classes:
  - `cards/AbstractBGCard.java`
  - `relics/AbstractBGRelic.java`
  - `monsters/AbstractBGMonster.java`
  - `powers/AbstractBGPower.java`
  - `characters/AbstractBGPlayer.java`
- IDs are string-based (`makeID(...)` usage throughout).

### Multiplayer Integration (current)

TogetherInSpire integration is reflection-based through:

- `src/main/java/CoopBoardGame/util/TogetherInSpireHelper.java`

Implemented multiplayer systems in this repo:

- Row assignment and row sync:
  - `multiplayer/rows/PlayerRowManager.java`
  - `multiplayer/rows/RowNetworkHelper.java`
  - `multiplayer/patches/RowMessagePatch.java`
- Multi-row enemy setup and row rendering/positioning:
  - `multiplayer/patches/MultiCombatEncounterPatches.java`
  - `multiplayer/patches/CreatureRowPositionPatch.java`
  - `multiplayer/patches/MultiplayerRowRenderPatch.java`
  - `multiplayer/rows/CombatRowManager.java`
- Row-based target filtering:
  - `multiplayer/patches/RowBasedTargetingPatch.java`
- Map voting:
  - `multiplayer/voting/RoomVotingManager.java`
  - `multiplayer/voting/VotingNetworkHelper.java`
  - `multiplayer/patches/MapVotingPatch.java`
  - `multiplayer/patches/VotingMessagePatch.java`
- Player-targeted effect messaging (framework + one production executor):
  - `targeting/PlayerEffectNetworkHelper.java`
  - `targeting/PlayerEffectRegistry.java`
  - `multiplayer/patches/PlayerEffectMessagePatch.java`
  - `actions/player/GainBlockOnPlayerAction.java` (registered executor)

## Board Game Mechanics (vs Vanilla StS)

### 1. Dice Roll System

- Core logic: `src/main/java/CoopBoardGame/thedie/TheDie.java`
- Dice UI buttons in `src/main/java/CoopBoardGame/ui/`:
  - `LockInRollButton.java`
  - `RerollButton.java`
  - `TheAbacusButton.java`
  - `ToolboxButton.java`
  - `PotionButton.java`
- Monster behavior hooks use `DieControlledMoves`.

### 2. Row-Based Multiplayer Combat

- Row state is stored via `MultiCreature.Field.currentRow`.
- Host spawns additional enemy groups per row in multiplayer.
- Damage/debuff/reduction from monsters is filtered by row in multiplayer mode.

### 3. Token Caps (partial)

Implemented:

- Strength cap = 8 (`src/main/java/CoopBoardGame/powers/StrengthCap.java`)
- Poison cap = 30 across enemies (`src/main/java/CoopBoardGame/powers/BGPoisonPower.java`)

Still not fully enforced globally:

- Block cap
- Weak/Vulnerable cap behavior matching physical token limits

### 4. Trading and Payment Mechanics

- Relic payment support in `src/main/java/CoopBoardGame/relics/AbstractBGRelic.java` (`usableAsPayment()`).
- Relic trading UI exists: `src/main/java/CoopBoardGame/screen/RelicTradingScreen.java`.

### 5. Player-Targeted Cards

- Custom targets added by patch enums in `src/main/java/CoopBoardGame/patches/CardTargetPatch.java`:
  - `PLAYER`
  - `ALL_PLAYERS`
- Drag/arrow targeting behavior in `src/main/java/CoopBoardGame/patches/PlayerCardTargetingPatch.java`
  and `src/main/java/CoopBoardGame/targeting/PlayerTargetingArrow.java`.
- Current in-use example: upgraded `BGDefend_Red` uses `GainBlockOnPlayerAction`.

## Common Development Tasks

### Add a New Card

1. Create class extending `AbstractBGCard` under the correct card set package.
2. Implement `use(AbstractPlayer p, AbstractMonster m)`.
3. Register the card in `CoopBoardGame.receiveEditCards()`.
4. Add localization entries in:
   - `src/main/resources/CoopBoardGameResources/localization/eng/DefaultMod-Card-Strings.json`

### Add a Patch

1. Create patch class under `src/main/java/CoopBoardGame/patches/` or `multiplayer/patches/`.
2. Use `@SpirePatch2` with Prefix/Insert/Postfix as needed.
3. Prefer `SpireField` for additional state instead of modifying base game classes.

### Add a Multiplayer Message Type

1. Add send/receive helper logic in an existing helper (`RowNetworkHelper`, `VotingNetworkHelper`, or `PlayerEffectNetworkHelper`) or a new helper.
2. Intercept message handling in a message patch class under `multiplayer/patches/`.
3. Keep host authority explicit for state that must be deterministic.

## Critical Files

- Mod bootstrap:
  - `src/main/java/CoopBoardGame/CoopBoardGame.java`
- Multiplayer core:
  - `src/main/java/CoopBoardGame/util/TogetherInSpireHelper.java`
  - `src/main/java/CoopBoardGame/multiplayer/patches/MultiCombatEncounterPatches.java`
  - `src/main/java/CoopBoardGame/multiplayer/rows/RowNetworkHelper.java`
  - `src/main/java/CoopBoardGame/multiplayer/voting/RoomVotingManager.java`
- Player targeting:
  - `src/main/java/CoopBoardGame/patches/PlayerCardTargetingPatch.java`
  - `src/main/java/CoopBoardGame/targeting/PlayerEffectNetworkHelper.java`
- Dice and board-game turn flavor:
  - `src/main/java/CoopBoardGame/thedie/TheDie.java`

## Known Issues & Limitations

### Technical faults (from README)

- Quick Start interface can softlock; map open/close can recover in many cases.
- Some events can softlock if the player lacks enough cards.
- Some events do not validate affordability.
- Card UI art can be wrong in some situations.
- Score bonuses can be calculated incorrectly.
- Egg relic interactions on shop screen can behave oddly.

### Board game accuracy gaps

- Token limits are only partially implemented (Strength/Poison present; others incomplete).
- Non-card-reward decks can still duplicate unique cards.
- Ironclad + Gremlin Nob + Feel No Pain ordering mismatch remains documented.

### Multiplayer gaps still pending

- Shared die roll authority/synchronization across players.
- True simultaneous-turn planning/resolution state machine.
- Potion trading UX in this repo (beyond existing TiS ecosystem capabilities).
- Boss relic reveal/pick flow matching board-game N+1 rule.

## Dependencies

Declared in `pom.xml`:

- `ModTheSpire` 3.30.0
- `BaseMod` 5.44.0
- `StSLib` 2.4.0
- Optional: `Bestiary` 0.1.1
- Slay the Spire reference version string: `12-18-2022`

Test dependencies declared:

- JUnit 5.9.3
- Mockito 4.11.0
- AssertJ 3.24.2

## Resources

Localization:

- `src/main/resources/CoopBoardGameResources/localization/eng/`
  - `DefaultMod-Card-Strings.json`
  - `DefaultMod-Relic-Strings.json`
  - `DefaultMod-Power-Strings.json`
  - `DefaultMod-Event-Strings.json`
  - `DefaultMod-Character-Strings.json`
  - `DefaultMod-Potion-Strings.json`
  - `DefaultMod-Orb-Strings.json`
  - `DefaultMod-Keyword-Strings.json`
  - `DefaultMod-UI-Strings.json`
  - `Bestiary-Monsters.json`

Assets:

- `src/main/resources/CoopBoardGameResources/images/`

## Reference Docs

- Board game rules reference: `docs/BoardGameRules.txt`
- TogetherInSpire architecture notes: `ModCopy/TogetherInSpire/stsogether_architecture_analysis.md`
- Additional local guides: `docs/guides/`

## Contributing Notes

When modifying code:

1. Follow established naming conventions (`BG` prefix for mod-specific content).
2. Register new content in `CoopBoardGame.java`.
3. Add localization entries instead of hardcoding player-facing text.
4. Validate single-player behavior first, then multiplayer behavior if affected.
5. Update this `CLAUDE.md` when architecture or implementation status changes.
