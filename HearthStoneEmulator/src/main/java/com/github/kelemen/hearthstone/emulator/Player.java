package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.abilities.AuraAwareIntProperty;
import com.github.kelemen.hearthstone.emulator.actions.ActionUtils;
import com.github.kelemen.hearthstone.emulator.actions.CardPlayArg;
import com.github.kelemen.hearthstone.emulator.actions.PlayTarget;
import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.WorldActionList;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardPlayActionDef;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class Player implements PlayerProperty {
    public static final int MAX_MANA = 10;
    public static final int MAX_HAND_SIZE = 10;
    public static final int MAX_BOARD_SIZE = 7;

    private final World world;
    private final PlayerId playerId;
    private Hero hero;
    private final BoardSide board;
    private final SecretContainer secrets;
    private final Hand hand;

    private final AuraAwareIntProperty deathRattleTriggerCount;
    private final AuraAwareIntProperty spellPower;

    private final ManaResource manaResource;

    private int fatique;

    private int cardsPlayedThisTurn;
    private int minionsPlayedThisTurn;

    private final FlagContainer auraFlags;

    private Weapon weapon;
    private final RefList<Weapon> deadWeapons;

    public Player(World world, PlayerId playerId) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        this.world = world;
        this.playerId = playerId;
        this.hero = new Hero(this, 30, 0, Keywords.CLASS_BOSS, Collections.emptySet());
        this.board = new BoardSide(this, MAX_BOARD_SIZE);
        this.hand = new Hand(this, MAX_HAND_SIZE);
        this.manaResource = new ManaResource();
        this.fatique = 0;
        this.spellPower = new AuraAwareIntProperty(0);
        this.cardsPlayedThisTurn = 0;
        this.minionsPlayedThisTurn = 0;
        this.secrets = new SecretContainer(this);
        this.deathRattleTriggerCount = new AuraAwareIntProperty(1);
        this.auraFlags = new FlagContainer();
        this.weapon = null;
        this.deadWeapons = new RefLinkedList<>();
    }

    public FlagContainer getAuraFlags() {
        return auraFlags;
    }

    public UndoAction startNewTurn() {
        UndoBuilder result = new UndoBuilder();

        int origCardsPlayedThisTurn = cardsPlayedThisTurn;
        cardsPlayedThisTurn = 0;
        result.addUndo(() -> cardsPlayedThisTurn = origCardsPlayedThisTurn);

        int origMinionsPlayedThisTurn = minionsPlayedThisTurn;
        minionsPlayedThisTurn = 0;
        result.addUndo(() -> minionsPlayedThisTurn = origMinionsPlayedThisTurn);

        result.addUndo(manaResource.refresh());
        result.addUndo(drawCardToHand());
        result.addUndo(board.refresh());
        result.addUndo(hero.refresh());

        WorldEvents events = getWorld().getEvents();
        result.addUndo(events.turnStartsListeners().triggerEvent(this));

        return result;
    }

    public UndoAction endTurn() {
        WorldEvents events = getWorld().getEvents();
        UndoAction eventUndo = events.turnEndsListeners().triggerEvent(this);

        UndoAction refreshHeroUndo = hero.refreshEndOfTurn();
        UndoAction boardRefreshUndo = board.refreshEndOfTurn();
        return () -> {
            boardRefreshUndo.undo();
            refreshHeroUndo.undo();
            eventUndo.undo();
        };
    }

    public Player getOpponent() {
        return world.getOpponent(playerId);
    }

    public SecretContainer getSecrets() {
        return secrets;
    }

    @Override
    public Player getOwner() {
        return this;
    }

    @Override
    public World getWorld() {
        return world;
    }

    public int getCardsPlayedThisTurn() {
        return cardsPlayedThisTurn;
    }

    public int getMinionsPlayedThisTurn() {
        return minionsPlayedThisTurn;
    }

    private void getOnPlayActions(CardPlayArg arg, CardDescr cardDescr, List<CardPlayActionDef> result) {
        for (CardPlayActionDef action: cardDescr.getOnPlayActions()) {
            if (action.getRequirement().meetsRequirement(this)) {
                result.add(action);
            }
        }
    }

    private List<CardPlayActionDef> getOnPlayActions(CardPlayArg arg, CardDescr chooseOneChoice) {
        CardDescr cardDescr = arg.getCard().getCardDescr();

        int playActionCount = cardDescr.getOnPlayActions().size()
                + (chooseOneChoice != null ? chooseOneChoice.getOnPlayActions().size() : 0);

        List<CardPlayActionDef> result = new ArrayList<>(playActionCount);
        getOnPlayActions(arg, arg.getCard().getCardDescr(), result);
        if (chooseOneChoice != null) {
            getOnPlayActions(arg, chooseOneChoice, result);
        }

        return result;
    }

    private UndoAction executeCardPlayActions(CardPlayArg arg, List<? extends CardPlayActionDef> actions) {
        if (actions.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder(actions.size());
        for (CardPlayActionDef actionDef: actions) {
            result.addUndo(actionDef.alterWorld(world, arg));
        }
        return result;
    }

    public UndoAction playCardEffect(Card card) {
        return playCard(card, 0, new PlayTargetRequest(playerId), false);
    }

    public UndoAction playCardEffect(Card card, PlayTargetRequest targetRequest) {
        return playCard(card, 0, targetRequest, false);
    }

    public UndoAction playCard(Card card, int manaCost, PlayTargetRequest targetRequest) {
        return playCard(card, manaCost, targetRequest, true);
    }

    private UndoAction playCard(Card card, int manaCost, PlayTargetRequest targetRequest, boolean playCardEvents) {
        ExceptionHelper.checkNotNullArgument(card, "card");
        ExceptionHelper.checkNotNullArgument(targetRequest, "target");

        Player castingPlayer = world.getPlayer(targetRequest.getCastingPlayerId());
        PlayTarget originalTarget = new PlayTarget(castingPlayer, world.findTarget(targetRequest.getTargetId()));
        CardPlayArg originalCardPlayArg = new CardPlayArg(card, originalTarget);

        // We request the on play actions before doing anything because
        // the requirements for playing a card action may no longer met
        // after firing events. However, in this case we must complete the
        // action under these circumstances (when its requirement is not met).
        List<CardPlayActionDef> onPlayActions = getOnPlayActions(originalCardPlayArg, targetRequest.getChoseOneChoice());

        UndoBuilder result = new UndoBuilder();
        result.addUndo(manaResource.spendMana(manaCost, card.getCardDescr().getOverload()));

        cardsPlayedThisTurn++;
        result.addUndo(() -> cardsPlayedThisTurn--);

        WorldEvents events = world.getEvents();

        CardPlayEvent playEvent = new CardPlayEvent(originalCardPlayArg, manaCost);

        Minion minion = card.getMinion();
        if (minion != null) {
            minionsPlayedThisTurn++;
            result.addUndo(() -> minionsPlayedThisTurn--);

            int minionLocation = targetRequest.getMinionLocation();

            UndoableResult<BoardReservationRef> reservationRefResult
                    = board.tryReservePosition(minion, minionLocation);
            // reservationRefResult shouldn't be null if we were allowed to play this card.
            if (reservationRefResult != null) {
                result.addUndo(reservationRefResult::undo);
            }

            if (playCardEvents) {
                result.addUndo(events.startPlayingCardListeners().triggerEvent(false, playEvent));
            }

            if (!playEvent.isVetodPlay() && reservationRefResult != null) {
                BoardReservationRef reservationRef = reservationRefResult.getResult();

                CardPlayArg cardPlayArg = playEvent.getCardPlayArg();
                result.addUndo(board.summonMinion(reservationRef, cardPlayArg.getTarget()));
                result.addUndo(executeCardPlayActions(cardPlayArg, onPlayActions));
            }
        }
        else {
            result.addUndo(events.startPlayingCardListeners().triggerEvent(false, playEvent));
            if (!playEvent.isVetodPlay()) {
                CardPlayArg cardPlayArg = playEvent.getCardPlayArg();
                result.addUndo(executeCardPlayActions(cardPlayArg, onPlayActions));
            }
        }

        if (playCardEvents && !playEvent.isVetodPlay()) {
            result.addUndo(events.donePlayingCardListeners().triggerEvent(new CardPlayedEvent(card, manaCost)));
        }

        return result;
    }

    public int getWeaponAttack() {
        return weapon != null ? weapon.getAttack() : 0;
    }

    public Weapon tryGetWeapon() {
        return weapon;
    }

    public UndoableResult<List<Weapon>> removeDeadWeapons() {
        Weapon weaponInHand = tryGetWeapon();

        UndoAction destroyHandUndo;
        if (weaponInHand != null && weaponInHand.getCharges() <= 0) {
            destroyHandUndo = destroyWeapon();
        }
        else {
            destroyHandUndo = UndoAction.DO_NOTHING;
        }

        List<Weapon> result = new ArrayList<>(deadWeapons.size());
        result.addAll(deadWeapons);
        deadWeapons.clear();

        return new UndoableResult<>(Collections.unmodifiableList(result), () -> {
            deadWeapons.addAll(result);
            destroyHandUndo.undo();
        });
    }

    public UndoAction destroyWeapon() {
        if (tryGetWeapon() == null) {
            return UndoAction.DO_NOTHING;
        }

        return equipWeapon(null);
    }

    public UndoAction equipWeapon(WeaponDescr newWeaponDescr) {
        Weapon currentWeapon = tryGetWeapon();
        UndoAction weaponKillUndo;
        if (currentWeapon != null) {
            deadWeapons.addLastGetReference(currentWeapon);
            weaponKillUndo = () -> deadWeapons.remove(deadWeapons.size() - 1);
        }
        else {
            weaponKillUndo = UndoAction.DO_NOTHING;
        }

        Weapon newWeapon = newWeaponDescr != null
                ? new Weapon(this, newWeaponDescr)
                : null;
        this.weapon = newWeapon;
        UndoAction abilityActivateUndo = newWeapon != null
                ? newWeapon.activatePassiveAbilities()
                : UndoAction.DO_NOTHING;

        return () -> {
            abilityActivateUndo.undo();
            this.weapon = currentWeapon;
            weaponKillUndo.undo();
        };
    }

    public UndoAction summonMinion(MinionDescr minionDescr) {
        return summonMinion(new Minion(this, minionDescr));
    }

    public UndoAction summonMinion(Minion minion) {
        UndoableResult<BoardReservationRef> reservationRef = board.tryReservePosition(minion);
        if (reservationRef == null) {
            return UndoAction.DO_NOTHING;
        }

        UndoAction summonUndo = board.summonMinion(reservationRef.getResult());
        return () -> {
            summonUndo.undo();
            reservationRef.undo();
        };
    }

    public Damage getSpellDamage(int baseDamage) {
        return new Damage(hero, baseDamage >= 0
                ? baseDamage + spellPower.getValue()
                : baseDamage);
    }

    public Damage getBasicDamage(int baseDamage) {
        return new Damage(hero, baseDamage);
    }

    public UndoAction doFatiqueDamage() {
        fatique++;
        UndoAction damageUndo = ActionUtils.damageCharacter(hero, fatique, hero);
        return () -> {
            damageUndo.undo();
            fatique--;
        };
    }

    public UndoAction drawCardToHand(CardDescr card) {
        return drawCardToHand(new Card(this, card));
    }

    public UndoAction drawCardToHand(Card card) {
        ExceptionHelper.checkNotNullArgument(card, "card");

        UndoAction drawActionsUndo = WorldActionList.executeActionsNow(world, this, card.getCardDescr().getOnDrawActions());
        UndoAction addCardUndo = hand.addCard(card, world.getEvents().drawCardListeners()::triggerEvent);

        return () -> {
            addCardUndo.undo();
            drawActionsUndo.undo();
        };
    }

    public UndoableResult<CardDescr> drawFromDeck() {
        UndoableResult<CardDescr> drawnCard = getBoard().getDeck().tryDrawOneCard();
        if (drawnCard == null) {
            UndoAction fatiqueUndo = doFatiqueDamage();
            return new UndoableResult<>(null, fatiqueUndo);
        }
        return drawnCard;
    }

    public UndoableResult<CardDescr> drawCardToHand() {
        UndoableResult<CardDescr> cardRef = drawFromDeck();
        CardDescr card = cardRef.getResult();
        if (card == null) {
            return cardRef;
        }

        UndoAction addCardUndo = drawCardToHand(card);

        return new UndoableResult<>(card, () -> {
            addCardUndo.undo();
            cardRef.undo();
        });
    }

    public Hand getHand() {
        return hand;
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public UndoAction setHero(Hero newHero) {
        ExceptionHelper.checkNotNullArgument(newHero, "newHero");
        if (newHero.getOwner() != this) {
            throw new IllegalArgumentException("Hero belongs to another player.");
        }

        Hero prevHero = hero;
        hero = newHero;
        return () -> hero = prevHero;
    }

    public Hero getHero() {
        return hero;
    }

    public BoardSide getBoard() {
        return board;
    }

    public ManaResource getManaResource() {
        return manaResource;
    }

    public int getMana() {
        return manaResource.getMana();
    }

    public AuraAwareIntProperty getSpellPower() {
        return spellPower;
    }

    public AuraAwareIntProperty getDeathRattleTriggerCount() {
        return deathRattleTriggerCount;
    }

    public UndoAction setMana(int mana) {
        return manaResource.setMana(mana);
    }
}
