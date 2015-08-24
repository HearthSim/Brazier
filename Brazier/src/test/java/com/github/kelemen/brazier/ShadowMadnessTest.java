package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;


public final class ShadowMadnessTest {
    @Test
    public void testShadowMadness() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", DREAD_CORSAIR, 0);
            script.refreshAttacks();
            script.attack("p1:0", "p2:hero"); // just to spend the attack

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3, false));
            script.expectBoard("p2");

            script.playCard("p2", SHADOW_MADNESS, "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(DREAD_CORSAIR, 3, 3, true));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3, true));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testAuraIsTakenWithShadowMadness() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", DIRE_WOLF_ALPHA, 1);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 2);

            script.playMinionCard("p2", DREAD_CORSAIR, 0);
            script.playMinionCard("p2", HAUNTED_CREEPER, 1);

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1));
            script.expectBoard("p2",
                    expectedMinion(DREAD_CORSAIR, 3, 3),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.setCurrentPlayer("p2");

            script.playCard("p2", SHADOW_MADNESS, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(DREAD_CORSAIR, 3, 3),
                    expectedMinion(HAUNTED_CREEPER, 2, 2),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2, true));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2));
            script.expectBoard("p2",
                    expectedMinion(DREAD_CORSAIR, 3, 3),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
        });
    }

    @Test
    public void testDeathRattleIsStolenByShadowMadness() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", FIRE_ELEMENTAL, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", SLUDGE_BELCHER, 1);

            script.expectBoard("p1",
                    expectedMinion(FIRE_ELEMENTAL, 6, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5));
            script.expectBoard("p2");

            script.setCurrentPlayer("p2");

            script.playCard("p2", SHADOW_MADNESS, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(FIRE_ELEMENTAL, 6, 5));
            script.expectBoard("p2",
                    expectedMinion(SLUDGE_BELCHER, 3, 5, true));

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinion(FIRE_ELEMENTAL, 6, 2));
            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2, false));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(FIRE_ELEMENTAL, 6, 2));
            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2, false));
        });
    }

    @Test
    public void testSilencingShadowMadness() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", DREAD_CORSAIR, 0);
            script.refreshAttacks();
            script.attack("p1:0", "p2:hero"); // just to spend the attack

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3, false));
            script.expectBoard("p2");

            script.playCard("p2", SHADOW_MADNESS, "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(DREAD_CORSAIR, 3, 3, true));

            script.playCard("p2", SILENCE, "p2:0");

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3));
            script.expectBoard("p2");

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testShadowMadnessFullBoardAfterReturn() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", SLUDGE_BELCHER, 0);
            script.playMinionCard("p1", HAUNTED_CREEPER, 1);
            script.playMinionCard("p1", HAUNTED_CREEPER, 2);
            script.setMana("p1", 10);
            script.playMinionCard("p1", HAUNTED_CREEPER, 3);
            script.playMinionCard("p1", HAUNTED_CREEPER, 4);
            script.playMinionCard("p1", HAUNTED_CREEPER, 5);
            script.playMinionCard("p1", HAUNTED_CREEPER, 6);

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2");

            script.playCard("p2", SHADOW_MADNESS, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(SLUDGE_BELCHER, 3, 5));

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(SLUDGE_BELCHER, 3, 4));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2));
        });
    }

    @Test
    public void testShadowMadnessWithIllidan() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", SLUDGE_BELCHER, 0);
            script.playMinionCard("p1", HAUNTED_CREEPER, 1);
            script.playMinionCard("p1", HAUNTED_CREEPER, 2);
            script.setMana("p1", 10);
            script.playMinionCard("p1", HAUNTED_CREEPER, 3);
            script.playMinionCard("p1", HAUNTED_CREEPER, 4);
            script.playMinionCard("p1", HAUNTED_CREEPER, 5);
            script.playMinionCard("p1", HAUNTED_CREEPER, 6);

            script.playMinionCard("p2", STONETUSK_BOAR, 0);
            script.playMinionCard("p2", STONETUSK_BOAR, 1);
            script.playMinionCard("p2", STONETUSK_BOAR, 2);
            script.playMinionCard("p2", STONETUSK_BOAR, 3);
            script.playMinionCard("p2", STONETUSK_BOAR, 4);
            script.setMana("p2", 10);
            script.playMinionCard("p2", ILLIDAN_STORMRAGE, 5);

            script.setCurrentPlayer("p2");

            script.expectBoard("p1",
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5));

            script.playCard("p2", SHADOW_MADNESS, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(STONETUSK_BOAR, 1, 1),
                    expectedMinion(ILLIDAN_STORMRAGE, 7, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));
        });
    }


    @Test
    public void testShadowMadnessWithEndOfTurnEffects() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.addToHand("p1", YETI, SLAM, WHIRLWIND);
            expectManaCost(script, "p1", 4, 2, 1);

            script.addToHand("p2", SLUDGE_BELCHER, PYROBLAST, DIRE_WOLF_ALPHA);
            expectManaCost(script, "p2", 5, 10, 2);

            script.playMinionCard("p1", EMPEROR_THAURISSAN, 0);

            script.setCurrentPlayer("p2");

            script.playMinionCard("p2", ALDOR_PEACEKEEPER, 0, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_THAURISSAN, 1, 5));
            script.expectBoard("p2",
                    expectedMinion(ALDOR_PEACEKEEPER, 3, 3));

            script.playCard("p2", SHADOW_MADNESS, "p1:0");

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(ALDOR_PEACEKEEPER, 3, 3),
                    expectedMinion(EMPEROR_THAURISSAN, 1, 5));

            script.endTurn();

            expectManaCost(script, "p1", 4, 2, 1);
            expectManaCost(script, "p2", 4, 9, 1);

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_THAURISSAN, 1, 5));
            script.expectBoard("p2",
                    expectedMinion(ALDOR_PEACEKEEPER, 3, 3));

            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_THAURISSAN, 1, 5));
            script.expectBoard("p2",
                    expectedMinion(ALDOR_PEACEKEEPER, 3, 3));

            expectManaCost(script, "p1", 3, 1, 0);
            expectManaCost(script, "p2", 4, 9, 1);
        });
    }

    private static void expectManaCost(PlayScript script, String playerName, int... manaCosts) {
        ManaCostManipulationTest.expectManaCost(script, playerName, manaCosts);
    }
}
