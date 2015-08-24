package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class SimplePlaysTest {
    @Test
    public void testSlamDrawsCards() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 4);
            script.setMana("p2", 10);
            script.deck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);

            script.playCard("p1", SLAM, "p2:0");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 3),
                    expectedMinion(YETI, 4, 5));

            script.expectDeck("p1", YETI, SLUDGE_BELCHER);
            script.expectHand("p1", WHIRLWIND);
        });
    }

    @Test
    public void testSlamDoesntDrawCards() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);
            script.deck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);

            script.playMinionCard("p1", YETI, 0);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.attack("p1:0", "p2:1");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 1));

            script.playCard("p1", SLAM, "p2:1");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.expectDeck("p1", YETI, SLUDGE_BELCHER, WHIRLWIND);
            script.expectHand("p1");
        });
    }

    @Test
    public void testPlaySimpleMinion() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 4);
            script.playMinionCard("p1", YETI, 0);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testPlayMultipleMinions() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", FROTHING_BERSERKER, 0);
            script.playMinionCard("p1", FROTHING_BERSERKER, 1);
            script.playMinionCard("p1", YETI, 1);

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 4),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(FROTHING_BERSERKER, 2, 4));
        });
    }
}
