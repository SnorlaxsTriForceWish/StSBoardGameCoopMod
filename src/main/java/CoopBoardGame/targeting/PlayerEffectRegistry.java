package CoopBoardGame.targeting;

import CoopBoardGame.util.TogetherInSpireHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry for player effect executors.
 * Each effect type maps to a specific executor that handles the gameplay logic.
 */
public class PlayerEffectRegistry {

    private static final Logger logger = LogManager.getLogger(PlayerEffectRegistry.class.getName());

    /**
     * Context provided to executors during effect execution.
     */
    public static class ExecutionContext {
        private final boolean isLocalTarget;
        private final boolean isNetworkExecute;
        private final PlayerTargetRef targetRef;

        public ExecutionContext(boolean isLocalTarget, boolean isNetworkExecute, PlayerTargetRef targetRef) {
            this.isLocalTarget = isLocalTarget;
            this.isNetworkExecute = isNetworkExecute;
            this.targetRef = targetRef;
        }

        /**
         * Returns true if the local player is the target.
         * Only the local target should apply gameplay state changes.
         */
        public boolean isLocalTarget() {
            return isLocalTarget;
        }

        /**
         * Returns true if this execution came from a network EXECUTE message.
         * False for direct local execution in single-player.
         */
        public boolean isNetworkExecute() {
            return isNetworkExecute;
        }

        /**
         * Gets the resolved target reference.
         * May be null if target could not be resolved.
         */
        public PlayerTargetRef getTargetRef() {
            return targetRef;
        }

        /**
         * Returns true if this execution should apply visual effects only.
         * Non-target clients should only show VFX, not modify state.
         */
        public boolean isVisualOnly() {
            return isNetworkExecute && !isLocalTarget;
        }

        /**
         * Creates context for local single-player execution.
         */
        public static ExecutionContext localSinglePlayer(PlayerTargetRef targetRef) {
            return new ExecutionContext(true, false, targetRef);
        }

        /**
         * Creates context for network execution where local player is target.
         */
        public static ExecutionContext networkLocalTarget(PlayerTargetRef targetRef) {
            return new ExecutionContext(true, true, targetRef);
        }

        /**
         * Creates context for network execution where local player is NOT target.
         */
        public static ExecutionContext networkRemoteTarget(PlayerTargetRef targetRef) {
            return new ExecutionContext(false, true, targetRef);
        }
    }

    /**
     * Interface for effect executors.
     * Implementations handle the specific gameplay logic for each effect type.
     */
    @FunctionalInterface
    public interface PlayerEffectExecutor {
        /**
         * Executes the player effect.
         *
         * @param command the effect command to execute
         * @param context execution context with target info
         */
        void execute(PlayerEffectCommand command, ExecutionContext context);
    }

    private static final Map<PlayerEffectType, PlayerEffectExecutor> executors = new HashMap<>();
    private static final Set<Long> processedCommandIds = new HashSet<>();
    private static final int MAX_PROCESSED_CACHE_SIZE = 1000;

    /**
     * Registers an executor for an effect type.
     *
     * @param type the effect type
     * @param executor the executor to handle this type
     */
    public static void register(PlayerEffectType type, PlayerEffectExecutor executor) {
        if (type == null || executor == null) {
            throw new IllegalArgumentException("Effect type and executor must not be null");
        }
        executors.put(type, executor);
        logger.info("Registered executor for player effect type: " + type);
    }

    /**
     * Checks if an executor is registered for the given effect type.
     */
    public static boolean hasExecutor(PlayerEffectType type) {
        return executors.containsKey(type);
    }

    /**
     * Executes a player effect command.
     *
     * @param command the command to execute
     * @param context the execution context
     * @return true if the command was executed, false if skipped (duplicate or no executor)
     */
    public static boolean execute(PlayerEffectCommand command, ExecutionContext context) {
        if (command == null) {
            logger.warn("Attempted to execute null command");
            return false;
        }

        // Validate command
        if (!command.isValid()) {
            logger.warn("Invalid command rejected: " + command);
            return false;
        }

        // Check for duplicate
        if (isDuplicate(command.getCommandId())) {
            logger.debug("Skipping duplicate command: " + command.getCommandId());
            return false;
        }

        // Find executor
        PlayerEffectExecutor executor = executors.get(command.getEffectType());
        if (executor == null) {
            logger.warn("No executor registered for effect type: " + command.getEffectType());
            return false;
        }

        // Mark as processed
        markProcessed(command.getCommandId());

        // Execute
        try {
            logger.info("Executing player effect: " + command);
            executor.execute(command, context);
            return true;
        } catch (Exception e) {
            logger.error("Error executing player effect: " + command, e);
            return false;
        }
    }

    /**
     * Executes a command with automatic context resolution.
     * Determines if the local player is the target and creates appropriate context.
     *
     * @param command the command to execute
     * @param isNetworkExecute true if this came from a network message
     * @return true if executed
     */
    public static boolean executeWithAutoContext(PlayerEffectCommand command, boolean isNetworkExecute) {
        if (command == null) {
            return false;
        }

        int localPlayerId = TogetherInSpireHelper.getLocalPlayerId();
        boolean isLocalTarget = command.getTargetPlayerId() == localPlayerId;
        PlayerTargetRef targetRef = PlayerTargetResolver.findById(command.getTargetPlayerId());

        ExecutionContext context;
        if (isNetworkExecute) {
            context = isLocalTarget
                    ? ExecutionContext.networkLocalTarget(targetRef)
                    : ExecutionContext.networkRemoteTarget(targetRef);
        } else {
            context = ExecutionContext.localSinglePlayer(targetRef);
        }

        return execute(command, context);
    }

    /**
     * Checks if a command ID has already been processed.
     */
    public static boolean isDuplicate(long commandId) {
        return processedCommandIds.contains(commandId);
    }

    /**
     * Marks a command ID as processed.
     */
    private static void markProcessed(long commandId) {
        // Prevent unbounded growth
        if (processedCommandIds.size() >= MAX_PROCESSED_CACHE_SIZE) {
            processedCommandIds.clear();
            logger.debug("Cleared processed command cache (reached max size)");
        }
        processedCommandIds.add(commandId);
    }

    /**
     * Clears the processed command cache.
     * Should be called at combat end or when leaving multiplayer.
     */
    public static void clearProcessedCache() {
        processedCommandIds.clear();
    }

    /**
     * Resets the registry (for testing or reinitialization).
     */
    public static void reset() {
        executors.clear();
        processedCommandIds.clear();
        logger.info("PlayerEffectRegistry reset");
    }
}
