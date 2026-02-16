package CoopBoardGame.multiplayer.patches;

import CoopBoardGame.targeting.PlayerEffectNetworkHelper;
import CoopBoardGame.util.TogetherInSpireHelper;
import com.evacipated.cardcrawl.modthespire.lib.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Patches TogetherInSpire's message analyzer to intercept player effect messages.
 * This allows clients to receive and process player effect commands for
 * cooperative gameplay in board game multiplayer mode.
 */
public class PlayerEffectMessagePatch {

    private static final Logger logger = LogManager.getLogger(PlayerEffectMessagePatch.class.getName());

    /**
     * Patch to intercept incoming network messages and handle player effect ones.
     *
     * We patch P2PMessageAnalyzer.AnalyzeMessage to check for our custom message types
     * before they're processed by the normal TogetherInSpire handlers.
     */
    @SpirePatch(
        cls = "spireTogether.network.P2P.P2PMessageAnalyzer",
        method = "AnalyzeMessage",
        requiredModId = "spireTogether"
    )
    public static class InterceptPlayerEffectMessagesPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Object msg) {
            // Only process in board game multiplayer mode
            if (!TogetherInSpireHelper.isMultiplayerBoardGameMode()) {
                return SpireReturn.Continue();
            }

            try {
                // Get the message fields
                Class<?> messageClass = msg.getClass();

                Field requestField = messageClass.getDeclaredField("request");
                requestField.setAccessible(true);
                String request = (String) requestField.get(msg);

                if (request == null) {
                    return SpireReturn.Continue();
                }

                // Handle player effect messages
                switch (request) {
                    case PlayerEffectNetworkHelper.MSG_PLAYER_EFFECT_INTENT:
                        handlePlayerEffectIntent(msg);
                        return SpireReturn.Return(); // We handled it, skip normal processing

                    case PlayerEffectNetworkHelper.MSG_PLAYER_EFFECT_EXECUTE:
                        handlePlayerEffectExecute(msg);
                        return SpireReturn.Return();

                    default:
                        // Not our message, let TogetherInSpire handle it
                        return SpireReturn.Continue();
                }

            } catch (Exception e) {
                logger.debug("Error checking message type: " + e.getMessage());
                return SpireReturn.Continue();
            }
        }

        /**
         * Handles a player effect intent message.
         * Intent messages are sent by clients to request an effect.
         * The host validates and rebroadcasts as execute.
         */
        private static void handlePlayerEffectIntent(Object message) {
            try {
                Class<?> messageClass = message.getClass();

                // Get the payload
                Field objectField = messageClass.getDeclaredField("object");
                objectField.setAccessible(true);
                Object payload = objectField.get(message);

                // Get the sender ID
                Field senderIdField = messageClass.getDeclaredField("senderID");
                senderIdField.setAccessible(true);
                Object senderIdObj = senderIdField.get(message);
                int senderId = (senderIdObj instanceof Integer) ? (Integer) senderIdObj : -1;

                if (payload instanceof String) {
                    String data = (String) payload;
                    logger.info("Received player effect intent from player " + senderId);
                    PlayerEffectNetworkHelper.onPlayerEffectIntentReceived(data, senderId);
                } else {
                    logger.warn("Unexpected player effect intent payload type: " +
                            (payload != null ? payload.getClass().getName() : "null"));
                }
            } catch (Exception e) {
                logger.error("Error handling player effect intent message: " + e.getMessage());
            }
        }

        /**
         * Handles a player effect execute message.
         * Execute messages are authoritative commands to apply an effect.
         */
        private static void handlePlayerEffectExecute(Object message) {
            try {
                Class<?> messageClass = message.getClass();

                // Get the payload
                Field objectField = messageClass.getDeclaredField("object");
                objectField.setAccessible(true);
                Object payload = objectField.get(message);

                if (payload instanceof String) {
                    String data = (String) payload;
                    logger.info("Received player effect execute");
                    PlayerEffectNetworkHelper.onPlayerEffectExecuteReceived(data);
                } else {
                    logger.warn("Unexpected player effect execute payload type: " +
                            (payload != null ? payload.getClass().getName() : "null"));
                }
            } catch (Exception e) {
                logger.error("Error handling player effect execute message: " + e.getMessage());
            }
        }
    }
}
