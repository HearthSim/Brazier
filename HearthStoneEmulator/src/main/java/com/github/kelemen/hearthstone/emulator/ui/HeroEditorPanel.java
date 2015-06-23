package com.github.kelemen.hearthstone.emulator.ui;

import com.github.kelemen.hearthstone.emulator.HearthStoneDb;
import com.github.kelemen.hearthstone.emulator.Hero;
import com.github.kelemen.hearthstone.emulator.HeroPowerDef;
import com.github.kelemen.hearthstone.emulator.HeroPowerId;
import com.github.kelemen.hearthstone.emulator.Keyword;
import com.github.kelemen.hearthstone.emulator.Keywords;
import com.github.kelemen.hearthstone.emulator.ManaResource;
import com.github.kelemen.hearthstone.emulator.Player;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.utils.ExceptionHelper;

@SuppressWarnings("serial")
public class HeroEditorPanel extends javax.swing.JPanel {
    private final PlayerUiAgent agent;
    private final HeroPowerId currentPowerId;

    public HeroEditorPanel(HearthStoneDb db, PlayerUiAgent agent) {
        ExceptionHelper.checkNotNullArgument(db, "db");
        ExceptionHelper.checkNotNullArgument(agent, "agent");

        this.agent = agent;

        initComponents();

        Player player = agent.getPlayer();
        Hero hero = player.getHero();

        jBonusAttackEditor.setModel(new SpinnerNumberModel(hero.getExtraAttackForThisTurn(), 0, Integer.MAX_VALUE, 1));
        jHealthEditor.setModel(new SpinnerNumberModel(hero.getCurrentHp(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        jMaxHealthEditor.setModel(new SpinnerNumberModel(hero.getMaxHp(), 1, Integer.MAX_VALUE, 1));
        jArmorEditor.setModel(new SpinnerNumberModel(hero.getCurrentArmor(), 0, Integer.MAX_VALUE, 1));

        ManaResource manaResource = player.getManaResource();
        jManaEditor.setValue(manaResource.getMana());
        jManaCrystalEditor.setValue(manaResource.getManaCrystals());
        jOverloadEditor.setValue(manaResource.getOverloadedMana());
        jOverloadNextTurnEditor.setValue(manaResource.getNextTurnOverload());

        currentPowerId = hero.getHeroPower().getPowerDef().getId();
        setupHeroPowerCombo(db, currentPowerId);
        setupHeroClassCombo(hero.getHeroClass());
    }

    private void setupHeroClassCombo(Keyword defaultSelection) {
        HeroClassItem[] classItems = getHeroClassItems();
        HeroClassItem selected = null;
        for (HeroClassItem item: classItems) {
            if (Objects.equals(defaultSelection, item.getHeroClass())) {
                selected = item;
                break;
            }
        }

        jHeroClassCombo.setModel(new DefaultComboBoxModel<>(classItems));
        if (selected != null) {
            jHeroClassCombo.setSelectedItem(selected);
        }
    }

    private void setupHeroPowerCombo(HearthStoneDb db, HeroPowerId defaultSelection) {
        HeroPowerItem[] powerItems = getPowerItems(db);
        HeroPowerItem selected = null;
        for (HeroPowerItem item: powerItems) {
            if (Objects.equals(defaultSelection, item.powerDef.getId())) {
                selected = item;
                break;
            }
        }

        jHeroPowerEditor.setModel(new DefaultComboBoxModel<>(powerItems));
        if (selected != null) {
            jHeroPowerEditor.setSelectedItem(selected);
        }
    }

    private Keyword tryGetHeroClass() {
        HeroClassItem heroClassItem = SwingProperties.comboBoxSelection(jHeroClassCombo).getValue();
        return heroClassItem != null ? heroClassItem.getHeroClass() : null;
    }

    private HeroPowerDef tryGetNewHeroPower() {
        HeroPowerItem heroPower = SwingProperties.comboBoxSelection(jHeroPowerEditor).getValue();
        if (heroPower == null) {
            return null;
        }
        return Objects.equals(heroPower.powerDef.getId(), currentPowerId)
                ? null
                : heroPower.powerDef;
    }

    private static HeroClassItem[] getHeroClassItems() {
        List<HeroClassItem> result = new ArrayList<>();
        result.add(new HeroClassItem(Keywords.CLASS_BOSS, "Unknown"));
        result.add(new HeroClassItem(Keywords.CLASS_DRUID, "Druid"));
        result.add(new HeroClassItem(Keywords.CLASS_HUNTER, "Hunter"));
        result.add(new HeroClassItem(Keywords.CLASS_MAGE, "Mage"));
        result.add(new HeroClassItem(Keywords.CLASS_PALADIN, "Paladin"));
        result.add(new HeroClassItem(Keywords.CLASS_PRIEST, "Priest"));
        result.add(new HeroClassItem(Keywords.CLASS_ROUGE, "Rouge"));
        result.add(new HeroClassItem(Keywords.CLASS_SHAMAN, "Shaman"));
        result.add(new HeroClassItem(Keywords.CLASS_WARLOCK, "Warlock"));
        result.add(new HeroClassItem(Keywords.CLASS_WARRIOR, "Warrior"));

        Collator strCmp = Collator.getInstance();
        result.sort((item1, item2) -> {
            return strCmp.compare(item1.toString(), item2.toString());
        });

        return result.toArray(new HeroClassItem[result.size()]);
    }

    private static HeroPowerItem[] getPowerItems(HearthStoneDb db) {
        List<HeroPowerDef> all = db.getHeroPowerDb().getAll();
        List<HeroPowerItem> result = new ArrayList<>(all.size());

        for (HeroPowerDef def: all) {
            result.add(new HeroPowerItem(def));
        }

        Collator strCmp = Collator.getInstance();
        result.sort((item1, item2) -> {
            return strCmp.compare(item1.powerDef.getDisplayName(), item2.powerDef.getDisplayName());
        });

        return result.toArray(new HeroPowerItem[result.size()]);
    }

    private static final class HeroClassItem {
        private final Keyword heroClass;
        private final String displayName;

        public HeroClassItem(Keyword heroClass, String displayName) {
            this.heroClass = heroClass;
            this.displayName = displayName;
        }

        public Keyword getHeroClass() {
            return heroClass;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final class HeroPowerItem {
        private final HeroPowerDef powerDef;

        public HeroPowerItem(HeroPowerDef powerDef) {
            ExceptionHelper.checkNotNullArgument(powerDef, "powerDef");
            this.powerDef = powerDef;
        }

        @Override
        public String toString() {
            return powerDef.getDisplayName();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jBonusAttackEditor = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jHealthEditor = new javax.swing.JSpinner();
        jSeparator1 = new javax.swing.JSeparator();
        jHeroPowerEditor = new javax.swing.JComboBox<HeroPowerItem>();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jManaEditor = new javax.swing.JSpinner();
        jManaCrystalEditor = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jOverloadEditor = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jOverloadNextTurnEditor = new javax.swing.JSpinner();
        jCancelButton = new javax.swing.JButton();
        jOkButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jArmorEditor = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jMaxHealthEditor = new javax.swing.JSpinner();
        jHeroClassLabel = new javax.swing.JLabel();
        jHeroClassCombo = new javax.swing.JComboBox<HeroClassItem>();

        jLabel1.setText("Bonus attack:");

        jLabel2.setText("Health:");

        jLabel3.setText("Hero power:");

        jLabel4.setText("Mana:");

        jLabel5.setText("Mana Crystals:");

        jLabel6.setText("Overload:");

        jLabel7.setText("Overload (next turn):");

        jCancelButton.setText("Cancel");
        jCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelButtonActionPerformed(evt);
            }
        });

        jOkButton.setText("Ok");
        jOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOkButtonActionPerformed(evt);
            }
        });

        jLabel8.setText("Armor:");

        jLabel9.setText("Max health:");

        jHeroClassLabel.setText("Hero class:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 114, Short.MAX_VALUE)
                        .addComponent(jOkButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCancelButton))
                    .addComponent(jHeroClassCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jHeroPowerEditor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBonusAttackEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jHealthEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jManaEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jManaCrystalEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jOverloadEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jOverloadNextTurnEditor, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jArmorEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jMaxHealthEditor))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jHeroClassLabel)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jHeroClassLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jHeroClassCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jHeroPowerEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jBonusAttackEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jHealthEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jArmorEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jMaxHealthEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jManaEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jManaCrystalEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jOverloadEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jOverloadNextTurnEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCancelButton)
                    .addComponent(jOkButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOkButtonActionPerformed
        int bonusAttack = (int)jBonusAttackEditor.getValue();
        int hp = (int)jHealthEditor.getValue();
        int armor = (int)jArmorEditor.getValue();
        int maxHp = (int)jMaxHealthEditor.getValue();
        int mana = (int)jManaEditor.getValue();
        int manaCrystals = (int)jManaCrystalEditor.getValue();
        int overload = (int)jOverloadEditor.getValue();
        int overloadNextTurn = (int)jOverloadNextTurnEditor.getValue();

        Keyword heroClass = tryGetHeroClass();
        HeroPowerDef heroPower = tryGetNewHeroPower();

        agent.alterPlayer((player) -> {
            UndoBuilder result = new UndoBuilder();

            Hero hero = player.getHero();
            result.addUndo(hero.addExtraAttackForThisTurn(bonusAttack - hero.getExtraAttackForThisTurn()));
            result.addUndo(hero.setCurrentHp(hp));
            result.addUndo(hero.setCurrentArmor(armor));
            result.addUndo(hero.setMaxHp(maxHp));

            ManaResource manaResource = player.getManaResource();
            result.addUndo(manaResource.setMana(mana));
            result.addUndo(manaResource.setManaCrystals(manaCrystals));
            result.addUndo(manaResource.setOverloadedMana(overload));
            result.addUndo(manaResource.setNextTurnOverload(overloadNextTurn));

            if (heroClass != null) {
                result.addUndo(hero.setHeroClass(heroClass));
            }
            if (heroPower != null) {
                result.addUndo(hero.setHeroPower(heroPower));
            }

            return result;
        });

        SwingUtilities.getWindowAncestor(this).dispose();
    }//GEN-LAST:event_jOkButtonActionPerformed

    private void jCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCancelButtonActionPerformed
        SwingUtilities.getWindowAncestor(this).dispose();
    }//GEN-LAST:event_jCancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner jArmorEditor;
    private javax.swing.JSpinner jBonusAttackEditor;
    private javax.swing.JButton jCancelButton;
    private javax.swing.JSpinner jHealthEditor;
    private javax.swing.JComboBox<HeroClassItem> jHeroClassCombo;
    private javax.swing.JLabel jHeroClassLabel;
    private javax.swing.JComboBox<HeroPowerItem> jHeroPowerEditor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSpinner jManaCrystalEditor;
    private javax.swing.JSpinner jManaEditor;
    private javax.swing.JSpinner jMaxHealthEditor;
    private javax.swing.JButton jOkButton;
    private javax.swing.JSpinner jOverloadEditor;
    private javax.swing.JSpinner jOverloadNextTurnEditor;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables
}
