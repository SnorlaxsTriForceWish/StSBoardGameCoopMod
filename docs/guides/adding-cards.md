# Adding Cards - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Adding-Cards

## Card Registration Overview

The process involves two main steps: creating the card class and registering it. BasicMod simplifies this through **AutoAdd**, a BaseMod feature that automatically registers cards without manual setup.

### Setting Up AutoAdd

In your main mod file, implement `EditCardsSubscriber` and add the `receiveEditCards` method:

```java
public class MyMod implements EditCardsSubscriber, EditStringsSubscriber {
    @Override
    public void receiveEditCards() {
        new AutoAdd(modID)
            .packageFilter(BaseCard.class)
            .setDefaultSeen(true)
            .cards();
    }
}
```

This automatically discovers and registers cards in the same package as `BaseCard`.

## Card Structure

### Basic Setup

All cards extend `BaseCard`, which extends `CustomCard` and `AbstractCard`. The foundational elements include:

```java
public class Strike extends BaseCard {
    public static final String ID = makeID(Strike.class.getSimpleName());
    private static final CardStats info = new CardStats(
        MyCharacter.Meta.CARD_COLOR,
        CardType.ATTACK,
        CardRarity.BASIC,
        CardTarget.ENEMY,
        1  // cost
    );

    private static final int DAMAGE = 6;
    private static final int UPG_DAMAGE = 3;
}
```

### Constructor

The constructor initializes card properties using helper methods:

```java
public Strike() {
    super(ID, info);
    setDamage(DAMAGE, UPG_DAMAGE);
    tags.add(CardTags.STARTER_STRIKE);
    tags.add(CardTags.STRIKE);
}
```

**BaseCard helper methods include:** `setDamage`, `setBlock`, `setMagic`, `setCostUpgrade`, `setExhaust`, `setEthereal`, `setInnate`, `setSelfRetain`

## Card Functionality

### The Use Method

Cards perform actions via the `use` method, which adds actions to the action queue:

```java
@Override
public void use(AbstractPlayer p, AbstractMonster m) {
    addToBot(new DamageAction(m,
        new DamageInfo(p, damage, DamageInfo.DamageType.NORMAL),
        AbstractGameAction.AttackEffect.SLASH_VERTICAL));
}
```

**Card Variables:**
- `damage` and `baseDamage` - for attacks
- `block` and `baseBlock` - for defense
- `magicNumber` and `baseMagicNumber` - for other scaling effects

These display in card text as `!D!`, `!B!`, and `!M!` respectively.

### Damage Types

Three types exist:
- **NORMAL**: Standard attacks
- **THORNS**: Blockable non-attack damage
- **HP_LOSS**: Damage ignoring block

## Card Properties

### Card Types

- **ATTACK**: Effects affected by damage modifiers when played
- **SKILL**: Anything not an attack or power
- **POWER**: Permanent buffs, removed from combat on play

### Rarities

- **BASIC**: Starter cards, excluded from rewards
- **COMMON/UNCOMMON/RARE**: Standard reward cards
- **SPECIAL**: Generated cards (like Shivs)

### Important Tags

For proper interactions with relics and other effects:

```
STARTER_STRIKE  // basic Strike
STRIKE          // any Strike card
STARTER_DEFEND  // basic Defend
FORM            // Form cards
HEALING         // permanent benefit cards
```

Custom tags must use format: `${modID}:YourKeyword`

## Card Text and Localization

Add descriptions in `localization/eng/CardStrings.json`:

```json
{
  "${modID}:Strike": {
    "NAME": "Strike",
    "DESCRIPTION": "Deal !D! damage."
  }
}
```

**Formatting rules:**
- Keywords capitalized (first letter)
- Use `NL` with spaces for line breaks
- `[E]` represents energy icons
- `*text*` highlights like keywords
- `!D!` damage, `!B!` block, `!M!` magic numbers

**Keyword order in descriptions:**
1. Innate
2. Retain/Ethereal
3. Card effects
4. Exhaust

## Card Images

Images must be 500x380 pixels. Store dual versions:
- `CardName_p.png` (500x380) - detailed view
- `CardName.png` (250x190) - hand view

Location depends on card type: `modid/images/cards/[type]/CardName.png`

Examples: `modid/images/cards/attack/Strike.png` or `modid/images/cards/skill/Defend.png`

## Upgrades

The `BaseCard` class handles most upgrade logic automatically. Set upgrade values in the constructor, and provide `UPGRADE_DESCRIPTION` in localization if needed. For complex multi-upgrade cards, override the `upgrade` method while calling `super.upgrade()`.

## Testing

Enable the BaseMod Console through in-game Mods settings. Test cards with:
```
hand add [modid:CardID] {count} {upgrades}
```

## Additional Features

- **cardsToPreview**: Display generated cards (like Shivs). Example: `Look at Blade Dance for implementation`
- **Custom variables**: See "Quick Custom Card Variables" wiki page for additional scaling numbers beyond damage/block/magic
- **Effect ordering**: Block before damage (helps against Thorns), match description order
