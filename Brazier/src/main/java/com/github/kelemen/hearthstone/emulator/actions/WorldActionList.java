package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldEvents;
import java.util.ArrayList;
import java.util.Collection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class WorldActionList<T> {
    private final RefList<ActionWrapper<T>> actions;

    public WorldActionList() {
        this.actions = new RefLinkedList<>();
    }

    public UndoableUnregisterRef addAction(WorldObjectAction<T> action) {
        return addAction(WorldEvents.NORMAL_PRIORITY, action);
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

    public UndoableUnregisterRef addAction(int priority, WorldObjectAction<T> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        ActionWrapper<T> wrappedAction = new ActionWrapper<>(priority, action);

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

    public WorldActionEvents<T> toEventContainer(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        return new WorldActionEvents<T>() {
            @Override
            public UndoableUnregisterRef addAction(int priority, WorldObjectAction<T> action) {
                return WorldActionList.this.addAction(priority, action);
            }

            @Override
            public UndoAction triggerEvent(boolean delayable, T object) {
                return WorldActionList.this.executeActionsNow(world, object);
            }
        };
    }

    public UndoAction executeActionsNow(World world, T object) {
        // We have to copy the actions to avoid problems when an action modifies this list.
        return executeActionsNow(world, object, new ArrayList<>(actions));
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

    public UndoAction queueActions(WorldActionQueue queue, T object) {
        if (actions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder();
        for (WorldObjectAction<? super T> action: actions) {
            result.addUndo(queue.queueAction(action.toWorldAction(object)));
        }

        return result;
    }

    private static final class ActionWrapper<T> implements WorldObjectAction<T> {
        private final int priority;
        private final WorldObjectAction<? super T> wrapped;

        public ActionWrapper(int priority, WorldObjectAction<? super T> wrapped) {
            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
            this.priority = priority;
            this.wrapped = wrapped;
        }

        @Override
        public UndoAction alterWorld(World world, T object) {
            return wrapped.alterWorld(world, object);
        }
    }

    private static final class Ref<T> {
        public T obj;

        public Ref(T obj) {
            this.obj = obj;
        }
    }
}
