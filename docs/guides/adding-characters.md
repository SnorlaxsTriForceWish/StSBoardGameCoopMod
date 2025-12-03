# Adding a Character to Slay the Spire - Complete Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Adding-a-Character

## Overview
This tutorial covers creating a playable character for Slay the Spire using the BasicMod framework. The process involves setting up Java classes, registering components, and configuring visual assets.

## Required Components

### 1. Character Assets
Download example image assets and extract them to `resources/yourmod/images/character/`. The folder structure should include:
- Character select images (button, portrait)
- Card background images (attack, skill, power variations)
- Energy orb textures
- Animation assets

### 2. The Character Class

Create a new `character` package in your mod's Java folder. Use the provided template file, which includes:

**Enum Definitions:**
The class requires enum values for player class and card color using the `@SpireEnum` annotation. These identify the character within the game's system and must have matching names.

**Static Fields:**
Configuration includes energy per turn, maximum HP, starting gold, card draw amount, and orb slots. Most characters only modify maximum HP (typically 70).

**Image Path Constants:**
References to all character-specific images through a `characterPath()` method that constructs proper file paths.

**Key Methods:**
- `registerColor()` - Registers the card color with BaseMod using image assets
- `registerCharacter()` - Registers the character class with BaseMod
- `getStartingDeck()` - Defines initial cards (requires placeholder until custom cards exist)
- `getStartingRelic()` - Defines the starter relic

**Constructor:**
Initializes the character with energy orb, animation, and character image. Includes hitbox positioning and energy manager configuration.

## Registration Process

### Step 1: Register Card Color
In the main mod file's `initialize()` method, call the character's `registerColor()` method:

```java
MyCharacter.Meta.registerColor();
```

### Step 2: Implement EditCharactersSubscriber
Add `EditCharactersSubscriber` to the main mod class's implements list, then implement the required method:

```java
@Override
public void receiveEditCharacters() {
    MyCharacter.Meta.registerCharacter();
}
```

This makes the character selectable in-game.

## Content Requirements

**Card Pool:**
- Minimum 3 each of common, uncommon, and rare cards (more if using question marks)
- At least 2 attacks, 2 skills, and 1 power for shop functionality
- Target: 75 total cards in approximately 1:2:1 common:uncommon:rare ratio

**Relic Pool:**
- 1 starter relic (plus upgrade)
- 1 common relic
- 1-2 uncommon relics
- 1-3 rare relics
- 2-3 boss relics (non-energy, energy, and upgrade variants)

## Optional Visual Customization

**Animation:**
Replace the default animation with static images by modifying the constructor to use `Type.NONE` animation and specifying a character image path.

**Energy Orb:**
`CustomEnergyOrb` accepts layered images with rotation speeds. Images should be 128x128 pixels (256x256 for VFX).

**Card Appearance:**
Modify cardback images to change card visual style. The `cardTrailColor` is particularly visible during gameplay.

**Cutscene:**
Override `getCutscenePanels()` to customize the Heart kill cutscene with custom images (1920x1200 pixels) and sound effects.

## Common Issues

The tutorial notes that "you'll crash at the end of combats for now" until sufficient card content exists. This is expected during development before the full card pool is implemented.

Insufficient card variety can cause card reward and shop systems to freeze. Using Prismatic Shard as a temporary starting relic can avoid some crashes during testing.

## Next Steps

After character setup, proceed to creating cards, relics, and potions. The wiki provides separate tutorials for each component, with card creation being the highest priority for gameplay stability.
