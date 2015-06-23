package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class StatBuffTest {
    @Test
    public void testAttackAndHpBuff() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));

            script.playMinionCard("p1", SHATTERED_SUN_CLERIC, 1, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 5, 6),
                    expectedMinion(SHATTERED_SUN_CLERIC, 3, 2));
        });
    }
}
