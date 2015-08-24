package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.cards.Card;
import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;

public final class PlayActionDef<Actor> implements WorldObjectAction<PlayArg<Actor>> {
    public static final PlayActionDef<Card> UNPLAYABLE_CARD = new PlayActionDef<>(
            TargetNeed.NO_NEED,
            (target) -> false,
            TargetedAction.DO_NOTHING);

    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final TargetedAction<? super Actor, ? super TargetableCharacter> action;

    public PlayActionDef(
            TargetNeed targetNeed,
            PlayActionRequirement requirement,
            TargetedAction<? super Actor, ? super TargetableCharacter> action) {
        ExceptionHelper.checkNotNullArgument(targetNeed, "targetNeed");
        ExceptionHelper.checkNotNullArgument(requirement, "requirement");
        ExceptionHelper.checkNotNullArgument(action, "action");

        this.targetNeed = targetNeed;
        this.requirement = requirement;
        this.action = action;
    }

    public static <Actor> TargetNeed combineNeeds(Player player, Collection<? extends PlayActionDef<Actor>> actions) {
        TargetNeed result = TargetNeed.NO_NEED;
        for (PlayActionDef<?> action: actions) {
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

    public TargetedAction<? super Actor, ? super TargetableCharacter> getAction() {
        return action;
    }

    @Override
    public UndoAction alterWorld(World world, PlayArg<Actor> arg) {
        return action.alterWorld(world, arg.getActor(), arg.getTarget().getTarget());
    }

    @Override
    public String toString() {
        return "PlayCardActionDef{" + "targetNeed=" + targetNeed + ", action=" + action + '}';
    }
}
