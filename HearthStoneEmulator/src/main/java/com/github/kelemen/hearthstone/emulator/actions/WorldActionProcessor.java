package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class WorldActionProcessor implements WorldActionQueue {
    private final World world;
    private final RefList<WorldAction> actionQueue;

    public WorldActionProcessor(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");

        this.world = world;
        this.actionQueue = new RefLinkedList<>();
    }

    private static WorldAction createSetUndoRefAction(WorldAction action, AtomicReference<UndoAction> actionUndoRef) {
        return (world) -> {
            UndoAction actionUndo = action.alterWorld(world);
            actionUndoRef.set(actionUndo);
            return actionUndo;
        };
    }

    @Override
    public UndoAction queueAction(WorldAction action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        AtomicReference<UndoAction> actionUndoRef = new AtomicReference<>(null);
        WorldAction wrapperAction = createSetUndoRefAction(action, actionUndoRef);
        RefList.ElementRef<WorldAction> ref = actionQueue.addLastGetReference(wrapperAction);

        return () -> {
            if (ref.isRemoved()) {
                UndoAction actionUndo = actionUndoRef.getAndSet(null);
                if (actionUndo != null) {
                    actionUndo.undo();
                }
            }
            else {
                ref.remove();
            }
        };
    }

    public void executeActions(CancellationToken cancelToken) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

        while (!actionQueue.isEmpty()) {
            // It is technically possible for checkCanceled
            // to remove items from the action queue but ...
            cancelToken.checkCanceled();

            WorldAction action = actionQueue.remove(0);
            action.alterWorld(world);
        }
    }
}
