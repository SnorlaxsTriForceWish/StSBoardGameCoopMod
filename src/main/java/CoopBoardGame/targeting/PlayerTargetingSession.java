package CoopBoardGame.targeting;

import CoopBoardGame.patches.CardTargetPatch;
import CoopBoardGame.targeting.PlayerTargetResolver.TargetFilter;
import com.megacrit.cardcrawl.cards.AbstractCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

/**
 * Shared session state for player arrow targeting mode.
 * Tracks the active targeting session, current hover state, and configuration.
 *
 * This avoids duplicate state in patches and render classes.
 */
public class PlayerTargetingSession {

    private static final Logger logger = LogManager.getLogger(PlayerTargetingSession.class.getName());

    // Singleton instance
    private static PlayerTargetingSession instance = null;

    // Session state
    private boolean active = false;
    private AbstractCard sourceCard = null;
    private int hoveredPlayerId = -1;
    private TargetFilter filter = null;
    private boolean allowSelf = true;

    private PlayerTargetingSession() {
    }

    /**
     * Gets the singleton instance.
     */
    public static PlayerTargetingSession getInstance() {
        if (instance == null) {
            instance = new PlayerTargetingSession();
        }
        return instance;
    }

    /**
     * Starts a new player targeting session for a card.
     *
     * @param card the card being used
     * @param allowSelf whether self-targeting is allowed
     */
    public void start(AbstractCard card, boolean allowSelf) {
        this.active = true;
        this.sourceCard = card;
        this.hoveredPlayerId = -1;
        this.allowSelf = allowSelf;
        this.filter = allowSelf ? TargetFilter.selfAndAllies() : TargetFilter.alliesOnly();

        logger.debug("Player targeting session started for card: " + card.cardID);
    }

    /**
     * Starts a new player targeting session with a custom filter.
     *
     * @param card the card being used
     * @param filter custom targeting filter
     */
    public void start(AbstractCard card, TargetFilter filter) {
        this.active = true;
        this.sourceCard = card;
        this.hoveredPlayerId = -1;
        this.filter = filter;
        this.allowSelf = filter.isAllowSelf();

        logger.debug("Player targeting session started for card: " + card.cardID + " with custom filter");
    }

    /**
     * Ends the current targeting session.
     */
    public void end() {
        if (active) {
            logger.debug("Player targeting session ended" +
                    (sourceCard != null ? " for card: " + sourceCard.cardID : ""));
        }

        this.active = false;
        this.sourceCard = null;
        this.hoveredPlayerId = -1;
        this.filter = null;
        this.allowSelf = true;
    }

    /**
     * Returns true if a player targeting session is currently active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the card that initiated the targeting session.
     */
    public AbstractCard getSourceCard() {
        return sourceCard;
    }

    /**
     * Gets the currently hovered player ID, or -1 if none.
     */
    public int getHoveredPlayerId() {
        return hoveredPlayerId;
    }

    /**
     * Sets the currently hovered player ID.
     */
    public void setHoveredPlayerId(int playerId) {
        this.hoveredPlayerId = playerId;
    }

    /**
     * Returns true if a valid player is currently being hovered.
     */
    public boolean hasValidHover() {
        return hoveredPlayerId >= 0;
    }

    /**
     * Gets the PlayerTargetRef for the currently hovered player.
     */
    public PlayerTargetRef getHoveredPlayerRef() {
        if (hoveredPlayerId < 0) {
            return null;
        }
        return PlayerTargetResolver.findById(hoveredPlayerId);
    }

    /**
     * Gets the targeting filter for this session.
     */
    public TargetFilter getFilter() {
        return filter;
    }

    /**
     * Returns true if self-targeting is allowed.
     */
    public boolean isAllowSelf() {
        return allowSelf;
    }

    /**
     * Checks if a card is a player-targeting card.
     */
    public static boolean isPlayerTargetCard(AbstractCard card) {
        return card != null && card.target == CardTargetPatch.PLAYER;
    }

    /**
     * Checks if a card is an all-players targeting card.
     */
    public static boolean isAllPlayersTargetCard(AbstractCard card) {
        return card != null && card.target == CardTargetPatch.ALL_PLAYERS;
    }

    /**
     * Returns true if the given card should use player arrow targeting.
     */
    public static boolean shouldUsePlayerTargeting(AbstractCard card) {
        return isPlayerTargetCard(card) || isAllPlayersTargetCard(card);
    }

    /**
     * Clears any active session. Call on combat end.
     */
    public static void reset() {
        if (instance != null) {
            instance.end();
        }
    }
}
