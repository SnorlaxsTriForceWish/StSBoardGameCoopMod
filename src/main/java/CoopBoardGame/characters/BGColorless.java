package CoopBoardGame.characters;

import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.Map;

public class BGColorless {

    public static class Enums {

        @SpireEnum
        public static AbstractPlayer.PlayerClass BG_COLORLESS_PLAYERCLASS;

        @SpireEnum(name = "Colorless (Board Game)") // These two HAVE to have the same absolutely identical name.
        public static AbstractCard.CardColor CARD_COLOR;

        @SpireEnum(name = "Colorless (Board Game)")
        public static CardLibrary.LibraryType LIBRARY_COLOR;
    }

    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        AbstractCard.CardColor targetColor = Enums.CARD_COLOR;

        for (Map.Entry<String, AbstractCard> entry : CardLibrary.cards.entrySet()) {
            AbstractCard card = entry.getValue();
            String cardKey = entry.getKey();

            // Skip cards that don't match our color
            if (!card.color.equals(targetColor)) {
                continue;
            }

            // Skip basic rarity cards
            if (card.rarity == AbstractCard.CardRarity.BASIC) {
                continue;
            }

            // Skip locked cards (unless in daily run)
            if (UnlockTracker.isCardLocked(cardKey) && !Settings.isDailyRun) {
                continue;
            }

            tmpPool.add(card);
        }

        return tmpPool;
    }
}
