package com.github.kelemen.hearthstone.emulator.actions;

public interface UndoableAction {
    public static final UndoableAction DO_NOTHING = () -> UndoAction.DO_NOTHING;

    public UndoAction doAction();
}
