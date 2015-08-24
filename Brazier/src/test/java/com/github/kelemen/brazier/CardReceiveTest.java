package com.github.kelemen.brazier;

import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;

public final class CardReceiveTest {
    @Test
    public void testHeadcrackNoCombo() {
        PlayScript.testScript((script) -> {
            script.setCurrentPlayer("p1");

            script.addToHand("p1", YETI, WHIRLWIND);
            script.addToHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER);

            script.deck("p1", BLUEGILL_WARRIOR, ABUSIVE_SERGEANT);
            script.deck("p2", ANCIENT_MAGE, CULT_MASTER);

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);

            script.setMana("p1", 10);
            script.playCard("p1", HEADCRACK);

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 18, 0);

            script.endTurn();

            script.expectHand("p1", YETI, WHIRLWIND);
            script.expectHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER, CULT_MASTER);
        });
    }

    @Test
    public void testHeadcrackCombo() {
        PlayScript.testScript((script) -> {
            script.setCurrentPlayer("p1");

            script.addToHand("p1", YETI, WHIRLWIND);
            script.addToHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER);

            script.deck("p1", BLUEGILL_WARRIOR, ABUSIVE_SERGEANT);
            script.deck("p2", ANCIENT_MAGE, CULT_MASTER);

            script.setHeroHp("p1", 30, 0);
            script.setHeroHp("p2", 20, 0);

            script.setMana("p1", 10);
            script.playCard("p1", THE_COIN);
            script.playCard("p1", HEADCRACK);

            script.expectHeroHp("p1", 30, 0);
            script.expectHeroHp("p2", 18, 0);

            script.endTurn();

            script.expectHand("p1", YETI, WHIRLWIND, HEADCRACK);
            script.expectHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER, CULT_MASTER);

            script.endTurn();

            script.expectHand("p1", YETI, WHIRLWIND, HEADCRACK, ABUSIVE_SERGEANT);
            script.expectHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER, CULT_MASTER);

            script.endTurn();

            script.expectHand("p1", YETI, WHIRLWIND, HEADCRACK, ABUSIVE_SERGEANT);
            script.expectHand("p2", SHADOW_MADNESS, DEFIAS_RINGLEADER, CULT_MASTER, ANCIENT_MAGE);
        });
    }
}
