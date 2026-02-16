package CoopBoardGame.actions;

import CoopBoardGame.screen.PlayerTargetScreen;
import CoopBoardGame.targeting.PlayerTargetRef;
import CoopBoardGame.targeting.PlayerTargetResolver;
import CoopBoardGame.targeting.PlayerTargetResolver.TargetFilter;
import basemod.BaseMod;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Action that opens the PlayerTargetScreen and executes a callback when a player is selected.
 *
 * Usage pattern:
 * <pre>
 * addToBot(new TargetPlayerAction(
 *     "Choose a player to grant block.",
 *     TargetFilter.selfAndAllies(),
 *     targetId -> {
 *         addToTop(new GainBlockOnPlayerAction(targetId, blockAmount));
 *     },
 *     null // no cancel callback
 * ));
 * </pre>
 */
public class TargetPlayerAction extends AbstractGameAction {

    private static final Logger logger = LogManager.getLogger(TargetPlayerAction.class.getName());

    private final String instruction;
    private final TargetFilter filter;
    private final Consumer<Integer> onPlayerSelected;
    private final Runnable onCancel;

    private boolean screenOpened = false;

    /**
     * Creates a new TargetPlayerAction with full configuration.
     *
     * @param instruction text displayed to the user
     * @param filter targeting filter configuration
     * @param onPlayerSelected callback when a player is selected (receives player ID)
     * @param onCancel optional callback when selection is cancelled
     */
    public TargetPlayerAction(String instruction, TargetFilter filter, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        this.instruction = instruction != null ? instruction : "Choose a player.";
        this.filter = filter != null ? filter : TargetFilter.defaultFilter();
        this.onPlayerSelected = onPlayerSelected;
        this.onCancel = onCancel;
        this.actionType = ActionType.SPECIAL;
    }

    /**
     * Creates a TargetPlayerAction with default filter (allow self, BG only).
     */
    public TargetPlayerAction(String instruction, Consumer<Integer> onPlayerSelected) {
        this(instruction, TargetFilter.defaultFilter(), onPlayerSelected, null);
    }

    /**
     * Creates a TargetPlayerAction with allies only filter.
     */
    public static TargetPlayerAction alliesOnly(String instruction, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        return new TargetPlayerAction(instruction, TargetFilter.alliesOnly(), onPlayerSelected, onCancel);
    }

    /**
     * Creates a TargetPlayerAction with self and allies filter.
     */
    public static TargetPlayerAction selfAndAllies(String instruction, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        return new TargetPlayerAction(instruction, TargetFilter.selfAndAllies(), onPlayerSelected, onCancel);
    }

    /**
     * Creates a TargetPlayerAction with a custom predicate.
     */
    public static TargetPlayerAction withPredicate(String instruction, Predicate<PlayerTargetRef> predicate, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        TargetFilter customFilter = TargetFilter.custom(true, true, predicate);
        return new TargetPlayerAction(instruction, customFilter, onPlayerSelected, onCancel);
    }

    @Override
    public void update() {
        if (!screenOpened) {
            screenOpened = true;

            // Check for single target auto-confirm
            PlayerTargetRef singleTarget = PlayerTargetResolver.getSingleTargetOrNull(filter);
            if (singleTarget != null) {
                logger.info("Auto-confirming single player target: " + singleTarget.getPlayerId());
                if (onPlayerSelected != null) {
                    onPlayerSelected.accept(singleTarget.getPlayerId());
                }
                isDone = true;
                return;
            }

            // Check for no valid targets
            if (PlayerTargetResolver.getTargetablePlayerCount(filter) == 0) {
                logger.warn("No valid player targets available");
                if (onCancel != null) {
                    onCancel.run();
                }
                isDone = true;
                return;
            }

            // Open the screen
            PlayerTargetScreen screen = (PlayerTargetScreen) BaseMod.getCustomScreen(
                    PlayerTargetScreen.Enum.PLAYER_TARGET
            );

            if (screen != null) {
                screen.open(instruction, filter, targetId -> {
                    if (onPlayerSelected != null) {
                        onPlayerSelected.accept(targetId);
                    }
                }, () -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });
            } else {
                logger.error("PlayerTargetScreen not registered!");
                if (onCancel != null) {
                    onCancel.run();
                }
                isDone = true;
            }
        }

        // Check if screen is done
        if (screenOpened) {
            if (AbstractDungeon.screen != PlayerTargetScreen.Enum.PLAYER_TARGET) {
                isDone = true;
            }
        }
    }
}
