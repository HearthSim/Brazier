package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class CardPlayedTest {
    @Test
    public void testIllidanPlayToItsRightSide() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", ILLIDAN_STORMRAGE, 1);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.playMinionCard("p1", SLIME, 2);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testIllidanPlayToItsLeftSide() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", ILLIDAN_STORMRAGE, 1);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.playMinionCard("p1", SLIME, 1);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testIllidanIsNotAffectedByTheOpponent() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", ILLIDAN_STORMRAGE, 1);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.playMinionCard("p2", SLIME, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2));
        });
    }
}
