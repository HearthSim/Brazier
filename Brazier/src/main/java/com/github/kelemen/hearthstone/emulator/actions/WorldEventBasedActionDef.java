package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.CompletableWorldActionEventsRegistry;
import com.github.kelemen.hearthstone.emulator.CompletableWorldEventBasedActionDef;
import com.github.kelemen.hearthstone.emulator.CompleteWorldEventAction;
import com.github.kelemen.hearthstone.emulator.PlayerProperty;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class WorldEventBasedActionDef<Self extends PlayerProperty, T> {
    private final boolean triggerOnce;
    private final int priority;
    private final Function<WorldEvents, ? extends WorldActionEventsRegistry<T>> actionEventListenersGetter;
    private final WorldEventFilter<? super Self, ? super T> sourceFilter;
    private final WorldEventAction<? super Self, ? super T> eventAction;

    public WorldEventBasedActionDef(
            int priority,
            Function<WorldEvents, ? extends WorldActionEventsRegistry<T>> actionEventListenersGetter,
            WorldEventFilter<? super Self, ? super T> condition,
            WorldEventAction<? super Self, ? super T> eventAction) {
        this(false, priority, actionEventListenersGetter, condition, eventAction);
    }

    public WorldEventBasedActionDef(
            boolean triggerOnce,
            int priority,
            Function<WorldEvents, ? extends WorldActionEventsRegistry<T>> actionEventListenersGetter,
            WorldEventFilter<? super Self, ? super T> condition,
            WorldEventAction<? super Self, ? super T> eventAction) {
        ExceptionHelper.checkNotNullArgument(actionEventListenersGetter, "actionEventListenersGetter");
        ExceptionHelper.checkNotNullArgument(condition, "condition");
        ExceptionHelper.checkNotNullArgument(eventAction, "eventAction");

        this.triggerOnce = triggerOnce;
        this.priority = priority;
        this.actionEventListenersGetter = actionEventListenersGetter;
        this.sourceFilter = condition;
        this.eventAction = eventAction;
    }

    public static <Self extends PlayerProperty, T> UndoableUnregisterRef registerAll(
            List<WorldEventBasedActionDef<Self, T>> actionDefs,
            WorldEvents worldEvents,
            Self self) {
        if (actionDefs.isEmpty()) {
            return UndoableUnregisterRef.UNREGISTERED_REF;
        }

        UndoableUnregisterRefBuilder result = new UndoableUnregisterRefBuilder(actionDefs.size());
        for (WorldEventBasedActionDef<Self, T> actionDef: actionDefs) {
            result.addRef(actionDef.registerForEvent(worldEvents, self));
        }
        return result;
    }

    private WorldEventAction<Self, T> getActionWithFilter() {
        return (World world, Self self, T eventSource) -> {
            if (sourceFilter.applies(world, self, eventSource)) {
                return eventAction.alterWorld(world, self, eventSource);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        };
    }

    public CompletableWorldEventBasedActionDef<Self, T> toStartEventDef(
            Function<WorldEvents, ? extends CompletableWorldActionEventsRegistry<T>> actionEventListenersGetter) {
        WorldEventAction<Self, T> filteredAction = getActionWithFilter();
        return new CompletableWorldEventBasedActionDef<>(triggerOnce, priority, actionEventListenersGetter, (World world, Self self, T eventSource) -> {
            UndoAction actionUndo = filteredAction.alterWorld(world, self, eventSource);
            return CompleteWorldEventAction.doNothing(actionUndo);
        });
    }

    public CompletableWorldEventBasedActionDef<Self, T> toDoneEventDef(
            Function<WorldEvents, ? extends CompletableWorldActionEventsRegistry<T>> actionEventListenersGetter) {
        WorldEventAction<Self, T> filteredAction = getActionWithFilter();
        return new CompletableWorldEventBasedActionDef<>(triggerOnce, priority, actionEventListenersGetter, (World world, Self self, T eventSource) -> {
            return CompleteWorldEventAction.nothingToUndo(filteredAction);
        });
    }

    private UndoableUnregisterRef registerForEvents(
            WorldActionEventsRegistry<T> actionEvents,
            Self self,
            WorldEventAction<? super Self, ? super T> appliedEventAction) {
        return actionEvents.addAction(priority, (World world, T object) -> {
            if (sourceFilter.applies(world, self, object)) {
                return appliedEventAction.alterWorld(world, self, object);
            }
            else {
                return UndoAction.DO_NOTHING;
            }
        });
    }

    public UndoableUnregisterRef registerForEvent(WorldEvents worldEvents, Self self) {
        WorldActionEventsRegistry<T> actionEvents = actionEventListenersGetter.apply(worldEvents);
        if (!triggerOnce) {
            return registerForEvents(actionEvents, self, eventAction);
        }

        AtomicReference<UndoableUnregisterRef> refRef = new AtomicReference<>();
        UndoableUnregisterRef ref = registerForEvents(actionEvents, self, (World world, Self eventSelf, T eventSource) -> {
            UndoAction unregisterUndo = refRef.get().unregister();
            UndoAction actionUndo = eventAction.alterWorld(world, eventSelf, eventSource);
            return () -> {
                actionUndo.undo();
                unregisterUndo.undo();
            };
        });
        refRef.set(ref);
        return ref;
    }
}
