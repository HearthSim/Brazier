package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class DamageEventTest {
    @Test
    public void testWhirlwind() {
        PlayScript.testScript((script) -> {
            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 30, 0);

            script.setMana("p1", 10);
            script.playMinionCard("p1", FROTHING_BERSERKER, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", YETI, 0);
            script.setMana("p1", 10);
            script.playMinionCard("p1", FROTHING_BERSERKER, 0);

            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);
            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);
            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);
            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 4),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.playCard("p1", WHIRLWIND);

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 16, 3),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(FROTHING_BERSERKER, 16, 3));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(YETI, 4, 4));

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 30, 0);
        });
    }

    @Test
    public void testFrothingBerserkerVsForthingBerserker() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", FROTHING_BERSERKER, 0);
            script.playMinionCard("p1", FROTHING_BERSERKER, 0);
            script.playMinionCard("p2", FROTHING_BERSERKER, 0);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 2, 4),
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.expectBoard("p2",
                    expectedMinion(FROTHING_BERSERKER, 2, 4));

            script.attack("p1:0", "p2:0");

            script.expectBoard("p1",
                    expectedMinion(FROTHING_BERSERKER, 4, 2),
                    expectedMinion(FROTHING_BERSERKER, 4, 4));

            script.expectBoard("p2",
                    expectedMinion(FROTHING_BERSERKER, 4, 2));
        });
    }

    @Test
    public void testGurubashiBerserker() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", GURUBASHI_BERSERKER, 0);
            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(GURUBASHI_BERSERKER, 2, 7));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(YETI, 4, 5));

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 1),
                    expectedMinion(GURUBASHI_BERSERKER, 2, 7));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 1),
                    expectedMinion(YETI, 4, 5));

            script.attack("p2:1", "p1:1");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 1),
                    expectedMinion(GURUBASHI_BERSERKER, 5, 3));

            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 1),
                    expectedMinion(YETI, 4, 3));
        });
    }

    @Test
    public void testGrimPatron() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", GRIM_PATRON, 0);
            script.playMinionCard("p1", GRIM_PATRON, 0);
            script.playMinionCard("p2", GRIM_PATRON, 0);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.expectBoard("p1",
                    expectedMinion(GRIM_PATRON, 3, 3),  // 2.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 1.

            script.expectBoard("p2",
                    expectedMinion(GRIM_PATRON, 3, 3)); // 1.

            script.playCard("p2", WHIRLWIND);

            script.expectBoard("p1",
                    expectedMinion(GRIM_PATRON, 3, 2),  // 2.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 4.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 1.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 3.

            script.expectBoard("p2",
                    expectedMinion(GRIM_PATRON, 3, 2),  // 1.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 2.

            script.playCard("p2", WHIRLWIND);

            script.expectBoard("p1",
                    expectedMinion(GRIM_PATRON, 3, 1),  // 2.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 6.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 4.
                    expectedMinion(GRIM_PATRON, 3, 1),  // 1.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 5.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 3.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 7.

            script.expectBoard("p2",
                    expectedMinion(GRIM_PATRON, 3, 1),  // 1.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 3.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 2.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 4.

            script.playCard("p2", WHIRLWIND);

            script.expectBoard("p1",
                    expectedMinion(GRIM_PATRON, 3, 2),  // 6.
                    expectedMinion(GRIM_PATRON, 3, 1),  // 4.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 5.
                    expectedMinion(GRIM_PATRON, 3, 1),  // 3.
                    expectedMinion(GRIM_PATRON, 3, 2)); // 7.

            script.expectBoard("p2",
                    expectedMinion(GRIM_PATRON, 3, 2),  // 3.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 6.
                    expectedMinion(GRIM_PATRON, 3, 1),  // 2.
                    expectedMinion(GRIM_PATRON, 3, 3),  // 5.
                    expectedMinion(GRIM_PATRON, 3, 2),  // 7.
                    expectedMinion(GRIM_PATRON, 3, 3)); // 4.
        });
    }

    @Test
    public void testEmperorCobra() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", EMPEROR_COBRA, 0);
            script.playMinionCard("p2", STORMWIND_CHAMPION, 0);

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_COBRA, 2, 3));
            script.expectBoard("p2",
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));

            script.refreshAttacks();
            script.attack("p1:0", "p2:0");

            script.expectBoard("p1");
            script.expectBoard("p2");
        });
    }

    @Test
    public void testWaterElementalAttacks() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WATER_ELEMENTAL, 0);
            script.playMinionCard("p2", SLUDGE_BELCHER, 0);

            script.setCurrentPlayer("p1");
            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 6));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 5, "taunt"));

            script.attack("p1:0", "p2:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt", "frozen"));

            script.endTurn(); // p1

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt", "frozen"));

            script.endTurn(); // p2
            script.endTurn(); // p1

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt"));
        });
    }

    @Test
    public void testWaterElementalAttacked() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WATER_ELEMENTAL, 0);
            script.playMinionCard("p2", SLUDGE_BELCHER, 0);

            script.setCurrentPlayer("p2");
            script.refreshAttacks();

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 6));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 5, "taunt"));

            script.attack("p2:0", "p1:0");

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt", "frozen"));

            script.endTurn(); // p2
            script.endTurn(); // p1

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt", "frozen"));

            script.endTurn(); // p2
            script.endTurn(); // p1

            script.expectBoard("p1",
                    expectedMinionWithFlags(WATER_ELEMENTAL, 3, 3));
            script.expectBoard("p2",
                    expectedMinionWithFlags(SLUDGE_BELCHER, 3, 2, "taunt"));
        });
    }
}
