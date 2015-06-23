package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import java.awt.Color;

@SuppressWarnings("serial")
public class CardComponent extends javax.swing.JPanel {
    private Card card;

    public CardComponent(Card card) {
        initComponents();

        setBackground(Color.LIGHT_GRAY);
        setCard(card);
    }

    public final void setCard(Card card) {
        this.card = card;

        if (card != null) {
            String name = card.getCardDescr().getDisplayName();
            jNameLabel.setText("<html>" + name + "</html>");

            String description = card.getCardDescr().getDescription();
            setToolTipText(description.isEmpty() ? null : description);

            int currentManaCost = card.getActiveManaCost();
            int originalManaCost = card.getCardDescr().getManaCost();

            jManaCostLabel.setText(Integer.toString(currentManaCost));
            if (currentManaCost > originalManaCost) {
                jManaCostLabel.setForeground(Color.RED);
            }
            else if (currentManaCost < originalManaCost) {
                jManaCostLabel.setForeground(Color.BLUE);
            }
            else {
                jManaCostLabel.setForeground(Color.BLACK);
            }

            Minion minion = card.getMinion();
            if (minion != null) {
                int attack = minion.getAttackTool().getAttack();
                int currentHp = minion.getBody().getMaxHp();

                jAttackLabel.setText(Integer.toString(attack));
                jHpLabel.setText(Integer.toString(currentHp));
            }
            else {
                jAttackLabel.setText("");
                jHpLabel.setText("");
            }
        }
        else {
            setToolTipText(null);
            jManaCostLabel.setText("");
            jNameLabel.setText("");
            jAttackLabel.setText("");
            jHpLabel.setText("");
        }
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jManaCostLabel = new javax.swing.JLabel();
        jNameLabel = new javax.swing.JLabel();
        jAttackLabel = new javax.swing.JLabel();
        jHpLabel = new javax.swing.JLabel();

        jManaCostLabel.setText("10");

        jNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jNameLabel.setText("Card's Name");
        jNameLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jAttackLabel.setText("0");

        jHpLabel.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jManaCostLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jAttackLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jHpLabel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jManaCostLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAttackLabel)
                    .addComponent(jHpLabel))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jAttackLabel;
    private javax.swing.JLabel jHpLabel;
    private javax.swing.JLabel jManaCostLabel;
    private javax.swing.JLabel jNameLabel;
    // End of variables declaration//GEN-END:variables
}
