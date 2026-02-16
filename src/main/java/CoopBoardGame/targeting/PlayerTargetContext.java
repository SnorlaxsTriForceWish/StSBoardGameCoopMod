package CoopBoardGame.targeting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.OptionalInt;

/**
 * Thread-local context for player targeting during card use.
 *
 * This context is set immediately before useCard() and cleared in a finally block.
 * It provides access to the selected player target ID during card execution.
 *
 * Usage pattern:
 * <pre>
 * try {
 *     PlayerTargetContext.setCurrentTarget(targetPlayerId);
 *     player.useCard(card, null, energy);
 * } finally {
 *     PlayerTargetContext.clear();
 * }
 * </pre>
 */
public class PlayerTargetContext {

    private static final Logger logger = LogManager.getLogger(PlayerTargetContext.class.getName());

    private static final ThreadLocal<Integer> currentTarget = new ThreadLocal<>();

    /**
     * Sets the current player target for the duration of card use.
     *
     * @param playerId the target player's ID
     */
    public static void setCurrentTarget(int playerId) {
        currentTarget.set(playerId);
        logger.debug("PlayerTargetContext set to player ID: " + playerId);
    }

    /**
     * Gets the current player target if set.
     *
     * @return OptionalInt containing the target player ID, or empty if not set
     */
    public static OptionalInt getCurrentTarget() {
        Integer target = currentTarget.get();
        if (target == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(target);
    }

    /**
     * Gets the current player target, throwing if not set.
     * Use this in card implementations that require a player target.
     *
     * @return the target player ID
     * @throws IllegalStateException if no target is set
     */
    public static int requireCurrentTargetOrThrow() {
        Integer target = currentTarget.get();
        if (target == null) {
            throw new IllegalStateException("PlayerTargetContext.requireCurrentTargetOrThrow() called but no target is set. " +
                    "Ensure the card is being used through the player targeting system.");
        }
        return target;
    }

    /**
     * Gets the current player target, returning a default if not set.
     *
     * @param defaultValue the value to return if no target is set
     * @return the target player ID or the default value
     */
    public static int getCurrentTargetOrDefault(int defaultValue) {
        Integer target = currentTarget.get();
        return target != null ? target : defaultValue;
    }

    /**
     * Returns true if a player target is currently set.
     */
    public static boolean hasTarget() {
        return currentTarget.get() != null;
    }

    /**
     * Clears the current player target context.
     * Must be called after card use completes (typically in a finally block).
     */
    public static void clear() {
        Integer previous = currentTarget.get();
        currentTarget.remove();
        if (previous != null) {
            logger.debug("PlayerTargetContext cleared (was player ID: " + previous + ")");
        }
    }

    /**
     * Executes an action with a temporary target context, ensuring cleanup.
     *
     * @param targetPlayerId the target player ID
     * @param action the action to execute
     */
    public static void withTarget(int targetPlayerId, Runnable action) {
        try {
            setCurrentTarget(targetPlayerId);
            action.run();
        } finally {
            clear();
        }
    }
}
