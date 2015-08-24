package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.cards.PlayAction;
import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;

public final class PlayActionDef<Actor> {
    public static final PlayActionDef<Card> UNPLAYABLE_CARD = new PlayActionDef<>(
            TargetNeed.NO_NEED,
            (target) -> false,
            PlayAction.doNothing());

    private final TargetNeed targetNeed;
    private final PlayActionRequirement requirement;
    private final PlayAction<Actor> action;

    public PlayActionDef(
            TargetNeed targetNeed,
            PlayActionRequirement requirement,
            PlayAction<Actor> action) {
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

    public PlayAction<Actor> getAction() {
        return action;
    }

    public UndoAction doPlay(World world, PlayArg<Actor> arg) {
        return action.doPlay(world, arg);
    }

    @Override
    public String toString() {
        return "PlayCardActionDef{" + "targetNeed=" + targetNeed + ", action=" + action + '}';
    }
}
