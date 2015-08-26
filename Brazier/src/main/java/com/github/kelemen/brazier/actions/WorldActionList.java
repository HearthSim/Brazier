package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Priorities;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class WorldActionList<T> {
    private final RefList<ActionWrapper<T>> actions;

    public WorldActionList() {
        this.actions = new RefLinkedList<>();
    }

    public UndoableUnregisterRef addAction(WorldObjectAction<T> action) {
        return addAction(Priorities.NORMAL_PRIORITY, (arg) -> true, action);
    }

    private static <T> int getPriority(RefList.ElementRef<ActionWrapper<T>> ref) {
        return ref.getElement().priority;
    }

    private RefList.ElementRef<?> insert(ActionWrapper<T> action) {
        int priority = action.priority;
        RefList.ElementRef<ActionWrapper<T>> previousRef = actions.getLastReference();
        while (previousRef != null && getPriority(previousRef) < priority) {
            previousRef = previousRef.getPrevious(1);
        }

        return previousRef != null
                ? previousRef.addAfter(action)
                : actions.addFirstGetReference(action);
    }

    public UndoableUnregisterRef addAction(int priority, Predicate<? super T> condition, WorldObjectAction<? super T> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        ActionWrapper<T> wrappedAction = new ActionWrapper<>(priority, condition, action);

        RefList.ElementRef<?> actionRef = insert(wrappedAction);
        Ref<RefList.ElementRef<?>> actionRefRef = new Ref<>(actionRef);
        return new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                if (actionRefRef.obj.isRemoved()) {
                    return UndoAction.DO_NOTHING;
                }

                int index = actionRefRef.obj.getIndex();
                actionRefRef.obj.remove();
                return () -> {
                    actionRefRef.obj = actions.addGetReference(index, wrappedAction);
                };
            }

            @Override
            public void undo() {
                actionRefRef.obj.remove();
            }
        };
    }

    public WorldAction snapshotCurrentEvents(T object) {
        List<WorldObjectAction<? super T>> snapshot = getApplicableActions(object);
        if (snapshot.isEmpty()) {
            return (world) -> UndoAction.DO_NOTHING;
        }

        return (world) -> executeActionsNow(world, object, snapshot);
    }


    public UndoAction executeActionsNowGreedily(World world, T object) {
        List<ActionWrapper<T>> remaining = new ArrayList<>(actions);
        List<ActionWrapper<T>> skippedActions = new LinkedList<>();

        UndoBuilder result = new UndoBuilder(remaining.size());

        while (!remaining.isEmpty()) {
            for (ActionWrapper<T> actionRef: remaining) {
                if (actionRef.isApplicable(object)) {
                    result.addUndo(actionRef.wrapped.alterWorld(world, object));
                }
                else {
                    skippedActions.add(actionRef);
                }
            }
            remaining.clear();

            Iterator<ActionWrapper<T>> skippedActionsItr = skippedActions.iterator();
            while (skippedActionsItr.hasNext()) {
                ActionWrapper<T> skippedAction = skippedActionsItr.next();
                // FIXME: isApplicable for the first item will be called again
                //   needlessly. This - in theory - can cause an infinite loop.
                //   However, it is reasonable to assume that filters are deterministic.
                //   Still it should be fixed.
                if (skippedAction.isApplicable(object)) {
                    skippedActionsItr.remove();
                    remaining.add(skippedAction);
                }
            }
        }

        return result;
    }

    private static <T> List<WorldObjectAction<? super T>> getApplicableActions(
            T object,
            Collection<ActionWrapper<T>> actions) {
        if (actions.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldObjectAction<? super T>> result = new ArrayList<>(actions.size());
        for (ActionWrapper<T> action: actions) {
            if (action.isApplicable(object)) {
                result.add(action.getAction());
            }
        }
        return result;
    }

    private List<WorldObjectAction<? super T>> getApplicableActions(T object) {
        return getApplicableActions(object, actions);
    }

    public UndoAction executeActionsNow(World world, T object, boolean greedy) {
        if (actions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        if (greedy) {
            return executeActionsNowGreedily(world, object);
        }
        else {
            // We have to first check if the action conditions are met, otherwise
            // two Hobgoblin would be the same as a single hobgoblin (because the first buff
            // would prevent the second to trigger).
            List<WorldObjectAction<? super T>> applicableActions = getApplicableActions(object);
            return executeActionsNow(world, object, applicableActions);
        }
    }

    public static <T> UndoAction executeActionsNow(
            World world,
            T object,
            Collection<? extends WorldObjectAction<? super T>> actions) {

        if (actions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder();
        for (WorldObjectAction<? super T> action: actions) {
            result.addUndo(action.alterWorld(world, object));
        }
        return result;
    }

    private static final class ActionWrapper<T> {
        private final int priority;
        private final Predicate<? super T> condition;
        private final WorldObjectAction<? super T> wrapped;

        public ActionWrapper(int priority, Predicate<? super T> condition, WorldObjectAction<? super T> wrapped) {
            ExceptionHelper.checkNotNullArgument(condition, "condition");
            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

            this.priority = priority;
            this.condition = condition;
            this.wrapped = wrapped;
        }

        public boolean isApplicable(T arg) {
            return condition.test(arg);
        }

        public WorldObjectAction<? super T> getAction() {
            return wrapped;
        }
    }

    private static final class Ref<T> {
        public T obj;

        public Ref(T obj) {
            this.obj = obj;
        }
    }
}
