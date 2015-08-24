package com.github.kelemen.brazier.ui;

import com.github.kelemen.brazier.HearthStoneDb;
import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.HeroPower;
import com.github.kelemen.brazier.ManaResource;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.cards.Card;
import com.github.kelemen.brazier.weapons.AttackTool;
import com.github.kelemen.brazier.weapons.Weapon;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.jtrim.utils.ExceptionHelper;


@SuppressWarnings("serial")
public class PlayerPanel extends javax.swing.JPanel {
    private final CardComponents cardComponents;
    private final Color defaultColor;
    private PlayerUiAgent uiAgent;
    private Player player;

    public PlayerPanel(HearthStoneDb db) {
        ExceptionHelper.checkNotNullArgument(db, "db");

        this.uiAgent = null;
        this.player = null;

        initComponents();

        JHorizontallyScrollablePanel handContainer = new JHorizontallyScrollablePanel();
        handContainer.setLayout(new ConstAspectRatioLayout());
        jHandScrollPane.setViewportView(handContainer);
        jHandScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        cardComponents = new CardComponents(handContainer);
        defaultColor = jHeroContainer.getBackground();

        setupEditors(db);
    }

    private void setupEditors(HearthStoneDb db) {
        jHeroContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    openHeroEditor(db);
                }
            }
        });
        UiUtils.forwardMouseEvents(jHeroContainer);
    }

    private void openHeroEditor(HearthStoneDb db) {
        if (uiAgent == null) {
            return;
        }
        HeroEditorPanel panel = new HeroEditorPanel(db, uiAgent);

        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog mainFrame = new JDialog(parent, "Hero Editor", ModalityType.DOCUMENT_MODAL);
        mainFrame.getContentPane().setLayout(new GridLayout(1, 1));
        mainFrame.getContentPane().add(panel);

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.setVisible(true);
    }

    public void setHighlight(boolean highlighted) {
        jHeroContainer.setBackground(highlighted ? Color.GREEN : defaultColor);
    }

    public void setState(PlayerUiAgent uiAgent, Player player) {
        ExceptionHelper.checkNotNullArgument(player, "player");

        this.uiAgent = uiAgent;
        this.player = player;

        Hero hero = player.getHero();
        jHealthValueLabel.setText(Integer.toString(hero.getCurrentHp())
                + " (max: " + Integer.toString(hero.getMaxHp()) + ")");

        int attack = hero.getAttackTool().getAttack();
        Weapon weapon = player.tryGetWeapon();
        int charges = weapon != null ? weapon.getCharges() : 1;

        String weaponSuffix = weapon != null
                ? " (" + weapon.getBaseDescr().getDisplayName() + ")"
                : "";

        jAttackValueLabel.setText(Integer.toString(attack) + "/" + Integer.toString(charges) + weaponSuffix);
        jArmorValueLabel.setText(Integer.toString(hero.getCurrentArmor()));
        ManaResource manaResource = player.getManaResource();
        jManaValue.setText(Integer.toString(manaResource.getMana()) + "/" + Integer.toString(manaResource.getManaCrystals()));
        jDeckSizeValueLabel.setText(Integer.toString(player.getBoard().getDeck().getNumberOfCards()));
        jAttackButton.setVisible(uiAgent != null);
        jAttackButton.setEnabled(player.getHero().getAttackTool().canAttackWith());

        HeroPower heroPower = player.getHero().getHeroPower();
        String heroPowerName = heroPower.getPowerDef().getDisplayName();
        jUsePowerButton.setText(heroPowerName.isEmpty() ? "Use power" : heroPowerName);
        jUsePowerButton.setVisible(uiAgent != null);
        jUsePowerButton.setEnabled(player.getHero().getHeroPower().isPlayable(player));

        updateHandCards(uiAgent, player.getHand().getCards());
    }

    private void updateHandCards(PlayerUiAgent uiAgent, List<Card> cards) {
        cardComponents.setCards(uiAgent, cards);
    }

    private static final class CardComponents {
        private final JComponent container;

        public CardComponents(JComponent container) {
            this.container = container;
        }

        public void setCards(PlayerUiAgent uiAgent, List<Card> cards) {
            container.removeAll();
            int index = 0;
            for (Card card: cards) {
                PlayableCardComponent cardComponent = new PlayableCardComponent(card);
                JButton playButton = cardComponent.getPlayButton();
                playButton.setVisible(uiAgent != null);

                if (uiAgent != null) {
                    playButton.setEnabled(uiAgent.canPlayCard(card));

                    int cardIndex = index;
                    playButton.addActionListener((e) -> {
                        uiAgent.playCard(cardIndex);
                    });
                }
                container.add(cardComponent);

                index++;
            }

            container.revalidate();
            container.repaint();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jHeroContainer = new javax.swing.JPanel();
        jHeroNameLable = new javax.swing.JLabel();
        jHpLabel = new javax.swing.JLabel();
        jHealthValueLabel = new javax.swing.JLabel();
        jArmorLabel = new javax.swing.JLabel();
        jArmorValueLabel = new javax.swing.JLabel();
        jAttackLabel = new javax.swing.JLabel();
        jAttackValueLabel = new javax.swing.JLabel();
        jDeckSizeLabel = new javax.swing.JLabel();
        jDeckSizeValueLabel = new javax.swing.JLabel();
        jManaLabel = new javax.swing.JLabel();
        jManaValue = new javax.swing.JLabel();
        jAttackButton = new javax.swing.JButton();
        jUsePowerButton = new javax.swing.JButton();
        jHandScrollPane = new javax.swing.JScrollPane();

        jHeroNameLable.setText("Custom hero");

        jHpLabel.setText("Health:");

        jHealthValueLabel.setText("30");

        jArmorLabel.setText("Armor:");

        jArmorValueLabel.setText("0");

        jAttackLabel.setText("Attack:");

        jAttackValueLabel.setText("0");

        jDeckSizeLabel.setText("Deck size:");

        jDeckSizeValueLabel.setText("0");

        jManaLabel.setText("Mana:");

        jManaValue.setText("0/0");

        jAttackButton.setText("Attack");
        jAttackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAttackButtonActionPerformed(evt);
            }
        });

        jUsePowerButton.setText("Use power");
        jUsePowerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUsePowerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jHeroContainerLayout = new javax.swing.GroupLayout(jHeroContainer);
        jHeroContainer.setLayout(jHeroContainerLayout);
        jHeroContainerLayout.setHorizontalGroup(
            jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jHeroContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jHeroContainerLayout.createSequentialGroup()
                        .addComponent(jAttackButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jUsePowerButton))
                    .addGroup(jHeroContainerLayout.createSequentialGroup()
                        .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jHeroNameLable)
                            .addGroup(jHeroContainerLayout.createSequentialGroup()
                                .addComponent(jHpLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jHealthValueLabel))
                            .addGroup(jHeroContainerLayout.createSequentialGroup()
                                .addComponent(jArmorLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jArmorValueLabel))
                            .addGroup(jHeroContainerLayout.createSequentialGroup()
                                .addComponent(jAttackLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jAttackValueLabel))
                            .addGroup(jHeroContainerLayout.createSequentialGroup()
                                .addComponent(jManaLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jManaValue))
                            .addGroup(jHeroContainerLayout.createSequentialGroup()
                                .addComponent(jDeckSizeLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jDeckSizeValueLabel)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jHeroContainerLayout.setVerticalGroup(
            jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jHeroContainerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jHeroNameLable)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jHpLabel)
                    .addComponent(jHealthValueLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jArmorLabel)
                    .addComponent(jArmorValueLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAttackLabel)
                    .addComponent(jAttackValueLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jManaLabel)
                    .addComponent(jManaValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jDeckSizeLabel)
                    .addComponent(jDeckSizeValueLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jHeroContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAttackButton)
                    .addComponent(jUsePowerButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jHeroContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jHandScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 641, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jHeroContainer, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jHandScrollPane))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jAttackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAttackButtonActionPerformed
        if (uiAgent == null || player == null) {
            return;
        }

        Hero hero = player.getHero();

        AttackTool attackTool = hero.getAttackTool();
        if (attackTool.canAttackWith()) {
            uiAgent.attack(hero);
        }
    }//GEN-LAST:event_jAttackButtonActionPerformed

    private void jUsePowerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUsePowerButtonActionPerformed
        if (uiAgent != null) {
            uiAgent.playHeroPower();
        }
    }//GEN-LAST:event_jUsePowerButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jArmorLabel;
    private javax.swing.JLabel jArmorValueLabel;
    private javax.swing.JButton jAttackButton;
    private javax.swing.JLabel jAttackLabel;
    private javax.swing.JLabel jAttackValueLabel;
    private javax.swing.JLabel jDeckSizeLabel;
    private javax.swing.JLabel jDeckSizeValueLabel;
    private javax.swing.JScrollPane jHandScrollPane;
    private javax.swing.JLabel jHealthValueLabel;
    private javax.swing.JPanel jHeroContainer;
    private javax.swing.JLabel jHeroNameLable;
    private javax.swing.JLabel jHpLabel;
    private javax.swing.JLabel jManaLabel;
    private javax.swing.JLabel jManaValue;
    private javax.swing.JButton jUsePowerButton;
    // End of variables declaration//GEN-END:variables
}
