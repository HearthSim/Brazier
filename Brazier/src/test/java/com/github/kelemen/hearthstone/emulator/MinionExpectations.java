package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.fail;

public final class MinionExpectations {
    public static final class Builder {
        private final MinionId id;
        private Integer attack;
        private Integer hp;
        private Boolean canAttackWith;

        private Set<MinionFlags> flags;

        public Builder(MinionId id) {
            ExceptionHelper.checkNotNullArgument(id, "id");
            this.id = id;
        }

        public void setFlags(Set<MinionFlags> flags) {
            ExceptionHelper.checkNotNullElements(flags, "flags");

            this.flags = flags != null
                    ? (flags.isEmpty() ? EnumSet.noneOf(MinionFlags.class) : EnumSet.copyOf(flags))
                    : null;
        }

        public void setAttack(Integer attack) {
            this.attack = attack;
        }

        public void setHp(Integer hp) {
            this.hp = hp;
        }

        public void setCanAttackWith(Boolean canAttackWith) {
            this.canAttackWith = canAttackWith;
        }

        public MinionExpectations create() {
            return new MinionExpectations(this);
        }
    }

    private final MinionId id;
    private final Integer attack;
    private final Integer hp;
    private final Boolean canAttackWith;
    private final Set<MinionFlags> flags;

    private MinionExpectations(Builder builder) {
        this.id = builder.id;
        this.attack = builder.attack;
        this.hp = builder.hp;
        this.canAttackWith = builder.canAttackWith;
        this.flags = builder.flags;
    }

    public void verifyExpectations(Minion minion, Supplier<String> additionalInfo) {
        if (!id.equals(minion.getBaseDescr().getId())) {
            fail("The ID of the minion is different than expected. " + errorMessage(minion, additionalInfo));
        }
        if (attack != null && minion.getAttackTool().getAttack() != attack) {
            fail("The attack of the minion is different than expected. " + errorMessage(minion, additionalInfo));
        }
        if (hp != null && minion.getBody().getCurrentHp() != hp) {
            fail("The HP of the minion is different than expected. " + errorMessage(minion, additionalInfo));
        }
        if (canAttackWith != null && minion.getAttackTool().canAttackWith() != canAttackWith) {
            String baseMessage = canAttackWith
                    ? "Minion must be allowed to attack but it is not."
                    : "Minion must not be allowed to attack but it is.";
            fail(baseMessage + " " + errorMessage(minion, additionalInfo));
        }
        if (flags != null && !MinionFlags.onlyHasFlags(minion, flags)) {
            fail("The minion flags are unexpected. " + errorMessage(minion, additionalInfo));
        }
    }

    private String errorMessage(Minion minion, Supplier<String> additionalInfo) {
        return additionalInfo.get() + " Expected minion: " + this + ". Actual minion: " + minion;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(64);
        result.append("Minion(");
        result.append(id);
        result.append(") ");
        result.append(attack);
        result.append("/");
        result.append(hp);

        if (flags != null) {
            result.append(" ");
            if (flags.isEmpty()) {
                result.append("NO FLAGS");
            }
            else {
                result.append("Flags: ");
                result.append(flags);
            }
        }
        else {
            result.append(" ?");
        }

        return result.toString();
    }
}
