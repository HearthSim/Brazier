package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HearthStoneEntityDatabase;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.PlayScript;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardType;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public final class DefaultDbTest {
    @Test
    public void testDbIsAvailable() throws IOException, ObjectParsingException {
        TestDb.getTestDbUnsafe();
    }

    @Test
    public void testAllMinionIsSummonable() {
        HearthStoneEntityDatabase<MinionDescr> minionDb = TestDb.getTestDb().getMinionDb();
        for (MinionDescr minion: minionDb.getAll()) {
            PlayScript.testScript((script) -> {
                script.adjustPlayer("p1", (player) -> {
                    return player.summonMinion(minion);
                });
            });
        }
    }

    @Test
    public void testCardTypeKeywords() {
        HearthStoneEntityDatabase<CardDescr> cardDb = TestDb.getTestDb().getCardDb();
        for (CardDescr card: cardDb.getAll()) {
            try {
                Set<Keyword> keywords = card.getKeywords();

                switch (card.getCardType()) {
                    case SPELL:
                        assertFalse(keywords.contains(Keywords.MINION));
                        assertTrue(keywords.contains(Keywords.SPELL));
                        assertFalse(keywords.contains(Keywords.WEAPON));
                        break;
                    case MINION:
                        assertTrue(keywords.contains(Keywords.MINION));
                        assertFalse(keywords.contains(Keywords.SPELL));
                        assertFalse(keywords.contains(Keywords.WEAPON));
                        break;
                    case WEAPON:
                        assertFalse(keywords.contains(Keywords.MINION));
                        assertFalse(keywords.contains(Keywords.SPELL));
                        assertTrue(keywords.contains(Keywords.WEAPON));
                        break;
                    default:
                        throw new AssertionError(card.getCardType().name());
                }
            } catch (Throwable ex) {
                throw new AssertionError("Card type error: " + card.getId(), ex);
            }
        }
    }

    @Test
    public void testBasicKeywordsInDescription() {
        HearthStoneEntityDatabase<CardDescr> cardDb = TestDb.getTestDb().getCardDb();
        for (CardDescr card: cardDb.getAll()) {
            try {
                String description = card.getDescription();

                if (card.getOverload() > 0) {
                    assertTrue("Overload", description.contains("Overload"));
                }

                boolean needDescription;
                MinionDescr minion = card.getMinion();
                if (minion != null) {
                    if (minion.isTaunt()) {
                        assertTrue("Taunt", description.contains("Taunt"));
                    }
                    if (minion.isDivineShield()) {
                        assertTrue("Divine Shield", description.contains("Divine Shield"));
                    }
                    if (minion.isStealth()) {
                        assertTrue("Stealth", description.contains("Stealth"));
                    }
                    if (!minion.getBattleCries().isEmpty()) {
                        assertTrue("Battlecry", description.contains("Battlecry"));
                    }
                    int overload = minion.getBaseCard().getOverload();
                    if (overload > 0) {
                        assertTrue("Overload", description.contains("Overload: (" + overload + ")"));
                    }
                    if (minion.getMaxAttackCount() == 2) {
                        assertTrue("Windfury", description.contains("Windfury"));
                    }
                    if (!minion.isCanAttack()) {
                        assertTrue("Can't Attack", description.contains("Can't Attack"));
                    }
                    if (!minion.isTargetable()) {
                        assertTrue("Not targetable", description.contains("Can't be targeted by spells or Hero Powers"));
                    }
                    needDescription = minion.getBattleCries().size() > 0
                            || minion.getEventActionDefs().hasAnyActionDef()
                            || minion.tryGetAbility() != null
                            || card.tryGetInHandAbility() != null;
                }
                else {
                    // Improve this for weapons
                    needDescription = card.getCardType() == CardType.SPELL;
                }

                if (needDescription) {
                    if (description.isEmpty()) {
                        fail("This card has some custom action defined, so need a description.");
                    }
                }
            } catch (Throwable ex) {
                throw new AssertionError("Wrong card description for " + card.getId(), ex);
            }
        }
    }

    @Test
    public void testAllMinionsHaveCard() {
        HearthStoneEntityDatabase<MinionDescr> minionDb = TestDb.getTestDb().getMinionDb();
        for (MinionDescr minion: minionDb.getAll()) {
            assertNotNull("baseCard", minion.getBaseCard());
        }
    }

    @Test
    public void testAllMinionCardsHaveMinion() {
        HearthStoneEntityDatabase<CardDescr> cardDb = TestDb.getTestDb().getCardDb();
        for (CardDescr card: cardDb.getAll()) {
            if (card.getCardType() == CardType.MINION) {
                assertNotNull("minion", card.getMinion());
            }
        }
    }

    @Test
    public void testKnownCardClasses() {
        Set<Keyword> allowedClasses = new HashSet<>(Arrays.asList(
                Keywords.CLASS_DRUID,
                Keywords.CLASS_HUNTER,
                Keywords.CLASS_MAGE,
                Keywords.CLASS_NEUTRAL,
                Keywords.CLASS_PALADIN,
                Keywords.CLASS_PRIEST,
                Keywords.CLASS_ROUGE,
                Keywords.CLASS_SHAMAN,
                Keywords.CLASS_WARLOCK,
                Keywords.CLASS_WARRIOR,
                Keywords.CLASS_BOSS));
        HearthStoneEntityDatabase<CardDescr> cardDb = TestDb.getTestDb().getCardDb();
        for (CardDescr card: cardDb.getAll()) {
            if (!allowedClasses.contains(card.getCardClass())) {
                fail("Card " + card.getId() + " has an unexpected card class: " + card.getCardClass());
            }
            if (card.getCardType() == CardType.MINION) {
                assertNotNull("minion", card.getMinion());
            }
        }
    }
}
