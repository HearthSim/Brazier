package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRefBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface ActivatableAbility<Self> {
    public UndoableUnregisterRef activate(Self self);

    public static <Self> ActivatableAbility<Self> merge(Collection<? extends ActivatableAbility<Self>> abilities) {
        List<ActivatableAbility<Self>> abilitiesCopy = new ArrayList<>(abilities);
        ExceptionHelper.checkNotNullElements(abilitiesCopy, "abilities");

        int count = abilitiesCopy.size();
        if (count == 0) {
            return (self) -> UndoableUnregisterRef.UNREGISTERED_REF;
        }
        if (count == 1) {
            return abilitiesCopy.get(0);
        }

        return (Self self) -> {
            UndoableUnregisterRefBuilder result = new UndoableUnregisterRefBuilder(abilitiesCopy.size());
            for (ActivatableAbility<Self> ability: abilitiesCopy) {
                result.addRef(ability.activate(self));
            }
            return result;
        };
    }
}
