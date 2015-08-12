package com.github.kelemen.hearthstone.emulator.actions;

import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.World;

public interface CharacterTargetedAction extends WorldObjectAction<TargetableCharacter> {
    @Override
    public UndoAction alterWorld(World world, TargetableCharacter target);

    public default BattleCryTargetedAction toBattleCryTargetedAction() {
        return (World world, BattleCryArg arg) -> {
            TargetableCharacter target = arg.getTarget().getTarget();
            if (target == null) {
                return UndoAction.DO_NOTHING;
            }

            return alterWorld(world, target);
        };
    }

    public default ActorlessTargetedAction toTargetedAction() {
        return (World world, PlayTarget target) -> alterWorld(world, target.getTarget());
    }
}
