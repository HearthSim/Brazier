package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.BoardSide;
import com.github.kelemen.hearthstone.emulator.PlayerId;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.jtrim.utils.ExceptionHelper;

@SuppressWarnings("serial")
public class BoardSidePanel extends javax.swing.JPanel {
    private final TargetManager targetManager;
    private final JComponent boardContainer;

    public BoardSidePanel(PlayerId playerId, TargetManager targetManager) {
        ExceptionHelper.checkNotNullArgument(playerId, "playerId");
        ExceptionHelper.checkNotNullArgument(targetManager, "targetManager");

        this.targetManager = targetManager;

        initComponents();

        boardContainer = new JHorizontallyScrollablePanel();
        boardContainer.setLayout(new ConstAspectRatioLayout());
        jBoardScrollPane.setViewportView(boardContainer);
        jBoardScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        trackMinionLocationNeed(playerId);
    }

    private void trackMinionLocationNeed(PlayerId playerId) {
        JPanel dummyPanel = new JPanel();
        dummyPanel.setBackground(new Color(0, 0, 255).brighter());

        AtomicInteger dummyIndex = new AtomicInteger(-1);
        jBoardScrollPane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UiTargetCondition condition = targetManager.getCondition();
                if (condition != null) {
                    Object conditionObj = condition.getCondition();
                    if (conditionObj instanceof UiMinionIndexNeed) {
                        PlayerId targetPlayerId = ((UiMinionIndexNeed)conditionObj).getPlayerId();
                        if (Objects.equals(playerId, targetPlayerId)) {
                            MouseEvent boardEvent = SwingUtilities.convertMouseEvent(jBoardScrollPane, e, boardContainer);
                            int insertIndex = findInsertIndex(boardContainer, boardEvent.getPoint());
                            dummyIndex.set(moveDummyPanelToIndex(boardContainer, dummyPanel, insertIndex));
                        }
                    }
                }
            }
        });

        jBoardScrollPane.addMouseListener(new MouseAdapter() {
            private int removeDummyPanel() {
                int result = dummyIndex.getAndSet(-1);
                if (result >= 0) {
                    boardContainer.remove(dummyPanel);
                    boardContainer.revalidate();
                    boardContainer.repaint();
                }
                return result;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int minionIndex = removeDummyPanel();
                if (minionIndex < 0) {
                    return;
                }

                UiTargetCondition condition = targetManager.getCondition();
                if (condition != null) {
                    Object conditionObj = condition.getCondition();
                    if (conditionObj instanceof UiMinionIndexNeed) {
                        PlayerId targetPlayerId = ((UiMinionIndexNeed)conditionObj).getPlayerId();
                        if (Objects.equals(playerId, targetPlayerId)) {
                            condition.getCallback().accept(minionIndex);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeDummyPanel();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }
        });
    }

    private static int findInsertIndex(JComponent parent, Point point) {
        Component[] components = parent.getComponents();
        int x = point.x;
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            int componentLeft = component.getX();
            int componentWidth = component.getWidth();
            if (x < componentLeft + componentWidth / 2) {
                return i;
            }
        }
        return components.length;
    }

    private static int moveDummyPanelToIndex(JComponent parent, Component dummyPanel, int newIndex) {
        Component[] children = parent.getComponents();
        int dummyIndex = -1;
        for (int i = 0; i < children.length; i++) {
            if (children[i] == dummyPanel) {
                dummyIndex = i;
                break;
            }
        }

        if (newIndex == dummyIndex) {
            return newIndex;
        }

        int addIndex;
        if (dummyIndex >= 0) {
            parent.remove(dummyIndex);
            if (newIndex > dummyIndex) {
                addIndex = newIndex - 1;
            }
            else {
                addIndex = newIndex;
            }
        }
        else {
            addIndex = newIndex;
        }

        parent.add(dummyPanel, addIndex);

        parent.revalidate();
        parent.repaint();

        return addIndex;
    }

    public void setBoard(PlayerUiAgent uiAgent, BoardSide board) {
        ExceptionHelper.checkNotNullArgument(board, "board");

        boardContainer.removeAll();
        for (Minion minion: board.getAllMinions()) {
            PlayableMinionComponent minionComponent = new PlayableMinionComponent(minion);
            boardContainer.add(minionComponent);
            JButton attackButton = minionComponent.getAttackButton();
            attackButton.setVisible(uiAgent != null);
            if (uiAgent != null) {
                attackButton.setEnabled(minion.getAttackTool().canAttackWith());
                minionComponent.getAttackButton().addActionListener((e) -> {
                    uiAgent.attack(minion);
                });
            }

            PlayerTargetNeed.trackForTarget(targetManager, minionComponent, minion, minionComponent::setHighlight);
            AttackTargetNeed.trackForTarget(targetManager, minionComponent, minion, minionComponent::setHighlight);

            UiUtils.forwardMouseEvents(minionComponent);
        }

        boardContainer.revalidate();
        boardContainer.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jBoardScrollPane = new javax.swing.JScrollPane();

        jBoardScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jBoardScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jBoardScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jBoardScrollPane;
    // End of variables declaration//GEN-END:variables
}
