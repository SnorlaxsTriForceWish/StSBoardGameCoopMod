package CoopBoardGame.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.cards.AbstractCard;

/**
 * Extends AbstractCard.CardTarget with player-targeting modes for the
 * cooperative multiplayer system.
 *
 * PLAYER: Target a single player (local or remote BG ally) via arrow or selection.
 * ALL_PLAYERS: Target all players at once (effect resolves to per-player commands).
 */
public class CardTargetPatch {

    /**
     * Target a single player (self or ally).
     * Cards with this target show an arrow that can be pointed at any valid player.
     */
    @SpireEnum
    public static AbstractCard.CardTarget PLAYER;

    /**
     * Target all players simultaneously.
     * Effects are expanded into per-player commands by the targeting system.
     */
    @SpireEnum
    public static AbstractCard.CardTarget ALL_PLAYERS;
}
