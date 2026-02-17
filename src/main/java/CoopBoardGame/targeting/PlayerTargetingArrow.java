package CoopBoardGame.targeting;

import CoopBoardGame.targeting.PlayerTargetResolver.TargetFilter;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Renders the targeting arrow and player highlights for player-targeted cards.
 * Uses a green arrow to differentiate from monster targeting (red arrow).
 */
public class PlayerTargetingArrow {

    private static final Logger logger = LogManager.getLogger(PlayerTargetingArrow.class.getName());

    // Arrow colors - green for player targeting
    private static final Color ARROW_COLOR = new Color(0.3f, 1.0f, 0.3f, 1.0f);
    private static final Color ARROW_HOVER_COLOR = new Color(0.5f, 1.0f, 0.5f, 1.0f);

    // Arrow animation state
    private static float arrowX = 0;
    private static float arrowY = 0;
    private static float arrowScale = Settings.scale;
    private static float arrowScaleTimer = 0;
    private static final Vector2 controlPoint = new Vector2();
    private static final Vector2 startArrowVector = new Vector2();
    private static final Vector2 endArrowVector = new Vector2();
    private static final Vector2 arrowTmp = new Vector2();

    /**
     * Updates the arrow position and determines which player is being hovered.
     * Should be called during the player targeting session update.
     */
    public static void update() {
        PlayerTargetingSession session = PlayerTargetingSession.getInstance();
        if (!session.isActive()) {
            return;
        }

        // Update arrow position to follow mouse
        arrowX = MathHelper.mouseLerpSnap(arrowX, InputHelper.mX);
        arrowY = MathHelper.mouseLerpSnap(arrowY, InputHelper.mY);

        // Find hovered player
        int hoveredId = findHoveredPlayer(session.getFilter());
        session.setHoveredPlayerId(hoveredId);

        // Update arrow scale animation
        if (hoveredId < 0) {
            arrowScale = Settings.scale;
            arrowScaleTimer = 0;
        } else {
            arrowScaleTimer += Gdx.graphics.getDeltaTime();
            if (arrowScaleTimer > 1.0f) {
                arrowScaleTimer = 1.0f;
            }
            arrowScale = Interpolation.elasticOut.apply(
                    Settings.scale,
                    Settings.scale * 1.2f,
                    arrowScaleTimer
            );
        }
    }

    /**
     * Finds which player's hitbox the mouse is hovering over.
     * Returns the player ID, or -1 if no valid hover.
     */
    private static int findHoveredPlayer(TargetFilter filter) {
        List<PlayerTargetRef> targets = PlayerTargetResolver.getTargetablePlayers(filter);

        for (PlayerTargetRef target : targets) {
            Hitbox hb = target.getHitbox();
            if (hb != null) {
                hb.update();
                if (hb.hovered) {
                    return target.getPlayerId();
                }
            }
        }

        return -1;
    }

    /**
     * Renders the targeting arrow and player highlights.
     * Should be called during player render phase.
     *
     * @param sb the sprite batch
     */
    public static void render(SpriteBatch sb) {
        PlayerTargetingSession session = PlayerTargetingSession.getInstance();
        if (!session.isActive()) {
            return;
        }

        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return;
        }

        // Render player highlights first (behind arrow)
        renderPlayerHighlights(sb, session);

        // Render the targeting arrow
        renderArrow(sb, player, session);
    }

    /**
     * Renders highlights on all valid player targets using hitbox rendering.
     */
    private static void renderPlayerHighlights(SpriteBatch sb, PlayerTargetingSession session) {
        TargetFilter filter = session.getFilter();
        List<PlayerTargetRef> targets = PlayerTargetResolver.getTargetablePlayers(filter);
        int hoveredId = session.getHoveredPlayerId();

        for (PlayerTargetRef target : targets) {
            Hitbox hb = target.getHitbox();
            if (hb == null) {
                continue;
            }

            boolean isHovered = (target.getPlayerId() == hoveredId);

            // Use hitbox's built-in render method for targeting reticle
            if (isHovered) {
                hb.render(sb);
            }
        }
    }

    /**
     * Renders the targeting arrow from the card to the mouse cursor.
     */
    private static void renderArrow(SpriteBatch sb, AbstractPlayer player, PlayerTargetingSession session) {
        // Get the source card position
        AbstractCard sourceCard = session.getSourceCard();
        if (sourceCard == null) {
            return;
        }

        // Set arrow color based on hover state
        boolean hasValidTarget = session.hasValidHover();
        sb.setColor(hasValidTarget ? ARROW_HOVER_COLOR : ARROW_COLOR);

        // Calculate control point for curved arrow (curve upward from card to cursor)
        controlPoint.x = (sourceCard.current_x + arrowX) / 2;
        controlPoint.y = sourceCard.current_y + 200f * Settings.scale;

        // Set arrow vectors - start from card position, end at mouse cursor
        startArrowVector.x = sourceCard.current_x;
        startArrowVector.y = sourceCard.current_y;
        endArrowVector.x = arrowX;
        endArrowVector.y = arrowY;

        // Calculate arrow angle
        arrowTmp.x = endArrowVector.x - startArrowVector.x;
        arrowTmp.y = endArrowVector.y - startArrowVector.y;
        arrowTmp.nor();

        // Draw curved line using reflection to access private method
        drawCurvedLine(sb, player, startArrowVector, endArrowVector, controlPoint);

        // Draw arrow head
        sb.draw(
                ImageMaster.TARGET_UI_ARROW,
                arrowX - 128f,
                arrowY - 128f,
                128f,
                128f,
                256f,
                256f,
                arrowScale,
                arrowScale,
                arrowTmp.angle() + 90f,
                0,
                0,
                256,
                256,
                false,
                false
        );
    }

    /**
     * Draws a curved line using the player's private method.
     */
    private static void drawCurvedLine(SpriteBatch sb, AbstractPlayer player,
                                        Vector2 start, Vector2 end, Vector2 control) {
        try {
            ReflectionHacks.RMethod drawMethod = ReflectionHacks.privateMethod(
                    AbstractPlayer.class,
                    "drawCurvedLine",
                    SpriteBatch.class,
                    Vector2.class,
                    Vector2.class,
                    Vector2.class
            );
            drawMethod.invoke(player, sb, start, end, control);
        } catch (Exception e) {
            logger.error("Failed to draw curved line: " + e.getMessage());
        }
    }

    /**
     * Resets the arrow state. Call when targeting session ends.
     */
    public static void reset() {
        arrowX = 0;
        arrowY = 0;
        arrowScale = Settings.scale;
        arrowScaleTimer = 0;
    }
}
