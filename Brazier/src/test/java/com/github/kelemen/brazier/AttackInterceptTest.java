package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class AttackInterceptTest {
    @Test
    public void testExplosiveTrapAttackerKillPreventsMinionAttack() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", EXPLOSIVE_TRAP, 0);

            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", STONETUSK_BOAR, 0);

            script.expectBoard("p1");
            script.expectBoard("p2", expectedMinion(STONETUSK_BOAR, 1, 1));

            script.setHeroHp("p1", 18, 0);
            script.setHeroHp("p2", 28, 0);

            script.attack("p2:0", "p1:hero");

            script.expectHeroHp("p1", 18, 0);
            script.expectHeroHp("p2", 26, 0);

            script.expectBoard("p1");
            script.expectBoard("p2");
        });
    }

    @Test
    public void testExplosiveTrapHeroKillPreventsMinionAttack() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", EXPLOSIVE_TRAP, 0);

            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", STONETUSK_BOAR, 0);

            script.expectBoard("p1");
            script.expectBoard("p2", expectedMinion(STONETUSK_BOAR, 1, 1));

            script.setHeroHp("p1", 1, 0);
            script.setHeroHp("p2", 2, 0);

            script.attack("p2:0", "p1:hero");
            script.expectHeroDeath("p2");

            script.expectHeroHp("p1", 1, 0);
            script.expectHeroHp("p2", 0, 0);
        });
    }

    @Test
    public void testExplosiveTrapKillPreventsAuraBeforeAttack() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", EXPLOSIVE_TRAP, 0);

            script.setCurrentPlayer("p2");
            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p2", DIRE_WOLF_ALPHA, 1);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 5, 5),
                    expectedMinion(DIRE_WOLF_ALPHA, 2, 2));

            script.setHeroHp("p1", 18, 0);
            script.setHeroHp("p2", 28, 0);

            script.refreshAttacks();
            script.attack("p2:0", "p1:hero");

            script.expectHeroHp("p1", 14, 0);
            script.expectHeroHp("p2", 26, 0);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 3));
        });
    }
}
