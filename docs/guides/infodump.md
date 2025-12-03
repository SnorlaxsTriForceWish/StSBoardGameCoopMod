# BasicMod Infodump - Practical Guidance

> Source: https://github.com/Alchyr/BasicMod/wiki/Infodump

This page contains practical guidance for Slay the Spire mod development using BasicMod.

## Card Type Classification

The page explains how to categorize cards:

- **Attack cards**: Should be used for effects modified by damage mechanics (Strength, Weakness). Avoid non-attack cards with damage modifiers to prevent "strange interactions with relics like Pen Nib."

- **Power cards**: Best for providing long-term combat or run benefits, especially those applying powers to the player.

- **Skill cards**: Use when a card targets enemies without granting long-term benefits.

- **Curse cards**: Permanent negative-effect cards.

- **Status cards**: Temporary negative-effect cards.

## Card Targeting Options

The page lists five targeting types with behavioral notes:

NONE, SELF, ENEMY, SELF_AND_ENEMY, ALL_ENEMY, and ALL. Functionally, only ENEMY and SELF_AND_ENEMY differ—they add targeting arrows and pass the targeted monster to the card's `use` method. Others pass `null` for the monster parameter.

## @Override Annotation

The `@Override` annotation serves as a compiler check. "When a method has the `@Override` annotation, that tells the compiler that you're trying to override a method." It doesn't affect functionality but catches errors during development.

## Saving Keyword Text

Developers can store keyword information by adding an `ID` field to Keywords.json, enabling easy access via a Map. The page provides implementation details for registering keywords and accessing them programmatically for tooltips.
