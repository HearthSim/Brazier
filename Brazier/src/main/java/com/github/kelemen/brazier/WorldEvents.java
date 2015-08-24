package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.AttackRequest;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.actions.WorldActionEvents;
import com.github.kelemen.brazier.actions.WorldActionEventsRegistry;
import com.github.kelemen.brazier.actions.WorldActionList;
import com.github.kelemen.brazier.actions.WorldObjectAction;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.weapons.Weapon;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class WorldEvents {
    private final World world;

    private final WorldActionEvents<Card> drawCardListeners;
    private final WorldActionEvents<CardPlayEvent> startPlayingCardListeners;
    private final WorldActionEvents<CardPlayedEvent> donePlayingCardListeners;

    private final CompletableWorldActionEvents<Minion> summoningListeners;

    private final WorldActionEvents<DamageRequest> prepareDamageListeners;

    private final WorldActionEvents<DamageEvent> heroDamagedListeners;
    private final WorldActionEvents<DamageEvent> minionDamagedListeners;
    private final WorldActionEvents<Minion> minionKilledListeners;
    private final WorldActionEvents<Weapon> weaponDestroyedListeners;

    private final WorldActionEvents<ArmorGainedEvent> armorGainedListeners;
    private final WorldActionEvents<DamageEvent> heroHealedListeners;
    private final WorldActionEvents<DamageEvent> minionHealedListeners;

    private final WorldActionEvents<Player> turnStartsListeners;
    private final WorldActionEvents<Player> turnEndsListeners;

    private final WorldActionEvents<AttackRequest> attackListeners;
    private final WorldActionEvents<Secret> secretRevealedListeners;

    private final AtomicReference<WorldActionList<Void>> pauseCollectorRef;

    // These listeners containers are just convenience methods to access
    // summoninListeners
    private final WorldActionEventsRegistry<Minion> startSummoningListeners;
    private final WorldActionEventsRegistry<Minion> doneSummoningListeners;

    public WorldEvents(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");

        this.world = world;
        this.pauseCollectorRef = new AtomicReference<>(null);

        this.drawCardListeners = createEventContainer();
        this.startPlayingCardListeners = createEventContainer();
        this.donePlayingCardListeners = createEventContainer();
        this.summoningListeners = createCompletableWorldActionEvents();
        this.prepareDamageListeners = createEventContainer();
        this.heroDamagedListeners = createEventContainer();
        this.minionDamagedListeners = createEventContainer();
        this.minionKilledListeners = createEventContainer();
        this.weaponDestroyedListeners = createEventContainer();
        this.armorGainedListeners = createEventContainer();
        this.heroHealedListeners = createEventContainer();
        this.minionHealedListeners = createEventContainer();
        this.attackListeners = createEventContainer();
        this.secretRevealedListeners = createEventContainer();
        this.turnStartsListeners = createEventContainer();
        this.turnEndsListeners = createEventContainer();

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

    public WorldActionEvents<ArmorGainedEvent> armorGainedListeners() {
        return armorGainedListeners;
    }

    public WorldActionEvents<DamageEvent> heroHealedListeners() {
        return heroHealedListeners;
    }

    public WorldActionEvents<DamageEvent> minionHealedListeners() {
        return minionHealedListeners;
    }

    public WorldActionEvents<DamageRequest> prepareDamageListeners() {
        return prepareDamageListeners;
    }

    public WorldActionEvents<DamageEvent> heroDamagedListeners() {
        return heroDamagedListeners;
    }

    public WorldActionEvents<DamageEvent> minionDamagedListeners() {
        return minionDamagedListeners;
    }

    public WorldActionEvents<Minion> minionKilledListeners() {
        return minionKilledListeners;
    }

    public WorldActionEvents<Weapon> weaponDestroyedListeners() {
        return weaponDestroyedListeners;
    }

    public WorldActionEvents<Card> drawCardListeners() {
        return drawCardListeners;
    }

    public WorldActionEvents<CardPlayEvent> startPlayingCardListeners() {
        return startPlayingCardListeners;
    }

    public WorldActionEvents<CardPlayedEvent> donePlayingCardListeners() {
        return donePlayingCardListeners;
    }

    public CompletableWorldActionEvents<Minion> summoningListeners() {
        return summoningListeners;
    }

    public WorldActionEvents<AttackRequest> attackListeners() {
        return attackListeners;
    }

    public WorldActionEvents<Player> turnStartsListeners() {
        return turnStartsListeners;
    }

    public WorldActionEvents<Player> turnEndsListeners() {
        return turnEndsListeners;
    }

    public WorldActionEventsRegistry<Minion> startSummoningListeners() {
        return startSummoningListeners;
    }

    public WorldActionEventsRegistry<Minion> doneSummoningListeners() {
        return doneSummoningListeners;
    }

    public WorldActionEvents<Secret> secretRevealedListeners() {
        return secretRevealedListeners;
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
