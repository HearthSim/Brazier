package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;
import static org.junit.Assert.*;

public final class SpellPowerTest {
    @Test
    public void testKoboldGeomancerWithTargetedSpell() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.setHeroHp("p1", 30, 0);

            script.playCard("p1", MOONFIRE, "p1:hero");
            script.expectHeroHp("p1", 29, 0);

            script.playCard("p1", KOBOLD_GEOMANCER);
            script.expectBoard("p1", expectedMinion(KOBOLD_GEOMANCER, 2, 2));

            script.playCard("p1", MOONFIRE, "p1:hero");
            script.expectHeroHp("p1", 27, 0);

            script.playCard("p1", MOONFIRE, "p1:0");
            script.expectBoard("p1");

            script.playCard("p1", MOONFIRE, "p1:hero");
            script.expectHeroHp("p1", 26, 0);
        });
    }

    @Test
    public void testKoboldGeomancerWithAoe() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", KOBOLD_GEOMANCER);
            script.playCard("p2", YETI);

            script.expectBoard("p1", expectedMinion(KOBOLD_GEOMANCER, 2, 2));
            script.expectBoard("p2", expectedMinion(YETI, 4, 5));

            script.playCard("p1", FLAMESTRIKE);

            script.expectBoard("p1", expectedMinion(KOBOLD_GEOMANCER, 2, 2));
            script.expectBoard("p2");

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p2", YETI);
            script.expectBoard("p1", expectedMinion(KOBOLD_GEOMANCER, 2, 2));
            script.expectBoard("p2", expectedMinion(YETI, 4, 5));

            script.playCard("p1", MOONFIRE, "p1:0");
            script.expectBoard("p1");
            script.expectBoard("p2", expectedMinion(YETI, 4, 5));

            script.playCard("p1", FLAMESTRIKE);
            script.expectBoard("p1");
            script.expectBoard("p2", expectedMinion(YETI, 4, 1));
        });
    }

    private static void expectSpellPower(
            PlayScript script,
            int expectedSpellPower1,
            int expectedSpellPower2) {
        script.expectPlayer("p1", (player) -> {
            assertEquals("spellPower[p1]", expectedSpellPower1, player.getSpellPower().getValue());

            Player opponent = player.getOpponent();
            assertEquals("spellPower[p2]", expectedSpellPower2, opponent.getSpellPower().getValue());
        });
    }

    @Test
    public void testAncientMageNoTarget() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", ANCIENT_MAGE, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 2);

            expectSpellPower(script, 0, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(ANCIENT_MAGE, 2, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));

            expectSpellPower(script, 0, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:1");
            expectSpellPower(script, 0, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.endTurn();

            expectSpellPower(script, 0, 0);
        });
    }

    @Test
    public void testAncientMageLeftTarget() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", ANCIENT_MAGE, 1);
            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 2);

            expectSpellPower(script, 1, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(ANCIENT_MAGE, 2, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));

            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:1");
            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.endTurn();

            expectSpellPower(script, 0, 0);
        });
    }

    @Test
    public void testAncientMageRightTarget() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 0);
            script.playMinionCard("p1", ANCIENT_MAGE, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);

            expectSpellPower(script, 1, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(ANCIENT_MAGE, 2, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));

            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:1");
            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.endTurn();

            expectSpellPower(script, 0, 0);
        });
    }

    @Test
    public void testAncientMageBothTarget() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", FLAME_OF_AZZINOTH, 0);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", ANCIENT_MAGE, 1);

            expectSpellPower(script, 2, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1),
                    expectedMinion(ANCIENT_MAGE, 2, 5),
                    expectedMinion(FLAME_OF_AZZINOTH, 2, 1));

            expectSpellPower(script, 2, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:1");
            expectSpellPower(script, 2, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 1, 0);

            script.setMana("p1", 10);
            script.playCard("p1", PYROBLAST, "p1:0");
            expectSpellPower(script, 0, 0);

            script.endTurn();

            expectSpellPower(script, 0, 0);
        });
    }
}
