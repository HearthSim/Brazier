package com.github.kelemen.brazier.actions;

import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface PermanentBuff<Target> {
    public UndoAction buff(World world, Target target);

    public static <Target> PermanentBuff<Target> merge(
            Collection<? extends PermanentBuff<Target>> buffs) {
        List<PermanentBuff<Target>> buffsCopy = new ArrayList<>(buffs);
        ExceptionHelper.checkNotNullElements(buffsCopy, "buffs");

        int count = buffsCopy.size();
        if (count == 0) {
            return (world, actor) -> UndoAction.DO_NOTHING;
        }
        if (count == 1) {
            return buffsCopy.get(0);
        }

        return (World world, Target target) -> {
            UndoBuilder result = new UndoBuilder(buffsCopy.size());
            for (PermanentBuff<Target> buff: buffsCopy) {
                result.addUndo(buff.buff(world, target));
            }
            return result;
        };
    }
}
