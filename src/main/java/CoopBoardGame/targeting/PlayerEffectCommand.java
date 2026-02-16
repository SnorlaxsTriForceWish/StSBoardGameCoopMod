package CoopBoardGame.targeting;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable command object representing a player-targeted effect.
 * Commands are used for both local execution and network transmission.
 */
public final class PlayerEffectCommand {

    private static final AtomicLong commandIdCounter = new AtomicLong(0);

    private final long commandId;
    private final PlayerEffectType effectType;
    private final int sourcePlayerId;
    private final int targetPlayerId;
    private final int[] intArgs;
    private final String[] stringArgs;

    /**
     * Creates a new PlayerEffectCommand.
     *
     * @param commandId unique command identifier for deduplication
     * @param effectType the type of effect to apply
     * @param sourcePlayerId the player initiating the effect
     * @param targetPlayerId the player receiving the effect
     * @param intArgs integer arguments for the effect
     * @param stringArgs string arguments for the effect
     */
    public PlayerEffectCommand(
            long commandId,
            PlayerEffectType effectType,
            int sourcePlayerId,
            int targetPlayerId,
            int[] intArgs,
            String[] stringArgs
    ) {
        this.commandId = commandId;
        this.effectType = effectType;
        this.sourcePlayerId = sourcePlayerId;
        this.targetPlayerId = targetPlayerId;
        this.intArgs = intArgs != null ? Arrays.copyOf(intArgs, intArgs.length) : new int[0];
        this.stringArgs = stringArgs != null ? Arrays.copyOf(stringArgs, stringArgs.length) : new String[0];
    }

    /**
     * Creates a new command with an auto-generated ID.
     */
    public static PlayerEffectCommand create(
            PlayerEffectType effectType,
            int sourcePlayerId,
            int targetPlayerId,
            int[] intArgs,
            String[] stringArgs
    ) {
        return new PlayerEffectCommand(
                generateCommandId(),
                effectType,
                sourcePlayerId,
                targetPlayerId,
                intArgs,
                stringArgs
        );
    }

    /**
     * Creates a simple command with only int args.
     */
    public static PlayerEffectCommand createWithIntArgs(
            PlayerEffectType effectType,
            int sourcePlayerId,
            int targetPlayerId,
            int... intArgs
    ) {
        return create(effectType, sourcePlayerId, targetPlayerId, intArgs, null);
    }

    /**
     * Creates a simple command with only string args.
     */
    public static PlayerEffectCommand createWithStringArgs(
            PlayerEffectType effectType,
            int sourcePlayerId,
            int targetPlayerId,
            String... stringArgs
    ) {
        return create(effectType, sourcePlayerId, targetPlayerId, null, stringArgs);
    }

    /**
     * Generates a unique command ID.
     * IDs are monotonically increasing per JVM instance.
     */
    public static long generateCommandId() {
        return commandIdCounter.incrementAndGet();
    }

    /**
     * Gets the unique command identifier.
     */
    public long getCommandId() {
        return commandId;
    }

    /**
     * Gets the effect type.
     */
    public PlayerEffectType getEffectType() {
        return effectType;
    }

    /**
     * Gets the source player ID (who initiated the effect).
     */
    public int getSourcePlayerId() {
        return sourcePlayerId;
    }

    /**
     * Gets the target player ID (who receives the effect).
     */
    public int getTargetPlayerId() {
        return targetPlayerId;
    }

    /**
     * Gets a copy of the integer arguments.
     */
    public int[] getIntArgs() {
        return Arrays.copyOf(intArgs, intArgs.length);
    }

    /**
     * Gets a specific integer argument.
     *
     * @param index the argument index
     * @return the argument value
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public int getIntArg(int index) {
        return intArgs[index];
    }

    /**
     * Gets a specific integer argument with a default value.
     *
     * @param index the argument index
     * @param defaultValue value to return if index is invalid
     * @return the argument value or default
     */
    public int getIntArgOrDefault(int index, int defaultValue) {
        if (index < 0 || index >= intArgs.length) {
            return defaultValue;
        }
        return intArgs[index];
    }

    /**
     * Gets the number of integer arguments.
     */
    public int getIntArgCount() {
        return intArgs.length;
    }

    /**
     * Gets a copy of the string arguments.
     */
    public String[] getStringArgs() {
        return Arrays.copyOf(stringArgs, stringArgs.length);
    }

    /**
     * Gets a specific string argument.
     *
     * @param index the argument index
     * @return the argument value
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public String getStringArg(int index) {
        return stringArgs[index];
    }

    /**
     * Gets a specific string argument with a default value.
     *
     * @param index the argument index
     * @param defaultValue value to return if index is invalid
     * @return the argument value or default
     */
    public String getStringArgOrDefault(int index, String defaultValue) {
        if (index < 0 || index >= stringArgs.length) {
            return defaultValue;
        }
        return stringArgs[index];
    }

    /**
     * Gets the number of string arguments.
     */
    public int getStringArgCount() {
        return stringArgs.length;
    }

    /**
     * Validates that this command has the required arguments for its effect type.
     *
     * @return true if the command is valid
     */
    public boolean isValid() {
        if (effectType == null) {
            return false;
        }

        int requiredIntArgs = effectType.getRequiredIntArgs();
        int requiredStringArgs = effectType.getRequiredStringArgs();

        return intArgs.length >= requiredIntArgs && stringArgs.length >= requiredStringArgs;
    }

    @Override
    public String toString() {
        return "PlayerEffectCommand{" +
                "commandId=" + commandId +
                ", effectType=" + effectType +
                ", sourcePlayerId=" + sourcePlayerId +
                ", targetPlayerId=" + targetPlayerId +
                ", intArgs=" + Arrays.toString(intArgs) +
                ", stringArgs=" + Arrays.toString(stringArgs) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerEffectCommand that = (PlayerEffectCommand) o;
        return commandId == that.commandId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(commandId);
    }
}
