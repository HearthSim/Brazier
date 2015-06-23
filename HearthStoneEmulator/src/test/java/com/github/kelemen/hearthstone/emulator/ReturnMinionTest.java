package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class ReturnMinionTest {
    private static void expectManaCost(PlayScript script, String playerName, int... manaCosts) {
        ManaCostManipulationTest.expectManaCost(script, playerName, manaCosts);
    }

    @Test
    public void testFreezingTrapAttackMinion() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FREEZING_TRAP);
            script.playMinionCard("p1", YETI, 0);

            script.setCurrentPlayer("p2");

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2", expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectHand("p1");
            script.expectHand("p2");

            script.attack("p2:0", "p1:0");

            script.expectHand("p1");
            script.expectHand("p2", BLUEGILL_WARRIOR);
            expectManaCost(script, "p2", 4);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testFreezingTrapAttackHero() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FREEZING_TRAP);
            script.playMinionCard("p1", YETI, 0);

            script.setCurrentPlayer("p2");

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2", expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectHand("p1");
            script.expectHand("p2");

            script.attack("p2:0", "p1:hero");

            script.expectHand("p1");
            script.expectHand("p2", BLUEGILL_WARRIOR);
            expectManaCost(script, "p2", 4);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testShadowStepDeathRattle() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", SLUDGE_BELCHER, 0);

            script.expectBoard("p1", expectedMinion(SLUDGE_BELCHER, 3, 5));
            script.expectBoard("p2");
            script.expectHand("p1");
            script.expectHand("p2");

            script.playCard("p1", SHADOW_STEP, "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2");
            script.expectHand("p1", SLUDGE_BELCHER);
            expectManaCost(script, "p1", 3);
            script.expectHand("p2");
        });
    }
}
