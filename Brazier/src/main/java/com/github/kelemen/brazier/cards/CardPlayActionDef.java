package com.github.kelemen.brazier.cards;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.CardPlayAction;
import com.github.kelemen.brazier.actions.CardPlayArg;
import com.github.kelemen.brazier.actions.PlayActionRequirement;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.UndoAction;
import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayActionDef implements CardPlayAction {
    public static final CardPlayActionDef UNPLAYABLE = new CardPlayActionDef(
            TargetNeed.NO_NEED,
            (target) -> false,
            CardPlayAction.DO_NOTHING);

    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final CardPlayAction action;

    public CardPlayActionDef(TargetNeed targetNeed, PlayActionRequirement requirement, CardPlayAction action) {
        ExceptionHelper.checkNotNullArgument(targetNeed, "targetNeed");
        ExceptionHelper.checkNotNullArgument(requirement, "requirement");
        ExceptionHelper.checkNotNullArgument(action, "action");

        this.targetNeed = targetNeed;
        this.requirement = requirement;
        this.action = action;
    }

    public static TargetNeed combineNeeds(Player player, Collection<? extends CardPlayActionDef> actions) {
        TargetNeed result = TargetNeed.NO_NEED;
        for (CardPlayActionDef action: actions) {
            if (action.getRequirement().meetsRequirement(player)) {
                result = result.combine(action.getTargetNeed());
            }
        }
        return result;
    }

    public TargetNeed getTargetNeed() {
        return targetNeed;
    }

    public PlayActionRequirement getRequirement() {
        return requirement;
    }

    public CardPlayAction getAction() {
        return action;
    }

    @Override
    public UndoAction alterWorld(World world, CardPlayArg arg) {
        return action.alterWorld(world, arg);
    }

    @Override
    public String toString() {
        return "PlayCardActionDef{" + "targetNeed=" + targetNeed + ", action=" + action + '}';
    }
}
