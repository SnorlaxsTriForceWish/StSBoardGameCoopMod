package CoopBoardGame.targeting;

import CoopBoardGame.multiplayer.rows.MultiCreature;
import CoopBoardGame.multiplayer.rows.PlayerRowManager;
import CoopBoardGame.util.TogetherInSpireHelper;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Resolves and builds the list of targetable players in combat.
 * Includes local player and remote BG players from TogetherInSpire.
 */
public class PlayerTargetResolver {

    private static final Logger logger = LogManager.getLogger(PlayerTargetResolver.class.getName());

    /**
     * Filter configuration for player targeting.
     */
    public static class TargetFilter {
        private final boolean allowSelf;
        private final boolean bgOnly;
        private final Predicate<PlayerTargetRef> customPredicate;

        public TargetFilter(boolean allowSelf, boolean bgOnly, Predicate<PlayerTargetRef> customPredicate) {
            this.allowSelf = allowSelf;
            this.bgOnly = bgOnly;
            this.customPredicate = customPredicate;
        }

        public boolean isAllowSelf() {
            return allowSelf;
        }

        public boolean isBgOnly() {
            return bgOnly;
        }

        public Predicate<PlayerTargetRef> getCustomPredicate() {
            return customPredicate;
        }

        /**
         * Default filter: allow self, BG players only.
         */
        public static TargetFilter defaultFilter() {
            return new TargetFilter(true, true, null);
        }

        /**
         * Filter for targeting allies only (not self).
         */
        public static TargetFilter alliesOnly() {
            return new TargetFilter(false, true, null);
        }

        /**
         * Filter for targeting self and allies.
         */
        public static TargetFilter selfAndAllies() {
            return new TargetFilter(true, true, null);
        }

        /**
         * Custom filter with predicate.
         */
        public static TargetFilter custom(boolean allowSelf, boolean bgOnly, Predicate<PlayerTargetRef> predicate) {
            return new TargetFilter(allowSelf, bgOnly, predicate);
        }
    }

    /**
     * Gets all targetable players matching the given filter.
     *
     * @param filter targeting filter configuration
     * @return list of targetable players
     */
    public static List<PlayerTargetRef> getTargetablePlayers(TargetFilter filter) {
        List<PlayerTargetRef> result = new ArrayList<>();

        int localPlayerId = TogetherInSpireHelper.getLocalPlayerId();

        // Add local player if allowed
        if (filter.isAllowSelf()) {
            PlayerTargetRef localRef = buildLocalPlayerRef(localPlayerId);
            if (localRef != null && passesFilter(localRef, filter)) {
                result.add(localRef);
            }
        }

        // Add remote BG players in multiplayer mode
        if (TogetherInSpireHelper.isMultiplayerBoardGameMode()) {
            List<Integer> bgPlayerIds = TogetherInSpireHelper.getBoardGamePlayerIds();
            for (Integer playerId : bgPlayerIds) {
                if (playerId == localPlayerId) {
                    continue; // Already handled local player
                }

                PlayerTargetRef remoteRef = buildRemotePlayerRef(playerId);
                if (remoteRef != null && passesFilter(remoteRef, filter)) {
                    result.add(remoteRef);
                }
            }
        }

        return result;
    }

    /**
     * Finds a player by ID from the current targetable set.
     *
     * @param playerId the player ID to find
     * @return PlayerTargetRef or null if not found
     */
    public static PlayerTargetRef findById(int playerId) {
        int localPlayerId = TogetherInSpireHelper.getLocalPlayerId();

        if (playerId == localPlayerId) {
            return buildLocalPlayerRef(localPlayerId);
        }

        if (TogetherInSpireHelper.isMultiplayerBoardGameMode()) {
            List<Integer> bgPlayerIds = TogetherInSpireHelper.getBoardGamePlayerIds();
            if (bgPlayerIds.contains(playerId)) {
                return buildRemotePlayerRef(playerId);
            }
        }

        return null;
    }

    /**
     * Checks if a player ID is a valid target given the filter.
     *
     * @param playerId the player ID to validate
     * @param filter targeting filter configuration
     * @return true if valid target
     */
    public static boolean isValidTargetId(int playerId, TargetFilter filter) {
        PlayerTargetRef ref = findById(playerId);
        if (ref == null) {
            return false;
        }

        int localPlayerId = TogetherInSpireHelper.getLocalPlayerId();
        if (playerId == localPlayerId && !filter.isAllowSelf()) {
            return false;
        }

        return passesFilter(ref, filter);
    }

    /**
     * Gets the number of targetable players for a given filter.
     */
    public static int getTargetablePlayerCount(TargetFilter filter) {
        return getTargetablePlayers(filter).size();
    }

    /**
     * Returns true if only one player is targetable (for auto-confirm logic).
     */
    public static boolean hasSingleTarget(TargetFilter filter) {
        return getTargetablePlayerCount(filter) == 1;
    }

    /**
     * Gets the single target if only one exists, otherwise null.
     */
    public static PlayerTargetRef getSingleTargetOrNull(TargetFilter filter) {
        List<PlayerTargetRef> targets = getTargetablePlayers(filter);
        if (targets.size() == 1) {
            return targets.get(0);
        }
        return null;
    }

    private static boolean passesFilter(PlayerTargetRef ref, TargetFilter filter) {
        if (filter.isBgOnly() && ref.getPlayerClass() != null) {
            if (!TogetherInSpireHelper.isBoardGameClass(ref.getPlayerClass())) {
                return false;
            }
        }

        Predicate<PlayerTargetRef> predicate = filter.getCustomPredicate();
        if (predicate != null && !predicate.test(ref)) {
            return false;
        }

        return true;
    }

    private static PlayerTargetRef buildLocalPlayerRef(int localPlayerId) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return null;
        }

        int row = MultiCreature.Field.currentRow.get(player);

        return new PlayerTargetRef(
                localPlayerId,
                true,
                getLocalPlayerUsername(),
                player.chosenClass,
                player.hb,
                player.drawX,
                player.drawY,
                row
        );
    }

    private static PlayerTargetRef buildRemotePlayerRef(int playerId) {
        String username = TogetherInSpireHelper.getPlayerUsername(playerId);
        AbstractPlayer.PlayerClass playerClass = TogetherInSpireHelper.getPlayerClassForId(playerId);
        int row = PlayerRowManager.getPlayerRow(playerId);

        // Try to find CharacterEntity for hitbox and position
        Hitbox hitbox = null;
        float renderX = 0;
        float renderY = 0;

        Object characterEntity = findCharacterEntityForPlayerId(playerId);
        if (characterEntity instanceof AbstractMonster) {
            AbstractMonster monster = (AbstractMonster) characterEntity;
            hitbox = monster.hb;
            renderX = monster.drawX;
            renderY = monster.drawY;
        }

        return new PlayerTargetRef(
                playerId,
                false,
                username,
                playerClass,
                hitbox,
                renderX,
                renderY,
                row
        );
    }

    private static String getLocalPlayerUsername() {
        if (TogetherInSpireHelper.isTogetherInSpireLoaded()) {
            int localId = TogetherInSpireHelper.getLocalPlayerId();
            return TogetherInSpireHelper.getPlayerUsername(localId);
        }
        // Fallback for single-player
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null && player.name != null) {
            return player.name;
        }
        return "Player";
    }

    private static Object findCharacterEntityForPlayerId(int playerId) {
        if (AbstractDungeon.getMonsters() == null) {
            return null;
        }

        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (TogetherInSpireHelper.isCharacterEntity(monster)) {
                int entityPlayerId = TogetherInSpireHelper.getCharacterEntityPlayerId(monster);
                if (entityPlayerId == playerId) {
                    return monster;
                }
            }
        }

        return null;
    }
}
