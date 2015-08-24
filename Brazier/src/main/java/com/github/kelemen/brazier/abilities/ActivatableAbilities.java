package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.PreparedResult;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class ActivatableAbilities<Self> {
    private final Self self;
    private List<CustomAbilityRef<Self>> customAbilities;

    public ActivatableAbilities(Self self) {
        ExceptionHelper.checkNotNullArgument(self, "self");

        this.self = self;
        this.customAbilities = new ArrayList<>();
    }

    public PreparedResult<ActivatableAbilities<Self>> copyFor(Self other) {
        ActivatableAbilities<Self> result = new ActivatableAbilities<>(other);

        List<ActivatableAbility<? super Self>> initialAbilities = new ArrayList<>(customAbilities.size());
        for (CustomAbilityRef<Self> ability: customAbilities) {
            initialAbilities.add(ability.abilityRegisterTask);
        }

        return new PreparedResult<>(result, () -> {
            if (initialAbilities.isEmpty()) {
                return UndoAction.DO_NOTHING;
            }

            UndoBuilder undos = new UndoBuilder(initialAbilities.size());
            for (ActivatableAbility<? super Self> ability: initialAbilities) {
                undos.addUndo(result.addAndActivateAbility(ability));
            }
            return undos;
        });
    }

    public UndoAction addAndActivateAbility(ActivatableAbility<? super Self> abilityRegisterTask) {
        ExceptionHelper.checkNotNullArgument(abilityRegisterTask, "abilityRegisterTask");

        UndoableUnregisterRef registerRef = abilityRegisterTask.activate(self);
        customAbilities.add(new CustomAbilityRef<>(abilityRegisterTask, registerRef));

        return () -> {
            customAbilities.remove(customAbilities.size() - 1);
            registerRef.undo();
        };
    }

    public UndoAction deactivate() {
        if (customAbilities.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        List<CustomAbilityRef<Self>> prevCustomAbilities = customAbilities;
        customAbilities = new ArrayList<>();

        UndoBuilder result = new UndoBuilder();
        result.addUndo(() -> customAbilities = prevCustomAbilities);

        for (CustomAbilityRef<Self> activatedAbility: prevCustomAbilities) {
            result.addUndo(activatedAbility.unregister());
        }

        return result;
    }

    private static final class CustomAbilityRef<Self> {
        public final ActivatableAbility<? super Self> abilityRegisterTask;
        public final UndoableUnregisterRef registerRef;

        public CustomAbilityRef(
                ActivatableAbility<? super Self> abilityRegisterTask,
                UndoableUnregisterRef registerRef) {
            ExceptionHelper.checkNotNullArgument(abilityRegisterTask, "abilityRegisterTask");
            ExceptionHelper.checkNotNullArgument(registerRef, "registerRef");

            this.abilityRegisterTask = abilityRegisterTask;
            this.registerRef = registerRef;
        }

        public UndoAction unregister() {
            return registerRef.unregister();
        }
    }
}
