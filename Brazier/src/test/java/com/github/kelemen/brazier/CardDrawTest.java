package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class CardDrawTest {
    private static void setupCultMasterBoards(PlayScript script) {
        script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);
        script.deck("p2", YETI, SCARLET_CRUSADER, SLAM);

        script.setMana("p1", 10);
        script.setMana("p2", 10);

        script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
        script.playMinionCard("p1", CULT_MASTER, 0);
        script.playMinionCard("p1", BLUEGILL_WARRIOR, 2);

        script.playMinionCard("p2", YETI, 0);

        script.expectBoard("p1",
                expectedMinion(CULT_MASTER, 4, 2),
                expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                expectedMinion(BLUEGILL_WARRIOR, 2, 1));
        script.expectBoard("p2",
                expectedMinion(YETI, 4, 5));

        script.setMana("p1", 10);
        script.setMana("p2", 10);

        script.refreshAttacks();
    }

    @Test
    public void testCultMasterKillOneMinion() {
        PlayScript.testScript((script) -> {
            setupCultMasterBoards(script);

            script.attack("p2:0", "p1:2");

            script.expectBoard("p1",
                    expectedMinion(CULT_MASTER, 4, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 3));

            script.expectHand("p1", FLAME_OF_AZZINOTH);
            script.expectHand("p2");
        });
    }

    @Test
    public void testCultMasterKillTwoMinions() {
        PlayScript.testScript((script) -> {
            setupCultMasterBoards(script);

            script.playCard("p2", WHIRLWIND);

            script.expectBoard("p1",
                    expectedMinion(CULT_MASTER, 4, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 4));

            script.expectHand("p1", FLAME_OF_AZZINOTH, FIERY_WAR_AXE);
            script.expectHand("p2");
        });
    }

    @Test
    public void testCultMasterDoesNotDrawForSelf() {
        PlayScript.testScript((script) -> {
            setupCultMasterBoards(script);

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 1));

            script.expectHand("p1");
            script.expectHand("p2");
        });
    }

    @Test
    public void testCultMasterDoesNotDrawWhenDying() {
        PlayScript.testScript((script) -> {
            setupCultMasterBoards(script);

            script.playCard("p2", FLAMESTRIKE);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.expectHand("p1");
            script.expectHand("p2");
        });
    }

    @Test
    public void testCultMasterDoesNotDrawForOpponentMinions() {
        PlayScript.testScript((script) -> {
            setupCultMasterBoards(script);

            script.playMinionCard("p2", BLUEGILL_WARRIOR, 1);

            script.expectBoard("p1",
                    expectedMinion(CULT_MASTER, 4, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.playCard("p2", MOONFIRE, "p2:1");

            script.expectBoard("p1",
                    expectedMinion(CULT_MASTER, 4, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.expectHand("p1");
            script.expectHand("p2");
        });
    }

    @Test
    public void testStarvingBuzzardDoesNotDrawForOpponentBeast() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);

            script.setMana("p1", 10);

            script.playMinionCard("p1", STARVING_BUZZARD, 0);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2));

            script.setMana("p2", 10);
            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", STONETUSK_BOAR, 1);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2));
            script.expectBoard("p2",
                    expectedMinion(STONETUSK_BOAR, 1, 1));

            script.expectHand("p1");
        });
    }

    @Test
    public void testStarvingBuzzardDoesNotDrawForNonBeast() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);

            script.setMana("p1", 10);

            script.playMinionCard("p1", STARVING_BUZZARD, 0);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2));

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));

            script.expectHand("p1");
        });
    }

    @Test
    public void testStarvingBuzzardDrawsForBeast() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);

            script.setMana("p1", 10);

            script.playMinionCard("p1", STARVING_BUZZARD, 0);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2));

            script.playMinionCard("p1", STONETUSK_BOAR, 1);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2),
                    expectedMinion(STONETUSK_BOAR, 1, 1));

            script.expectHand("p1", FLAME_OF_AZZINOTH);
        });
    }

    @Test
    public void testStarvingBuzzardDrawsForCopiedBeast() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p2", HAUNTED_CREEPER, 0);
            script.playMinionCard("p1", STARVING_BUZZARD, 0);

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2));
            script.expectBoard("p2",
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.expectHand("p1");
            script.expectHand("p2");

            script.playMinionCard("p1", FACELESS_MANIPULATOR, 1, "p2:0");

            script.expectBoard("p1",
                    expectedMinion(STARVING_BUZZARD, 3, 2),
                    expectedMinion(HAUNTED_CREEPER, 1, 2));
            script.expectBoard("p2",
                    expectedMinion(HAUNTED_CREEPER, 1, 2));

            script.expectHand("p1", FLAME_OF_AZZINOTH);
            script.expectHand("p2");
        });
    }

    @Test
    public void testBlessingOfWisdom() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);
            script.deck("p2", YETI, SCARLET_CRUSADER, SLAM);

            script.setMana("p1", 10);

            script.playMinionCard("p1", STONETUSK_BOAR, 0);

            script.expectBoard("p1",
                    expectedMinion(STONETUSK_BOAR, 1, 1));

            script.playCard("p1", BLESSING_OF_WISDOM, "p1:0");

            script.attack("p1:0", "p2:hero");

            script.expectHand("p1", FLAME_OF_AZZINOTH);

            script.refreshAttacks();
            script.attack("p1:0", "p2:hero");

            script.expectHand("p1", FLAME_OF_AZZINOTH, FIERY_WAR_AXE);
            script.expectHand("p2");
        });
    }

    @Test
    public void testBlessingOfWisdomOfOpponent() {
        PlayScript.testScript((script) -> {
            script.deck("p1", MOONFIRE, FIERY_WAR_AXE, FLAME_OF_AZZINOTH);
            script.deck("p2", YETI, SCARLET_CRUSADER, SLAM);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p2", STONETUSK_BOAR, 0);

            script.expectBoard("p2",
                    expectedMinion(STONETUSK_BOAR, 1, 1));

            script.playCard("p1", BLESSING_OF_WISDOM, "p2:0");

            script.attack("p2:0", "p1:hero");

            script.expectHand("p1", FLAME_OF_AZZINOTH);

            script.refreshAttacks();
            script.attack("p2:0", "p1:hero");

            script.expectHand("p1", FLAME_OF_AZZINOTH, FIERY_WAR_AXE);
            script.expectHand("p2");
        });
    }
}
