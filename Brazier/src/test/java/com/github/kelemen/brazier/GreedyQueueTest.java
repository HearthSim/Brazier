package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;
import static org.junit.Assert.*;

public final class GreedyQueueTest {
    public int testExplosiveTrapWithExplicitAttack(int roll) {
        return RandomTestUtils.singleMinionScript("p1:0", (minion) -> minion.getBody().getCurrentHp(), (script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playCard("p2", EXPLOSIVE_TRAP);

            script.playMinionCard("p1", OGRE_BRUTE, 0);
            script.playMinionCard("p2", FLAMETONGUE_TOTEM, 0);

            script.setCurrentPlayer("p1");
            script.refreshAttacks();

            script.addRoll(2, roll);
            script.attack("p1:0", "p2:hero");
        });
    }

    @Test
    public void testExplosiveTrapWithExplicitAttack() {
        int hp1 = testExplosiveTrapWithExplicitAttack(0);
        int hp2 = testExplosiveTrapWithExplicitAttack(1);

        if (hp1 != 2 || hp2 != 2) {
            fail("Ogre hp should be 2 after trigger but was " + hp1 + " and " + hp2);
        }
    }
}
