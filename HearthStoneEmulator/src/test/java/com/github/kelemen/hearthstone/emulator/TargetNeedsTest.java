package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.TestDb;
import com.github.kelemen.hearthstone.emulator.ui.PlayerTargetNeed;
import java.util.List;
import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;
import static org.junit.Assert.*;

public final class TargetNeedsTest {
    private static boolean canTarget(Player player, CardDescr card, TargetableCharacter target) {
        TargetNeed cardNeed = card.getCombinedTargetNeed(player);
        boolean hero = target instanceof Hero;
        TargeterDef targeterDef = new TargeterDef(player.getPlayerId(), hero, false);
        PlayerTargetNeed targetNeed = new PlayerTargetNeed(targeterDef, cardNeed);
        return targetNeed.isAllowedTarget(target);
    }

    private static CardDescr getCard(String cardId) {
        return TestDb.getTestDb().getCardDb().getById(new CardId(cardId));
    }

    @Test
    public void testExecute() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", YETI, 0);
            script.playMinionCard("p1", SLIME, 1);

            script.playCard("p2", MOONFIRE, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 4),
                    expectedMinion(SLIME, 1, 2));

            script.expectPlayer("p1", (player) -> {
                Player opponent = player.getWorld().getOpponent(player.getPlayerId());

                List<Minion> minions = player.getBoard().getAllMinions();
                CardDescr execute = getCard(EXECUTE);

                assertTrue("Target:Yeti", canTarget(opponent, execute, minions.get(0)));
                assertFalse("Target:Slime", canTarget(opponent, execute, minions.get(1)));
            });
        });
    }
}
