package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.DeathResolutionResult;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.PlayerId;
import com.github.kelemen.hearthstone.emulator.TargetId;
import com.github.kelemen.hearthstone.emulator.World;
import com.github.kelemen.hearthstone.emulator.WorldPlayAgent;
import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.actions.WorldAction;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class WorldPlayUiAgent {
    private WorldPlayAgent playAgent;
    private final TargetManager targetManager;
    private final UndoManager undoManager;
    private final ListenerManager<Runnable> refreshWorldActions;

    public WorldPlayUiAgent(World world, PlayerId startingPlayer, TargetManager targetManager) {
        ExceptionHelper.checkNotNullArgument(targetManager, "targetManager");

        this.playAgent = new WorldPlayAgent(world, startingPlayer);
        this.targetManager = targetManager;
        this.undoManager = new UndoManager();
        this.refreshWorldActions = new CopyOnTriggerListenerManager<>();
    }

    public void resetWorld(World world) {
        resetWorld(world, world.getPlayer1().getPlayerId());
    }

    public void resetWorld(World world, PlayerId startingPlayer) {
        WorldPlayAgent newAgent = new WorldPlayAgent(world, startingPlayer);

        WorldPlayAgent prevAgent = playAgent;
        playAgent = newAgent;

        undoManager.addUndo(() -> playAgent = prevAgent);
        refreshWorld();
    }

    public void alterWorld(WorldAction action) {
        undoManager.addUndo(action.alterWorld(playAgent.getWorld()));
        refreshWorld();
    }

    public ListenerRef addRefreshWorldAction(Runnable action) {
        return refreshWorldActions.registerListener(action);
    }

    public void undoLastAction() {
        undoManager.undo();
        refreshWorld();
    }

    public PropertySource<Boolean> hasUndos() {
        return undoManager.hasUndos();
    }

    public World getWorld() {
        return playAgent.getWorld();
    }

    public TargetManager getTargetManager() {
        return targetManager;
    }

    public Player getCurrentPlayer() {
        return playAgent.getCurrentPlayer();
    }

    public PlayerId getCurrentPlayerId() {
        return playAgent.getCurrentPlayerId();
    }

    private void refreshWorld() {
        EventListeners.dispatchRunnable(refreshWorldActions);
    }

    public void endTurn() {
        undoManager.addUndo(playAgent.endTurn());
        refreshWorld();
    }

    public void attack(TargetId attacker, TargetId defender) {
        DeathResolutionResult result = playAgent.attack(attacker, defender);
        undoManager.addUndo(result.getUndoAction());

        // TODO: Update game over state
        refreshWorld();
    }

    public void playCard(int cardIndex, PlayTargetRequest playTarget) {
        DeathResolutionResult result = playAgent.playCard(cardIndex, playTarget);
        undoManager.addUndo(result.getUndoAction());

        // TODO: Update game over state
        refreshWorld();
    }

    public void playHeroPower(PlayTargetRequest playTarget) {
        DeathResolutionResult result = playAgent.playHeroPower(playTarget);
        undoManager.addUndo(result.getUndoAction());

        // TODO: Update game over state
        refreshWorld();
    }
}
