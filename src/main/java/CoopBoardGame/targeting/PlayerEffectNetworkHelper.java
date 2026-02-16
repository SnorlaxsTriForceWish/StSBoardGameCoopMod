package CoopBoardGame.targeting;

import CoopBoardGame.util.TogetherInSpireHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for sending and receiving player effect commands over the network.
 * Follows the same pattern as RowNetworkHelper and VotingNetworkHelper.
 *
 * Authority Model (Phase 3):
 * - Source client sends INTENT to host
 * - Host validates and broadcasts EXECUTE to all clients
 * - Target client applies the gameplay effect
 * - Non-target clients apply optional VFX only
 *
 * Single-player executes immediately with no network path.
 */
public class PlayerEffectNetworkHelper {

    private static final Logger logger = LogManager.getLogger(PlayerEffectNetworkHelper.class.getName());

    // Message type identifiers
    public static final String MSG_PLAYER_EFFECT_INTENT = "BGPlayerEffectIntent";
    public static final String MSG_PLAYER_EFFECT_EXECUTE = "BGPlayerEffectExecute";

    // Cached reflection lookups
    private static Class<?> networkMessageClass = null;
    private static Class<?> p2pManagerClass = null;
    private static Method sendDataMethod = null;
    private static Constructor<?> networkMessageConstructor = null;
    private static boolean initialized = false;

    // Dedupe cache for received commands (separate from registry's cache for network-level dedupe)
    private static final Set<Long> receivedCommandIds = new HashSet<>();
    private static final int MAX_RECEIVED_CACHE_SIZE = 500;

    /**
     * Initialize reflection lookups for TogetherInSpire networking classes.
     */
    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        if (!TogetherInSpireHelper.isTogetherInSpireLoaded()) {
            return false;
        }

        try {
            // Load classes
            networkMessageClass = Class.forName("spireTogether.util.NetworkMessage");
            p2pManagerClass = Class.forName("spireTogether.network.P2P.P2PManager");

            // NetworkMessage constructor: (String request, Object object, Integer senderID)
            networkMessageConstructor = networkMessageClass.getConstructor(
                String.class, Object.class, Integer.class
            );

            // Find P2PManager.SendData method
            for (Method method : p2pManagerClass.getMethods()) {
                if (method.getName().equals("SendData") && method.getParameterCount() == 1) {
                    sendDataMethod = method;
                    break;
                }
            }

            if (sendDataMethod == null) {
                logger.warn("Could not find P2PManager.SendData method");
                return false;
            }

            initialized = true;
            logger.info("PlayerEffectNetworkHelper initialized successfully");
            return true;

        } catch (Exception e) {
            logger.error("Failed to initialize PlayerEffectNetworkHelper: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a player effect intent to the host for validation.
     * In the current implementation, this broadcasts directly to all peers
     * for simplicity. A future iteration may add host-relay validation.
     *
     * @param command the effect command to send
     */
    public static void sendPlayerEffectIntent(PlayerEffectCommand command) {
        if (!initialize()) {
            logger.warn("Cannot send player effect - networking not initialized");
            return;
        }

        if (command == null || !command.isValid()) {
            logger.warn("Cannot send invalid command: " + command);
            return;
        }

        try {
            String payload = serializeCommand(command);
            sendMessage(MSG_PLAYER_EFFECT_INTENT, payload);
            logger.info("Sent player effect intent: " + command.getEffectType() +
                       " to player " + command.getTargetPlayerId() +
                       " (commandId=" + command.getCommandId() + ")");
        } catch (Exception e) {
            logger.error("Failed to send player effect intent: " + e.getMessage());
        }
    }

    /**
     * Sends a validated player effect execute message to all clients.
     * Called by the host after validating an intent.
     *
     * @param command the validated effect command to broadcast
     */
    public static void sendPlayerEffectExecute(PlayerEffectCommand command) {
        if (!initialize()) {
            logger.warn("Cannot send player effect execute - networking not initialized");
            return;
        }

        if (!TogetherInSpireHelper.isHost()) {
            logger.warn("Only host should broadcast execute messages");
            return;
        }

        if (command == null || !command.isValid()) {
            logger.warn("Cannot send invalid command: " + command);
            return;
        }

        try {
            String payload = serializeCommand(command);
            sendMessage(MSG_PLAYER_EFFECT_EXECUTE, payload);
            logger.info("Host sent player effect execute: " + command.getEffectType() +
                       " to player " + command.getTargetPlayerId() +
                       " (commandId=" + command.getCommandId() + ")");
        } catch (Exception e) {
            logger.error("Failed to send player effect execute: " + e.getMessage());
        }
    }

    /**
     * Sends a player effect command for immediate network execution.
     * This is a simplified path that sends directly to all peers.
     * Used when host-relay validation is not required.
     *
     * @param command the effect command to send
     */
    public static void sendPlayerEffect(PlayerEffectCommand command) {
        if (!initialize()) {
            logger.warn("Cannot send player effect - networking not initialized");
            return;
        }

        if (command == null || !command.isValid()) {
            logger.warn("Cannot send invalid command: " + command);
            return;
        }

        try {
            String payload = serializeCommand(command);
            // Use EXECUTE message type for direct send (no host validation)
            sendMessage(MSG_PLAYER_EFFECT_EXECUTE, payload);
            logger.info("Sent player effect: " + command.getEffectType() +
                       " to player " + command.getTargetPlayerId() +
                       " (commandId=" + command.getCommandId() + ")");
        } catch (Exception e) {
            logger.error("Failed to send player effect: " + e.getMessage());
        }
    }

    /**
     * Handles a received player effect intent message.
     * Called when a client receives an INTENT from another player.
     *
     * If we are the host, validate and rebroadcast as EXECUTE.
     * If we are not the host, ignore (host will rebroadcast).
     *
     * @param payload the serialized command payload
     * @param senderId the ID of the player who sent this intent
     */
    public static void onPlayerEffectIntentReceived(String payload, int senderId) {
        if (payload == null || payload.isEmpty()) {
            logger.warn("Received empty player effect intent payload");
            return;
        }

        PlayerEffectCommand command = deserializeCommand(payload);
        if (command == null) {
            logger.warn("Failed to deserialize player effect intent");
            return;
        }

        logger.info("Received player effect intent from player " + senderId +
                   ": " + command.getEffectType() +
                   " targeting player " + command.getTargetPlayerId());

        // Validate sender matches command source
        if (command.getSourcePlayerId() != senderId) {
            logger.warn("Intent rejected: sender ID " + senderId +
                       " does not match command source " + command.getSourcePlayerId());
            return;
        }

        // If we're the host, validate and rebroadcast
        if (TogetherInSpireHelper.isHost()) {
            if (validateCommand(command)) {
                sendPlayerEffectExecute(command);
                // Also execute locally on host
                executeCommandLocally(command);
            } else {
                logger.warn("Host rejected invalid command: " + command);
            }
        }
        // Non-host clients wait for EXECUTE from host
    }

    /**
     * Handles a received player effect execute message.
     * Called when a client receives an EXECUTE (from host or direct peer).
     *
     * @param payload the serialized command payload
     */
    public static void onPlayerEffectExecuteReceived(String payload) {
        if (payload == null || payload.isEmpty()) {
            logger.warn("Received empty player effect execute payload");
            return;
        }

        PlayerEffectCommand command = deserializeCommand(payload);
        if (command == null) {
            logger.warn("Failed to deserialize player effect execute");
            return;
        }

        // Check network-level dedupe (before registry's dedupe)
        if (isReceivedDuplicate(command.getCommandId())) {
            logger.debug("Skipping duplicate network command: " + command.getCommandId());
            return;
        }
        markAsReceived(command.getCommandId());

        logger.info("Received player effect execute: " + command.getEffectType() +
                   " targeting player " + command.getTargetPlayerId() +
                   " (commandId=" + command.getCommandId() + ")");

        executeCommandLocally(command);
    }

    /**
     * Executes a command locally through the PlayerEffectRegistry.
     */
    private static void executeCommandLocally(PlayerEffectCommand command) {
        PlayerEffectRegistry.executeWithAutoContext(command, true);
    }

    /**
     * Validates a command before host rebroadcast.
     *
     * @param command the command to validate
     * @return true if valid
     */
    private static boolean validateCommand(PlayerEffectCommand command) {
        if (command == null || !command.isValid()) {
            return false;
        }

        // Validate target is a valid BG player
        int targetId = command.getTargetPlayerId();
        if (!isValidTargetId(targetId)) {
            logger.warn("Invalid target player ID: " + targetId);
            return false;
        }

        // Effect-specific validation can be added here
        return true;
    }

    /**
     * Checks if a player ID is a valid target.
     */
    private static boolean isValidTargetId(int playerId) {
        // In single-player, only local player (0) is valid
        if (!TogetherInSpireHelper.isMultiplayerActive()) {
            return playerId == TogetherInSpireHelper.getLocalPlayerId();
        }

        // In multiplayer, check if ID is in BG player list
        return TogetherInSpireHelper.getBoardGamePlayerIds().contains(playerId) ||
               playerId == TogetherInSpireHelper.getLocalPlayerId();
    }

    /**
     * Creates and sends a NetworkMessage via P2PManager.SendData.
     */
    private static void sendMessage(String messageType, Object payload) throws Exception {
        Integer senderId = TogetherInSpireHelper.getLocalPlayerId();
        Object message = networkMessageConstructor.newInstance(messageType, payload, senderId);
        sendDataMethod.invoke(null, message);
    }

    /**
     * Serializes a PlayerEffectCommand to a string payload.
     * Format: commandId|effectType|sourceId|targetId|intArg1,intArg2,...|stringArg1,stringArg2,...
     */
    static String serializeCommand(PlayerEffectCommand command) {
        StringBuilder sb = new StringBuilder();

        // commandId (long)
        sb.append(command.getCommandId()).append("|");

        // effectType (enum name)
        sb.append(command.getEffectType().name()).append("|");

        // sourcePlayerId
        sb.append(command.getSourcePlayerId()).append("|");

        // targetPlayerId
        sb.append(command.getTargetPlayerId()).append("|");

        // intArgs (comma-separated)
        int[] intArgs = command.getIntArgs();
        for (int i = 0; i < intArgs.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(intArgs[i]);
        }
        sb.append("|");

        // stringArgs (comma-separated, with escaping)
        String[] stringArgs = command.getStringArgs();
        for (int i = 0; i < stringArgs.length; i++) {
            if (i > 0) sb.append(",");
            // Escape pipe and comma characters
            sb.append(escapeString(stringArgs[i]));
        }

        return sb.toString();
    }

    /**
     * Deserializes a string payload to a PlayerEffectCommand.
     */
    static PlayerEffectCommand deserializeCommand(String payload) {
        try {
            String[] parts = payload.split("\\|", -1); // -1 to keep empty trailing parts
            if (parts.length < 5) {
                logger.warn("Invalid payload format: expected 6 parts, got " + parts.length);
                return null;
            }

            // Parse fields
            long commandId = Long.parseLong(parts[0]);
            PlayerEffectType effectType = PlayerEffectType.valueOf(parts[1]);
            int sourceId = Integer.parseInt(parts[2]);
            int targetId = Integer.parseInt(parts[3]);

            // Parse intArgs
            int[] intArgs;
            if (parts[4].isEmpty()) {
                intArgs = new int[0];
            } else {
                String[] intParts = parts[4].split(",");
                intArgs = new int[intParts.length];
                for (int i = 0; i < intParts.length; i++) {
                    intArgs[i] = Integer.parseInt(intParts[i]);
                }
            }

            // Parse stringArgs
            String[] stringArgs;
            if (parts.length < 6 || parts[5].isEmpty()) {
                stringArgs = new String[0];
            } else {
                String[] stringParts = parts[5].split(",");
                stringArgs = new String[stringParts.length];
                for (int i = 0; i < stringParts.length; i++) {
                    stringArgs[i] = unescapeString(stringParts[i]);
                }
            }

            return new PlayerEffectCommand(commandId, effectType, sourceId, targetId, intArgs, stringArgs);

        } catch (Exception e) {
            logger.error("Failed to deserialize command: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escapes special characters in string arguments for serialization.
     */
    private static String escapeString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace(",", "\\c");
    }

    /**
     * Unescapes special characters in string arguments after deserialization.
     */
    private static String unescapeString(String s) {
        if (s == null) return "";
        return s.replace("\\c", ",")
                .replace("\\p", "|")
                .replace("\\\\", "\\");
    }

    /**
     * Checks if a command ID has already been received at the network level.
     */
    private static boolean isReceivedDuplicate(long commandId) {
        return receivedCommandIds.contains(commandId);
    }

    /**
     * Marks a command ID as received at the network level.
     */
    private static void markAsReceived(long commandId) {
        if (receivedCommandIds.size() >= MAX_RECEIVED_CACHE_SIZE) {
            receivedCommandIds.clear();
            logger.debug("Cleared network received command cache (reached max size)");
        }
        receivedCommandIds.add(commandId);
    }

    /**
     * Resets the network helper state.
     * Should be called at combat end or when leaving multiplayer.
     */
    public static void reset() {
        receivedCommandIds.clear();
        logger.debug("PlayerEffectNetworkHelper reset");
    }

    /**
     * Returns true if the network helper is initialized and ready.
     */
    public static boolean isReady() {
        return initialized;
    }
}
