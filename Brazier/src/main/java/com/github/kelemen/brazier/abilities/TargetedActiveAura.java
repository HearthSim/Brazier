package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class TargetedActiveAura<Source, Target> implements ActiveAura {
    private final Source source;
    private final AuraTargetProvider<? super Source, ? extends Target> targetProvider;
    private final AuraFilter<? super Source, ? super Target> targetFilter;
    private final Aura<? super Source, ? super Target> aura;

    private Map<Target, UndoableUnregisterRef> currentlyApplied;

    public TargetedActiveAura(
            Source source,
            AuraTargetProvider<? super Source, ? extends Target> targetProvider,
            AuraFilter<? super Source, ? super Target> targetFilter,
            Aura<? super Source, ? super Target> aura) {
        ExceptionHelper.checkNotNullArgument(source, "source");
        ExceptionHelper.checkNotNullArgument(targetProvider, "targetProvider");
        ExceptionHelper.checkNotNullArgument(targetFilter, "targetFilter");
        ExceptionHelper.checkNotNullArgument(aura, "aura");

        this.source = source;
        this.targetProvider = targetProvider;
        this.targetFilter = targetFilter;
        this.aura = aura;
        this.currentlyApplied = new IdentityHashMap<>(2 * Player.MAX_BOARD_SIZE);
    }

    @Override
    public UndoAction updateAura(World world) {
        ExceptionHelper.checkNotNullArgument(world, "world");

        List<? extends Target> targets = targetProvider.getPossibleTargets(world, source);

        Map<Target, UndoableUnregisterRef> newCurrentlyApplied = new IdentityHashMap<>();

        UndoBuilder result = new UndoBuilder();

        boolean didAnything = false;
        Map<Target, UndoableUnregisterRef> currentlyAppliedCopy = new HashMap<>(currentlyApplied);
        for (Target target: targets) {
            UndoableUnregisterRef ref = currentlyAppliedCopy.remove(target);
            boolean needAura = targetFilter.isApplicable(world, source, target);

            if (ref == null) {
                if (needAura) {
                    UndoableUnregisterRef newRef = aura.applyAura(world, source, target);
                    Objects.requireNonNull(newRef, "Aura.applyAura");

                    result.addUndo(newRef);

                    newCurrentlyApplied.put(target, newRef);
                }
                didAnything = true;
            }
            else {
                if (needAura) {
                    newCurrentlyApplied.put(target, ref);
                }
                else {
                    result.addUndo(ref.unregister());
                    didAnything = true;
                }
            }
        }

        for (UndoableUnregisterRef ref: currentlyAppliedCopy.values()) {
            didAnything = true;
            result.addUndo(ref.unregister());
        }

        if (didAnything) {
            Map<Target, UndoableUnregisterRef> prevCurrentlyApplied = currentlyApplied;
            currentlyApplied = newCurrentlyApplied;
            result.addUndo(() -> currentlyApplied = prevCurrentlyApplied);
        }

        return result;
    }

    @Override
    public UndoAction deactivate() {
        if (currentlyApplied.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder();

        Map<Target, UndoableUnregisterRef> prevCurrentlyApplied = currentlyApplied;
        currentlyApplied = new IdentityHashMap<>();
        result.addUndo(() -> currentlyApplied = prevCurrentlyApplied);

        for (UndoableUnregisterRef ref: prevCurrentlyApplied.values()) {
            result.addUndo(ref.unregister());
        }

        return result;
    }
}
