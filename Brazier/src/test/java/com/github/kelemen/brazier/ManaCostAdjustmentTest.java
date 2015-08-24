package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class ManaCostAdjustmentTest {
    @Test
    public void testDreadCorsairCost4ByDefault() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", DREAD_CORSAIR, 0);
            script.expectedMana("p1", 6);

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testDreadCorsairCostIsReducedCorrectly() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playCard("p1", FIERY_WAR_AXE);
            script.expectedMana("p1", 8);
            script.playMinionCard("p1", DREAD_CORSAIR, 0);
            script.expectedMana("p1", 7);

            script.expectBoard("p1",
                    expectedMinion(DREAD_CORSAIR, 3, 3));
            script.expectBoard("p2");
        });
    }
}
