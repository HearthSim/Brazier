package com.github.kelemen.brazier.weapons;

import com.github.kelemen.brazier.HearthStoneEntity;
import com.github.kelemen.brazier.Keyword;
import com.github.kelemen.brazier.abilities.ActivatableAbility;
import com.github.kelemen.brazier.abilities.LivingEntitiesAbilities;
import com.github.kelemen.brazier.events.WorldEventAction;
import com.github.kelemen.brazier.events.WorldEventActionDefs;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class WeaponDescr implements HearthStoneEntity {
    public static final class Builder {
        private final WeaponId id;
        private final int attack;
        private final int charges;

        private int maxAttackCount;
        private boolean canRetaliateWith;
        private boolean canTargetRetaliate;

        private final Set<Keyword> keywords;

        private LivingEntitiesAbilities<Weapon> abilities;

        public Builder(WeaponId id, int attack, int charges) {
            ExceptionHelper.checkNotNullArgument(id, "id");

            this.id = id;
            this.attack = attack;
            this.charges = charges;
            this.maxAttackCount = 1;
            this.canRetaliateWith = false;
            this.canTargetRetaliate = true;
            this.keywords = new HashSet<>();
            this.abilities = LivingEntitiesAbilities.noAbilities();
        }

        public void setAbilities(LivingEntitiesAbilities<Weapon> abilities) {
            ExceptionHelper.checkNotNullArgument(abilities, "abilities");
            this.abilities = abilities;
        }

        public void setMaxAttackCount(int maxAttackCount) {
            this.maxAttackCount = maxAttackCount;
        }

        public void addKeyword(Keyword keyword) {
            ExceptionHelper.checkNotNullArgument(keyword, "keyword");
            keywords.add(keyword);
        }

        public void setCanRetaliateWith(boolean canRetaliateWith) {
            this.canRetaliateWith = canRetaliateWith;
        }

        public void setCanTargetRetaliate(boolean canTargetRetaliate) {
            this.canTargetRetaliate = canTargetRetaliate;
        }

        public WeaponDescr create() {
            return new WeaponDescr(this);
        }
    }

    private final WeaponId id;
    private final int attack;
    private final int charges;

    private final int maxAttackCount;
    private final boolean canRetaliateWith;
    private final boolean canTargetRetaliate;

    private final Set<Keyword> keywords;

    private final LivingEntitiesAbilities<Weapon> abilities;

    private WeaponDescr(Builder builder) {
        this.id = builder.id;
        this.attack = builder.attack;
        this.charges = builder.charges;
        this.maxAttackCount = builder.maxAttackCount;
        this.canRetaliateWith = builder.canRetaliateWith;
        this.canTargetRetaliate = builder.canTargetRetaliate;
        this.keywords = readOnlyCopySet(builder.keywords);
        this.abilities = builder.abilities;
    }

    private <T> Set<T> readOnlyCopySet(Collection<? extends T> src) {
        if (src.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(src));
    }

    public String getDisplayName() {
        // TODO: Allow customizing the display name.
        return id.getName();
    }

    @Override
    public WeaponId getId() {
        return id;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return keywords;
    }

    public int getAttack() {
        return attack;
    }

    public int getMaxAttackCount() {
        return maxAttackCount;
    }

    public int getCharges() {
        return charges;
    }

    public boolean canRetaliateWith() {
        return canRetaliateWith;
    }

    public boolean canTargetRetaliate() {
        return canTargetRetaliate;
    }

    public WorldEventActionDefs<Weapon> getEventActionDefs() {
        return abilities.getEventActionDefs();
    }

    public WorldEventAction<? super Weapon, ? super Weapon> tryGetDeathRattle() {
        return abilities.tryGetDeathRattle();
    }

    public ActivatableAbility<? super Weapon> tryGetAbility() {
        return abilities.tryGetAbility();
    }
}
