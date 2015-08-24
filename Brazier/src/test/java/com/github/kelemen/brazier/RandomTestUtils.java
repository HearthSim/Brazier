package com.github.kelemen.brazier;

import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.minions.MinionId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public final class RandomTestUtils {
    public static MinionId singleMinionScript(String minionLocation, Consumer<PlayScript> scriptConfig) {
        AtomicReference<MinionId> resultRef = new AtomicReference<>(null);

        PlayScript.testScript((script) -> {
            scriptConfig.accept(script);

            script.expectMinion(minionLocation, (minion) -> {
                MinionId id = minion.getBaseDescr().getId();
                MinionId prevRef = resultRef.get();
                if (prevRef != null) {
                    assertEquals("Expected same minion for all runs.", prevRef, id);
                }
                else {
                    resultRef.set(id);
                }
            });
        });

        return resultRef.get();
    }

    public static List<MinionId> boardMinionScript(String playerName, Consumer<PlayScript> scriptConfig) {
        AtomicReference<List<MinionId>> resultRef = new AtomicReference<>(null);

        PlayScript.testScript((script) -> {
            scriptConfig.accept(script);

            script.expectPlayer(playerName, (player) -> {
                List<MinionId> minions = new ArrayList<>();
                for (Minion minion: player.getBoard().getAllMinions()) {
                    minions.add(minion.getBaseDescr().getId());
                }

                List<MinionId> prevRef = resultRef.get();
                if (prevRef != null) {
                    assertEquals("Expected same minion for all runs.", prevRef, minions);
                }
                else {
                    resultRef.set(minions);
                }
            });
        });

        return resultRef.get();
    }

    private RandomTestUtils() {
        throw new AssertionError();
    }
}
