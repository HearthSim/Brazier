package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class HeroAttackedTest {
    @Test
    public void testOneOffLethal() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setHeroHp("p2", 5, 0);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.attack("p1:0", "p2:hero");

            script.expectGameContinues();
        });
    }

    @Test
    public void testExactLethal() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setHeroHp("p2", 4, 0);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.attack("p1:0", "p2:hero");

            script.expectHeroDeath("p2");
        });
    }

    @Test
    public void testExactLethalWithArmor() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setHeroHp("p2", 3, 1);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.attack("p1:0", "p2:hero");

            script.expectHeroDeath("p2");
        });
    }

    @Test
    public void testOverLethal() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.setHeroHp("p2", 3, 0);

            script.playMinionCard("p1", YETI, 0);
            script.refreshAttacks();

            script.attack("p1:0", "p2:hero");

            script.expectHeroDeath("p2");
        });
    }

    @Test
    public void testAttackHero() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 4);
            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.attack("p1:0", "p2:hero");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 26, 0);
        });
    }

    @Test
    public void testAttackHeroWithLowArmor() {
        PlayScript.testScript((script) -> {
            script.setHeroHp("p2", 30, 3);

            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.attack("p1:0", "p2:hero");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 29, 0);
        });
    }

    @Test
    public void testAttackHeroWithJustEnoughArmor() {
        PlayScript.testScript((script) -> {
            script.setHeroHp("p2", 30, 4);

            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.attack("p1:0", "p2:hero");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 30, 0);
        });
    }

    @Test
    public void testAttackHeroWithHighArmor() {
        PlayScript.testScript((script) -> {
            script.setHeroHp("p2", 30, 10);

            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);

            script.refreshAttacks();

            script.expectBoard("p1", expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.attack("p1:0", "p2:hero");

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 30, 6);
        });
    }
}
