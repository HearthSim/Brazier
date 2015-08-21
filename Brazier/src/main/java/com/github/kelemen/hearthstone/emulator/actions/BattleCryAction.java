package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.brazier.actions.TargetedAction;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import org.jtrim.utils.ExceptionHelper;

public final class BattleCryAction {
    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final TargetedAction<? super Minion, ? super TargetableCharacter> action;

    public BattleCryAction(
            TargetNeed targetNeed,
            PlayActionRequirement requirement,
            TargetedAction<? super Minion, ? super TargetableCharacter> action) {
        ExceptionHelper.checkNotNullArgument(targetNeed, "targetNeed");
        ExceptionHelper.checkNotNullArgument(requirement, "requirement");
        ExceptionHelper.checkNotNullArgument(action, "action");

        this.targetNeed = targetNeed;
        this.requirement = requirement;
        this.action = action;
    }

    public TargetNeed getTargetNeed() {
        return targetNeed;
    }

    public PlayActionRequirement getRequirement() {
        return requirement;
    }

    public TargetedAction<? super Minion, ? super TargetableCharacter> getAction() {
        return action;
    }
}
