package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.PlayActionDef;
import com.github.kelemen.brazier.actions.PlayArg;
import com.github.kelemen.brazier.actions.PlayTarget;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.cards.CardId;
import com.github.kelemen.brazier.cards.CardType;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class HeroPower implements PlayerProperty {
    private final Hero hero;
    private final CardDescr powerDef;

    private int useCount;

    private final AtomicReference<Card> baseCardRef;

    public HeroPower(Hero hero, CardDescr powerDef) {
        ExceptionHelper.checkNotNullArgument(hero, "hero");
        ExceptionHelper.checkNotNullArgument(powerDef, "powerDef");

        this.hero = hero;
        this.powerDef = powerDef;
        this.useCount = 0;
        this.baseCardRef = new AtomicReference<>(null);
    }

    private Card getBaseCard() {
        Card card = baseCardRef.get();
        if (card == null) {
            String name = "Power:" + powerDef.getId().getName();
            CardId id = new CardId(name);
            CardDescr.Builder result = new CardDescr.Builder(id, CardType.UNKNOWN, powerDef.getManaCost());
            card = new Card(hero.getOwner(), result.create());

            if (!baseCardRef.compareAndSet(null, card)) {
                card = baseCardRef.get();
            }
        }

        return card;
    }

    @Override
    public Player getOwner() {
        return hero.getOwner();
    }

    public CardDescr getPowerDef() {
        return powerDef;
    }

    public int getManaCost() {
        return powerDef.getManaCost();
    }

    public TargetNeed getTargetNeed(Player player) {
        return PlayActionDef.combineNeeds(player, powerDef.getOnPlayActions());
    }

    public boolean isPlayable(Player player) {
        ExceptionHelper.checkNotNullArgument(player, "player");

        if (powerDef.getManaCost() > getOwner().getMana()) {
            return false;
        }
        if (useCount >= 1) {
            return false;
        }

        for (PlayActionDef<Card> action: powerDef.getOnPlayActions()) {
            if (action.getRequirement().meetsRequirement(player)) {
                return true;
            }
        }
        return false;
    }

    public UndoAction alterWorld(World world, PlayTarget target) {
        PlayArg<Card> playArg = new PlayArg<>(getBaseCard(), target);

        Player owner = getOwner();

        UndoBuilder result = new UndoBuilder();
        result.addUndo(owner.getManaResource().spendMana(powerDef.getManaCost(), 0));

        useCount++;
        result.addUndo(() -> useCount--);

        for (PlayActionDef<Card> action: powerDef.getOnPlayActions()) {
            result.addUndo(action.alterWorld(world, playArg));
        }
        return result;
    }

    public UndoAction refresh() {
        int prevUseCount = useCount;
        useCount = 0;
        return () -> useCount = prevUseCount;
    }
}
