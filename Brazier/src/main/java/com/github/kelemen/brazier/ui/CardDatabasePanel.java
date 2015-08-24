package com.github.kelemen.brazier.ui;

import com.github.kelemen.brazier.HearthStoneDb;
import com.github.kelemen.brazier.Keywords;
import com.github.kelemen.brazier.Player;
import com.github.kelemen.brazier.cards.CardDescr;
import com.github.kelemen.brazier.ui.jtable.FormattedTableModel;
import com.github.kelemen.brazier.ui.jtable.JTableBuilder;
import com.github.kelemen.brazier.ui.jtable.JTableUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.SingleThreadedExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.utils.ExceptionHelper;

@SuppressWarnings("serial")
public class CardDatabasePanel extends javax.swing.JPanel {
    private static final TaskExecutor BCKG_EXECUTOR = newExecutor("Card-Search");

    private final HearthStoneDb db;
    private final WorldPlayUiAgent uiAgent;
    private final FormattedTableModel<CardDescr> cardsTableModel;
    private final UpdateTaskExecutor searchExecutor;

    public CardDatabasePanel(HearthStoneDb db, WorldPlayUiAgent uiAgent) {
        ExceptionHelper.checkNotNullArgument(db, "db");
        ExceptionHelper.checkNotNullArgument(uiAgent, "uiAgent");

        this.db = db;
        this.uiAgent = uiAgent;
        this.searchExecutor = new GenericUpdateTaskExecutor(BCKG_EXECUTOR);

        initComponents();

        cardsTableModel = setupTable(jCardsTable);

        jCardPattern.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchForCards();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchForCards();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchForCards();
            }
        });
        SwingProperties.buttonSelected(jCollectibleCheck).addChangeListener(this::searchForCards);

        searchForCards();
    }

    private void searchForCards() {
        boolean collectibleOnly = jCollectibleCheck.isSelected();
        searchExecutor.execute(() -> {
            String pattern = UiUtils.getOnEdt(jCardPattern::getText);
            searchForCardsNow(pattern, collectibleOnly);
        });
    }

    private static boolean isWordChar(char ch) {
        return ch == '-' || Character.isDigit(ch) || Character.isLetter(ch);
    }

    private static void toWords(String str, Consumer<? super String> worldConsumer) {
        StringBuilder currentWord = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (isWordChar(ch)) {
                currentWord.append(ch);
            }
            else {
                if (currentWord.length() > 0) {
                    worldConsumer.accept(currentWord.toString().toLowerCase(Locale.ROOT));
                    currentWord.setLength(0);
                }
            }
        }

        if (currentWord.length() > 0) {
            worldConsumer.accept(currentWord.toString().toLowerCase(Locale.ROOT));
        }
    }

    private static boolean matchesCardWord(
            String searchPattern,
            Collection<String> cardWords) {
        for (String cardWord: cardWords) {
            if (cardWord.startsWith(searchPattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCardWords(
            Collection<String> searchPattern,
            Collection<String> cardWords) {
        for (String patternWord: searchPattern) {
            if (!matchesCardWord(patternWord, cardWords)) {
                return false;
            }
        }
        return true;
    }

    private List<CardDescr> getCardsByPattern(String pattern, boolean collectibleOnly) {
        List<CardDescr> allCards = collectibleOnly
                ? db.getCardDb().getByKeywords(Keywords.COLLECTIBLE)
                : db.getCardDb().getAll();

        String normPattern = pattern.trim();
        if (normPattern.isEmpty()) {
            return allCards;
        }

        Collection<String> patternWords = new HashSet<>();
        toWords(pattern, patternWords::add);

        List<CardDescr> result = new ArrayList<>(allCards.size());
        Collection<String> currentCardWords = new HashSet<>();
        for (CardDescr card: allCards) {
            currentCardWords.clear();

            toWords(card.getDescription(), currentCardWords::add);
            toWords(card.getDisplayName(), currentCardWords::add);
            card.getKeywords().forEach((keyword) ->{
                currentCardWords.add(keyword.getName().toLowerCase(Locale.ROOT));
            });

            if (matchesCardWords(patternWords, currentCardWords)) {
                result.add(card);
            }
        }
        return result;
    }

    private void searchForCardsNow(String pattern, boolean collectibleOnly) {
        List<CardDescr> cards = getCardsByPattern(pattern, collectibleOnly);
        SwingUtilities.invokeLater(() -> cardsTableModel.setRows(cards));
    }

    private static TaskExecutor newExecutor(String name) {
        SingleThreadedExecutor result = new SingleThreadedExecutor(name, Integer.MAX_VALUE, 5, TimeUnit.MILLISECONDS);
        result.dontNeedShutdown();
        result.setThreadFactory(new ExecutorsEx.NamedThreadFactory(true, name));
        return result;
    }

    private static FormattedTableModel<CardDescr> setupTable(JTable table) {
        JTableBuilder<CardDescr> result = new JTableBuilder<>(table);

        result.addStringColumn("Name", CardDescr::getDisplayName);
        result.addIntegerColumn("Mana cost", CardDescr::getManaCost);
        result.addStringColumn("Description", CardDescr::getDescription);

        result.setInitialSortColumnIndex(0);
        result.setSortableColumns(true);
        result.setSortsOnUpdates(true);

        return result.setupTable();
    }

    private List<CardDescr> getSelectedCards() {
        int[] selectedRows = jCardsTable.getSelectedRows();
        List<CardDescr> result = new ArrayList<>(selectedRows.length);
        for (int index: selectedRows) {
            result.add(cardsTableModel.getRow(JTableUtils.rowToModelIndex(jCardsTable, index)));
        }
        return result;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Lambda"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSearchPatternCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jCardsTable = new javax.swing.JTable();
        jAddToHandButton = new javax.swing.JButton();
        jCardPattern = new javax.swing.JTextField();
        jAddToDeckButton = new javax.swing.JButton();
        jCollectibleCheck = new javax.swing.JCheckBox();

        jSearchPatternCaption.setText("Search pattern:");

        jCardsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(jCardsTable);

        jAddToHandButton.setText("Add to hand");
        jAddToHandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddToHandButtonActionPerformed(evt);
            }
        });

        jAddToDeckButton.setText("Add to deck");
        jAddToDeckButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddToDeckButtonActionPerformed(evt);
            }
        });

        jCollectibleCheck.setSelected(true);
        jCollectibleCheck.setText("Collectible only");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 429, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jAddToDeckButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jAddToHandButton))
                    .addComponent(jCardPattern)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSearchPatternCaption)
                            .addComponent(jCollectibleCheck))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSearchPatternCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCardPattern, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCollectibleCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAddToHandButton)
                    .addComponent(jAddToDeckButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jAddToHandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddToHandButtonActionPerformed
        List<CardDescr> cards = getSelectedCards();
        for (CardDescr card: cards) {
            uiAgent.alterWorld((world) -> {
                Player player = uiAgent.getCurrentPlayer();
                return player.getHand().addCard(card);
            });
        }
    }//GEN-LAST:event_jAddToHandButtonActionPerformed

    private void jAddToDeckButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAddToDeckButtonActionPerformed
        List<CardDescr> cards = getSelectedCards();
        for (CardDescr card: cards) {
            uiAgent.alterWorld((world) -> {
                Player player = uiAgent.getCurrentPlayer();
                return player.getBoard().getDeck().putOnTop(card);
            });
        }
    }//GEN-LAST:event_jAddToDeckButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jAddToDeckButton;
    private javax.swing.JButton jAddToHandButton;
    private javax.swing.JTextField jCardPattern;
    private javax.swing.JTable jCardsTable;
    private javax.swing.JCheckBox jCollectibleCheck;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel jSearchPatternCaption;
    // End of variables declaration//GEN-END:variables
}
