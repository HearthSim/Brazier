package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.PlayTargetRequest;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.WorldAction;
import com.github.kelemen.brazier.cards.Card;
import java.util.Optional;
import org.jtrim.utils.ExceptionHelper;

public final class WorldPlayAgent {
    private final World world;

    public WorldPlayAgent(World world) {
        this(world, world.getPlayer1().getPlayerId());
    }

    public WorldPlayAgent(World world, PlayerId startingPlayer) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        this.world = world;
        this.world.setCurrentPlayerId(startingPlayer);
    }

    public World getWorld() {
        return world;
    }

    public UndoAction endTurn() {
        return doWorldAction(World::endTurn);
    }

    public UndoAction setCurrentPlayerId(PlayerId currentPlayerId) {
        return world.setCurrentPlayerId(currentPlayerId);
    }

    public Player getCurrentPlayer() {
        return world.getCurrentPlayer();
    }

    public PlayerId getCurrentPlayerId() {
        return getCurrentPlayer().getPlayerId();
    }

    public UndoAction doWorldAction(WorldAction worldAction) {
        ExceptionHelper.checkNotNullArgument(worldAction, "worldAction");

        UndoAction action = worldAction.alterWorld(world);
        UndoAction deathResults = world.endPhase();
        return () -> {
            deathResults.undo();
            action.undo();
        };
    }

    public UndoAction attack(TargetId attacker, TargetId defender) {
        // TODO: Check if the action is actually a valid move.
        return doWorldAction((currentWorld) -> currentWorld.attack(attacker, defender));
    }

    public UndoAction playHeroPower(PlayTargetRequest targetRequest) {
        return doWorldAction((currentWorld) -> {
            Player castingPlayer = currentWorld.getPlayer(targetRequest.getCastingPlayerId());
            TargetableCharacter target = currentWorld.findTarget(targetRequest.getTargetId());

            HeroPower selectedPower = castingPlayer.getHero().getHeroPower();
            return selectedPower.alterWorld(currentWorld, Optional.ofNullable(target));
        });
    }

    public UndoAction playCard(int cardIndex, PlayTargetRequest playTarget) {
        // TODO: Check if the action is actually a valid move.
        return doWorldAction((currentWorld) -> {
            Player player = currentWorld.getPlayer(playTarget.getCastingPlayerId());
            Hand hand = player.getHand();
            int manaCost = hand.getCard(cardIndex).getActiveManaCost();

            UndoableResult<Card> cardRef = hand.removeAtIndex(cardIndex);
            if (cardRef == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction playUndo = player.playCard(cardRef.getResult(), manaCost, playTarget);
            return () -> {
                playUndo.undo();
                cardRef.undo();
            };
        });
    }
}
