# Keywords - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Keywords

## Core Guidelines

Keywords must always be capitalized in card/relic/power text. Base game keywords are used as-is, while mod keywords require a mod ID prefix.

**Key distinction by context:**
- **Cards**: Keywords are auto-detected; use format `${modID}:KeywordName`
- **Relics/Powers**: Requires `#y` prefix for proper formatting (e.g., `#y${modID}:Hello`)
- **Power descriptions**: No auto-detection; write text directly as tooltips

## JSON Structure Example

```json
{
  "PROPER_NAME": "Hello",
  "NAMES": ["hello"],
  "DESCRIPTION": "This is a description of what the keyword does."
}
```

## Usage Examples

- **Card text**: `"Gain 1 Strength. NL Next turn, gain 1 ${modID}:Hello."`
- **Relic**: `"At the start of each combat, gain #b1 #yStrength and #b1 #y${modID}:Hello."`
- **Power**: `"At the start of each turn, gain #b1 #yStrength and #b1 #yHello."`

## Saving Keyword Text

Adding an `ID` field allows keyword access in your main mod file via `MainModFile.keywords.get("keywordID")`. This enables tooltip creation using `.PROPER_NAME` and `.DESCRIPTION` properties, useful for potion tooltips and similar features.
