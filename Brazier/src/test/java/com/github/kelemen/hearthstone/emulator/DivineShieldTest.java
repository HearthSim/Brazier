package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class DivineShieldTest {
    @Test
    public void testScarletCrusaderAttacks() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", SCARLET_CRUSADER, 0);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(SCARLET_CRUSADER, 3, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.attack("p1:0", "p2:0");

            script.expectBoard("p1",
                    expectedMinion(SCARLET_CRUSADER, 3, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 5));

            script.refreshAttacks();

            script.attack("p1:0", "p2:1");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 2));
        });
    }

    @Test
    public void testScarletCrusaderAttacked() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", SCARLET_CRUSADER, 0);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(SCARLET_CRUSADER, 3, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinion(SCARLET_CRUSADER, 3, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 5));

            script.attack("p2:1", "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 2));
        });
    }
}
