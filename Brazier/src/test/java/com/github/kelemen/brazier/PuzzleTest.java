package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class PuzzleTest {
    private static void setupInitialBoardOfTrumpsPuzzle(PlayScript script) {
            script.setMana("p2", 7);
            script.playCard("p2", FIERY_WAR_AXE);

            script.setMana("p1", 10);
            script.playMinionCard("p1", GRIM_PATRON, 0);
            script.playMinionCard("p1", GRIM_PATRON, 1);
            script.setMana("p1", 10);
            script.playCard("p1", WHIRLWIND);

            script.attack("p2:hero", "p1:3");
            script.refreshAttacks();
            script.attack("p2:hero", "p1:1");

            script.playMinionCard("p2", SLUDGE_BELCHER, 0);

            script.expectBoard("p1",
                    expectedMinion(GRIM_PATRON, 3, 2),
                    expectedMinion(GRIM_PATRON, 3, 2));
            script.expectBoard("p2",
                    expectedMinion(SLUDGE_BELCHER, 3, 5));

            script.setHeroHp("p1", 26, 0);
            script.setHeroHp("p2", 29, 2);

            script.refreshAttacks();
    }

    @Test
    public void testTrumpsPatronPuzzle32Damage() {
        PlayScript.testScript((script) -> {
            setupInitialBoardOfTrumpsPuzzle(script);

            script.setMana("p1", 8);
            script.addToHand("p1",
                    SLAM,
                    FIERY_WAR_AXE,
                    DEATHS_BITE,
                    FROTHING_BERSERKER,
                    DREAD_CORSAIR,
                    DREAD_CORSAIR,
                    WARSONG_COMMANDER);
            script.decreaseManaCostOfHand("p1");
            script.addToHand("p1", WHIRLWIND);

            script.playCard("p1", 1); // War axe
            script.expectedMana("p1", 7);
            script.playMinionCard("p1", 5, 0); // Warsong
            script.expectedMana("p1", 5);
            script.playMinionCard("p1", 2, 1); // Frothing
            script.expectedMana("p1", 3);
            script.playMinionCard("p1", 2, 2); // Dread
            script.playMinionCard("p1", 2, 3); // Dread
            script.expectedMana("p1", 3);

            script.attack("p1:3", "p2:0");
            script.attack("p1:2", "p2:0");

            script.expectBoard("p1",
                    expectedMinion(WARSONG_COMMANDER, 2, 3),
                    expectedMinion(FROTHING_BERSERKER, 6, 4),
                    expectedMinion(GRIM_PATRON, 3, 2),
                    expectedMinion(GRIM_PATRON, 3, 2));
            script.expectBoard("p2",
                    expectedMinion(SLIME, 1, 2));

            script.attack("p1:2", "p2:0");

            script.expectBoard("p1",
                    expectedMinion(WARSONG_COMMANDER, 2, 3),
                    expectedMinion(FROTHING_BERSERKER, 8, 4),
                    expectedMinion(GRIM_PATRON, 3, 1),
                    expectedMinion(GRIM_PATRON, 3, 3),
                    expectedMinion(GRIM_PATRON, 3, 2));
            script.expectBoard("p2");

            script.playCard("p1", 2); // Whirlwind
            script.expectedMana("p1", 2);

            script.expectBoard("p1",
                    expectedMinion(WARSONG_COMMANDER, 2, 2),
                    expectedMinion(FROTHING_BERSERKER, 13, 3),
                    expectedMinion(GRIM_PATRON, 3, 2),
                    expectedMinion(GRIM_PATRON, 3, 3),
                    expectedMinion(GRIM_PATRON, 3, 1),
                    expectedMinion(GRIM_PATRON, 3, 3));

            script.playCard("p1", 0, "p1:5");
            script.expectedMana("p1", 1);

            script.expectBoard("p1",
                    expectedMinion(WARSONG_COMMANDER, 2, 2),
                    expectedMinion(FROTHING_BERSERKER, 14, 3),
                    expectedMinion(GRIM_PATRON, 3, 2),
                    expectedMinion(GRIM_PATRON, 3, 3),
                    expectedMinion(GRIM_PATRON, 3, 1),
                    expectedMinion(GRIM_PATRON, 3, 1),
                    expectedMinion(GRIM_PATRON, 3, 3));

            script.attack("p1:1", "p2:hero");
            script.attack("p1:2", "p2:hero");
            script.attack("p1:3", "p2:hero");
            script.attack("p1:4", "p2:hero");
            script.attack("p1:5", "p2:hero");
            script.attack("p1:6", "p2:hero");
            script.attack("p1:hero", "p2:hero");

            script.expectHeroDeath("p2");
            script.expectHeroHp("p2", -1, 0);
        });
    }
}
