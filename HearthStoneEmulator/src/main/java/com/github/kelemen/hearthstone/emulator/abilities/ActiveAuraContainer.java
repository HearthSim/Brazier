package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class ActiveAuraContainer {
    private final List<AuraWrapper> auras;

    public ActiveAuraContainer() {
        this.auras = new ArrayList<>();
    }

    private <T> int removeAndGetIndex(List<T> list, T value) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (value == list.get(i)) {
                list.remove(i);
                return i;
            }
        }
        return -1;
    }

    public UndoableUnregisterRef addAura(ActiveAura aura) {
        // We wrap "aura" to ensure that we remove the one
        // added by this method call in the returned reference.
        AuraWrapper auraWrapper = new AuraWrapper(aura);
        auras.add(auraWrapper);

        return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                int prevIndex = removeAndGetIndex(auras, auraWrapper);
                UndoAction deactivateUndo = auraWrapper.deactivate();

                if (prevIndex >= 0) {
                    return () -> {
                        deactivateUndo.undo();
                        auras.add(prevIndex, auraWrapper);
                    };
                }
                else {
                    return deactivateUndo;
                }
            }

            @Override
            public void undo() {
                removeAndGetIndex(auras, auraWrapper);
            }
        });
    }

    public UndoAction updateAllAura(World world) {
        if (auras.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder(auras.size());

        // Copy the list to ensure that it does not change during iteration
        for (AuraWrapper aura: new ArrayList<>(auras)) {
            result.addUndo(aura.updateAura(world));
        }

        return result;
    }

    private static final class AuraWrapper {
        private final ActiveAura aura;

        public AuraWrapper(ActiveAura aura) {
            ExceptionHelper.checkNotNullArgument(aura, "aura");
            this.aura = aura;
        }

        public UndoAction updateAura(World world) {
            return aura.updateAura(world);
        }

        public UndoAction deactivate() {
            return aura.deactivate();
        }
    }
}
