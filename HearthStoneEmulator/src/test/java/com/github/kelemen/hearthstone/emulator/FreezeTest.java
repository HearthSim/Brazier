package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class FreezeTest {
    @Test
    public void testFreezingOpponent() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5));

            script.playCard("p2", FROST_NOVA);

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5));
        });
    }

    @Test
    public void testFreezingSelfWithoutAttack() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setCurrentPlayer("p1");
            script.playMinionCard("p1", YETI, 0);

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5));

            script.playCard("p1", CONE_OF_COLD, "p1:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 4, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 4));
        });
    }

    @Test
    public void testFreezingSelfWithAttack() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setCurrentPlayer("p1");
            script.playMinionCard("p1", STORMWIND_KNIGHT, 0);

            script.expectBoard("p1",
                    expectedMinionWithFlags(STORMWIND_KNIGHT, 2, 5));

            script.attack("p1:0", "p2:hero");

            script.playCard("p1", CONE_OF_COLD, "p1:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(STORMWIND_KNIGHT, 2, 4, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(STORMWIND_KNIGHT, 2, 4, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(STORMWIND_KNIGHT, 2, 4, "frozen"));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinionWithFlags(STORMWIND_KNIGHT, 2, 4));
        });
    }
}
