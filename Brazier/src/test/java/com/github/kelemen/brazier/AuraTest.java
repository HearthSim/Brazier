package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class AuraTest {
    @Test
    public void testDireWolfAlpha() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.playMinionCard("p1", SLIME, 2);
            script.playMinionCard("p1", TREANT, 3);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(TREANT, 2, 2));

            script.playMinionCard("p1", DIRE_WOLF_ALPHA, 2);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2),
                    expectedMinion(SLIME, 2, 2),
                    expectedMinion(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2),
                    expectedMinion(SLIME, 2, 2),
                    expectedMinion(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2),
                    expectedMinion(TREANT, 3, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2));
        });
    }

    @Test
    public void testDireWolfAlphaDies() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.playMinionCard("p1", DIRE_WOLF_ALPHA, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2),
                    expectedMinion(SLIME, 2, 2),
                    expectedMinion(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(TREANT, 2, 2));
        });
    }

    @Test
    public void testWeeSpellstopper() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.playMinionCard("p1", SLIME, 2);
            script.playMinionCard("p1", TREANT, 3);

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5),
                    expectedMinionWithFlags(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinionWithFlags(SLIME, 1, 2, "taunt"),
                    expectedMinionWithFlags(TREANT, 2, 2));

            script.setMana("p1", 10);
            script.playMinionCard("p1", WEE_SPELLSTOPPER, 2);

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5),
                    expectedMinionWithFlags(BLUEGILL_WARRIOR, 2, 1, "untargetable"),
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 5),
                    expectedMinionWithFlags(SLIME, 1, 2, "taunt", "untargetable"),
                    expectedMinionWithFlags(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:1");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5, "untargetable"),
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 5),
                    expectedMinionWithFlags(SLIME, 1, 2, "taunt", "untargetable"),
                    expectedMinionWithFlags(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5, "untargetable"),
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 5),
                    expectedMinionWithFlags(TREANT, 2, 2, "untargetable"));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5, "untargetable"),
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 5));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 5));
        });
    }

    @Test
    public void testWeeSpellStopperDies() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.setMana("p1", 10);
            script.playMinionCard("p1", WEE_SPELLSTOPPER, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            script.setMana("p2", 10);
            script.playMinionCard("p2", BLUEGILL_WARRIOR, 0);

            script.attack("p2:0", "p1:2");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5),
                    expectedMinionWithFlags(BLUEGILL_WARRIOR, 2, 1, "untargetable"),
                    expectedMinionWithFlags(WEE_SPELLSTOPPER, 2, 3),
                    expectedMinionWithFlags(SLIME, 1, 2, "taunt", "untargetable"),
                    expectedMinionWithFlags(TREANT, 2, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinionWithFlags(YETI, 4, 5),
                    expectedMinionWithFlags(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinionWithFlags(SLIME, 1, 2, "taunt"),
                    expectedMinionWithFlags(TREANT, 2, 2));
        });
    }

    @Test
    public void testHealWithHuntersMark() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);
            script.playCard("p1", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));

            script.playCard("p1", HUNTERS_MARK, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 2),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
        });
    }

    @Test
    public void testStormwindChampion() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.setMana("p1", 10);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 6),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6),
                    expectedMinion(SLIME, 2, 3),
                    expectedMinion(TREANT, 3, 3));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(TREANT, 2, 2));
        });
    }

    @Test
    public void testStormwindChampionUpdatesAuraAfterDeathRattle() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", SLUDGE_BELCHER, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);

            script.expectBoard("p1",
                    expectedMinion(SLUDGE_BELCHER, 4, 6),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(SLIME, 2, 3),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
        });
    }

    @Test
    public void testKillingStormwindChampionDoesNotReduceCurrentHealth() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 1);
            script.setMana("p1", 10);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 2);
            script.playMinionCard("p1", SLIME, 3);
            script.playMinionCard("p1", TREANT, 4);

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 6),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6),
                    expectedMinion(SLIME, 2, 3),
                    expectedMinion(TREANT, 3, 3));

            script.setMana("p1", 10);
            script.playCard("p1", MOONFIRE, "p1:0");
            script.playCard("p1", MOONFIRE, "p1:1");
            script.playCard("p1", MOONFIRE, "p1:2");
            script.playCard("p1", MOONFIRE, "p1:3");
            script.playCard("p1", MOONFIRE, "p1:4");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 3, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 5),
                    expectedMinion(SLIME, 2, 2),
                    expectedMinion(TREANT, 3, 2));

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:2");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(SLIME, 1, 2),
                    expectedMinion(TREANT, 2, 2));
        });
    }

    @Test
    public void testRedemptionResurrectsHpAuraProvider() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playCard("p1", REDEMPTION);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", KORKRON_ELITE, 1);
            script.playCard("p1", DARKBOMB, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(STORMWIND_CHAMPION, 6, 6),
                    expectedMinion(KORKRON_ELITE, 5, 1));

            script.setCurrentPlayer("p2");
            script.setMana("p2", 10);
            script.playCard("p2", FIREBALL, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(STORMWIND_CHAMPION, 6, 1),
                    expectedMinion(KORKRON_ELITE, 5, 1));
        });
    }

    @Test
    public void testAutoPlayedMalganisSavesDeadMinion() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.addToHand("p1", MALGANIS);

            script.playMinionCard("p1", VOIDWALKER, 0);
            script.playMinionCard("p1", VOIDCALLER, 1);
            script.playMinionCard("p1", EXPLOSIVE_SHEEP, 2);
            script.playCard("p1", DARKBOMB, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(VOIDWALKER, 1, 3),
                    expectedMinion(VOIDCALLER, 3, 1),
                    expectedMinion(EXPLOSIVE_SHEEP, 1, 1));

            script.setCurrentPlayer("p2");
            script.setMana("p2", 10);
            script.playCard("p2", CONSECRATION);

            script.expectBoard("p1",
                    expectedMinion(VOIDWALKER, 3, 1),
                    expectedMinion(MALGANIS, 9, 5));
        });
    }
}
