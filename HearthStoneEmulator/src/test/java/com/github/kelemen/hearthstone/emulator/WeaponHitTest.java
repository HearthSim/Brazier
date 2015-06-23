package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;
import static org.junit.Assert.*;

public final class WeaponHitTest {
    @Test
    public void testCantAttackSecondTimeAfterWeaponReplace() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playCard("p1", FIERY_WAR_AXE);
            script.expectPlayer("p1", (player) -> {
                assertTrue(player.getHero().getAttackTool().canAttackWith());
            });

            script.attack("p1:hero", "p2:hero");
            script.expectPlayer("p1", (player) -> {
                assertFalse(player.getHero().getAttackTool().canAttackWith());
            });

            script.playCard("p1", FIERY_WAR_AXE);
            script.expectPlayer("p1", (player) -> {
                assertFalse(player.getHero().getAttackTool().canAttackWith());
            });
        });
    }

    @Test
    public void testLoseWeapon() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FIERY_WAR_AXE);

            script.expectBoard("p1");
            script.expectBoard("p2");

            script.expectWeapon("p1", 3, 2);
            script.attack("p1:hero", "p2:hero");
            script.expectWeapon("p1", 3, 1);

            script.refreshAttacks();
            script.attack("p1:hero", "p2:hero");
            script.expectNoWeapon("p1", 0);

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 24, 0);
        });
    }

    @Test
    public void testHitWeaponlessHeroWithWeapon() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FIERY_WAR_AXE);

            script.expectBoard("p1");
            script.expectBoard("p2");

            script.expectWeapon("p1", 3, 2);
            script.attack("p1:hero", "p2:hero");
            script.expectWeapon("p1", 3, 1);

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 27, 0);
        });
    }

    @Test
    public void testHitWeaponedHeroWithWeapon() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FIERY_WAR_AXE);
            script.playCard("p2", FIERY_WAR_AXE);

            script.expectBoard("p1");
            script.expectBoard("p2");

            script.expectWeapon("p1", 3, 2);
            script.attack("p1:hero", "p2:hero");
            script.expectWeapon("p1", 3, 1);

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 27, 0);

            script.expectWeapon("p1", 3, 1);
        });
    }

    @Test
    public void testHitMinionWithWeapon() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p1", FIERY_WAR_AXE);
            script.playMinionCard("p2", YETI, 0);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.expectWeapon("p1", 3, 2);
            script.attack("p1:hero", "p2:0");
            script.expectWeapon("p1", 3, 1);

            script.expectHeroHp("p1", 26, 0);
            script.expectHeroHp("p2", 30, 0);
        });
    }
}
