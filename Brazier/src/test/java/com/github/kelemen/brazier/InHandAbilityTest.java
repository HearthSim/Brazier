package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class InHandAbilityTest {
    @Test
    public void testBolvarWithoutDeaths() {
        PlayScript.testScript((script) -> {
            script.addToHand("p1", BOLVAR_FORDRAGON);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p2", WISP, 0);
            script.playMinionCard("p1", 0, 1);

            script.expectBoard("p1",
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(BOLVAR_FORDRAGON, 1, 7));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));
        });
    }

    @Test
    public void testBolvarWithDeaths() {
        PlayScript.testScript((script) -> {
            script.addToHand("p1", BOLVAR_FORDRAGON);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 1);
            script.playMinionCard("p1", WISP, 2);
            script.playMinionCard("p2", WISP, 0);

            script.playCard("p2", FLAMESTRIKE);
            script.playMinionCard("p1", 0, 0);

            script.expectBoard("p1",
                    expectedMinion(BOLVAR_FORDRAGON, 4, 7));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));
        });
    }

    @Test
    public void testBolvarWithEnemyDeaths() {
        PlayScript.testScript((script) -> {
            script.addToHand("p1", BOLVAR_FORDRAGON);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p2", WISP, 0);
            script.playMinionCard("p2", WISP, 1);
            script.playMinionCard("p2", WISP, 2);

            script.playCard("p1", FLAMESTRIKE);
            script.setMana("p1", 10);
            script.playMinionCard("p1", 0, 1);

            script.expectBoard("p1",
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(BOLVAR_FORDRAGON, 1, 7));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testBolvarIsNotBuffedAfterPlay() {
        PlayScript.testScript((script) -> {
            script.addToHand("p1", BOLVAR_FORDRAGON);

            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p2", WISP, 0);
            script.playMinionCard("p1", 0, 1);

            script.expectBoard("p1",
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(BOLVAR_FORDRAGON, 1, 7));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));

            script.playCard("p2", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BOLVAR_FORDRAGON, 1, 7));
            script.expectBoard("p2",
                    expectedMinion(WISP, 1, 1));
        });
    }
}
