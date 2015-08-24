package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.World;

public interface WorldAction extends WorldObjectAction<Void> {
    public static final WorldAction DO_NOTHING = (world) -> UndoAction.DO_NOTHING;

    /**
     * Alters the world and returns an action which can undo the action
     * done by this method, assuming the world is in the same state as
     * it was right after calling this method.
     *
     * @param world the world to be altered by this action. This argument
     *   cannot be {@code null}.
     * @return the action which can undo the action done by this method call.
     *   This may never return {@code null}.
     */
    public UndoAction alterWorld(World world);

    @Override
    public default UndoAction alterWorld(World world, Void object) {
        return alterWorld(world);
    }
}
