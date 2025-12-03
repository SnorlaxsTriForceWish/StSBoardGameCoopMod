# Powers (Buffs) - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Powers-(Buffs)

## Overview
Powers represent the game's system for buffs and debuffs applied to players or enemies. They extend from `AbstractPower`, with `BasePower` providing additional utility features.

## Key Concepts

**Power Definition**: "Power is the game's general term for all buffs and debuffs that go on the player or enemies, not to be confused with Power cards."

### Structure Example
The documentation provides a `NotStrengthPower` class that increases attack damage. Key components include:

- **Power ID**: A unique identifier using `makeID("NotStrength")`
- **Type**: Designated as BUFF or DEBUFF
- **Turn-based flag**: Controls color display (white for turn-based, red/green for non-turn-based)
- **Core method**: `atDamageGive()` modifies damage based on the power's amount

## Description System

Power text loads from `PowerStrings.json`. Descriptions use array segments combined in code:
- `#b` colors text blue (for numbers)
- `#y` colors text yellow (for keywords, which should be capitalized)

Example structure breaks descriptions into parts assembled conditionally.

## Icon Requirements

Images are loaded from `modid/images/powers/`:
- Standard version: ~30x30 PNG
- Large version (optional): ~84x84 for visual effects in `large/` subfolder

## Applying Powers

Use `ApplyPowerAction` with the primary constructor:
```
ApplyPowerAction(target, source, powerToApply)
```

The power's owner must match the action's target. Optional parameters include `stackAmount` (adjusting existing power amounts instead of stacking new instances), `isFast` (for rapid application), and `AttackEffect` (visual/sound effects).
