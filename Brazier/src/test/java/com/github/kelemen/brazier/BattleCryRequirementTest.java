package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class BattleCryRequirementTest {
    @Test
    public void testBlackwingTechnicianRequirementIsNotMet() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", BLACKWING_TECHNICIAN, 0);

            script.expectBoard("p1",
                    expectedMinion(BLACKWING_TECHNICIAN, 2, 4));
        });
    }

    @Test
    public void testBlackwingTechnicianRequirementIsMet() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.addToHand("p1", BLUEGILL_WARRIOR, MALYGOS, YETI);

            script.playMinionCard("p1", BLACKWING_TECHNICIAN, 0);

            script.expectBoard("p1",
                    expectedMinion(BLACKWING_TECHNICIAN, 3, 5));
        });
    }

    @Test
    public void testBlackwingCorruptorRequirementIsNotMet() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p1", BLACKWING_CORRUPTOR, 0, "p2:0"); // Target must be ignored

            script.expectBoard("p1",
                    expectedMinion(BLACKWING_CORRUPTOR, 5, 4));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));
        });
    }

    @Test
    public void testBlackwingCorruptorRequirementIsMet() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.addToHand("p1", BLUEGILL_WARRIOR, MALYGOS, YETI);

            script.playMinionCard("p2", YETI, 0);
            script.playMinionCard("p1", BLACKWING_CORRUPTOR, 0, "p2:0");

            script.expectBoard("p1",
                    expectedMinion(BLACKWING_CORRUPTOR, 5, 4));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 2));
        });
    }
}
