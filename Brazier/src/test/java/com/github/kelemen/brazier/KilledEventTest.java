package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class KilledEventTest {
    @Test
    public void testSoulOfTheForest() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p2", HAUNTED_CREEPER, 1);

            script.expectBoard("p2",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.setMana("p2", 10);
            script.playCard("p2", SOUL_OF_THE_FOREST);

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 2);

            script.expectBoard("p2",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.setMana("p1", 10);
            script.playCard("p1", FLAMESTRIKE);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(TREANT, 2, 2),
                    expectedMinion(TREANT, 2, 2));
        });
    }

    @Test
    public void testHauntedCreeper() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", HAUNTED_CREEPER, 0);

            script.expectBoard("p2",
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.setMana("p1", 10);
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");

            script.expectBoard("p2",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1));
        });
    }

    @Test
    public void testHauntedCreeperWithFullBoard() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", HAUNTED_CREEPER, 0);
            script.playMinionCard("p2", HAUNTED_CREEPER, 1);
            script.playMinionCard("p2", HAUNTED_CREEPER, 2);
            script.playMinionCard("p2", HAUNTED_CREEPER, 3);
            script.setMana("p2", 10);
            script.playMinionCard("p2", HAUNTED_CREEPER, 4);
            script.playMinionCard("p2", HAUNTED_CREEPER, 5);
            script.playMinionCard("p2", HAUNTED_CREEPER, 6);

            script.expectBoard("p2",
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.setMana("p1", 10);
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");

            script.expectBoard("p2",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
        });
    }

    @Test
    public void testHauntedCreeperWithFullBoardBoardClear() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", HAUNTED_CREEPER, 0);
            script.playMinionCard("p2", HAUNTED_CREEPER, 1);
            script.playMinionCard("p2", HAUNTED_CREEPER, 2);
            script.playMinionCard("p2", HAUNTED_CREEPER, 3);
            script.setMana("p2", 10);
            script.playMinionCard("p2", HAUNTED_CREEPER, 4);
            script.playMinionCard("p2", HAUNTED_CREEPER, 5);
            script.playMinionCard("p2", HAUNTED_CREEPER, 6);

            script.expectBoard("p2",
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.setMana("p1", 10);
            script.playCard("p1", FLAMESTRIKE);

            script.expectBoard("p2",
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1),
                    expectedMinion(SPECTRAL_SPIDER, 1, 1));
        });
    }

    @Test
    public void testSludgeBelcherDeathRattleWorksWithFullBoard() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", SLUDGE_BELCHER, 0);
            script.playMinionCard("p2", SLUDGE_BELCHER, 1);
            script.setMana("p2", 10);
            script.playMinionCard("p2", SLUDGE_BELCHER, 2);
            script.playMinionCard("p2", SLUDGE_BELCHER, 3);
            script.setMana("p2", 10);
            script.playMinionCard("p2", SLUDGE_BELCHER, 4);
            script.playMinionCard("p2", SLUDGE_BELCHER, 5);
            script.setMana("p2", 10);
            script.playMinionCard("p2", SLUDGE_BELCHER, 6);

            script.expectBoard("p2",
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5));

            script.setMana("p1", 10);
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");
            script.playCard("p1", MOONFIRE, "p2:0");

            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5));
        });
    }

    @Test
    public void testSludgeBelcher() {
        PlayScript.testScript((script) -> {
            script.setMana("p2", 10);
            script.playMinionCard("p2", SLUDGE_BELCHER, 0);
            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 2);

            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 1);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 5),
                    expectedMinion(YETI, 4, 5));

            script.attack("p1:0", "p2:1"); // YETI 1 -> SLUDGE

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 5));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(SLUDGE_BELCHER, 3, 1),
                    expectedMinion(YETI, 4, 5));

            script.attack("p1:1", "p2:1"); // YETI 2 -> SLUDGE

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 2),
                    expectedMinion(YETI, 4, 2));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(YETI, 4, 5));

            script.refreshAttacks();

            script.attack("p1:0", "p2:1"); // YETI 1 -> SLIME

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 1),
                    expectedMinion(YETI, 4, 2));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));
        });
    }

    @Test
    public void testDeathsBite() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", DEATHS_BITE);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p2", FROTHING_BERSERKER, 0);
            script.playMinionCard("p2", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.attack("p1:hero", "p2:hero");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 26, 0);

            script.refreshAttacks();

            script.attack("p1:hero", "p2:0");

            script.expectHeroHp("p1", 26, 0);
            script.expectHeroHp("p2", 26, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 4));
            script.expectBoard("p2",
                    expectedMinion(FROTHING_BERSERKER, 6, 3));
        });
    }

    @Test
    public void testDeathsBiteKilledByJaraxxus() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playCard("p1", DEATHS_BITE);
            script.playMinionCard("p1", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));

            script.setMana("p1", 10);
            script.playMinionCard("p1", JARAXXUS, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 4));

            script.expectWeapon("p1", 3, 8);
        });
    }
}
