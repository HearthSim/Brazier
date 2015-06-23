package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import java.util.HashMap;
import java.util.Map;
import org.jtrim.utils.ExceptionHelper;

public final class PropertyContainer {
    private final Map<Object, Object> values;

    public PropertyContainer() {
        this.values = new HashMap<>();
    }

    private Object getAndSet(Object key, Object value) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        if (value == null) {
            return values.remove(key);
        }
        else {
            return values.put(key, value);
        }
    }

    public UndoAction setValue(Object key, Object value) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        Object prevValue = getAndSet(key, value);
        if (prevValue == value) {
            return UndoAction.DO_NOTHING;
        }

        return () -> {
            getAndSet(key, prevValue);
        };
    }

    public Object getValue(Object key) {
        return values.get(key);
    }
}
