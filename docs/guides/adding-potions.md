# Adding Potions - BasicMod Wiki Guide

> Source: https://github.com/Alchyr/BasicMod/wiki/Adding-Potions

## Two-Step Process

Creating potions requires two steps: "(1) Make the potion. (2) Register the potion." The BasicMod framework includes AutoAdd to streamline registration.

## Registration Setup

Potions register through `receivePostInitialize()`. Create a dedicated `registerPotions()` method:

```java
@Override
public void receivePostInitialize() {
    registerPotions();
    // rest of method
}

public static void registerPotions() {
    new AutoAdd(modID)
        .packageFilter(BasePotion.class)
        .any(BasePotion.class, (info, potion) -> {
            BaseMod.addPotion(potion.getClass(), null, null, null,
                            potion.ID, potion.playerClass);
        });
}
```

This automatically discovers and registers all classes extending `BasePotion` in the same package.

## Creating a Potion Class

1. **Create a class in the `potions` package** extending `BasePotion`
2. **Define unique ID**: `public static final String ID = makeID("MyPotion");`
3. **Set colors** using `CardHelper.getColor()` or existing color objects
4. **Implement constructor** specifying ID, potency, rarity, size, and colors
5. **Add character-specific logic** if needed via `playerClass` field

## Constructor Example

```java
private static final Color LIQUID_COLOR = CardHelper.getColor(255, 0, 255);
private static final Color HYBRID_COLOR = CardHelper.getColor(255, 0, 255);
private static final Color SPOTS_COLOR = null; // Not supported by all shapes

public MyPotion() {
    super(ID, 5, PotionRarity.COMMON, PotionSize.MOON,
          LIQUID_COLOR, HYBRID_COLOR, SPOTS_COLOR);
}
```

**Optional additions**:
- Character exclusivity: `playerClass = AbstractPlayer.PlayerClass.IRONCLAD;`
- Lab color: `labOutlineColor = Settings.RED_RELIC_COLOR;`
- Thrown potions: `isThrown = true;` and `targetRequired = true;`

## Required Methods

### `getDescription()`
Retrieve text from `PotionStrings.json` and format with potency values:

```java
@Override
public String getDescription() {
    return DESCRIPTIONS[0] + potency + DESCRIPTIONS[1];
}
```

JSON structure:
```json
"${modID}:MyPotion": {
    "NAME": "Some Kind of Potion",
    "DESCRIPTIONS": ["Gain #b", " #yStrength."]
}
```

### `use(AbstractCreature target)`
Queue actions using `addToBot()`:

```java
@Override
public void use(AbstractCreature target) {
    if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
        addToBot(new ApplyPowerAction(AbstractDungeon.player,
                AbstractDungeon.player,
                new StrengthPower(AbstractDungeon.player, potency),
                potency));
    }
}
```

## Advanced Features

**Tooltips**: Override `addAdditionalTips()` to add keyword references or explanations.

**Custom appearance**: Use `setContainerImg()`, `setLiquidImg()`, `setHybridImg()`, `setSpotsImg()`, and `setOutlineImg()` with 64x64 images. Rendering order: outline → liquid → hybrid → spots → container.

**Sacred Bark compatibility**: Automatically works with potency-based effects; no additional coding needed.
