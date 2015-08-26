package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;
import static org.junit.Assert.*;

public final class GreedyQueueTest {
    private int testExplosiveTrapWithAttack(boolean trapFirst, boolean attackFace, int roll) {
        return RandomTestUtils.singleMinionScript("p1:0", (minion) -> minion.getBody().getCurrentHp(), (script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            if (trapFirst) {
                script.playCard("p2", EXPLOSIVE_TRAP);
            }

            script.playMinionCard("p1", OGRE_BRUTE, 0);
            script.playMinionCard("p2", FLAMETONGUE_TOTEM, 0);

            if (!trapFirst) {
                script.playCard("p2", EXPLOSIVE_TRAP);
            }

            script.setCurrentPlayer("p1");
            script.refreshAttacks();

            script.addRoll(2, roll);
            script.attack("p1:0", attackFace ? "p2:hero" : "p2:0");
        });
    }

    private void testExplosiveTrapWithAttackFace(boolean trapFirst) {
        int hp1 = testExplosiveTrapWithAttack(trapFirst, true, 0);
        int hp2 = testExplosiveTrapWithAttack(trapFirst, true, 1);

        if (hp1 != 2 || hp2 != 2) {
            fail("Ogre hp should be 2 after trigger but was " + hp1 + " and " + hp2);
        }
    }

    private void testExplosiveTrapWithAttackMinion(boolean trapFirst) {
        int hp1 = testExplosiveTrapWithAttack(trapFirst, false, 0);
        int hp2 = testExplosiveTrapWithAttack(trapFirst, false, 1);

        if (hp1 == 2 && hp2 == 4) {
            return;
        }
        if (hp1 == 4 && hp2 == 2) {
            return;
        }

        fail("Ogre hp should be 2 in case and 4 in the other case but was " + hp1 + " and " + hp2);
    }

    @Test
    public void testExplosiveTrapWithAttackFaceTrapFirst() {
        testExplosiveTrapWithAttackFace(true);
    }

    @Test
    public void testExplosiveTrapWithAttackFaceTrapLast() {
        testExplosiveTrapWithAttackFace(false);
    }

    @Test
    public void testExplosiveTrapWithAttackMinionTrapFirst() {
        testExplosiveTrapWithAttackMinion(true);
    }

    @Test
    public void testExplosiveTrapWithAttackMinionTrapLast() {
        testExplosiveTrapWithAttackMinion(false);
    }
}
