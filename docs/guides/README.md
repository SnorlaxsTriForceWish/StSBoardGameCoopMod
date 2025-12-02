# BasicMod Wiki Guides

This directory contains local copies of the BasicMod wiki guides for Slay the Spire mod development.

> **Source**: https://github.com/Alchyr/BasicMod/wiki

## Available Guides

### Core Mechanics
- **[Action Queue](action-queue.md)** - Understanding the action queue system and when to add actions to top vs bottom
- **[Abstract Card Reference](abstract-card.md)** - Complete field reference for the AbstractCard class

### Adding Content
- **[Adding Cards](adding-cards.md)** - Complete guide to creating custom cards with AutoAdd registration
- **[Adding Characters](adding-characters.md)** - Full tutorial for creating playable characters
- **[Adding Potions](adding-potions.md)** - How to create custom potions with proper registration
- **[Adding Relics](adding-relics.md)** - Guide to creating relics with passive effects
- **[Events](events.md)** - Creating custom events using the PhasedEvent framework

### Systems & Features
- **[Keywords](keywords.md)** - Implementing and using custom keywords in cards/relics/powers
- **[Powers (Buffs)](powers.md)** - Creating buffs and debuffs using the power system

### Reference
- **[Infodump](infodump.md)** - Practical tips and common patterns for mod development

## Quick Reference

### Card Type Guidelines
- **ATTACK**: Use for damage effects modified by Strength/Weakness
- **SKILL**: Non-attack, non-power cards
- **POWER**: Long-term buffs applied to the player

### Damage Types
- **NORMAL**: Standard attacks
- **THORNS**: Blockable non-attack damage
- **HP_LOSS**: Unblockable damage

### Action Queue Rules
- **In card's use()**: Add to bottom (`addToBot`)
- **In action's update()**: Add to top (`addToTop`)
- **When uncertain**: Default to bottom

### Text Formatting
- `!D!` - Damage value
- `!B!` - Block value
- `!M!` - Magic number
- `#b` - Blue color (numbers)
- `#y` - Yellow color (keywords)
- `[E]` - Energy icon
- `NL` - Line break

### Image Sizes
- **Cards**: 500x380 (detailed), 250x190 (hand view)
- **Relics**: 128x128 (base), 256x256 (large, optional)
- **Powers**: 30x30 (standard), 84x84 (large, optional)
- **Potions**: 64x64 (all layers)

## AutoAdd Pattern

All content types use the AutoAdd system for automatic registration:

```java
new AutoAdd(modID)
    .packageFilter(BaseClass.class)
    .setDefaultSeen(true)  // for cards
    .cards();  // or .any() for other types
```

This discovers and registers all classes extending the base class in the same package.
