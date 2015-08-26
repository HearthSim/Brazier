package com.github.kelemen.brazier.events;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.UndoableResult;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableAction;
import com.github.kelemen.brazier.actions.WorldActionList;
import com.github.kelemen.brazier.actions.WorldObjectAction;
import com.github.kelemen.brazier.minions.Minion;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class WorldEvents {
    private final World world;

    private final Map<SimpleEventType, WorldActionEvents<?>> simpleListeners;
    private final CompletableWorldActionEvents<Minion> summoningListeners;

    private final AtomicReference<WorldActionList<Void>> pauseCollectorRef;

    // These listeners containers are just convenience methods to access
    // summoninListeners
    private final WorldActionEventsRegistry<Minion> startSummoningListeners;
    private final WorldActionEventsRegistry<Minion> doneSummoningListeners;

    public WorldEvents(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");

        this.world = world;
        this.pauseCollectorRef = new AtomicReference<>(null);

        this.simpleListeners = new EnumMap<>(SimpleEventType.class);
        this.summoningListeners = createCompletableWorldActionEvents();

        this.startSummoningListeners = (int priority, WorldObjectAction<Minion> action) -> {
            return summoningListeners.addListener(priority, (World eventWorld, Minion minion) -> {
                UndoAction actionUndo = action.alterWorld(world, minion);
                return CompleteWorldObjectAction.doNothing(actionUndo);
            });
        };
        this.doneSummoningListeners = (int priority, WorldObjectAction<Minion> action) -> {
            return summoningListeners.addListener(priority, (World eventWorld, Minion minion) -> {
                return CompleteWorldObjectAction.nothingToUndo(action);
            });
        };
    }

    private <T> WorldActionEvents<T> tryGetSimpleListeners(SimpleEventType eventType, Class<T> argType) {
        if (argType != eventType.getArgumentType()) {
            throw new IllegalArgumentException("The requested listener has a different argument type."
                    + " Requested: " + argType.getName()
                    + ". Expected: " + eventType.getArgumentType());
        }

        @SuppressWarnings("unchecked")
        WorldActionEvents<T> result = (WorldActionEvents<T>)simpleListeners.get(eventType);
        return result;
    }

    public <T> WorldActionEvents<T> simpleListeners(SimpleEventType eventType, Class<T> argType) {
        WorldActionEvents<T> result = tryGetSimpleListeners(eventType, argType);
        if (result == null) {
            result = createEventContainer();
            simpleListeners.put(eventType, result);
        }

        return result;
    }

    public <T> UndoAction triggerEventNow(SimpleEventType eventType, T arg) {
        return triggerEvent(eventType, arg, false);
    }

    public <T> UndoAction triggerEvent(SimpleEventType eventType, T arg) {
        return triggerEvent(eventType, arg, true);
    }

    private <T> UndoAction triggerEvent(SimpleEventType eventType, T arg, boolean delayable) {
        Class<?> expectedArgType = eventType.getArgumentType();
        if (!expectedArgType.isInstance(arg)) {
            throw new IllegalArgumentException("The requested listener has a different argument type."
                    + " Requested: " + arg.getClass().getName()
                    + ". Expected: " + eventType.getArgumentType());
        }

        @SuppressWarnings("unchecked")
        WorldActionEvents<T> listeners = tryGetSimpleListeners(eventType, (Class<T>)eventType.getArgumentType());
        if (listeners == null) {
            return UndoAction.DO_NOTHING;
        }
        return listeners.triggerEvent(delayable, arg);
    }

    public CompletableWorldActionEvents<Minion> summoningListeners() {
        return summoningListeners;
    }

    public WorldActionEventsRegistry<Minion> startSummoningListeners() {
        return startSummoningListeners;
    }

    public WorldActionEventsRegistry<Minion> doneSummoningListeners() {
        return doneSummoningListeners;
    }

    public WorldActionEvents<Player> turnStartsListeners() {
        return simpleListeners(SimpleEventType.TURN_STARTS, Player.class);
    }

    public WorldActionEvents<Player> turnEndsListeners() {
        return simpleListeners(SimpleEventType.TURN_ENDS, Player.class);
    }

    /**
     * Executes the given action, ensuring that it won't be interrupted by event notifications
     * scheduled to any of the event listeners of this {@code WorlEvents} object. If the
     * given action would be interrupted by an event notification, the event notification
     * is suspended until the specified action returns.
     * <P>
     * Calls to {@code doAtomic} can be nested in which case event notifications will
     * be executed once the outer most atomic action returns.
     * <P>
     * <B>Warning</B>: This method is <B>not</B> thread-safe.
     *
     * @param action the action to be executed. This argument cannot be {@code null}.
     * @return the action which might be used to undo what this call did including the actions
     *   done by the caused events. This method may never return {@code null}.
     */
    public UndoAction doAtomic(UndoableAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        if (pauseCollectorRef.get() != null) {
            // A caller already ensures this call to be atomic
            return action.doAction();
        }
        else {
            WorldActionList<Void> currentCollector = new WorldActionList<>();
            UndoAction actionUndo;
            try {
                pauseCollectorRef.compareAndSet(null, currentCollector);
                actionUndo = action.doAction();
            } finally {
                pauseCollectorRef.compareAndSet(currentCollector, null);
            }

            UndoAction eventUndo = currentCollector.executeActionsNow(world, null);
            return () -> {
                eventUndo.undo();
                actionUndo.undo();
            };
        }
    }

    private static <T> WorldObjectAction<Void> toAction(WorldActionList<T> actionList, T object) {
        return (world, voidArg) -> actionList.executeActionsNow(world, object);
    }

    private <T> WorldActionEvents<T> createEventContainer() {
        WorldActionList<T> actionList = new WorldActionList<>();

        return new WorldActionEvents<T>() {
            @Override
            public UndoableUnregisterRef addAction(int priority, WorldObjectAction<T> action) {
                return actionList.addAction(priority, action);
            }

            @Override
            public UndoAction triggerEvent(boolean delayable, T object) {
                WorldActionList<Void> pauseCollector = pauseCollectorRef.get();
                if (pauseCollector != null && delayable) {
                    UndoableUnregisterRef actionRegRef = pauseCollector.addAction(toAction(actionList, object));
                    return actionRegRef::undo;
                }
                else {
                    return actionList.executeActionsNow(world, object);
                }
            }
        };
    }

    private <T> CompletableWorldActionEvents<T> createCompletableWorldActionEvents() {
        CompletableWorldActionEvents<T> wrapped = new DefaultCompletableWorldActionEvents<>(world);

        return new CompletableWorldActionEvents<T>() {
            @Override
            public UndoableUnregisterRef addListener(int priority, CompletableWorldObjectAction<? super T> listener) {
                return wrapped.addListener(priority, listener);
            }

            @Override
            public UndoableResult<UndoableAction> triggerEvent(boolean delayable, T object) {
                WorldActionList<Void> pauseCollector = pauseCollectorRef.get();
                if (pauseCollector != null && delayable) {
                    // If the returned finalizer has been called, then
                    // we have to call the finalizer immediately after triggering
                    // the event. Otherwise, we set a reference after triggering the
                    // event and then it is the client's responsiblity to notify
                    // the finalizer.

                    AtomicReference<UndoableAction> finalizerRef = new AtomicReference<>(null);

                    WorldObjectAction<Void> delayedAction = (World actionWorld, Void ignored) -> {
                        UndoableResult<UndoableAction> triggerResult = wrapped.triggerEvent(object);

                        UndoAction finalizeUndo;
                        UndoableAction finalizer = triggerResult.getResult();
                        if (!finalizerRef.compareAndSet(null, finalizer)) {
                            finalizeUndo = finalizer.doAction();
                        }
                        else {
                            finalizeUndo = UndoAction.DO_NOTHING;
                        }

                        return () -> {
                            finalizeUndo.undo();
                            triggerResult.undo();
                        };
                    };

                    UndoableAction finalizer = () -> {
                        UndoableAction currentFinalizer = finalizerRef.getAndSet(() -> UndoAction.DO_NOTHING);
                        return currentFinalizer != null
                                ? currentFinalizer.doAction()
                                : UndoAction.DO_NOTHING;
                    };

                    UndoableUnregisterRef actionRegRef = pauseCollector.addAction(delayedAction);
                    return new UndoableResult<>(finalizer, actionRegRef);
                }
                else {
                    return wrapped.triggerEvent(object);
                }
            }
        };
    }
}
