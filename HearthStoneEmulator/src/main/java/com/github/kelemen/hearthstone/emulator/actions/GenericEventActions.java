package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class GenericEventActions {
    public static WorldEventAction<PlayerProperty, Object> applyTargetedActionToRandomOwnMinion(
            @NamedArg("action") TargetedAction action) {
        return applyTargetedActionToRandomOwnMinion(action, WorldEventFilter.ANY);
    }

    public static WorldEventAction<PlayerProperty, Object> applyTargetedActionToRandomOwnMinion(
            @NamedArg("action") TargetedAction action,
            @NamedArg("filter") WorldEventFilter<? super PlayerProperty, ? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        return (World world, PlayerProperty self, Object eventSource) -> {
            List<Minion> candidates = new ArrayList<>(Player.MAX_BOARD_SIZE);
            self.getOwner().getBoard().collectAliveMinions(candidates, toPredicate(world, self, filter));

            Minion minion = ActionUtils.pickRandom(world, candidates);
            if (minion == null) {
                return UndoAction.DO_NOTHING;
            }

            PlayTarget playTarget = new PlayTarget(self.getOwner(), minion);
            return action.alterWorld(world, playTarget);
        };
    }

    private static <Self, Target> Predicate<Target> toPredicate(
            World world,
            Self self,
            WorldEventFilter<? super Self, ? super Target> filter) {
        return (target) -> filter.applies(world, self, target);
    }

    private GenericEventActions() {
        throw new AssertionError();
    }
}
