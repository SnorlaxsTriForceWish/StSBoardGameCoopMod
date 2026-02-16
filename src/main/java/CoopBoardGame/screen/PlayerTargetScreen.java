package CoopBoardGame.screen;

import CoopBoardGame.targeting.PlayerTargetRef;
import CoopBoardGame.targeting.PlayerTargetResolver;
import CoopBoardGame.targeting.PlayerTargetResolver.TargetFilter;
import basemod.BaseMod;
import basemod.abstracts.CustomScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Custom screen for selecting a player target.
 * Used by player-targeted cards, relics, and potions.
 */
public class PlayerTargetScreen extends CustomScreen {

    private static final Logger logger = LogManager.getLogger(PlayerTargetScreen.class.getName());

    // Screen dimensions for player buttons
    private static final float BUTTON_WIDTH = 200f * Settings.scale;
    private static final float BUTTON_HEIGHT = 60f * Settings.scale;
    private static final float BUTTON_SPACING = 20f * Settings.scale;
    private static final float START_Y = Settings.HEIGHT * 0.6f;

    public static class Enum {
        @SpireEnum
        public static AbstractDungeon.CurrentScreen PLAYER_TARGET;
    }

    @Override
    public AbstractDungeon.CurrentScreen curScreen() {
        return Enum.PLAYER_TARGET;
    }

    // Current session state
    private String instruction = "Choose a player.";
    private TargetFilter filter = TargetFilter.defaultFilter();
    private Consumer<Integer> onPlayerSelected = null;
    private Runnable onCancel = null;
    private boolean allowCancel = true;
    private boolean isDone = false;

    // Cached player list and UI state
    private List<PlayerTargetRef> targetablePlayers = new ArrayList<>();
    private List<Hitbox> playerButtons = new ArrayList<>();
    private int hoveredIndex = -1;

    /**
     * Opens the player selection screen.
     *
     * @param instruction text displayed to the user
     * @param filter targeting filter configuration
     * @param onPlayerSelected callback when a player is selected (receives player ID)
     * @param onCancel optional callback when selection is cancelled
     */
    public void open(String instruction, TargetFilter filter, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        this.instruction = instruction != null ? instruction : "Choose a player.";
        this.filter = filter != null ? filter : TargetFilter.defaultFilter();
        this.onPlayerSelected = onPlayerSelected;
        this.onCancel = onCancel;
        this.allowCancel = true;
        this.isDone = false;

        // Build targetable player list
        this.targetablePlayers = PlayerTargetResolver.getTargetablePlayers(this.filter);

        // Auto-confirm if only one target
        if (targetablePlayers.size() == 1) {
            logger.info("Auto-confirming single player target: " + targetablePlayers.get(0).getPlayerId());
            selectPlayer(targetablePlayers.get(0).getPlayerId());
            return;
        }

        // No valid targets
        if (targetablePlayers.isEmpty()) {
            logger.warn("No valid player targets available");
            if (onCancel != null) {
                onCancel.run();
            }
            return;
        }

        // Build hitboxes for each player
        buildPlayerButtons();

        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NONE) {
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }

        reopen();
    }

    /**
     * Opens the screen with default filter (allow self, BG only).
     */
    public void open(String instruction, Consumer<Integer> onPlayerSelected) {
        open(instruction, TargetFilter.defaultFilter(), onPlayerSelected, null);
    }

    /**
     * Opens the screen with a custom predicate filter.
     */
    public void open(String instruction, Predicate<PlayerTargetRef> predicate, Consumer<Integer> onPlayerSelected, Runnable onCancel) {
        TargetFilter customFilter = TargetFilter.custom(true, true, predicate);
        open(instruction, customFilter, onPlayerSelected, onCancel);
    }

    private void buildPlayerButtons() {
        playerButtons.clear();

        float centerX = Settings.WIDTH / 2f;
        float totalHeight = targetablePlayers.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        float currentY = START_Y + totalHeight / 2f;

        for (int i = 0; i < targetablePlayers.size(); i++) {
            Hitbox hb = new Hitbox(
                    centerX - BUTTON_WIDTH / 2f,
                    currentY - BUTTON_HEIGHT / 2f,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT
            );
            playerButtons.add(hb);
            currentY -= (BUTTON_HEIGHT + BUTTON_SPACING);
        }
    }

    @Override
    public void reopen() {
        AbstractDungeon.screen = curScreen();
        AbstractDungeon.isScreenUp = true;
    }

    @Override
    public void openingSettings() {
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void close() {
        genericScreenOverlayReset();
        GameCursor.hidden = false;
        targetablePlayers.clear();
        playerButtons.clear();
        hoveredIndex = -1;
    }

    @Override
    public void update() {
        // Close if combat ends
        if (AbstractDungeon.getCurrRoom() == null ||
                AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
            logger.info("Combat ended, closing player target screen");
            cancel();
            return;
        }

        // Update hitboxes and check for hover
        hoveredIndex = -1;
        for (int i = 0; i < playerButtons.size(); i++) {
            Hitbox hb = playerButtons.get(i);
            hb.update();
            if (hb.hovered) {
                hoveredIndex = i;
            }
        }

        // Handle left click
        if (InputHelper.justClickedLeft) {
            InputHelper.justClickedLeft = false;
            if (hoveredIndex >= 0 && hoveredIndex < targetablePlayers.size()) {
                PlayerTargetRef selected = targetablePlayers.get(hoveredIndex);
                selectPlayer(selected.getPlayerId());
                return;
            }
        }

        // Handle right click (cancel)
        if (InputHelper.justClickedRight && allowCancel) {
            InputHelper.justClickedRight = false;
            cancel();
        }

        // Handle escape key
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE) && allowCancel) {
            cancel();
        }
    }

    private void selectPlayer(int playerId) {
        if (isDone) {
            return;
        }
        isDone = true;

        logger.info("Player selected: " + playerId);
        CardCrawlGame.sound.play("UI_CLICK_1");

        Consumer<Integer> callback = this.onPlayerSelected;
        AbstractDungeon.closeCurrentScreen();

        if (callback != null) {
            callback.accept(playerId);
        }
    }

    private void cancel() {
        if (isDone) {
            return;
        }
        isDone = true;

        logger.info("Player selection cancelled");
        CardCrawlGame.sound.play("UI_CLICK_2");

        Runnable callback = this.onCancel;
        AbstractDungeon.closeCurrentScreen();

        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        // Darken background
        sb.setColor(new Color(0f, 0f, 0f, 0.7f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0, 0, Settings.WIDTH, Settings.HEIGHT);

        // Render instruction
        FontHelper.renderFontCentered(
                sb,
                FontHelper.buttonLabelFont,
                this.instruction,
                Settings.WIDTH / 2f,
                Settings.HEIGHT - 150f * Settings.scale,
                Settings.GOLD_COLOR
        );

        // Render player buttons
        for (int i = 0; i < targetablePlayers.size(); i++) {
            PlayerTargetRef player = targetablePlayers.get(i);
            Hitbox hb = playerButtons.get(i);

            boolean isHovered = (i == hoveredIndex);

            // Button background
            Color bgColor = isHovered ? new Color(0.4f, 0.6f, 0.4f, 0.9f) : new Color(0.2f, 0.2f, 0.2f, 0.8f);
            sb.setColor(bgColor);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y, hb.width, hb.height);

            // Button border
            Color borderColor = isHovered ? Settings.GREEN_TEXT_COLOR : Settings.CREAM_COLOR;
            sb.setColor(borderColor);
            float borderWidth = 2f * Settings.scale;
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y, hb.width, borderWidth); // bottom
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y + hb.height - borderWidth, hb.width, borderWidth); // top
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x, hb.y, borderWidth, hb.height); // left
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, hb.x + hb.width - borderWidth, hb.y, borderWidth, hb.height); // right

            // Player name
            String displayName = player.getUsername();
            if (player.isLocal()) {
                displayName += " (You)";
            }

            Color textColor = isHovered ? Settings.GREEN_TEXT_COLOR : Settings.CREAM_COLOR;
            FontHelper.renderFontCentered(
                    sb,
                    FontHelper.cardTitleFont,
                    displayName,
                    hb.cX,
                    hb.cY,
                    textColor
            );

            // Render hitbox for debugging (optional)
            hb.render(sb);
        }

        // Cancel hint
        if (allowCancel) {
            FontHelper.renderFontCentered(
                    sb,
                    FontHelper.tipBodyFont,
                    "Right-click or ESC to cancel",
                    Settings.WIDTH / 2f,
                    100f * Settings.scale,
                    Settings.CREAM_COLOR
            );
        }
    }

    /**
     * Returns true if the screen has finished (selected or cancelled).
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Gets the current list of targetable players.
     */
    public List<PlayerTargetRef> getTargetablePlayers() {
        return new ArrayList<>(targetablePlayers);
    }

    // Patch to ensure screen updates properly
    @SpirePatch2(clz = AbstractDungeon.class, method = "update", paramtypez = {})
    public static class DungeonUpdatePatch {

        @SpireInsertPatch(locator = Locator.class)
        public static void Insert(AbstractDungeon __instance) {
            if (AbstractDungeon.screen == Enum.PLAYER_TARGET) {
                AbstractDungeon.overlayMenu.hideBlackScreen();
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch)
                    throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                        AbstractDungeon.class,
                        "isScreenUp"
                );
                return LineFinder.findInOrder(
                        ctMethodToPatch,
                        new ArrayList<Matcher>(),
                        finalMatcher
                );
            }
        }
    }
}
