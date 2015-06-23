package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldActionEventsRegistry;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventAction;
import com.github.kelemen.hearthstone.emulator.actions.WorldEventFilter;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.NamedArg;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class CardAbilities {
    public static ActivatableAbility<Card> onMinionKilledAbility(
            @NamedArg("filter") WorldEventFilter<? super Card, ? super Minion> filter,
            @NamedArg("action") WorldEventAction<? super Card, ? super Minion> action) {
        return onEventAbility(filter, action, WorldEvents::minionKilledListeners);
    }

    public static <EventArg> ActivatableAbility<Card> onEventAbility(
            WorldEventFilter<? super Card, ? super EventArg> filter,
            WorldEventAction<? super Card, ? super EventArg> action,
            Function<WorldEvents, WorldActionEventsRegistry<EventArg>> listenerRegsitryGetter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        ExceptionHelper.checkNotNullArgument(action, "action");
        ExceptionHelper.checkNotNullArgument(listenerRegsitryGetter, "listenerRegsitryGetter");

        return (Card self) -> {
            WorldEvents events = self.getWorld().getEvents();
            WorldActionEventsRegistry<EventArg> listeners = listenerRegsitryGetter.apply(events);
            return listeners.addAction((World world, EventArg eventArg) -> {
                if (filter.applies(world, self, eventArg)) {
                    return action.alterWorld(world, self, eventArg);
                }
                else {
                    return UndoAction.DO_NOTHING;
                }
            });
        };
    }

    private CardAbilities() {
        throw new AssertionError();
    }
}
