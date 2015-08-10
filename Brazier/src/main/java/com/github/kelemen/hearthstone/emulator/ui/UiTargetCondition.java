package com.github.kelemen.hearthstone.emulator.ui;

import java.util.function.Consumer;
import org.jtrim.utils.ExceptionHelper;

public final class UiTargetCondition {
    private final Object condition;
    private final Consumer<Object> callback;

    public UiTargetCondition(Object condition, Consumer<Object> callback) {
        ExceptionHelper.checkNotNullArgument(condition, "condition");

        this.condition = condition;
        this.callback = callback;
    }

    public Object getCondition() {
        return condition;
    }

    public Consumer<Object> getCallback() {
        return callback;
    }
}
