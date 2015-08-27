package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.TargetableCharacter;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class AttackRequest {
    private final TargetableCharacter attacker;
    private TargetableCharacter defender;

    public AttackRequest(TargetableCharacter attacker, TargetableCharacter defender) {
        ExceptionHelper.checkNotNullArgument(attacker, "attacker");

        this.attacker = attacker;
        this.defender = defender;
    }

    public TargetableCharacter getAttacker() {
        return attacker;
    }

    public TargetableCharacter getDefender() {
        return defender;
    }

    private static boolean testExistingDefender(
            TargetableCharacter defender,
            Predicate<? super TargetableCharacter> check) {
        if (defender == null) {
            return false;
        }
        return check.test(defender);
    }

    public boolean testExistingDefender(Predicate<? super TargetableCharacter> check) {
        return testExistingDefender(defender, check);
    }

    public UndoAction replaceDefender(TargetableCharacter newDefender) {
        TargetableCharacter prevDefender = defender;
        defender = newDefender;
        return () -> defender = prevDefender;
    }
}
