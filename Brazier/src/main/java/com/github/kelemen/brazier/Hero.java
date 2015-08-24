package com.github.kelemen.brazier;

import com.github.kelemen.brazier.abilities.AuraAwareBoolProperty;
import com.github.kelemen.brazier.abilities.HpProperty;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.actions.WorldActionEvents;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.weapons.AttackTool;
import com.github.kelemen.brazier.weapons.Weapon;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class Hero implements TargetableCharacter {
    private final Player owner;
    private final TargetId heroId;
    private final long birthDate;
    private HeroPower heroPower;

    private final HpProperty hp;
    private int currentArmor;

    private final AuraAwareBoolProperty immune;

    private final HeroAttackTool attackTool;
    private final Set<Keyword> keywords;
    private Keyword heroClass;

    private boolean poisoned;

    public Hero(
            Player owner,
            int maxHp,
            int startingArmor,
            Keyword heroClass,
            Collection<? extends Keyword> keywords) {
        this(owner, new HpProperty(maxHp), startingArmor, heroClass, keywords);
    }

    public Hero(
            Player owner,
            HpProperty hp,
            int startingArmor,
            Keyword heroClass,
            Collection<? extends Keyword> keywords) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkNotNullArgument(hp, "hp");
        ExceptionHelper.checkArgumentInRange(startingArmor, 0, Integer.MAX_VALUE, "startingArmor");
        ExceptionHelper.checkNotNullArgument(heroClass, "heroClass");

        this.heroId = new TargetId();
        this.heroPower = new HeroPower(this, CardDescr.DO_NOTHING);
        this.owner = owner;
        this.hp = hp;
        this.currentArmor = startingArmor;
        this.attackTool = new HeroAttackTool();
        this.immune = new AuraAwareBoolProperty(false);
        this.heroClass = heroClass;
        this.keywords = copySet(keywords);
        this.poisoned = false;
        this.birthDate = owner.getWorld().getCurrentTime();

        ExceptionHelper.checkNotNullElements(this.keywords, "keywords");
    }

    private static <T> Set<T> copySet(Collection<? extends T> other) {
        int otherSize = other.size();
        if (otherSize == 0) {
            return Collections.emptySet();
        }
        if (otherSize == 0) {
            return Collections.singleton(other.iterator().next());
        }

        Set<T> result = new HashSet<>(other);
        return Collections.unmodifiableSet(result);
    }

    public UndoAction refresh() {
        UndoAction attackRefreshUndo = attackTool.refresh();
        UndoAction heroPowerRefreshUndo = heroPower.refresh();

        return () -> {
            heroPowerRefreshUndo.undo();
            attackRefreshUndo.undo();
        };
    }

    public UndoAction applyAuras() {
        return hp.applyAura();
    }

    @Override
    public long getBirthDate() {
        return birthDate;
    }

    public UndoAction refreshEndOfTurn() {
        return attackTool.refreshEndOfTurn();
    }

    @Override
    public UndoAction poison() {
        if (poisoned) {
            return UndoAction.DO_NOTHING;
        }

        poisoned = true;
        return () -> poisoned = false;
    }

    @Override
    public UndoableResult<Damage> createDamage(int damage) {
        return new UndoableResult<>(new Damage(this, damage));
    }

    public Keyword getHeroClass() {
        return heroClass;
    }

    public UndoAction setHeroClass(Keyword newClass) {
        ExceptionHelper.checkNotNullArgument(newClass, "newClass");

        Keyword prevClass = heroClass;
        heroClass = newClass;
        return () -> heroClass = prevClass;
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public boolean isDead() {
        return poisoned || hp.isDead();
    }

    @Override
    public boolean isDamaged() {
        return hp.isDamaged();
    }

    @Override
    public boolean isTargetable(TargeterDef targeterDef) {
        if (immune.getValue()) {
            // Currently there are no useful things to target an immune hero.
            return false;
        }
        if (targeterDef.isDirectAttack() && targeterDef.hasSameOwner(this)) {
            return false;
        }

        return !targeterDef.isDirectAttack() || !getOwner().getBoard().hasNonStealthTaunt();
    }

    public AuraAwareBoolProperty getImmuneProperty() {
        return immune;
    }

    public int getExtraAttackForThisTurn() {
        return attackTool.extraAttack;
    }

    public UndoableUnregisterRef addExtraAttackForThisTurn(int amount) {
        return attackTool.addAttack(amount);
    }

    @Override
    public AttackTool getAttackTool() {
        return attackTool;
    }

    @Override
    public TargetId getTargetId() {
        return heroId;
    }

    public HeroPower getHeroPower() {
        return heroPower;
    }

    public UndoAction setHeroPower(CardDescr power) {
        ExceptionHelper.checkNotNullArgument(power, "power");

        HeroPower prevHeroPower = this.heroPower;
        this.heroPower = new HeroPower(this, power);

        return () -> this.heroPower = prevHeroPower;
    }

    public HpProperty getHp() {
        return hp;
    }

    public int getMaxHp() {
        return hp.getMaxHp();
    }

    public UndoAction setMaxHp(int maxHp) {
        return hp.setMaxHp(maxHp);
    }

    public int getCurrentHp() {
        return hp.getCurrentHp();
    }

    public UndoAction setCurrentHp(int currentHp) {
        return hp.setCurrentHp(currentHp);
    }

    public int getCurrentArmor() {
        return currentArmor;
    }

    public UndoAction setCurrentArmor(int currentArmor) {
        ExceptionHelper.checkArgumentInRange(currentArmor, 0, Integer.MAX_VALUE, "currentArmor");
        int prevValue = this.currentArmor;
        this.currentArmor = currentArmor;
        return () -> this.currentArmor = prevValue;
    }

    public UndoAction armorUp(int armor) {
        if (armor > 0) {
            UndoAction armorUndo = setCurrentArmor(getCurrentArmor() + armor);
            WorldActionEvents<ArmorGainedEvent> listeners = getWorld().getEvents().armorGainedListeners();
            UndoAction eventUndo = listeners.triggerEvent(new ArmorGainedEvent(this, armor));
            return () -> {
                eventUndo.undo();
                armorUndo.undo();
            };
        }
        else {
            return setCurrentArmor(Math.max(0, getCurrentArmor() + armor));
        }
    }

    public static UndoableIntResult doPreparedDamage(
            Damage damage,
            TargetableCharacter target,
            Function<Damage, UndoableIntResult> damageMethod) {
        ExceptionHelper.checkNotNullArgument(damage, "damage");
        ExceptionHelper.checkNotNullArgument(target, "target");
        ExceptionHelper.checkNotNullArgument(damageMethod, "damageMethod");

        WorldActionEvents<DamageRequest> listeners = target.getWorld().getEvents().prepareDamageListeners();

        DamageRequest request = new DamageRequest(damage, target);
        UndoAction eventUndo = listeners.triggerEvent(false, request);

        UndoableIntResult applyDamageRef = damageMethod.apply(request.getDamage());

        return new UndoableIntResult(applyDamageRef.getResult(), () -> {
            applyDamageRef.undo();
            eventUndo.undo();
        });
    }

    @Override
    public boolean isLethalDamage(int damage) {
        if (immune.getValue()) {
            return false;
        }
        return damage > getCurrentArmor() + getCurrentHp();
    }

    @Override
    public UndoableIntResult damage(Damage damage) {
        return doPreparedDamage(damage, this, this::applyDamage);
    }

    private UndoableIntResult applyDamage(Damage damage) {
        int attack = damage.getAttack();

        if (attack == 0) {
            return UndoableIntResult.ZERO;
        }
        if (attack > 0 && immune.getValue()) {
            return UndoableIntResult.ZERO;
        }

        final int originalArmor = getCurrentArmor();
        final int originalHp = getCurrentHp();

        int newHp;
        int newArmor;

        if (attack < 0) {
            // Healing path

            newArmor = originalArmor;
            newHp = Math.min(getMaxHp(), originalHp - attack);
        }
        else {
            // Damage path

            if (originalArmor >= attack) {
                newHp = originalHp;
                newArmor = originalArmor - attack;
            }
            else {
                newArmor = 0;

                int remainingDamage = attack - originalArmor;
                newHp = originalHp - remainingDamage;
            }
        }

        UndoAction adjustArmorUndo = setCurrentArmor(newArmor);
        UndoAction adjustHpUndo = setCurrentHp(newHp);

        WorldEvents events = getOwner().getWorld().getEvents();
        UndoAction eventUndo;

        DamageEvent event = new DamageEvent(damage.getSource(), this, originalHp - newHp);
        if (newHp > originalHp) {
            eventUndo = events.heroHealedListeners().triggerEvent(event);
        }
        else if (newHp < originalHp) {
            eventUndo = events.heroDamagedListeners().triggerEvent(event);
        }
        else {
            eventUndo = UndoAction.DO_NOTHING;
        }

        return new UndoableIntResult(event.getDamageDealt(), () -> {
            eventUndo.undo();
            adjustHpUndo.undo();
            adjustArmorUndo.undo();
        });
    }

    @Override
    public Set<Keyword> getKeywords() {
        return keywords;
    }

    private final class HeroAttackTool implements AttackTool {
        private int attackCount;
        private int extraAttack;
        private final FreezeManager freezeManager;

        public HeroAttackTool() {
            this.freezeManager = new FreezeManager();
            this.attackCount = 0;
            this.extraAttack = 0;
        }

        private Weapon tryGetWeapon() {
            return getOwner().tryGetWeapon();
        }

        @Override
        public int getAttack() {
            return getOwner().getWeaponAttack() + extraAttack;
        }

        public UndoableUnregisterRef addAttack(int attackAddition) {
            extraAttack += attackAddition;
            return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
                @Override
                public UndoAction unregister() {
                    extraAttack -= attackAddition;
                    return () -> extraAttack += attackAddition;
                }

                @Override
                public void undo() {
                    extraAttack -= attackAddition;
                }
            });
        }

        private int getMaxAttackCount() {
            Weapon currentWeapon = tryGetWeapon();
            return currentWeapon != null
                    ? currentWeapon.getBaseDescr().getMaxAttackCount()
                    : 1;
        }

        @Override
        public boolean canAttackWith() {
            return !freezeManager.isFrozen()
                    && getAttack() > 0
                    && attackCount < getMaxAttackCount();
        }

        @Override
        public boolean canRetaliateWith() {
            Weapon weapon = tryGetWeapon();
            return weapon != null && weapon.canRetaliateWith();
        }

        @Override
        public boolean canTargetRetaliate() {
            Weapon weapon = tryGetWeapon();
            return weapon == null || weapon.canTargetRetaliate();
        }

        @Override
        public UndoAction incUseCount() {
            Weapon currentWeapon = tryGetWeapon();
            UndoAction decreaseChargesAction;
            if (currentWeapon != null) {
                decreaseChargesAction = currentWeapon.decreaseCharges();
            }
            else {
                decreaseChargesAction = UndoAction.DO_NOTHING;
            }

            attackCount++;
            return () -> {
                attackCount--;
                decreaseChargesAction.undo();
            };
        }

        @Override
        public UndoAction freeze() {
            return freezeManager.freeze();
        }

        @Override
        public boolean isFrozen() {
            return freezeManager.isFrozen();
        }

        private UndoAction refresh() {
            int prevAttackCount = attackCount;
            int prevExtraAttack = extraAttack;

            attackCount = 0;
            extraAttack = 0;

            return () -> {
                attackCount = prevAttackCount;
                extraAttack = prevExtraAttack;
            };
        }

        private UndoAction refreshEndOfTurn() {
            return freezeManager.endTurn(attackCount);
        }

        @Override
        public boolean attacksLeft() {
            return false;
        }

        @Override
        public boolean attacksRight() {
            return false;
        }
    }
}
