package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class Graveyard {
    private final List<Minion> deadMinions;
    private final List<Minion> deadMinionsView;

    private List<Minion> minionsDiedThisTurn;

    public Graveyard() {
        this.deadMinions = new ArrayList<>();
        this.deadMinionsView = Collections.unmodifiableList(deadMinions);
        this.minionsDiedThisTurn = new ArrayList<>();
    }

    private static <T> boolean containsAll(Set<? extends T> set, T[] elements) {
        for (T element: elements) {
            if (!set.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public UndoAction refresh() {
        if (minionsDiedThisTurn.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        List<Minion> prevMinionsDiedThisTurn = minionsDiedThisTurn;
        minionsDiedThisTurn = new ArrayList<>();
        return () -> minionsDiedThisTurn = prevMinionsDiedThisTurn;
    }

    public int getNumberOfMinionsDiedThisTurn() {
        return minionsDiedThisTurn.size();
    }

    public List<Minion> getMinionsDiedThisTurn() {
        if (minionsDiedThisTurn.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(minionsDiedThisTurn);
    }

    public boolean hasWithKeyword(Keyword[] keywords) {
        ExceptionHelper.checkNotNullElements(keywords, "keywords");

        for (Minion deadMinion: deadMinions) {
            if (containsAll(deadMinion.getKeywords(), keywords)) {
                return true;
            }
        }
        return false;
    }

    public List<Minion> getDeadMinions() {
        return deadMinionsView;
    }

    public UndoAction addDeadMinion(Minion minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        minionsDiedThisTurn.add(minion);
        deadMinions.add(minion);
        return () -> {
            deadMinions.remove(deadMinions.size() - 1);
            minionsDiedThisTurn.remove(minionsDiedThisTurn.size() - 1);
        };
    }
}
