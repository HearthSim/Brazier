package com.github.kelemen.brazier;

import com.github.kelemen.brazier.cards.CardDescr;
import java.util.List;

public interface UserAgent {
    public CardDescr selectCard(boolean allowCancel, List<? extends CardDescr> cards);
}
