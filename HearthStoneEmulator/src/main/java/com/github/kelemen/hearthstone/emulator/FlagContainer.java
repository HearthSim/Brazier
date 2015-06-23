package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import java.util.HashMap;
import java.util.Map;
import org.jtrim.utils.ExceptionHelper;

public final class FlagContainer {
    private final Map<Object, Integer> flags;

    public FlagContainer() {
        this.flags = new HashMap<>();
    }

    public boolean hasFlag(Object flag) {
        ExceptionHelper.checkNotNullArgument(flag, "flag");
        return flags.containsKey(flag);
    }

    public UndoableUnregisterRef registerFlag(Object flag) {
        ExceptionHelper.checkNotNullArgument(flag, "flag");

        int newValue = flags.compute(flag, (key, prevValue) -> prevValue != null ? prevValue + 1 : 1);
        int prevValue = newValue - 1;

        return UndoableUnregisterRef.makeIdempotent(() -> {
            Integer currentValue;
            if (prevValue > 0) {
                currentValue = flags.put(flag, prevValue);
            }
            else {
                currentValue = flags.remove(flag);
            }
            return () -> {
                if (currentValue != null) {
                    flags.put(flag, currentValue);
                }
                else {
                    flags.remove(flag);
                }
            };
        });
    }
}
