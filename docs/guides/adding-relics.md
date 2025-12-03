# Adding Relics - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Adding-Relics

## Overview
This guide covers creating relics for Slay the Spire mods using BasicMod. Relics are game items that provide passive effects during gameplay.

## Registration Process

### AutoAdd Setup
Implement `EditRelicsSubscriber` in your main mod file:

```java
public class MyMod implements EditRelicsSubscriber {
    @Override
    public void receiveEditRelics() {
        new AutoAdd(modID)
            .packageFilter(BaseRelic.class)
            .any(BaseRelic.class, (info, relic) -> {
                if (relic.pool != null)
                    BaseMod.addRelicToCustomPool(relic, relic.pool);
                else
                    BaseMod.addRelic(relic, relic.relicType);
                if (info.seen)
                    UnlockTracker.markRelicAsSeen(relic.relicId);
            });
    }
}
```

This automatically registers relics extending `BaseRelic` without manual registration.

## Creating a Relic

### Basic Structure

Create a class in your `relics` package extending `BaseRelic`:

```java
public class MyRelic extends BaseRelic {
    private static final String NAME = "MyRelic";
    public static final String ID = makeID(NAME);
    private static final RelicTier RARITY = RelicTier.COMMON;
    private static final LandingSound SOUND = LandingSound.CLINK;

    public MyRelic() {
        super(ID, NAME, MyCharacter.Meta.CARD_COLOR, RARITY, SOUND);
    }
}
```

### Relic Tiers
- `STARTER`: Starting relics
- `COMMON`, `UNCOMMON`, `RARE`: Standard rarities
- `SHOP`: Shop-exclusive relics
- `BOSS`: Boss chest relics
- `SPECIAL`: Event/unobtainable relics

## Adding Functionality

Relics use hook methods tied to gameplay events. Override methods like `onUseCard()`, `onVictory()`, etc.

### Example: Strength on Card Play

```java
@Override
public void onUseCard(AbstractCard targetCard, UseCardAction useCardAction) {
    addToBot(new ApplyPowerAction(AbstractDungeon.player,
        AbstractDungeon.player, new StrengthPower(AbstractDungeon.player, STRENGTH)));
}
```

## Localization

Add relic text to `RelicStrings.json`:

```json
"${modID}:MyRelic": {
    "NAME": "My Relic",
    "FLAVOR": "Flavor text here",
    "DESCRIPTIONS": [
        "Whenever you play a card, gain #b",
        " #yStrength."
    ]
}
```

Override `getUpdatedDescription()` to insert dynamic values:

```java
@Override
public String getUpdatedDescription() {
    return DESCRIPTIONS[0] + STRENGTH + DESCRIPTIONS[1];
}
```

## Image Assets

Place images in the `images/relics` folder:

- **Base image** (128×128): `MyRelic.png` - only center 64×64 used
- **Large image** (256×256): `relics/large/MyRelic.png` - optional
- **Outline** (128×128): `MyRelicOutline.png` - optional white silhouette

## Text Formatting Guidelines

- Capitalize keyword first letters; use `#y` prefix for keywords
- Color numbers blue with `#b`
- Use `[E]` for energy icons
- Keep starter relic descriptions concise
- Use `[#rrggbb]` for custom hex colors
