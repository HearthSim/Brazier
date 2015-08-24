package com.github.kelemen.brazier.cards;

import com.github.kelemen.brazier.HearthStoneEntity;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.actions.BattleCryAction;
import com.github.kelemen.brazier.actions.ManaCostAdjuster;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.actions.TargetlessAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.minions.MinionDescr;
import com.github.kelemen.brazier.weapons.WeaponDescr;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class CardDescr implements HearthStoneEntity {
    public static final CardDescr DO_NOTHING = new Builder(new CardId(""), CardType.UNKNOWN, 0).create();

    public static final class Builder {
        private final CardId cardId;
        private final CardType cardType;
        private final int manaCost;
        private int overload;
        private String displayName;
        private Keyword cardClass;
        private String description;
        private CardRarity rarity;

        private final List<TargetlessAction<? super Card>> onDrawActions;
        private final List<CardPlayActionDef> onPlayActions;
        private final List<ManaCostAdjuster> manaCostAdjusters;
        private final List<CardProvider> chooseOneActions;
        private ActivatableAbility<? super Card> inHandAbility;

        private final Set<Keyword> keywords;

        private MinionDescr minion;
        private WeaponDescr weapon;

        public Builder(CardId cardId, CardType cardType, int manaCost) {
            ExceptionHelper.checkNotNullArgument(cardId, "cardId");
            ExceptionHelper.checkNotNullArgument(cardType, "cardType");

            this.manaCost = manaCost;
            this.cardId = cardId;
            this.cardType = cardType;
            this.displayName = cardId.getName();
            this.description = "";
            this.cardClass = Keywords.CLASS_NEUTRAL;
            this.onDrawActions = new LinkedList<>();
            this.onPlayActions = new LinkedList<>();
            this.manaCostAdjusters = new LinkedList<>();
            this.chooseOneActions = new LinkedList<>();
            this.keywords = new HashSet<>();
            this.rarity = CardRarity.COMMON;
            this.minion = null;
            this.weapon = null;
            this.overload = 0;
            this.inHandAbility = null;
        }

        public void addChooseOneAction(CardProvider cardRef) {
            ExceptionHelper.checkNotNullArgument(cardRef, "cardRef");
            chooseOneActions.add(cardRef);
        }

        public void setOverload(int overload) {
            ExceptionHelper.checkArgumentInRange(overload, 0, Integer.MAX_VALUE, "overload");
            this.overload = overload;
        }

        public void setDisplayName(String displayName) {
            ExceptionHelper.checkNotNullArgument(displayName, "displayName");
            this.displayName = displayName;
        }

        public void setCardClass(Keyword cardClass) {
            ExceptionHelper.checkNotNullArgument(cardClass, "cardClass");
            this.cardClass = cardClass;
        }

        public void setRarity(CardRarity rarity) {
            ExceptionHelper.checkNotNullArgument(rarity, "rarity");
            this.rarity = rarity;
        }

        public void addKeyword(Keyword keyword) {
            ExceptionHelper.checkNotNullArgument(keyword, "keyword");
            keywords.add(keyword);
        }

        public void setDescription(String description) {
            ExceptionHelper.checkNotNullArgument(description, "description");
            this.description = description;
        }

        public void addOnDrawAction(TargetlessAction<? super Card> onDrawAction) {
            ExceptionHelper.checkNotNullArgument(onDrawAction, "onDrawAction");
            this.onDrawActions.add(onDrawAction);
        }

        public void addOnPlayAction(CardPlayActionDef onPlayAction) {
            ExceptionHelper.checkNotNullArgument(onPlayAction, "onPlayAction");
            this.onPlayActions.add(onPlayAction);
        }

        public void addManaCostAdjuster(ManaCostAdjuster manaCostAdjuster) {
            ExceptionHelper.checkNotNullArgument(manaCostAdjuster, "manaCostAdjuster");
            this.manaCostAdjusters.add(manaCostAdjuster);
        }

        public void setInHandAbility(ActivatableAbility<? super Card> inHandAbility) {
            this.inHandAbility = inHandAbility;
        }

        public void setMinion(MinionDescr minion) {
            this.minion = minion;
        }

        public void setWeapon(WeaponDescr weapon) {
            this.weapon = weapon;
        }

        private Set<Keyword> getCombinedKeywords() {
            if (minion == null && weapon == null) {
                return keywords;
            }

            Set<Keyword> result = new HashSet<>(keywords);
            if (minion != null) {
                result.addAll(minion.getKeywords());
            }
            if (weapon != null) {
                result.addAll(weapon.getKeywords());
            }
            return result;
        }

        public CardDescr create() {
            return new CardDescr(this);
        }
    }

    private final int manaCost;
    private final int overload;
    private final CardId cardId;
    private final String displayName;
    private final CardType cardType;
    private final String description;
    private final CardRarity rarity;
    private final Keyword cardClass;
    private final Set<Keyword> keywords;
    private final MinionDescr minion;
    private final WeaponDescr weapon;

    private final List<TargetlessAction<? super Card>> onDrawActions;
    private final List<CardPlayActionDef> onPlayActions;
    private final List<ManaCostAdjuster> manaCostAdjusters;
    private final List<CardProvider> chooseOneActionsRef;
    private final ActivatableAbility<? super Card> inHandAbility;

    private final AtomicReference<List<CardDescr>> chooseOneActions;

    private CardDescr(Builder builder) {
        this.manaCost = builder.manaCost;
        this.overload = builder.overload;
        this.cardId = builder.cardId;
        this.displayName = builder.displayName;
        this.cardType = builder.cardType;
        this.description = builder.description;
        this.rarity = builder.rarity;
        this.cardClass = builder.cardClass;
        this.minion = builder.minion;
        this.weapon = builder.weapon;
        this.keywords = readOnlyCopySet(builder.getCombinedKeywords());
        this.onDrawActions = CollectionsEx.readOnlyCopy(builder.onDrawActions);
        this.onPlayActions = CollectionsEx.readOnlyCopy(builder.onPlayActions);
        this.inHandAbility = builder.inHandAbility;
        this.manaCostAdjusters = CollectionsEx.readOnlyCopy(builder.manaCostAdjusters);
        this.chooseOneActionsRef = CollectionsEx.readOnlyCopy(builder.chooseOneActions);
        this.chooseOneActions = new AtomicReference<>(chooseOneActionsRef.isEmpty() ? Collections.emptyList() : null);

        if (this.cardType == CardType.MINION && this.minion == null) {
            throw new IllegalStateException("Must have a minion when the card tpye is MINION.");
        }
        if (this.cardType != CardType.MINION && this.minion != null) {
            throw new IllegalStateException("May not have a minion when the card tpye is not MINION.");
        }
    }

    private <T> Set<T> readOnlyCopySet(Collection<? extends T> src) {
        if (src.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(src));
    }

    public String getDisplayName() {
        return displayName;
    }

    public CardRarity getRarity() {
        return rarity;
    }

    public Keyword getCardClass() {
        return cardClass;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getOverload() {
        return overload;
    }

    @Override
    public CardId getId() {
        return cardId;
    }

    public ActivatableAbility<? super Card> tryGetInHandAbility() {
        return inHandAbility;
    }

    public ActivatableAbility<? super Card> getInHandAbility() {
        return inHandAbility != null
                ? inHandAbility
                : (card) -> UndoableUnregisterRef.UNREGISTERED_REF;
    }

    public TargetNeed getCombinedTargetNeed(Player player) {
        ExceptionHelper.checkNotNullArgument(player, "player");

        TargetNeed need = CardPlayActionDef.combineNeeds(player, onPlayActions);
        if (minion != null) {
            for (BattleCryAction battleCry: minion.getBattleCries()) {
                if (battleCry.getRequirement().meetsRequirement(player)) {
                    need = need.combine(battleCry.getTargetNeed());
                }
            }
        }
        return need;
    }

    public CardType getCardType() {
        return cardType;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return keywords;
    }

    public List<TargetlessAction<? super Card>> getOnDrawActions() {
        return onDrawActions;
    }

    public List<CardPlayActionDef> getOnPlayActions() {
        return onPlayActions;
    }

    public List<CardDescr> getChooseOneActions() {
        List<CardDescr> result = chooseOneActions.get();
        if (result == null) {
            result = new ArrayList<>(chooseOneActionsRef.size());
            for (CardProvider cardRef: chooseOneActionsRef) {
                result.add(cardRef.getCard());
            }
            result = Collections.unmodifiableList(result);
            if (!chooseOneActions.compareAndSet(null, result)) {
                result = chooseOneActions.get();
            }
        }
        return result;
    }

    public boolean doesSomethingWhenPlayed(Player player) {
        if (minion != null) {
            return true;
        }

        for (CardPlayActionDef action: onPlayActions) {
            if (action.getRequirement().meetsRequirement(player)) {
                return true;
            }
        }

        for (CardDescr optional: getChooseOneActions()) {
            if (optional.doesSomethingWhenPlayed(player)) {
                return true;
            }
        }

        return false;
    }

    public List<ManaCostAdjuster> getManaCostAdjusters() {
        return manaCostAdjusters;
    }

    public MinionDescr getMinion() {
        return minion;
    }

    public WeaponDescr getWeapon() {
        return weapon;
    }

    @Override
    public String toString() {
        return "Card: " + cardId;
    }
}
