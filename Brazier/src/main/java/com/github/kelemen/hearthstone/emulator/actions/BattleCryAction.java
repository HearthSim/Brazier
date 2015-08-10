package com.github.kelemen.hearthstone.emulator.actions;

import org.jtrim.utils.ExceptionHelper;

public final class BattleCryAction {
    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final BattleCryTargetedAction action;

    public BattleCryAction(
            TargetNeed targetNeed,
            PlayActionRequirement requirement,
            BattleCryTargetedAction action) {
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

    public BattleCryTargetedAction getAction() {
        return action;
    }
}
