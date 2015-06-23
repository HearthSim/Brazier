package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.HeroPower;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerId;
import com.github.kelemen.hearthstone.emulator.TargetId;
import com.github.kelemen.hearthstone.emulator.TargetableCharacter;
import com.github.kelemen.hearthstone.emulator.TargeterDef;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.actions.TargetNeed;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

public final class PlayerUiAgent {
    private final WorldPlayUiAgent worldAgent;
    private final PlayerId playerId;

    public PlayerUiAgent(WorldPlayUiAgent worldAgent, PlayerId playerId) {
        ExceptionHelper.checkNotNullArgument(worldAgent, "worldAgent");
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");

        this.worldAgent = worldAgent;
        this.playerId = playerId;
    }

    public Player getPlayer() {
        return worldAgent.getWorld().getPlayer(playerId);
    }

    public void alterPlayer(Function<? super Player, ? extends UndoAction> task) {
        ExceptionHelper.checkNotNullArgument(task, "task");
        worldAgent.alterWorld((world) -> {
            return task.apply(world.getPlayer(playerId));
        });
    }

    public boolean canPlayCard(Card card) {
        Player player = worldAgent.getWorld().getPlayer(playerId);
        if (player.getMana() < card.getActiveManaCost()) {
            return false;
        }

        return card.getCardDescr().doesSomethingWhenPlayed(player);
    }

    public void playHeroPower() {
        Player player = worldAgent.getWorld().getPlayer(playerId);
        HeroPower heroPower = player.getHero().getHeroPower();

        if (!heroPower.isPlayable(player)) {
            throw new IllegalStateException("Cannot play the hero power.");
        }

        TargetNeed targetNeed = heroPower.getTargetNeed(player);
        if (targetNeed.hasTarget()) {
            TargetManager targetManager = worldAgent.getTargetManager();
            TargeterDef targeterDef = new TargeterDef(playerId, true, false);
            PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
            targetManager.requestTarget(playerTargetNeed, (targetId) -> {
                if (targetId instanceof TargetId) {
                    targetManager.clearRequest();
                    playHeroPowerNow((TargetId)targetId);
                }
            });
        }
        else {
            playHeroPowerNow(null);
        }
    }

    private void playHeroPowerNow(TargetId targetId) {
        PlayTargetRequest target = new PlayTargetRequest(playerId, -1, targetId);
        worldAgent.playHeroPower(target);
    }

    public void playCard(int cardIndex) {
        Player player = worldAgent.getWorld().getPlayer(playerId);
        Card card = player.getHand().getCard(cardIndex);
        if (!canPlayCard(card)) {
            throw new IllegalArgumentException("Cannot play the given card.");
        }

        List<CardDescr> chooseOneActions = card.getCardDescr().getChooseOneActions();
        CardDescr chooseOneChoice;
        if (!chooseOneActions.isEmpty()) {
            chooseOneChoice = worldAgent.getWorld().getUserAgent().selectCard(true, chooseOneActions);
            if (chooseOneChoice == null) {
                return;
            }
        }
        else {
            chooseOneChoice = null;
        }

        boolean needsMinion = card.getCardDescr().getMinion() != null;
        if (needsMinion) {
            findMinionTarget(player, cardIndex, card, chooseOneChoice);
            return;
        }

        TargetNeed targetNeed = card.getCardDescr().getCombinedTargetNeed(player);
        if (chooseOneChoice != null) {
            targetNeed = targetNeed.combine(chooseOneChoice.getCombinedTargetNeed(player));
        }

        if (targetNeed.hasTarget()) {
            TargeterDef targeterDef = new TargeterDef(playerId, true, false);
            PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
            findTarget(playerTargetNeed, cardIndex, -1, chooseOneChoice);
        }
        else {
            worldAgent.playCard(cardIndex, new PlayTargetRequest(playerId, -1, null, chooseOneChoice));
        }
    }

    private static boolean hasValidTarget(Player player, PlayerTargetNeed targetNeed) {
        if (targetNeed.isAllowedTarget(player.getHero())) {
            return true;
        }

        return player.getBoard().findMinion((minion) -> targetNeed.isAllowedTarget(minion)) != null;
    }

    private static boolean hasValidTarget(World world, PlayerTargetNeed targetNeed) {
        return hasValidTarget(world.getPlayer1(), targetNeed)
                || hasValidTarget(world.getPlayer2(), targetNeed);
    }

    private void findMinionTarget(Player player, int cardIndex, Card card, CardDescr chooseOneChoice) {
        if (player.getBoard().isFull()) {
            return;
        }

        TargetManager targetManager = worldAgent.getTargetManager();
        UiMinionIndexNeed minionIndexNeed = new UiMinionIndexNeed(playerId);
        targetManager.requestTarget(minionIndexNeed, (minionIndex) -> {
            if (minionIndex instanceof Integer) {
                targetManager.clearRequest();
                TargetNeed targetNeed = card.getCardDescr().getCombinedTargetNeed(player);

                if (targetNeed.hasTarget()) {
                    TargeterDef targeterDef = new TargeterDef(playerId, false, false);
                    PlayerTargetNeed playerTargetNeed = new PlayerTargetNeed(targeterDef, targetNeed);
                    if (hasValidTarget(player.getWorld(), playerTargetNeed)) {
                        findTarget(playerTargetNeed, cardIndex, (int)minionIndex, chooseOneChoice);
                        return;
                    }
                }

                worldAgent.playCard(cardIndex, new PlayTargetRequest(playerId, (int)minionIndex, null));
            }
        });
    }

    private void findTarget(PlayerTargetNeed targetNeed, int cardIndex, int minionIndex, CardDescr chooseOneChoice) {
        TargetManager targetManager = worldAgent.getTargetManager();
        targetManager.requestTarget(targetNeed, (targetId) -> {
            if (targetId instanceof TargetId) {
                targetManager.clearRequest();
                worldAgent.playCard(cardIndex,
                        new PlayTargetRequest(playerId, minionIndex, (TargetId)targetId, chooseOneChoice));
            }
        });
    }

    public void attack(TargetableCharacter attacker) {
        ExceptionHelper.checkNotNullArgument(attacker, "attacker");
        if (!Objects.equals(attacker.getOwner().getPlayerId(), playerId)) {
            throw new IllegalArgumentException("Must attack with player: " + playerId.getName());
        }

        TargetManager targetManager = worldAgent.getTargetManager();
        TargeterDef targeterDef = new TargeterDef(playerId, attacker instanceof Hero, true);
        targetManager.requestTarget(new AttackTargetNeed(targeterDef), (targetId) -> {
            if (targetId instanceof TargetId) {
                worldAgent.attack(attacker.getTargetId(), (TargetId)targetId);
            }
        });
    }

    @Override
    public String toString() {
        return "UI agent of " + playerId.getName();
    }
}
