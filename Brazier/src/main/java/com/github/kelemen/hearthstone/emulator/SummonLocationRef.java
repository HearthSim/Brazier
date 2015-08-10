package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import org.jtrim.utils.ExceptionHelper;

public interface SummonLocationRef extends BoardLocationRef {
    public static SummonLocationRef ignoreSummon(Minion minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return new SummonLocationRef() {
            @Override
            public UndoAction replace(Minion summonedMinion) {
                return UndoAction.DO_NOTHING;
            }

            @Override
            public UndoAction summonLeft(Minion summonedMinion) {
                return UndoAction.DO_NOTHING;
            }

            @Override
            public UndoAction summonRight(Minion summonedMinion) {
                return UndoAction.DO_NOTHING;
            }

            @Override
            public BoardLocationRef tryGetLeft() {
                return null;
            }

            @Override
            public BoardLocationRef tryGetRight() {
                return null;
            }

            @Override
            public boolean isOnBoard() {
                return false;
            }

            @Override
            public Minion getMinion() {
                return minion;
            }

            @Override
            public UndoAction removeFromBoard() {
                return minion.completeKillAndDeactivate(false);
            }

            @Override
            public UndoAction destroy() {
                return minion.completeKillAndDeactivate(true);
            }
        };
    }

    public UndoAction replace(Minion summonedMinion);

    public UndoAction summonLeft(Minion summonedMinion);
    public UndoAction summonRight(Minion summonedMinion);

    public default UndoAction replace(MinionDescr minionDescr) {
        return replace(new Minion(getMinion().getOwner(), minionDescr));
    }

    public default UndoAction summonLeft(MinionDescr minionDescr) {
        return summonLeft(new Minion(getMinion().getOwner(), minionDescr));
    }

    public default UndoAction summonRight(MinionDescr minionDescr) {
        return summonRight(new Minion(getMinion().getOwner(), minionDescr));
    }
}
