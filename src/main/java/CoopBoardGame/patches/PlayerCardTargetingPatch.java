package CoopBoardGame.patches;

import CoopBoardGame.targeting.PlayerTargetContext;
import CoopBoardGame.targeting.PlayerTargetingArrow;
import CoopBoardGame.targeting.PlayerTargetingSession;
import CoopBoardGame.targeting.PlayerTargetRef;
import CoopBoardGame.targeting.PlayerTargetResolver;
import CoopBoardGame.targeting.PlayerTargetResolver.TargetFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Patches for player-targeted card arrow targeting.
 *
 * Intercepts input handling to support player-targeting cards that use
 * CardTargetPatch.PLAYER as their target type.
 */
public class PlayerCardTargetingPatch {

    private static final Logger logger = LogManager.getLogger(PlayerCardTargetingPatch.class.getName());

    /**
     * Patch to detect when a player-targeting card is being dragged.
     * Starts the targeting session when the card is dragged high enough.
     */
    @SpirePatch2(clz = AbstractPlayer.class, method = "updateInput")
    public static class CardDragDetectionPatch {

        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance) {
            AbstractCard hoveredCard = __instance.hoveredCard;
            PlayerTargetingSession session = PlayerTargetingSession.getInstance();

            // Check if we should start a new targeting session
            if (hoveredCard != null &&
                    PlayerTargetingSession.isPlayerTargetCard(hoveredCard) &&
                    __instance.isDraggingCard &&
                    hoveredCard.current_y > __instance.hb.cY - 60f) {

                // Start session if not already active or if card changed
                if (!session.isActive() || session.getSourceCard() != hoveredCard) {
                    session.start(hoveredCard, TargetFilter.selfAndAllies());
                    logger.debug("Started player targeting session for: " + hoveredCard.cardID);
                }
            }

            // End session if card is no longer being dragged
            if (session.isActive()) {
                if (hoveredCard == null ||
                        !__instance.isDraggingCard ||
                        !PlayerTargetingSession.isPlayerTargetCard(hoveredCard)) {
                    // Don't end if we're about to use the card
                    if (!isAboutToPlayCard()) {
                        session.end();
                        PlayerTargetingArrow.reset();
                    }
                }
            }
        }

        private static boolean isAboutToPlayCard() {
            return InputHelper.justReleasedClickLeft;
        }
    }

    /**
     * Patch to handle player targeting input when a PLAYER card is hovered.
     * Intercepts the updateInput method to handle player selection.
     */
    @SpirePatch2(clz = AbstractPlayer.class, method = "updateInput")
    public static class UpdateInputPatch {

        @SpireInsertPatch(locator = Locator.class)
        public static SpireReturn<Void> Insert(AbstractPlayer __instance) {
            PlayerTargetingSession session = PlayerTargetingSession.getInstance();

            // Only intercept if we have an active player targeting session
            if (!session.isActive()) {
                return SpireReturn.Continue();
            }

            AbstractCard hoveredCard = session.getSourceCard();
            if (hoveredCard == null) {
                return SpireReturn.Continue();
            }

            // Update arrow position and find hovered player
            PlayerTargetingArrow.update();

            // Fix card position at the bottom of the screen (same as vanilla attack targeting)
            // This prevents the card from following the mouse cursor
            float targetX = Settings.WIDTH / 2.0f;
            float targetY = Settings.HEIGHT * 0.25f;
            hoveredCard.target_x = targetX;
            hoveredCard.target_y = targetY;
            hoveredCard.current_x = targetX;
            hoveredCard.current_y = targetY;
            hoveredCard.drawScale = 0.6f;

            // Handle click release - play the card on the selected player
            if (InputHelper.justReleasedClickLeft) {
                int hoveredPlayerId = session.getHoveredPlayerId();

                if (hoveredPlayerId >= 0) {
                    // Valid player target - use the card
                    PlayerTargetRef targetRef = PlayerTargetResolver.findById(hoveredPlayerId);
                    if (targetRef != null) {
                        logger.info("Playing card " + hoveredCard.cardID + " on player " + hoveredPlayerId);

                        // Set target context and use the card
                        try {
                            PlayerTargetContext.setCurrentTarget(hoveredPlayerId);
                            __instance.useCard(hoveredCard, null, hoveredCard.energyOnUse);
                            CardCrawlGame.sound.play("CARD_OBTAIN");
                        } finally {
                            PlayerTargetContext.clear();
                        }

                        // Clean up targeting state
                        session.end();
                        PlayerTargetingArrow.reset();
                        __instance.releaseCard();

                        return SpireReturn.Return();
                    }
                }

                // No valid target - release the card back to hand
                logger.debug("No valid player target, releasing card");
                session.end();
                PlayerTargetingArrow.reset();
                __instance.releaseCard();
                return SpireReturn.Return();
            }

            // Handle right click - cancel targeting
            if (InputHelper.justClickedRight) {
                logger.debug("Player targeting cancelled");
                session.end();
                PlayerTargetingArrow.reset();
                __instance.releaseCard();
                return SpireReturn.Return();
            }

            // Continue holding the card in targeting mode
            return SpireReturn.Continue();
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                        AbstractPlayer.class,
                        "isDraggingCard"
                );
                return LineFinder.findInOrder(ctMethodToPatch, new ArrayList<>(), finalMatcher);
            }
        }
    }

    /**
     * Patch to render the player targeting arrow during hand rendering.
     */
    @SpirePatch2(clz = AbstractPlayer.class, method = "renderHand", paramtypez = {SpriteBatch.class})
    public static class RenderHandPatch {

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, SpriteBatch sb) {
            PlayerTargetingSession session = PlayerTargetingSession.getInstance();

            if (session.isActive()) {
                PlayerTargetingArrow.render(sb);
            }
        }
    }

    /**
     * Patch to clean up targeting session on combat end.
     */
    @SpirePatch2(clz = AbstractRoom.class, method = "endBattle")
    public static class EndBattlePatch {

        @SpirePrefixPatch
        public static void Prefix(AbstractRoom __instance) {
            PlayerTargetingSession.reset();
            PlayerTargetingArrow.reset();
            logger.debug("Player targeting session reset on combat end");
        }
    }

    /**
     * Patch to prevent vanilla single-target input handling for PLAYER cards.
     * This ensures the vanilla monster targeting doesn't interfere.
     */
    @SpirePatch2(clz = AbstractPlayer.class, method = "updateSingleTargetInput")
    public static class UpdateSingleTargetInputPatch {

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance) {
            // If we have an active player targeting session, skip vanilla targeting
            PlayerTargetingSession session = PlayerTargetingSession.getInstance();
            if (session.isActive()) {
                return SpireReturn.Return();
            }

            // Also check if the currently hovered card is a PLAYER card
            if (__instance.hoveredCard != null &&
                    PlayerTargetingSession.isPlayerTargetCard(__instance.hoveredCard)) {
                // Start session if dragging high enough
                if (__instance.isDraggingCard &&
                        __instance.hoveredCard.current_y > __instance.hb.cY - 60f) {
                    session.start(__instance.hoveredCard, TargetFilter.selfAndAllies());
                }
                return SpireReturn.Return();
            }

            return SpireReturn.Continue();
        }
    }
}
