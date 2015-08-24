package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class TargetedBattleCryTest {
    @Test
    public void testFireElemental() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p2", YETI, 0);

            script.expectBoard("p1");
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.playMinionCard("p1", FIRE_ELEMENTAL, 0, "p2:0");

            script.expectBoard("p1",
                    expectedMinion(FIRE_ELEMENTAL, 6, 5));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2));
        });
    }
}
