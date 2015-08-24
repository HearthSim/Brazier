package com.github.kelemen.brazier.event;

import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.actions.UndoableAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class DefaultCompletableWorldActionEvents<T>
implements
        CompletableWorldActionEvents<T> {

    private final World world;
    private final RefList<ListenerWrapper<? super T>> listeners;

    public DefaultCompletableWorldActionEvents(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        this.world = world;
        this.listeners = new RefLinkedList<>();
    }

    private static <T> int getPriority(RefList.ElementRef<ListenerWrapper<? super T>> ref) {
        return ref.getElement().priority;
    }

    private RefList.ElementRef<?> insert(ListenerWrapper<? super T> listener) {
        int priority = listener.priority;
        RefList.ElementRef<ListenerWrapper<? super T>> previousRef = listeners.getLastReference();
        while (previousRef != null && getPriority(previousRef) < priority) {
            previousRef = previousRef.getPrevious(1);
        }

        return previousRef != null
                ? previousRef.addAfter(listener)
                : listeners.addFirstGetReference(listener);
    }

    @Override
    public UndoableUnregisterRef addListener(int priority, CompletableWorldObjectAction<? super T> listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        ListenerWrapper<? super T> wrappedAction = new ListenerWrapper<>(priority, listener);

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
                    actionRefRef.obj = listeners.addGetReference(index, wrappedAction);
                };
            }

            @Override
            public void undo() {
                actionRefRef.obj.remove();
            }
        };
    }

    private UndoableAction combineCompleteActions(
            T object,
            List<CompleteWorldObjectAction<? super T>> actions) {
        return () -> {
            UndoBuilder result = new UndoBuilder(actions.size());
            for (CompleteWorldObjectAction<? super T> action: actions) {
                result.addUndo(action.alterWorld(world, object));
            }
            return result;
        };
    }

    @Override
    public UndoableResult<UndoableAction> triggerEvent(boolean delayable, T object) {
        if (listeners.isEmpty()) {
            return new UndoableResult<>(() -> UndoAction.DO_NOTHING);
        }

        List<CompletableWorldObjectAction<? super T>> currentListeners = new ArrayList<>(listeners);
        List<CompleteWorldObjectAction<? super T>> result = new ArrayList<>(currentListeners.size());

        UndoBuilder undos = new UndoBuilder(currentListeners.size());
        for (CompletableWorldObjectAction<? super T> listener: currentListeners) {
            CompleteWorldObjectAction<? super T> completeAction = listener.startAlterWorld(world, object);
            undos.addUndo(completeAction);
            result.add(completeAction);
        }

        return new UndoableResult<>(combineCompleteActions(object, result), undos);
    }

    @Override
    public UndoableResult<UndoableAction> triggerEvent(T object) {
        return triggerEvent(true, object);
    }

    private static final class ListenerWrapper<T> implements CompletableWorldObjectAction<T> {
        private final int priority;
        private final CompletableWorldObjectAction<T> wrapped;

        public ListenerWrapper(int priority, CompletableWorldObjectAction<T> wrapped) {
            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
            this.priority = priority;
            this.wrapped = wrapped;
        }

        @Override
        public CompleteWorldObjectAction<T> startAlterWorld(World world, T object) {
            return wrapped.startAlterWorld(world, object);
        }
    }

    private static final class Ref<T> {
        public T obj;

        public Ref(T obj) {
            this.obj = obj;
        }
    }
}
