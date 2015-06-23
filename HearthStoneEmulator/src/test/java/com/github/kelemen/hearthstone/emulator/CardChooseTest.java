package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class CardChooseTest {
    @Test
    public void testTracking() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.deck("p1", YETI, CULT_MASTER, BLACKWING_CORRUPTOR, BLUEGILL_WARRIOR, ABUSIVE_SERGEANT);

            script.addCardChoice(1, ABUSIVE_SERGEANT, BLUEGILL_WARRIOR, BLACKWING_CORRUPTOR);
            script.playCard("p1", TRACKING);

            script.expectHand("p1", BLUEGILL_WARRIOR);
            script.expectDeck("p1", YETI, CULT_MASTER);
        });
    }

    @Test
    public void testTrackingWith2Cards() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.deck("p1", YETI, CULT_MASTER);

            script.addCardChoice(0, CULT_MASTER, YETI);
            script.playCard("p1", TRACKING);

            script.expectHand("p1", CULT_MASTER);
            script.expectDeck("p1");
        });
    }

    @Test
    public void testTrackingWith1Card() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.deck("p1", YETI);

            script.playCard("p1", TRACKING);

            script.expectHand("p1", YETI);
            script.expectDeck("p1");
        });
    }

    @Test
    public void testTrackingWithNoCards() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playCard("p1", TRACKING);

            script.expectHand("p1");
            script.expectDeck("p1");
        });
    }
}
