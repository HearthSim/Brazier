package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class MinionParserTest {
    private void assertKeywords(MinionDescr minion, Keyword... expectedKeywords) {
        Set<Keyword> expected = new HashSet<>(Arrays.asList(expectedKeywords));
        assertEquals("keywords", expected, minion.getKeywords());
    }

    private static MinionDescr getMinion(String minionName) {
        MinionDescr result = TestDb.getTestDb().getMinionDb().getById(new MinionId(minionName));
        assertEquals(minionName, result.getId().getName());
        return result;
    }

    @Test
    public void testChillwindYeti() throws Exception {
        MinionDescr minion = getMinion("Chillwind Yeti");

        assertEquals(4, minion.getAttack());
        assertEquals(5, minion.getHp());
        assertFalse("taunt", minion.isTaunt());

        assertKeywords(minion);
    }
}
