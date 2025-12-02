# Action Queue - Slay the Spire Modding Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Action-Queue

## Overview
"Slay the Spire's combat works based on an action queue." Actions are processed sequentially, with new actions added either to the bottom (end) or top (beginning) of the queue.

## How It Works

**Basic Flow:**
When a card is played, it queues multiple actions in order:
1. Card-specific actions (damage, apply effects)
2. Card movement action (to discard/exhaust pile)

These execute sequentially before the next queue item processes.

## Adding to Queue

Two methods exist for queue management:

**Add to Bottom:** Places actions at the queue's end, executing after existing actions. Used primarily in card `use` methods.

**Add to Top:** Places actions at the queue's front, executing immediately. Used in action `update` methods and certain power/relic triggers.

## General Guidelines

The documentation provides this guidance:
- "In a card's use: add to bottom. In an action's update: add to top."
- When uncertain, default to bottom
- Powers and relics typically use bottom, with exceptions like Thorns (which uses top to ensure damage applies before subsequent hits)

## Best Practice

"Look for an example of something similar first if you're not sure, and if you really have no idea, try asking in #modding-technical."

The queue system ensures predictable combat sequencing and proper trigger order.
