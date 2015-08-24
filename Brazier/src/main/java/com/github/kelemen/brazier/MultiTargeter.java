package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.minions.Minion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.utils.ExceptionHelper;

public final class MultiTargeter {
    public static final class Builder {
        private boolean enemy;
        private boolean self;
        private boolean minions;
        private boolean heroes;
        private boolean atomic;
        private Predicate<? super TargetableCharacter> customFilter;

        public Builder() {
            this.enemy = false;
            this.self = false;
            this.minions = false;
            this.heroes = false;
            this.atomic = false;
            this.customFilter = (target) -> true;
        }

        public void setCustomFilter(Predicate<? super TargetableCharacter> customFilter) {
            ExceptionHelper.checkNotNullArgument(customFilter, "customFilter");
            this.customFilter = customFilter;
        }

        public void setEnemy(boolean enemy) {
            this.enemy = enemy;
        }

        public void setSelf(boolean self) {
            this.self = self;
        }

        public void setMinions(boolean minions) {
            this.minions = minions;
        }

        public void setHeroes(boolean heroes) {
            this.heroes = heroes;
        }

        public void setAtomic(boolean atomic) {
            this.atomic = atomic;
        }

        public MultiTargeter create() {
            return new MultiTargeter(this);
        }
    }

    private final boolean enemy;
    private final boolean self;
    private final boolean minions;
    private final boolean heroes;
    private final boolean atomic;
    private final Predicate<? super TargetableCharacter> customFilter;

    private MultiTargeter(Builder builder) {
        this.enemy = builder.enemy;
        this.self = builder.self;
        this.minions = builder.minions;
        this.heroes = builder.heroes;
        this.atomic = builder.atomic;
        this.customFilter = builder.customFilter;
    }

    public UndoAction damageTargets(
            Player player,
            DamageSource damageSource,
            int damage) {

        UndoableResult<Damage> damageRef = damageSource.createDamage(damage);
        Damage appliedDamage = damageRef.getResult();
        UndoAction applyDamageUndo = forTargets(player, (target) -> target.damage(appliedDamage));
        return () -> {
            applyDamageUndo.undo();
            damageRef.undo();
        };
    }

    public UndoAction forTargets(
            Player player,
            Function<TargetableCharacter, UndoAction> applier) {

        if (atomic) {
            return player.getWorld().getEvents().doAtomic(() -> forTargetsNonAtomic(player, applier));
        }
        else {
            return forTargetsNonAtomic(player, applier);
        }
    }

    private UndoAction forTargetsNonAtomic(
            Player player,
            Function<TargetableCharacter, UndoAction> applier) {
        ExceptionHelper.checkNotNullArgument(player, "player");
        ExceptionHelper.checkNotNullArgument(applier, "applier");

        Player opponent = player.getWorld().getOpponent(player.getPlayerId());

        UndoBuilder result = new UndoBuilder();
        if (heroes) {
            if (enemy && customFilter.test(opponent.getHero())) {
                result.addUndo(applier.apply(opponent.getHero()));
            }
            if (self && customFilter.test(player.getHero())) {
                result.addUndo(applier.apply(player.getHero()));
            }
        }

        if (minions) {
            List<Minion> attackedMinions = new ArrayList<>();
            if (enemy) {
                opponent.getBoard().collectMinions(attackedMinions, customFilter);
            }
            if (self) {
                player.getBoard().collectMinions(attackedMinions, customFilter);
            }

            BornEntity.sortEntities(attackedMinions);
            for (Minion minion: attackedMinions) {
                result.addUndo(applier.apply(minion));
            }
        }

        return result;
    }
}
