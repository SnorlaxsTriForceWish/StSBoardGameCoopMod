package CoopBoardGame.targeting;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.Hitbox;

/**
 * Immutable reference to a targetable player in combat.
 * Represents either the local player or a remote BG player (CharacterEntity).
 */
public class PlayerTargetRef {

    private final int playerId;
    private final boolean local;
    private final String username;
    private final AbstractPlayer.PlayerClass playerClass;
    private final Hitbox hitbox;
    private final float renderX;
    private final float renderY;
    private final int row;

    public PlayerTargetRef(
            int playerId,
            boolean local,
            String username,
            AbstractPlayer.PlayerClass playerClass,
            Hitbox hitbox,
            float renderX,
            float renderY,
            int row
    ) {
        this.playerId = playerId;
        this.local = local;
        this.username = username;
        this.playerClass = playerClass;
        this.hitbox = hitbox;
        this.renderX = renderX;
        this.renderY = renderY;
        this.row = row;
    }

    /**
     * Gets the TogetherInSpire player ID.
     * In single-player mode, local player ID is always 0.
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Returns true if this is the local player (AbstractDungeon.player).
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Gets the player's display username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the player's character class.
     */
    public AbstractPlayer.PlayerClass getPlayerClass() {
        return playerClass;
    }

    /**
     * Gets the hitbox for arrow-mode targeting.
     * May be null for selection-only fallback (remote player with missing entity).
     */
    public Hitbox getHitbox() {
        return hitbox;
    }

    /**
     * Gets the render X position for visual effects.
     */
    public float getRenderX() {
        return renderX;
    }

    /**
     * Gets the render Y position for visual effects.
     */
    public float getRenderY() {
        return renderY;
    }

    /**
     * Gets the player's combat row.
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns true if this player can be targeted via arrow mode.
     * Requires a valid hitbox.
     */
    public boolean hasValidHitbox() {
        return hitbox != null;
    }

    @Override
    public String toString() {
        return "PlayerTargetRef{" +
                "playerId=" + playerId +
                ", local=" + local +
                ", username='" + username + '\'' +
                ", playerClass=" + playerClass +
                ", row=" + row +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTargetRef that = (PlayerTargetRef) o;
        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(playerId);
    }
}
