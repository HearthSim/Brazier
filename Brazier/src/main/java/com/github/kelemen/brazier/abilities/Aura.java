package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.actions.UndoableUnregisterRefBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface Aura<Source, Target> {
    public UndoableUnregisterRef applyAura(World world, Source source, Target target);

    public static <Source, Target> Aura<Source, Target> merge(Collection<Aura<? super Source, ? super Target>> auras) {
        List<Aura<? super Source, ? super Target>> aurasCopy = new ArrayList<>(auras);
        ExceptionHelper.checkNotNullElements(aurasCopy, "auras");

        int count = aurasCopy.size();
        if (count == 0) {
            return (world, source, target) -> UndoableUnregisterRef.UNREGISTERED_REF;
        }
        if (count == 1) {
            // Even if it is not, it shouldn't matter because the returned aura
            // will accept anything a "<Source, Target>" would.
            @SuppressWarnings("unchecked")
            Aura<Source, Target> result = (Aura<Source, Target>)aurasCopy.get(0);
            return result;
        }

        return (world, source, target) -> {
            UndoableUnregisterRefBuilder result = new UndoableUnregisterRefBuilder(aurasCopy.size());
            for (Aura<? super Source, ? super Target> aura: aurasCopy) {
                result.addRef(aura.applyAura(world, source, target));
            }
            return result;
        };
    }
}
