# Events - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Events

## Overview
The wiki explains how to create custom events in Slay the Spire mods using BaseMod's `PhasedEvent` framework. Two approaches exist: extending base game event classes or using `PhasedEvent`, which provides a cleaner phase-based structure.

## Class Setup
Events require:
- A dedicated package (typically `events`)
- Extension of `PhasedEvent`
- Static constants for ID, text strings, and image paths
- A constructor calling `super(ID, NAME, IMG)`

Example ID pattern: `makeID("ExampleEvent")` maps to `${modID}:ExampleEvent` in localization files.

## Phase System
`PhasedEvent` uses "phases" instead of method overriding. Each phase represents one interaction screen. Key concepts:

**TextPhase** displays text with button options. Phases are registered via:
```
registerPhase("key", new TextPhase(text))
```

Options are added with `.addOption()` and can include:
- Conditional enabling based on player state
- Custom result handlers executing actions
- Transitions to other phases via `transitionKey()`

## Example Structure
The tutorial demonstrates a "Rude Merchant" event offering a random relic for 70 gold:
- Start phase with two options (buy/decline)
- Conditional logic preventing purchase without sufficient gold
- Result handlers spawning relics and logging metrics
- Conclusion phases returning to map

## Event Text
Text is defined in `EventStrings.json`:
```
"NAME": display name
"DESCRIPTIONS": array for body text per phase
"OPTIONS": array for button labels
```

Arrays must match code indices exactly.

## Registration
Events register in `receivePostInitialize()`:
```
BaseMod.addEvent(ExampleEvent.ID, ExampleEvent.class);
```

Optional spawn conditions available via BaseMod documentation.

## Complexity Note
The wiki notes events are more complicated than cards/relics and references a complex example from PackmasterCharacter for advanced implementations.
