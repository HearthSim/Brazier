package com.github.kelemen.brazier.minions;

import com.github.kelemen.brazier.HearthStoneEntity;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.LivingEntitysAbilities;
import com.github.kelemen.brazier.abilities.OwnedIntPropertyBuff;
import com.github.kelemen.brazier.actions.PlayActionDef;
import com.github.kelemen.brazier.actions.PlayArg;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.cards.PlayAction;
import com.github.kelemen.brazier.events.WorldEventAction;
import com.github.kelemen.brazier.events.WorldEventActionDefs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class MinionDescr implements HearthStoneEntity {
    public static final class Builder {
        private final MinionId minionId;
        private String displayName;
        private final int attack;
        private final int hp;
        private final Supplier<? extends CardDescr> baseCardRef;

        private final Set<Keyword> keywords;
        private final List<PlayActionDef<Minion>> battleCries;
        private boolean taunt;
        private boolean charge;
        private boolean canAttack;
        private LivingEntitysAbilities<Minion> abilities;
        private boolean divineShield;
        private int maxAttackCount;
        private boolean targetable;
        private boolean stealth;
        private boolean attackLeft;
        private boolean attackRight;

        private OwnedIntPropertyBuff<? super Minion> attackFinalizer;

        public Builder(MinionId minionId, int attack, int hp, Supplier<? extends CardDescr> baseCardRef) {
            ExceptionHelper.checkNotNullArgument(minionId, "minionId");
            ExceptionHelper.checkNotNullArgument(baseCardRef, "baseCardRef");

            this.minionId = minionId;
            this.attack = attack;
            this.hp = hp;
            this.baseCardRef = baseCardRef;
            this.displayName = minionId.getName();
            this.keywords = new HashSet<>();
            this.battleCries = new LinkedList<>();
            this.taunt = false;
            this.divineShield = false;
            this.charge = false;
            this.targetable = true;
            this.stealth = false;
            this.maxAttackCount = 1;
            this.canAttack = true;
            this.abilities = LivingEntitysAbilities.noAbilities();
            this.attackLeft = false;
            this.attackRight = false;
            this.attackFinalizer = OwnedIntPropertyBuff.IDENTITY;
        }

        public void setAbilities(LivingEntitysAbilities<Minion> abilities) {
            ExceptionHelper.checkNotNullArgument(abilities, "abilities");
            this.abilities = abilities;
        }

        public void setDisplayName(String displayName) {
            ExceptionHelper.checkNotNullArgument(displayName, "displayName");
            this.displayName = displayName;
        }

        public void setCanAttack(boolean canAttack) {
            this.canAttack = canAttack;
        }

        public void setAttackFinalizer(OwnedIntPropertyBuff<? super Minion> attackFinalizer) {
            ExceptionHelper.checkNotNullArgument(attackFinalizer, "attackFinalizer");
            this.attackFinalizer = attackFinalizer;
        }

        public void setAttackLeft(boolean attackLeft) {
            this.attackLeft = attackLeft;
        }

        public void setAttackRight(boolean attackRight) {
            this.attackRight = attackRight;
        }

        public void setStealth(boolean stealth) {
            this.stealth = stealth;
        }

        public void setTargetable(boolean targetable) {
            this.targetable = targetable;
        }

        public void setMaxAttackCount(int maxAttackCount) {
            this.maxAttackCount = maxAttackCount;
        }

        public void setCharge(boolean charge) {
            this.charge = charge;
        }

        public void setDivineShield(boolean divineShield) {
            this.divineShield = divineShield;
        }

        public void setTaunt(boolean taunt) {
            this.taunt = taunt;
        }

        public void addKeyword(Keyword keyword) {
            ExceptionHelper.checkNotNullArgument(keyword, "keyword");
            keywords.add(keyword);
        }

        public void addBattleCry(PlayActionDef<Minion> battleCry) {
            ExceptionHelper.checkNotNullArgument(battleCry, "battleCry");
            battleCries.add(battleCry);
        }

        public MinionDescr create() {
            return new MinionDescr(this);
        }
    }

    private final MinionId minionId;
    private final String displayName;
    private final Supplier<CardDescr> baseCardRef;
    private final int attack;
    private final int hp;
    private final Set<Keyword> keywords;
    private final List<PlayActionDef<Minion>> battleCries;
    private final LivingEntitysAbilities<Minion> abilities;
    private final boolean taunt;
    private final boolean divineShield;
    private final boolean charge;
    private final boolean canAttack;
    private final int maxAttackCount;
    private final boolean targetable;
    private final boolean stealth;
    private final boolean attackLeft;
    private final boolean attackRight;
    private final OwnedIntPropertyBuff<? super Minion> attackFinalizer;

    private MinionDescr(Builder builder) {
        this.minionId = builder.minionId;
        this.displayName = builder.displayName;
        this.baseCardRef = new CachedSupplier<>(builder.baseCardRef);
        this.attack = builder.attack;
        this.hp = builder.hp;
        this.keywords = Collections.unmodifiableSet(new HashSet<>(builder.keywords));
        this.battleCries = CollectionsEx.readOnlyCopy(builder.battleCries);
        this.abilities = builder.abilities;
        this.taunt = builder.taunt;
        this.divineShield = builder.divineShield;
        this.charge = builder.charge;
        this.canAttack = builder.canAttack;
        this.attackFinalizer = builder.attackFinalizer;
        this.maxAttackCount = builder.maxAttackCount;
        this.targetable = builder.targetable;
        this.stealth = builder.stealth;
        this.attackLeft = builder.attackLeft;
        this.attackRight = builder.attackRight;
    }

    public ActivatableAbility<? super Minion> tryGetAbility() {
        return abilities.tryGetAbility();
    }

    public WorldEventAction<? super Minion, ? super Minion> tryGetDeathRattle() {
        return abilities.tryGetDeathRattle();
    }

    public boolean isAttackLeft() {
        return attackLeft;
    }

    public boolean isAttackRight() {
        return attackRight;
    }

    public boolean isStealth() {
        return stealth;
    }

    public boolean isTargetable() {
        return targetable;
    }

    public boolean isCanAttack() {
        return canAttack;
    }

    public String getDisplayName() {
        return displayName;
    }

    public OwnedIntPropertyBuff<? super Minion> getAttackFinalizer() {
        return attackFinalizer;
    }

    @Override
    public MinionId getId() {
        return minionId;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return keywords;
    }

    public CardDescr getBaseCard() {
        CardDescr baseCard = baseCardRef.get();
        if (baseCard == null) {
            throw new IllegalStateException("Base card is not available for minion: " + minionId);
        }
        return baseCard;
    }

    public int getAttack() {
        return attack;
    }

    public int getMaxAttackCount() {
        return maxAttackCount;
    }

    public int getHp() {
        return hp;
    }

    public boolean isTaunt() {
        return taunt;
    }

    public boolean isDivineShield() {
        return divineShield;
    }

    public boolean isCharge() {
        return charge;
    }

    public List<PlayActionDef<Minion>> getBattleCries() {
        return battleCries;
    }

    public UndoAction executeBattleCriesNow(Player player, PlayArg<Minion> target) {
        ExceptionHelper.checkNotNullArgument(player, "player");
        ExceptionHelper.checkNotNullArgument(target, "target");

        if (battleCries.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        List<PlayAction<Minion>> actions
                = new ArrayList<>(battleCries.size());
        for (PlayActionDef<Minion> action: battleCries) {
            if (action.getRequirement().meetsRequirement(player)) {
                actions.add(action.getAction());
            }
        }

        if (actions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        World world = player.getWorld();

        UndoBuilder result = new UndoBuilder(actions.size());
        for (PlayAction<Minion> action: actions) {
            result.addUndo(action.doPlay(world, target));
        }
        return result;
    }

    public WorldEventActionDefs<Minion> getEventActionDefs() {
        return abilities.getEventActionDefs();
    }

    private static final class CachedSupplier<T> implements Supplier<T> {
        private final Supplier<? extends T> src;
        private final AtomicReference<T> cache;

        public CachedSupplier(Supplier<? extends T> src) {
            this.src = src;
            this.cache = new AtomicReference<>(null);
        }

        @Override
        public T get() {
            T result = cache.get();
            if (result == null) {
                result = src.get();
                if (!cache.compareAndSet(null, result)) {
                    result = cache.get();
                }
            }
            return result;
        }
    }
}
