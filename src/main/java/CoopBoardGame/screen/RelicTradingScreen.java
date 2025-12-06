package CoopBoardGame.screen;

import CoopBoardGame.ui.FakeTradingRelic;
import basemod.abstracts.CustomScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RelicTradingScreen extends CustomScreen {

    //TODO: emergency cancel if no relics are available for whatever reason
    //TODO: right-click to cancel (but will make event logic rather more complicated)

    public boolean isDone = false;

    public interface RelicTradingAction {
        void execute(AbstractRelic relic);
    }

    final Logger logger = LogManager.getLogger(RelicTradingScreen.class.getName());

    public static class Enum {

        @SpireEnum
        public static AbstractDungeon.CurrentScreen RELIC_TRADING;
    }

    @Override
    public AbstractDungeon.CurrentScreen curScreen() {
        return RelicTradingScreen.Enum.RELIC_TRADING;
    }

    public RelicTradingScreen.RelicTradingAction action;

    public String description = "(DNT) Relic Select Screen.  Choose a Relic.";
    public boolean displayRelicPrice = false;

    public ArrayList<FakeTradingRelic> relics;

    public void open() {
        AbstractDungeon.screen = curScreen();
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.showBlackScreen();
    }

    @Override
    public void reopen() {
        AbstractDungeon.screen = curScreen();
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.showBlackScreen();
    }

    @Override
    public void openingSettings() {
        // Required if you want to reopen your screen when the settings screen closes
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void close() {
        genericScreenOverlayReset();
        GameCursor.hidden = false;
        AbstractDungeon.overlayMenu.hideBlackScreen();
    }

    @Override
    public void update() {
        //logger.info("RTS: update");
        if (!((AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.EVENT)) {
            isDone = true;
            AbstractDungeon.closeCurrentScreen();
            return;
        }
        for (FakeTradingRelic r : relics) {
            r.update();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        //FontHelper.renderDeckViewTip(sb, this.description, 96.0F * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderFontCentered(
            sb,
            FontHelper.buttonLabelFont,
            this.description,
            (Settings.WIDTH / 2),
            Settings.HEIGHT - 180.0F * Settings.scale,
            Settings.CREAM_COLOR
        );
        for (FakeTradingRelic r : relics) {
            r.render(sb);
        }
    }

    public void onRelicChosen(AbstractRelic relic) {
        action.execute(relic);
        isDone = true;
        AbstractDungeon.closeCurrentScreen();
    }
}
