package CoopBoardGame.targeting;

/**
 * Enumeration of player-targeted effect types.
 * Each type maps to a specific executor in the PlayerEffectRegistry.
 */
public enum PlayerEffectType {

    /**
     * Grants block to the target player.
     * Args: intArgs[0] = block amount
     */
    GAIN_BLOCK,

    /**
     * Heals the target player.
     * Args: intArgs[0] = heal amount
     */
    HEAL,

    /**
     * Removes a specific power from the target player.
     * Args: stringArgs[0] = power ID
     */
    REMOVE_POWER,

    /**
     * Applies a power to the target player.
     * Args: stringArgs[0] = power ID, intArgs[0] = stack amount
     */
    APPLY_POWER,

    /**
     * Swaps combat rows between two players.
     * Target is one player, args specify the other.
     * Args: intArgs[0] = other player ID
     */
    SWAP_ROWS,

    /**
     * Draws cards for the target player.
     * Args: intArgs[0] = number of cards
     */
    DRAW_CARDS,

    /**
     * Grants energy to the target player.
     * Args: intArgs[0] = energy amount
     */
    GAIN_ENERGY,

    /**
     * Deals damage to the target player (for specific effects).
     * Args: intArgs[0] = damage amount
     */
    DEAL_DAMAGE;

    /**
     * Returns true if this effect type requires network synchronization.
     * Some effects (like visual-only) may be local-only.
     */
    public boolean requiresNetworkSync() {
        return true;
    }

    /**
     * Returns true if this effect modifies gameplay state.
     * False for visual-only effects.
     */
    public boolean modifiesGameplayState() {
        return true;
    }

    /**
     * Returns the number of required int arguments for this effect type.
     */
    public int getRequiredIntArgs() {
        switch (this) {
            case GAIN_BLOCK:
            case HEAL:
            case DRAW_CARDS:
            case GAIN_ENERGY:
            case DEAL_DAMAGE:
            case SWAP_ROWS:
                return 1;
            case APPLY_POWER:
                return 1; // stack amount
            case REMOVE_POWER:
                return 0;
            default:
                return 0;
        }
    }

    /**
     * Returns the number of required string arguments for this effect type.
     */
    public int getRequiredStringArgs() {
        switch (this) {
            case REMOVE_POWER:
            case APPLY_POWER:
                return 1; // power ID
            default:
                return 0;
        }
    }
}
