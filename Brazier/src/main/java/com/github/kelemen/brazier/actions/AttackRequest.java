package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.TargetableCharacter;
import org.jtrim.utils.ExceptionHelper;

public final class AttackRequest {
    private final TargetableCharacter attacker;
    private final TargetableCharacter originalTarget;
    private TargetableCharacter defender;

    public AttackRequest(TargetableCharacter attacker, TargetableCharacter defender) {
        ExceptionHelper.checkNotNullArgument(attacker, "attacker");

        this.attacker = attacker;
        this.originalTarget = defender;
        this.defender = defender;
    }

    public TargetableCharacter getAttacker() {
        return attacker;
    }

    public TargetableCharacter getDefender() {
        return defender;
    }

    public TargetableCharacter getOriginalTarget() {
        return originalTarget;
    }

    public UndoAction replaceDefender(TargetableCharacter newDefender) {
        TargetableCharacter prevDefender = defender;
        defender = newDefender;
        return () -> defender = prevDefender;
    }
}
