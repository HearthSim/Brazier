package com.github.kelemen.brazier.actions;

public interface UndoableAction {
    public static final UndoableAction DO_NOTHING = () -> UndoAction.DO_NOTHING;

    public UndoAction doAction();
}
