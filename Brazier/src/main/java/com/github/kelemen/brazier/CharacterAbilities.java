package com.github.kelemen.brazier;

import com.github.kelemen.brazier.abilities.ActivatableAbilities;
import com.github.kelemen.brazier.actions.UndoAction;

public final class CharacterAbilities<Self extends WorldProperty> implements Silencable {
    private final ActivatableAbilities<Self> ownedAbilities;
    private final ActivatableAbilities<Self> externalAbilities;

    public CharacterAbilities(Self self) {
        this.ownedAbilities = new ActivatableAbilities<>(self);
        this.externalAbilities = new ActivatableAbilities<>(self);
    }

    private CharacterAbilities(ActivatableAbilities<Self> ownedAbilities, ActivatableAbilities<Self> externalAbilities) {
        this.ownedAbilities = ownedAbilities;
        this.externalAbilities = externalAbilities;
    }

    public PreparedResult<CharacterAbilities<Self>> copyFor(Self other) {
        PreparedResult<ActivatableAbilities<Self>> newOwnedAbilities = ownedAbilities.copyFor(other);
        ActivatableAbilities<Self> newExternalAbilities = new ActivatableAbilities<>(other);
        CharacterAbilities<Self> result = new CharacterAbilities<>(
                newOwnedAbilities.getResult(),
                newExternalAbilities);

        return new PreparedResult<>(result, newOwnedAbilities::activate);
    }

    public ActivatableAbilities<Self> getOwned() {
        return ownedAbilities;
    }

    public ActivatableAbilities<Self> getExternal() {
        return externalAbilities;
    }

    @Override
    public UndoAction silence() {
        return ownedAbilities.deactivate();
    }

    public UndoAction deactivateAll() {
        return ownedAbilities.deactivate();
    }
}
