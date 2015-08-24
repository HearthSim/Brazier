package com.github.kelemen.brazier.cards;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.CardPlayArg;
import com.github.kelemen.brazier.actions.PlayActionRequirement;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.TargetedAction;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.WorldObjectAction;
import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;

public final class CardPlayActionDef implements WorldObjectAction<CardPlayArg> {
    public static final CardPlayActionDef UNPLAYABLE = new CardPlayActionDef(
            TargetNeed.NO_NEED,
            (target) -> false,
            TargetedAction.DO_NOTHING);

    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final TargetedAction<? super Card, ? super TargetableCharacter> action;

    public CardPlayActionDef(
            TargetNeed targetNeed,
            PlayActionRequirement requirement,
            TargetedAction<? super Card, ? super TargetableCharacter> action) {
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

    public TargetedAction<? super Card, ? super TargetableCharacter> getAction() {
        return action;
    }

    @Override
    public UndoAction alterWorld(World world, CardPlayArg arg) {
        return action.alterWorld(world, arg.getCard(), arg.getTarget().getTarget());
    }

    @Override
    public String toString() {
        return "PlayCardActionDef{" + "targetNeed=" + targetNeed + ", action=" + action + '}';
    }
}
