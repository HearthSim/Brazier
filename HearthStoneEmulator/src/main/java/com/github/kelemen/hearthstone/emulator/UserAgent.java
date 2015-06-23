package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import java.util.List;

public interface UserAgent {
    public CardDescr selectCard(boolean allowCancel, List<? extends CardDescr> cards);
}
