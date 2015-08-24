package com.github.kelemen.brazier;

import com.github.kelemen.brazier.abilities.ActiveAura;
import com.github.kelemen.brazier.abilities.ActiveAuraContainer;
import com.github.kelemen.brazier.actions.AttackRequest;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import com.github.kelemen.brazier.actions.UndoableUnregisterRef;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.event.WorldEvents;
import com.github.kelemen.brazier.minions.Minion;
import com.github.kelemen.brazier.weapons.AttackTool;
import com.github.kelemen.brazier.weapons.Weapon;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class World {
    private static final Random RNG = new SecureRandom();

    private static final RandomProvider DEFAULT_RANDOM_PROVIDER = (int bound) -> {
        return bound > 1 ? RNG.nextInt(bound) : 0;
    };

    private RandomProvider randomProvider;
    private UserAgent userAgent;
    private final HearthStoneDb db;
    private final Player player1;
    private final Player player2;

    private final ActiveAuraContainer activeAuras;

    private final WorldEvents events;

    private final AtomicLong currentTime;

    private Player currentPlayer;

    public World(HearthStoneDb db, PlayerId player1Id, PlayerId player2Id) {
        ExceptionHelper.checkNotNullArgument(db, "db");

        this.db = db;
        this.currentTime = new AtomicLong(Long.MIN_VALUE);
        this.player1 = new Player(this, player1Id);
        this.player2 = new Player(this, player2Id);
        this.activeAuras = new ActiveAuraContainer();

        this.events = new WorldEvents(this);
        this.randomProvider = DEFAULT_RANDOM_PROVIDER;
        this.currentPlayer = player1;

        this.userAgent = (boolean allowCancel, List<? extends CardDescr> cards) -> {
            return cards.get(randomProvider.roll(cards.size()));
        };
    }

    public HearthStoneDb getDb() {
        return db;
    }

    public void setRandomProvider(RandomProvider randomProvider) {
        ExceptionHelper.checkNotNullArgument(randomProvider, "randomProvider");
        // We wrap the random provider to avoid generating a random number
        // when there is only one possiblity. This helps test code and simplifies
        // AI.
        this.randomProvider = (bound) -> {
            return bound > 1 ? randomProvider.roll(bound) : 0;
        };
    }

    public RandomProvider getRandomProvider() {
        return randomProvider;
    }

    public long getCurrentTime() {
        return currentTime.getAndIncrement();
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public UndoAction setCurrentPlayerId(PlayerId newPlayerId) {
        ExceptionHelper.checkNotNullArgument(newPlayerId, "newPlayerId");

        Player prevPlayer = currentPlayer;
        currentPlayer = getPlayer(newPlayerId);
        return () -> currentPlayer = prevPlayer;
    }

    public UndoAction endTurn() {
        UndoBuilder result = new UndoBuilder();

        result.addUndo(currentPlayer.endTurn());

        Player nextPlayer = getOpponent(currentPlayer.getPlayerId());
        Player origCurrentPlayer = currentPlayer;
        currentPlayer = nextPlayer;
        result.addUndo(() -> currentPlayer = origCurrentPlayer);

        result.addUndo(nextPlayer.startNewTurn());

        return result;
    }

    public TargetableCharacter findTarget(TargetId target) {
        if (target == null) {
            return null;
        }

        Hero hero1 = player1.getHero();
        if (target.equals(hero1.getTargetId())) {
            return hero1;
        }

        Hero hero2 = player2.getHero();
        if (target.equals(hero2.getTargetId())) {
            return hero2;
        }

        Minion result = player1.getBoard().findMinion(target);
        if (result != null) {
            return result;
        }

        return player2.getBoard().findMinion(target);
    }

    private boolean isTargetExist(TargetableCharacter character) {
        return findTarget(character.getTargetId()) != null;
    }

    public DeathResolutionResult attack(TargetId attackerId, TargetId defenderId) {
        ExceptionHelper.checkNotNullArgument(attackerId, "attackerId");
        ExceptionHelper.checkNotNullArgument(defenderId, "defenderId");

        TargetableCharacter attacker = findTarget(attackerId);
        if (attacker == null) {
            return DeathResolutionResult.NO_DEATHS;
        }

        TargetableCharacter defender = findTarget(defenderId);
        if (defender == null) {
            return DeathResolutionResult.NO_DEATHS;
        }

        UndoBuilder resultUndo = new UndoBuilder();

        AttackRequest attackRequest = new AttackRequest(attacker, defender);
        resultUndo.addUndo(events.attackListeners().triggerEvent(false, attackRequest));

        DeathResolutionResult deathResolution = resolveDeaths();
        resultUndo.addUndo(deathResolution.getUndoAction());
        if (deathResolution.isGameOver()) {
            return new DeathResolutionResult(true, deathResolution.getDeadPlayers(), resultUndo);
        }

        TargetableCharacter newDefender = attackRequest.getDefender();
        if (newDefender == null) {
            return new DeathResolutionResult(true, resultUndo);
        }

        if (!isTargetExist(attacker) || !isTargetExist(newDefender)) {
            return new DeathResolutionResult(true, resultUndo);
        }

        // We request all this info prior attacking, because we do not want
        // damage triggers to alter these values.
        AttackTool attackTool = attacker.getAttackTool();
        int attack = attackTool.getAttack();
        boolean swipLeft = attackTool.attacksLeft();
        boolean swipeRight = attackTool.attacksRight();

        UndoAction attackUndo = events.doAtomic(() -> resolveAttackNonAtomic(attacker, newDefender));
        resultUndo.addUndo(attackUndo);

        if (swipLeft || swipeRight) {
            resultUndo.addUndo(doSwipeAttack(attack, swipLeft, swipeRight, attacker, newDefender));
        }

        return new DeathResolutionResult(true, resultUndo);
    }

    private static UndoAction doSwipeAttack(
            int attack,
            boolean swipeLeft,
            boolean swipeRight,
            TargetableCharacter attacker,
            TargetableCharacter mainTarget) {
        if (!(mainTarget instanceof Minion)) {
            return UndoAction.DO_NOTHING;
        }
        Minion minionTarget = (Minion)mainTarget;
        SummonLocationRef locationRef = minionTarget.getLocationRef();

        UndoBuilder result = new UndoBuilder();
        UndoableResult<Damage> damageRef = attacker.createDamage(attack);
        result.addUndo(damageRef.getUndoAction());

        if (swipeLeft) {
            BoardLocationRef left = locationRef.tryGetLeft();
            if (left != null) {
                result.addUndo(left.getMinion().damage(damageRef.getResult()));
            }
        }
        if (swipeRight) {
            BoardLocationRef right = locationRef.tryGetRight();
            if (right != null) {
                result.addUndo(right.getMinion().damage(damageRef.getResult()));
            }
        }
        return result;
    }

    private static UndoAction dealDamage(AttackTool weapon, TargetableCharacter attacker, TargetableCharacter target) {
        UndoableResult<Damage> damageRef = attacker.createDamage(weapon.getAttack());
        UndoAction damageUndo = target.damage(damageRef.getResult());
        UndoAction swingDecUndo = weapon.incUseCount();
        return () -> {
            swingDecUndo.undo();
            damageUndo.undo();
            damageRef.undo();
        };
    }

    private UndoAction resolveAttackNonAtomic(TargetableCharacter attacker, TargetableCharacter defender) {
        AttackTool attackerWeapon = attacker.getAttackTool();
        if (!attackerWeapon.canAttackWith()) {
            throw new IllegalArgumentException("Attacker is not allowed to attack with its weapon.");
        }

        UndoAction attackUndo = dealDamage(attackerWeapon, attacker, defender);

        AttackTool defenderWeapon = defender.getAttackTool();
        UndoAction defendUndo;
        if (attackerWeapon.canTargetRetaliate()
                && defenderWeapon.canRetaliateWith()) {
            defendUndo = dealDamage(defenderWeapon, defender, attacker);
        }
        else {
            defendUndo = UndoAction.DO_NOTHING;
        }

        return () -> {
            defendUndo.undo();
            attackUndo.undo();
        };
    }

    private UndoableResult<List<Weapon>> removeDeadWeapons() {
        UndoableResult<List<Weapon>> deadWeaponsResult1 = player1.removeDeadWeapons();
        UndoableResult<List<Weapon>> deadWeaponsResult2 = player2.removeDeadWeapons();

        List<Weapon> deadWeapons1 = deadWeaponsResult1.getResult();
        List<Weapon> deadWeapons2 = deadWeaponsResult2.getResult();
        if (deadWeapons1.isEmpty() && deadWeapons2.isEmpty()) {
            return new UndoableResult<>(Collections.emptyList(), UndoAction.DO_NOTHING);
        }

        return new UndoableResult<>(CollectionsEx.viewConcatList(deadWeapons1, deadWeapons2), () -> {
            deadWeaponsResult2.undo();
            deadWeaponsResult1.undo();
        });
    }

    public UndoableUnregisterRef addAura(ActiveAura aura) {
        return activeAuras.addAura(aura);
    }

    private UndoAction updateAllAuras() {
        UndoBuilder result = new UndoBuilder();
        result.addUndo(activeAuras.updateAllAura(this));
        result.addUndo(player1.applyAuras());
        result.addUndo(player2.applyAuras());
        return result;
    }

    public DeathResolutionResult endPhase() {
        DeathResolutionResult deathResults = resolveDeaths();
        if (!deathResults.deathOccurred()) {
            return deathResults;
        }

        UndoBuilder undos = new UndoBuilder();
        undos.addUndo(deathResults.getUndoAction());

        do {
            deathResults = resolveDeaths();
            undos.addUndo(deathResults.getUndoAction());
        } while (deathResults.deathOccurred() && !deathResults.isGameOver());

        return new DeathResolutionResult(true, deathResults.getDeadPlayers(), undos);
    }

    private DeathResolutionResult resolveDeaths() {
        UndoAction auraUndo = updateAllAuras();
        DeathResolutionResult result = resolveDeathsWithoutAura();
        if (!result.deathOccurred()) {
            return new DeathResolutionResult(false, result.getDeadPlayers(), auraUndo);
        }
        else {
            return new DeathResolutionResult(true, result.getDeadPlayers(), () -> {
                result.undo();
                auraUndo.undo();
            });
        }
    }

    private DeathResolutionResult resolveDeathsWithoutAura() {
        List<PlayerId> deadPlayers = new ArrayList<>(2);
        if (player1.getHero().isDead()) {
            deadPlayers.add(player1.getPlayerId());
        }
        if (player2.getHero().isDead()) {
            deadPlayers.add(player2.getPlayerId());
        }

        if (!deadPlayers.isEmpty()) {
            // We could finish the death-rattles but why would we?
            return new DeathResolutionResult(true, deadPlayers, UndoAction.DO_NOTHING);
        }

        List<Minion> deadMinions = new ArrayList<>();
        player1.getBoard().collectMinions(deadMinions, Minion::isDead);
        player2.getBoard().collectMinions(deadMinions, Minion::isDead);

        UndoableResult<List<Weapon>> deadWeaponsResult = removeDeadWeapons();
        List<Weapon> deadWeapons = deadWeaponsResult.getResult();

        if (deadWeapons.isEmpty() && deadMinions.isEmpty()) {
            return DeathResolutionResult.NO_DEATHS;
        }

        UndoBuilder result = new UndoBuilder();
        result.addUndo(deadWeaponsResult);

        List<DestroyableEntity> deadEntities = new ArrayList<>(deadWeapons.size() + deadMinions.size());
        for (Minion minion: deadMinions) {
            Graveyard graveyard = minion.getOwner().getBoard().getGraveyard();
            result.addUndo(graveyard.addDeadMinion(minion));

            deadEntities.add(minion);
        }

        for (Weapon weapon: deadWeapons) {
            deadEntities.add(weapon);
        }

        BornEntity.sortEntities(deadEntities);

        for (DestroyableEntity dead: deadEntities) {
            result.addUndo(dead.scheduleToDestroy());
        }
        for (DestroyableEntity dead: deadEntities) {
            result.addUndo(dead.destroy());
        }

        return new DeathResolutionResult(true, deadPlayers, result);
    }

    public WorldEvents getEvents() {
        return events;
    }

    public void setUserAgent(UserAgent userAgent) {
        ExceptionHelper.checkNotNullArgument(userAgent, "userAgent");
        this.userAgent = userAgent;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public Player getOpponent(PlayerId playerId) {
        Player result = tryGetOpponent(playerId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown player: " + playerId.getName());
        }
        return result;
    }

    public Player tryGetOpponent(PlayerId playerId) {
        if (playerId.equals(player1.getPlayerId())) {
            return player2;
        }
        if (playerId.equals(player2.getPlayerId())) {
            return player1;
        }
        return null;
    }

    public Player getPlayer(PlayerId playerId) {
        Player result = tryGetPlayer(playerId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown player: " + playerId.getName());
        }
        return result;
    }

    public Player tryGetPlayer(PlayerId playerId) {
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        if (player1.getPlayerId().equals(playerId)) {
            return player1;
        }
        if (player2.getPlayerId().equals(playerId)) {
            return player2;
        }
        return null;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }
}
