package com.github.kelemen.brazier;

import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.minions.MinionId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.jtrim.collections.CollectionsEx;
import org.junit.Test;

import static com.github.kelemen.brazier.TestCards.*;
import static org.junit.Assert.*;

public final class SummonEventTest {
    @Test
    public void testWarsongCommander() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.playMinionCard("p1", WARSONG_COMMANDER, 0);
            script.playMinionCard("p1", YETI, 1);
            script.playMinionCard("p1", SLIME, 2);

            script.expectMinion("p1:1", (minion) -> {
                assertFalse("charge", minion.isCharge());
            });
            script.expectMinion("p1:2", (minion) -> {
                assertTrue("charge", minion.isCharge());
            });

            script.playMinionCard("p2", SLIME, 0);

            script.expectMinion("p2:0", (minion) -> {
                assertFalse("charge", minion.isCharge());
            });
        });
    }

    private MinionId singleMinionScript(String minionLocation, Consumer<PlayScript> scriptConfig) {
        List<Set<MinionId>> results = new LinkedList<>();

        PlayScript.testScript((script) -> {
            scriptConfig.accept(script);

            script.expectMinion(minionLocation, (minion) -> {
                results.add(Collections.singleton(minion.getBaseDescr().getId()));
            });
        });

        Set<MinionId> firstResult = results.remove(0);
        for (Set<MinionId> result: results) {
            assertEquals("board result", firstResult, result);
        }

        return firstResult.iterator().next();
    }

    private MinionId testAlarmOBot(int roll) {
        return singleMinionScript("p1:0", (script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.addToHand("p1", YETI, PYROBLAST, FIRE_ELEMENTAL, FIERY_WAR_AXE);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", ALARM_O_BOT, 0);

            script.expectBoard("p1",
                    expectedMinion(ALARM_O_BOT, 0, 3),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1));

            script.setCurrentPlayer("p1");
            script.endTurn();
            script.addRoll(2, roll);
            script.endTurn();

            script.expectPlayer("p1", (player) -> {
                MinionId minionId = player.getBoard().getAllMinions().get(0).getBaseDescr().getId();

                List<String> expectedCards;
                if (YETI.equals(minionId.getName())) {
                    expectedCards = Arrays.asList(ALARM_O_BOT, PYROBLAST, FIRE_ELEMENTAL, FIERY_WAR_AXE);
                }
                else {
                    expectedCards = Arrays.asList(YETI, PYROBLAST, ALARM_O_BOT, FIERY_WAR_AXE);
                }

                Hand hand = player.getHand();
                List<Card> handCards = hand.getCards();
                List<String> handCardNames = new ArrayList<>(handCards.size());
                handCards.forEach((card) -> handCardNames.add(card.getCardDescr().getId().getName()));

                assertEquals("hand", expectedCards, handCardNames);
            });
        });
    }

    private Set<MinionId> minionIds(String... names) {
        Set<MinionId> result = CollectionsEx.newHashSet(names.length);
        for (String name: names) {
            result.add(new MinionId(name));
        }
        return result;
    }

    @Test
    public void testAlarmOBot() {
        Set<MinionId> ids = new HashSet<>();
        ids.add(testAlarmOBot(0));
        ids.add(testAlarmOBot(1));

        assertEquals("possibilities", minionIds(YETI, FIRE_ELEMENTAL), ids);
    }

    @Test
    public void testAlarmOBotWithNoMinions() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.setMana("p2", 10);

            script.addToHand("p1", PYROBLAST, FIERY_WAR_AXE);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", ALARM_O_BOT, 0);

            script.expectBoard("p1",
                    expectedMinion(ALARM_O_BOT, 0, 3),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1));

            script.setCurrentPlayer("p1");
            script.endTurn();
            script.endTurn();

            script.expectBoard("p1",
                    expectedMinion(ALARM_O_BOT, 0, 3),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1),
                    expectedMinion(WISP, 1, 1));
        });
    }
}
