package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class SilenceTest {
    @Test
    public void testSilenceStatBuff() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", SHATTERED_SUN_CLERIC, 1, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 6),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));

            script.playCard("p1", SILENCE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));
        });
    }

    @Test
    public void testSilenceStatBuffDoesNotKillMinion() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", SHATTERED_SUN_CLERIC, 1, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));

            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));

            script.playCard("p1", SILENCE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));
        });
    }

    @Test
    public void testSilenceTriggeredAbility() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", FROTHING_BERSERKER, 0);

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 3, 3));

            script.playCard("p1", SILENCE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 3));

            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 2));
        });
    }

    @Test
    public void testSilenceAuraGiver() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.setMana("p1", 10);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            expectTestStormwindBoard(script);

            script.playCard("p2", SILENCE, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(TREANT, 2, 2));
        });
    }

    @Test
    public void testSilenceRemovesSpellDamage() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", KOBOLD_GEOMANCER, 0);
            script.playMinionCard("p1", YETI, 1);

            script.expectBoard("p1",
                    expectedMinion(KOBOLD_GEOMANCER, 2, 2),
                    expectedMinion(YETI, 4, 5));

            script.playCard("p1", SILENCE, "p1:0");
            script.playCard("p1", MOONFIRE, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(KOBOLD_GEOMANCER, 2, 2),
                    expectedMinion(YETI, 4, 4));
        });
    }

    private static void expectTestStormwindBoard(PlayScript script) {
        script.expectBoard("p1",
                expectedMinion(YETI, 5, 6),
                expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                expectedMinion(STORMWIND_CHAMPION, 6, 6),
                expectedMinion(SLIME, 2, 3),
                expectedMinion(TREANT, 3, 3));
    }

    @Test
    public void testCannotSilenceAuraBuffs() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.setMana("p1", 10);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            expectTestStormwindBoard(script);

            script.playCard("p2", SILENCE, "p1:0");
            expectTestStormwindBoard(script);

            script.playCard("p2", SILENCE, "p1:1");
            expectTestStormwindBoard(script);

            script.playCard("p2", SILENCE, "p1:3");
            expectTestStormwindBoard(script);

            script.playCard("p2", SILENCE, "p1:4");
            expectTestStormwindBoard(script);
        });
    }

    @Test
    public void testSilenceDeathRattle() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", SLUDGE_BELCHER, 0);

            script.expectBoard("p1",
                    expectedMinion(SLUDGE_BELCHER, 3, 5));

            script.playCard("p1", SILENCE, "p1:0");

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");

            script.expectBoard("p1");
        });
    }

    @Test
    public void testSilenceTaunt() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", SLUDGE_BELCHER, 0);

            script.expectBoard("p1",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 5, "taunt"));

            script.playCard("p1", SILENCE, "p1:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 5));
        });
    }
}
