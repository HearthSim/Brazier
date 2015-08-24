package com.github.kelemen.brazier;

import com.github.kelemen.brazier.minions.MinionId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;
import static org.junit.Assert.*;

public final class ResurrectTest {
    @Test
    public void testResurrectNoDeaths() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);
            script.deck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2");

            script.playCard("p1", RESURRECT);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testResurrectSingleDeath() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);
            script.deck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", YETI, 1);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.playCard("p1", RESURRECT);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testResurrectTwoDeaths() {
        Set<String> resurrected = new HashSet<>();
        resurrected.add(runTwoMinionsResurrect(0).getName());
        resurrected.add(runTwoMinionsResurrect(1).getName());

        assertEquals("Possible minions",
                new HashSet<>(Arrays.asList(BLUEGILL_WARRIOR, FLAME_OF_AZZINOTH)),
                resurrected);
    }

    private MinionId runTwoMinionsResurrect(int roll) {
        List<MinionId> board = RandomTestUtils.boardMinionScript("p1", (script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);
            script.deck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 1);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));
            script.expectBoard("p2");

            script.playCard("p1", MOONFIRE, "p1:0");
            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2");

            script.addRoll(2, roll);
            script.playCard("p1", RESURRECT);
        });

        if (board.size() != 1) {
            fail("Expected exactly one minion to be resurrected but the resulting board was " + board);
        }

        return board.get(0);
    }
}
