package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.PlayerProperty;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.actions.WorldEventAction;
import com.github.kelemen.brazier.actions.WorldEventActionDefs;
import org.jtrim.utils.ExceptionHelper;

public final class LivingEntitysAbilities<Self extends PlayerProperty> {
    private final ActivatableAbility<? super Self> ability;
    private final WorldEventActionDefs<Self> eventActionDefs;
    private final WorldEventAction<? super Self, ? super Self> deathRattle;

    public LivingEntitysAbilities(
            ActivatableAbility<? super Self> ability,
            WorldEventActionDefs<Self> eventActionDefs,
            WorldEventAction<? super Self, ? super Self> deathRattle) {
        ExceptionHelper.checkNotNullArgument(eventActionDefs, "eventActionDefs");

        this.ability = ability;
        this.eventActionDefs = eventActionDefs;
        this.deathRattle = deathRattle;
    }

    public static <Self extends PlayerProperty> LivingEntitysAbilities<Self> noAbilities() {
        return new LivingEntitysAbilities<>(
            null,
            new WorldEventActionDefs.Builder<Self>().create(),
            null);
    }

    public ActivatableAbility<? super Self> tryGetAbility() {
        return ability;
    }

    public ActivatableAbility<? super Self> getAbility() {
        return ability != null
                ? ability
                : (self) -> UndoableUnregisterRef.UNREGISTERED_REF;
    }

    public WorldEventActionDefs<Self> getEventActionDefs() {
        return eventActionDefs;
    }

    public WorldEventAction<? super Self, ? super Self> tryGetDeathRattle() {
        return deathRattle;
    }

    public WorldEventAction<? super Self, ? super Self> getDeathRattle() {
        return deathRattle != null
                ? deathRattle
                : (world, self, eventSource) -> UndoAction.DO_NOTHING;
    }
}
