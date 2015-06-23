package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class SecretTest {
    @Test
    public void testSnipeJaraxxus() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playCard("p1", SNIPE);

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", JARAXXUS, 0);

            script.expectBoard("p1");
            script.expectBoard("p2");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 11, 0);
        });
    }

    @Test
    public void testRepentanceSnipeJaraxxus() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playCard("p1", REPENTANCE);
            script.playCard("p1", SNIPE);

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", JARAXXUS, 0);

            script.expectHeroDeath("p2");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", -3, 0);
        });
    }

    @Test
    public void testAvengeDetectsPreviousDeathRattle() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", HARVEST_GOLEM, 0);
            script.playMinionCard("p1", WISP, 1);
            script.playCard("p1", AVENGE);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", WISP, 0);

            script.expectBoard("p1",
                    expectedMinion(HARVEST_GOLEM, 2, 3),
                    expectedMinion(WISP, 1, 1));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.playCard("p2", FLAMESTRIKE);

            script.expectBoard("p1",
                    expectedMinion(DAMAGED_GOLEM, 5, 3));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.expectSecret("p1");
            script.expectSecret("p2");
        });
    }

    @Test
    public void testAvengeDoesNotDetectDeathRattleSummon() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", HARVEST_GOLEM, 0);
            script.playCard("p1", AVENGE);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", WISP, 0);

            script.expectBoard("p1",
                    expectedMinion(HARVEST_GOLEM, 2, 3),
                    expectedMinion(WISP, 1, 1));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.playCard("p2", FLAMESTRIKE);

            script.expectBoard("p1",
                    expectedMinion(DAMAGED_GOLEM, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.expectSecret("p1", AVENGE);
            script.expectSecret("p2");
        });
    }

    @Test
    public void testAvengeDoesNotTriggerForSingleTarget() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", WISP, 0);
            script.playCard("p1", AVENGE);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", WISP, 0);

            script.expectBoard("p1",
                    expectedMinion(WISP, 1, 1));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.playCard("p2", FLAMESTRIKE);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.expectSecret("p1", AVENGE);
            script.expectSecret("p2");

            script.setCurrentPlayer("p1");
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 1);

            script.expectBoard("p1",
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.setCurrentPlayer("p2");
            script.playCard("p2", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(WISP, 4, 3));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.expectSecret("p1");
            script.expectSecret("p2");
        });
    }

    @Test
    public void testIceBarrierOnEnemyTurn() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playCard("p1", ICE_BARRIER);
            script.expectSecret("p1", ICE_BARRIER);

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 30, 0);

            script.playMinionCard("p2", STONETUSK_BOAR, 0);
            script.attack("p2:0", "p1:hero");

            script.expectSecret("p1");

            script.expectHeroHp("p1", 30, 7);
            script.expectHeroHp("p2", 30, 0);

            script.playMinionCard("p2", STONETUSK_BOAR, 1);
            script.attack("p2:1", "p1:hero");

            script.expectHeroHp("p1", 30, 6);
            script.expectHeroHp("p2", 30, 0);
        });
    }

    private static void expectManaCost(PlayScript script, String playerName, int... manaCosts) {
        ManaCostManipulationTest.expectManaCost(script, playerName, manaCosts);
    }

    @Test
    public void testFreezingTrapBeforeExplosive() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FREEZING_TRAP);
            script.playCard("p1", EXPLOSIVE_TRAP);
            script.playMinionCard("p1", YETI, 0);

            script.expectSecret("p1", FREEZING_TRAP, EXPLOSIVE_TRAP);

            script.setCurrentPlayer("p2");

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2", expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectHand("p1");
            script.expectHand("p2");

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);
            script.attack("p2:0", "p1:hero");

            script.expectSecret("p1");

            script.expectHand("p1");
            script.expectHand("p2", BLUEGILL_WARRIOR);
            expectManaCost(script, "p2", 4);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 18, 0);
        });
    }

    @Test
    public void testFreezingTrapAfterExplosive() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", EXPLOSIVE_TRAP);
            script.playCard("p1", FREEZING_TRAP);
            script.playMinionCard("p1", YETI, 0);

            script.expectSecret("p1", EXPLOSIVE_TRAP, FREEZING_TRAP);

            script.setCurrentPlayer("p2");

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2", expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectHand("p1");
            script.expectHand("p2");

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);
            script.attack("p2:0", "p1:hero");

            script.expectSecret("p1", FREEZING_TRAP);

            script.expectHand("p1");
            script.expectHand("p2");

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 18, 0);
        });
    }

    private void testFreezingTrapAfterExplosiveOrderDoesNotChangeWithKezan(int secretRoll) {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", EXPLOSIVE_TRAP);
            script.playCard("p1", FREEZING_TRAP);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);

            script.expectSecret("p1", EXPLOSIVE_TRAP, FREEZING_TRAP);

            script.setCurrentPlayer("p2");

            script.addRoll(2, secretRoll);
            script.playMinionCard("p2", KEZAN_MYSTIC, 0);
            script.playMinionCard("p2", BLUEGILL_WARRIOR, 1);

            script.setCurrentPlayer("p1");
            script.playMinionCard("p1", KEZAN_MYSTIC, 0);

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectHand("p1");
            script.expectHand("p2");

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);
            script.attack("p2:1", "p1:hero");

            script.expectSecret("p1", FREEZING_TRAP);

            script.expectHand("p1");
            script.expectHand("p2");

            script.expectBoard("p1",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 1));

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 18, 0);
        });
    }

    @Test
    public void testFreezingTrapAfterExplosiveOrderDoesNotChangeWithKezan() {
        for (int secretRoll: new int[]{0, 1}) {
            testFreezingTrapAfterExplosiveOrderDoesNotChangeWithKezan(secretRoll);
        }
    }

    @Test
    public void testKezanStealsANewSecret() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", EXPLOSIVE_TRAP);
            script.playCard("p1", FREEZING_TRAP);
            script.playCard("p2", EXPLOSIVE_TRAP);

            script.expectSecret("p1", EXPLOSIVE_TRAP, FREEZING_TRAP);
            script.expectSecret("p2", EXPLOSIVE_TRAP);

            script.playMinionCard("p2", KEZAN_MYSTIC, 0);

            script.expectSecret("p1", EXPLOSIVE_TRAP);
            script.expectSecret("p2", EXPLOSIVE_TRAP, FREEZING_TRAP);
        });
    }

    @Test
    public void testKezanStealsSecret() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FREEZING_TRAP);

            script.expectSecret("p1", FREEZING_TRAP);
            script.expectSecret("p2");

            script.playMinionCard("p2", KEZAN_MYSTIC, 0);

            script.expectSecret("p1");
            script.expectSecret("p2",FREEZING_TRAP);

            script.setCurrentPlayer("p1");
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);

            script.expectHand("p1");
            script.expectHand("p2");
            script.expectBoard("p1", expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2", expectedMinion(KEZAN_MYSTIC, 4, 3));

            script.attack("p1:0", "p2:hero");

            script.expectHand("p1", BLUEGILL_WARRIOR);
            script.expectHand("p2");
            script.expectBoard("p1");
            script.expectBoard("p2", expectedMinion(KEZAN_MYSTIC, 4, 3));
        });
    }

    @Test
    public void testKezanDestroysSecret() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FREEZING_TRAP);
            script.playCard("p2", FREEZING_TRAP);

            script.expectSecret("p1", FREEZING_TRAP);
            script.expectSecret("p2", FREEZING_TRAP);

            script.playMinionCard("p2", KEZAN_MYSTIC, 0);

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p2", BLUEGILL_WARRIOR, 1);

            script.expectSecret("p1");
            script.expectSecret("p2",FREEZING_TRAP);

            script.expectHand("p1");
            script.expectHand("p2");
            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);

            script.setCurrentPlayer("p1");
            script.attack("p1:0", "p2:hero");

            script.expectHand("p1", BLUEGILL_WARRIOR);
            script.expectHand("p2");
            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.setCurrentPlayer("p2");
            script.attack("p2:1", "p1:hero");

            script.expectHand("p1", BLUEGILL_WARRIOR);
            script.expectHand("p2");
            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.refreshAttacks();
            script.attack("p2:1", "p1:hero");

            script.expectHand("p1", BLUEGILL_WARRIOR);
            script.expectHand("p2");
            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(KEZAN_MYSTIC, 4, 3),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.expectHeroHp("p1", 26, 0);
            script.expectHeroHp("p2", 20, 0);
        });
    }
}
