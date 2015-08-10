package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.abilities.ActivatableAbility;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoableUnregisterRef;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class Secret implements PlayerProperty, WorldProperty, LabeledEntity {
    private Player owner;
    private final CardDescr baseCard;
    private final ActivatableAbility<? super Secret> ability;
    private UndoableUnregisterRef ref;

    public Secret(Player owner, CardDescr baseCard, ActivatableAbility<? super Secret> ability) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(baseCard, "baseCard");
        ExceptionHelper.checkNotNullArgument(ability, "ability");

        this.owner = owner;
        this.baseCard = baseCard;
        this.ability = ability;
        this.ref = null;
    }

    public UndoAction setOwner(Player newOwner) {
        ExceptionHelper.checkNotNullArgument(newOwner, "newOwner");

        Player prevOwner = owner;
        owner = newOwner;
        return () -> owner = prevOwner;
    }

    public CardDescr getBaseCard() {
        return baseCard;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return baseCard.getKeywords();
    }

    public EntityId getSecretId() {
        return baseCard.getId();
    }

    public UndoAction activate() {
        if (ref != null) {
            return UndoAction.DO_NOTHING;
        }

        UndoableUnregisterRef newRef = ability.activate(this);
        ref = newRef;
        return () -> {
            newRef.unregister();
            ref = null;
        };
    }

    public UndoAction deactivate() {
        if (ref == null) {
            return UndoAction.DO_NOTHING;
        }

        UndoableUnregisterRef prevRef = ref;
        UndoableUnregisterRef currentRef = ref;
        ref = null;

        UndoAction unregisterUndo = currentRef.unregister();
        return () -> {
            unregisterUndo.undo();
            ref = prevRef;
        };
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public World getWorld() {
        return owner.getWorld();
    }

    @Override
    public String toString() {
        return "Secret: " + getSecretId().getName();
    }
}
