# AbstractCard Reference

> Source: https://github.com/Alchyr/BasicMod/wiki/AbstractCard

This document provides a reference for the AbstractCard class from Slay the Spire, which is the base class for all cards.

## Fields Overview

**Card Classification Fields:**
- `type`: Card category (ATTACK, SKILL, POWER, STATUS, CURSE)
- `rarity`: Determines availability in rewards (BASIC, SPECIAL, COMMON, UNCOMMON, RARE, CURSE)
- `color`: Visual appearance styling
- `target`: Targeting behavior affecting reticles and arrow display

**Cost Management:**
- `cost`/`costForTurn`: Card expense; -2 displays nothing, -1 indicates X cost
- `isCostModified`/`isCostModifiedForTurn`: Alters cost display color when true

**Damage Attributes:**
- `damage`/`baseDamage`: Attack values calculated via `applyPowers` and `calculateCardDamage`
- `isDamageModified`: Controls whether `!D!` shows modified or base damage
- `isMultiDamage`/`multiDamage`: For cards dealing "damage to ALL enemies"

**Block & Special Values:**
- `block`/`baseBlock`: Defense calculated in `applyPowersToBlock`
- `magicNumber`/`baseMagicNumber`: For non-damage/block mechanics

**Card Effects:**
- `retain`: One-time end-of-turn retention
- `selfRetain`: Permanent retention (use `setRetain()` method)
- `isInnate`/`isEthereal`: Use recommended setter methods
- `exhaust`: Use `setExhaust()` method
- `upgraded`/`timesUpgraded`: Track upgrade state

**Visual & Evoke:**
- `showEvokeValue`/`showEvokeOrbCount`: Display upcoming orb evoke values while dragging
- `dontTriggerOnUseCard`: For cards "played" without counting toward action limit

## Fields to Avoid

"unused" fields include `baseHeal`, `baseDraw`, `baseDiscard`, `isUsed`, and `chargeCost`.
