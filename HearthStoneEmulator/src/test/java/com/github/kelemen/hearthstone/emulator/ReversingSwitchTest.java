package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class ReversingSwitchTest {
    @Test
    public void testReverseWithStormwind() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", EMPEROR_COBRA, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_COBRA, 3, 4),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));

            script.setMana("p1", 10);

            script.playCard("p1", REVERSING_SWITCH, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_COBRA, 5, 4),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
        });
    }
}
