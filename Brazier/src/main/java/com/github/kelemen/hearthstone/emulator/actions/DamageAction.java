package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.DamageSource;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface DamageAction extends WorldObjectAction<DamageSource> {
    @Override
    public UndoAction alterWorld(World world, DamageSource minion);

    public default BattleCryTargetedAction toBattleCryTargetedAction() {
        return (World world, BattleCryArg arg) -> alterWorld(world, arg.getSource());
    }

    public default CharacterTargetedAction toCharacterTargetedAction() {
        return (World world, TargetableCharacter target) -> {
            return target instanceof DamageSource
                    ? alterWorld(world, (DamageSource)target)
                    : UndoAction.DO_NOTHING;
        };
    }

    public default MinionAction toMinionAction() {
        return (World world, Minion minion) -> alterWorld(world, minion);
    }

    public default WeaponAction toWeaponAction() {
        return (World world, Weapon weapon) -> alterWorld(world, weapon);
    }

    public static DamageAction merge(Collection<? extends DamageAction> actions) {
        List<DamageAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, object) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, DamageSource self) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (DamageAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, self));
            }
            return result;
        };
    }
}
