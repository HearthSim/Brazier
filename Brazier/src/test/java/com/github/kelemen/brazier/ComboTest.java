package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class ComboTest {
    @Test
    public void testEviscerateNoCombo() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.setCurrentPlayer("p1");

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));

            script.endTurn();

            script.setMana("p2", 10);
            script.playCard("p2", EVISCERATE, "p1:0");

            script.expectBoard("p1", expectedMinion(YETI, 4, 3));
        });
    }

    @Test
    public void testEviscerateNoComboNextTurn() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.setCurrentPlayer("p1");

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));

            script.endTurn();

            script.setMana("p2", 10);
            script.playCard("p2", MOONFIRE, "p1:hero");

            script.endTurn();
            script.endTurn();

            script.playCard("p2", EVISCERATE, "p1:0");

            script.expectBoard("p1", expectedMinion(YETI, 4, 3));
        });
    }

    @Test
    public void testEviscerateCombo() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.setCurrentPlayer("p1");

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));

            script.endTurn();

            script.setMana("p2", 10);
            script.playCard("p2", MOONFIRE, "p1:hero");
            script.playCard("p2", EVISCERATE, "p1:0");

            script.expectBoard("p1", expectedMinion(YETI, 4, 1));
        });
    }

    @Test
    public void testDefiasRingLeaderCombo() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setCurrentPlayer("p1");

            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 0);
            script.playMinionCard("p1", SLIME, 1);

            script.playMinionCard("p1", DEFIAS_RINGLEADER, 1);

            script.expectBoard("p1",
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(DEFIAS_BANDIT, 2, 1),
                    expectedMinion(SLIME, 1, 2));

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(DEFIAS_BANDIT, 2, 1),
                    expectedMinion(SLIME, 1, 2));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(DEFIAS_BANDIT, 2, 1),
                    expectedMinion(SLIME, 1, 2));
        });
    }

    @Test
    public void testDefiasRingLeaderNoCombo() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setCurrentPlayer("p1");

            script.playMinionCard("p1", DEFIAS_RINGLEADER, 0);
            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 0);
            script.playMinionCard("p1", SLIME, 2);

            script.expectBoard("p1",
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(SLIME, 1, 2));

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(SLIME, 1, 2));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(DEFIAS_RINGLEADER, 2, 2),
                    expectedMinion(SLIME, 1, 2));
        });
    }
}
