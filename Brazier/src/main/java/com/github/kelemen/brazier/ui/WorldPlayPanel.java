package com.github.kelemen.brazier.ui;

import com.github.kelemen.brazier.HearthStoneDb;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.PlayerId;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.World;
import com.github.kelemen.brazier.cards.CardDescr;
import java.awt.GridLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.utils.ExceptionHelper;

import static org.jtrim.property.swing.AutoDisplayState.*;

@SuppressWarnings("serial")
public class WorldPlayPanel extends javax.swing.JPanel {
    private final HearthStoneDb db;
    private final TargetManager targetManager;
    private final WorldPlayUiAgent uiAgent;

    private final BoardSidePanel board1;
    private final BoardSidePanel board2;

    private final List<ListenerRef> trackRefs;
    private final PlayerPanel player1;
    private final PlayerPanel player2;

    public WorldPlayPanel(World world, PlayerId startingPlayer) {
        ExceptionHelper.checkNotNullArgument(world, "world");
        ExceptionHelper.checkNotNullArgument(startingPlayer, "startingPlayer");

        this.db = world.getDb();
        this.trackRefs = new LinkedList<>();
        this.targetManager = new TargetManager(this);

        uiAgent = new WorldPlayUiAgent(world, startingPlayer, targetManager);
        uiAgent.addRefreshWorldAction(this::refreshWorld);

        initComponents();

        jLeftControlContainer.setLayout(new SerialLayoutManager(5, false, SerialLayoutManager.Alignment.LEFT));
        jRightControlContainer.setLayout(new SerialLayoutManager(5, false, SerialLayoutManager.Alignment.RIGHT));

        board1 = new BoardSidePanel(world.getPlayer1().getPlayerId(), targetManager);
        board2 = new BoardSidePanel(world.getPlayer2().getPlayerId(), targetManager);

        player1 = new PlayerPanel(db);
        player2 = new PlayerPanel(db);

        jPlayer1BoardPanel.add(board1);
        jPlayer2BoardPanel.add(board2);
        jPlayer1PlayerPanel.add(player1);
        jPlayer2PlayerPanel.add(player2);

        setupEnableDisable();

        refreshWorld();

        setUserAgent(world);
    }

    private void setUserAgent(World world) {
        world.setUserAgent((boolean allowCancel, List<? extends CardDescr> cards) -> {
            // Doesn't actually matter which player we use.
            Player player = world.getCurrentPlayer();
            return UiUtils.getOnEdt(() -> ChooseCardPanel.selectCard(this, allowCancel, player, cards));
        });
    }

    private void setupEnableDisable() {
        addSwingStateListener(uiAgent.hasUndos(), jUndoButton::setEnabled);
    }

    public final void setWorld(World world, PlayerId startingPlayer) {
        setUserAgent(world);
        uiAgent.resetWorld(world, startingPlayer);
    }

    private void refreshWorld() {
        targetManager.clearRequest();

        World world = uiAgent.getWorld();
        PlayerId currentPlayerId = uiAgent.getCurrentPlayerId();

        PlayerUiAgent player1UiAgent;
        PlayerUiAgent player2UiAgent;
        if (Objects.equals(world.getPlayer1().getPlayerId(), currentPlayerId)) {
            player1UiAgent = new PlayerUiAgent(uiAgent, currentPlayerId);
            player2UiAgent = null;
        }
        else {
            player1UiAgent = null;
            player2UiAgent = new PlayerUiAgent(uiAgent, currentPlayerId);
        }

        player1.setState(player1UiAgent, world.getPlayer1());
        player2.setState(player2UiAgent, world.getPlayer2());

        board1.setBoard(player1UiAgent, world.getPlayer1().getBoard());
        board2.setBoard(player2UiAgent, world.getPlayer2().getBoard());

        setupHeroTracking(world);
    }

    private ListenerRef trackForTarget(
            TargetManager targetManager,
            JComponent component,
            TargetableCharacter target,
            Consumer<Boolean> highlightSetter) {
        ListenerRef ref1 = PlayerTargetNeed.trackForTarget(targetManager, component, target, highlightSetter);
        ListenerRef ref2 = AttackTargetNeed.trackForTarget(targetManager, component, target, highlightSetter);
        return ListenerRegistries.combineListenerRefs(ref1, ref2);
    }

    private void setupHeroTracking(World world) {
        trackRefs.forEach(ListenerRef::unregister);
        trackRefs.clear();

        trackRefs.add(trackForTarget(
                uiAgent.getTargetManager(),
                player1,
                world.getPlayer1().getHero(),
                player1::setHighlight));
        trackRefs.add(trackForTarget(
                uiAgent.getTargetManager(),
                player2,
                world.getPlayer2().getHero(),
                player2::setHighlight));
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jWorldContainer = new javax.swing.JPanel();
        jPlayer1Panel = new javax.swing.JPanel();
        jPlayer1PlayerPanel = new javax.swing.JPanel();
        jPlayer1BoardPanel = new javax.swing.JPanel();
        jPlayer2Panel = new javax.swing.JPanel();
        jPlayer2BoardPanel = new javax.swing.JPanel();
        jPlayer2PlayerPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jRightControlContainer = new javax.swing.JPanel();
        jEndTurnButton = new javax.swing.JButton();
        jLeftControlContainer = new javax.swing.JPanel();
        jUndoButton = new javax.swing.JButton();
        jAddCardButton = new javax.swing.JButton();
        jResetWorldButton = new javax.swing.JButton();

        jPlayer1PlayerPanel.setLayout(new java.awt.GridLayout(1, 1));

        jPlayer1BoardPanel.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout jPlayer1PanelLayout = new javax.swing.GroupLayout(jPlayer1Panel);
        jPlayer1Panel.setLayout(jPlayer1PanelLayout);
        jPlayer1PanelLayout.setHorizontalGroup(
            jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPlayer1BoardPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPlayer1PlayerPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPlayer1PanelLayout.setVerticalGroup(
            jPlayer1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPlayer1PlayerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPlayer1BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPlayer2BoardPanel.setLayout(new java.awt.GridLayout(1, 1));

        jPlayer2PlayerPanel.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout jPlayer2PanelLayout = new javax.swing.GroupLayout(jPlayer2Panel);
        jPlayer2Panel.setLayout(jPlayer2PanelLayout);
        jPlayer2PanelLayout.setHorizontalGroup(
            jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                .addGroup(jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPlayer2PlayerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPlayer2BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPlayer2PanelLayout.setVerticalGroup(
            jPlayer2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPlayer2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPlayer2BoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPlayer2PlayerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jRightControlContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        jEndTurnButton.setText("End Turn");
        jEndTurnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEndTurnButtonActionPerformed(evt);
            }
        });
        jRightControlContainer.add(jEndTurnButton);

        jLeftControlContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        jUndoButton.setText("Undo");
        jUndoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUndoButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jUndoButton);

        jAddCardButton.setText("Card database");
        jAddCardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddCardButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jAddCardButton);

        jResetWorldButton.setText("Reset world");
        jResetWorldButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jResetWorldButtonActionPerformed(evt);
            }
        });
        jLeftControlContainer.add(jResetWorldButton);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLeftControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, 791, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRightControlContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLeftControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jRightControlContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout jWorldContainerLayout = new javax.swing.GroupLayout(jWorldContainer);
        jWorldContainer.setLayout(jWorldContainerLayout);
        jWorldContainerLayout.setHorizontalGroup(
            jWorldContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPlayer1Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPlayer2Panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jWorldContainerLayout.setVerticalGroup(
            jWorldContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jWorldContainerLayout.createSequentialGroup()
                .addComponent(jPlayer1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(jPlayer2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jWorldContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jWorldContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jEndTurnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEndTurnButtonActionPerformed
        uiAgent.endTurn();
    }//GEN-LAST:event_jEndTurnButtonActionPerformed

    private void jUndoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUndoButtonActionPerformed
        uiAgent.undoLastAction();
    }//GEN-LAST:event_jUndoButtonActionPerformed

    private void jResetWorldButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jResetWorldButtonActionPerformed
        World prevWorld = uiAgent.getWorld();
        PlayerId player1Id = prevWorld.getPlayer1().getPlayerId();
        PlayerId player2Id = prevWorld.getPlayer2().getPlayerId();

        World newWorld = new World(prevWorld.getDb(), player1Id, player2Id);
        setWorld(newWorld, newWorld.getPlayer1().getPlayerId());
    }//GEN-LAST:event_jResetWorldButtonActionPerformed

    private void jAddCardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddCardButtonActionPerformed
        CardDatabasePanel panel = new CardDatabasePanel(db, uiAgent);

        JFrame dbFrame = new JFrame("Card database");
        dbFrame.getContentPane().setLayout(new GridLayout(1, 1));
        dbFrame.getContentPane().add(panel);

        dbFrame.pack();
        dbFrame.setLocationRelativeTo(this);
        dbFrame.setVisible(true);
        dbFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }//GEN-LAST:event_jAddCardButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddCardButton;
    private javax.swing.JButton jEndTurnButton;
    private javax.swing.JPanel jLeftControlContainer;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPlayer1BoardPanel;
    private javax.swing.JPanel jPlayer1Panel;
    private javax.swing.JPanel jPlayer1PlayerPanel;
    private javax.swing.JPanel jPlayer2BoardPanel;
    private javax.swing.JPanel jPlayer2Panel;
    private javax.swing.JPanel jPlayer2PlayerPanel;
    private javax.swing.JButton jResetWorldButton;
    private javax.swing.JPanel jRightControlContainer;
    private javax.swing.JButton jUndoButton;
    private javax.swing.JPanel jWorldContainer;
    // End of variables declaration//GEN-END:variables
}
