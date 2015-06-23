package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface WeaponAction extends WorldObjectAction<Weapon> {
    @Override
    public UndoAction alterWorld(World world, Weapon weapon);

    public static WeaponAction merge(Collection<? extends WeaponAction> actions) {
        List<WeaponAction> actionsCopy = new ArrayList<>(actions);
        ExceptionHelper.checkNotNullElements(actionsCopy, "actions");

        int count = actionsCopy.size();
        if (count == 0) {
            return (world, object) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return actionsCopy.get(0);
        }

        return (World world, Weapon self) -> {
            UndoBuilder result = new UndoBuilder(actionsCopy.size());
            for (WeaponAction action: actionsCopy) {
                result.addUndo(action.alterWorld(world, self));
            }
            return result;
        };
    }
}
