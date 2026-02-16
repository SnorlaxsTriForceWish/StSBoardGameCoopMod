package CoopBoardGame.actions.player;

import CoopBoardGame.targeting.PlayerEffectCommand;
import CoopBoardGame.targeting.PlayerEffectRegistry;
import CoopBoardGame.targeting.PlayerEffectType;
import CoopBoardGame.targeting.PlayerTargetRef;
import CoopBoardGame.targeting.PlayerTargetResolver;
import CoopBoardGame.util.TogetherInSpireHelper;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Action that grants block to a specific player.
 * Works for both local player and remote players in multiplayer.
 *
 * In single-player or when targeting local player, executes directly.
 * In multiplayer targeting remote player, routes through the network layer (Phase 3).
 * For Phase 2 (local-only), this always executes directly.
 */
public class GainBlockOnPlayerAction extends AbstractGameAction {

    private static final Logger logger = LogManager.getLogger(GainBlockOnPlayerAction.class.getName());

    private final int targetPlayerId;
    private final int blockAmount;

    /**
     * Creates a new GainBlockOnPlayerAction.
     *
     * @param targetPlayerId the player ID to grant block to
     * @param blockAmount the amount of block to grant
     */
    public GainBlockOnPlayerAction(int targetPlayerId, int blockAmount) {
        this.targetPlayerId = targetPlayerId;
        this.blockAmount = blockAmount;
        this.actionType = ActionType.BLOCK;
    }

    @Override
    public void update() {
        int localPlayerId = TogetherInSpireHelper.getLocalPlayerId();
        PlayerTargetRef targetRef = PlayerTargetResolver.findById(targetPlayerId);

        if (targetRef == null) {
            logger.warn("Target player not found: " + targetPlayerId);
            isDone = true;
            return;
        }

        // Phase 2: Local-only execution path
        // In Phase 3, this will be replaced with network routing for remote targets
        if (targetRef.isLocal() || targetPlayerId == localPlayerId) {
            // Target is local player - apply block directly
            executeLocalBlockGain();
        } else {
            // Target is remote player
            // Phase 2: Execute locally as fallback (for single-player testing)
            // Phase 3: Will send network intent instead
            executeLocalBlockGain();
            logger.info("Phase 2: Local execution for remote target (will be networked in Phase 3)");
        }

        isDone = true;
    }

    /**
     * Executes the block gain effect on the local player.
     */
    private void executeLocalBlockGain() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            logger.warn("No local player available");
            return;
        }

        // Use the built-in GainBlockAction for proper effect handling
        addToTop(new GainBlockAction((AbstractCreature) player, (AbstractCreature) player, blockAmount));

        logger.info("Granted " + blockAmount + " block to player " + targetPlayerId);
    }

    /**
     * Gets the target player ID.
     */
    public int getTargetPlayerId() {
        return targetPlayerId;
    }

    /**
     * Gets the block amount.
     */
    public int getBlockAmount() {
        return blockAmount;
    }

    /**
     * Registers the GAIN_BLOCK executor with the PlayerEffectRegistry.
     * Should be called during mod initialization.
     */
    public static void registerExecutor() {
        PlayerEffectRegistry.register(PlayerEffectType.GAIN_BLOCK, (command, context) -> {
            int blockAmount = command.getIntArg(0);

            if (context.isVisualOnly()) {
                // Non-target clients only show visual effect
                PlayerTargetRef targetRef = context.getTargetRef();
                if (targetRef != null && targetRef.hasValidHitbox()) {
                    // Could add shield VFX here in future
                }
                return;
            }

            // Target client applies the block
            AbstractPlayer player = AbstractDungeon.player;
            if (player != null) {
                AbstractDungeon.actionManager.addToTop(
                        new GainBlockAction((AbstractCreature) player, (AbstractCreature) player, blockAmount)
                );
                logger.info("Applied " + blockAmount + " block from network command");
            }
        });

        logger.info("Registered GAIN_BLOCK executor");
    }

    private static final Logger staticLogger = LogManager.getLogger(GainBlockOnPlayerAction.class.getName());
}
