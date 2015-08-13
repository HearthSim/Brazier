package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.minions.MinionId;
import com.github.kelemen.hearthstone.emulator.ui.UiUtils;
import com.github.kelemen.hearthstone.emulator.ui.WorldPlayPanel;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponId;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class HearthStoneEmulator {
    private static final Random RNG = new Random();

    private static List<CardDescr> getRandomCards(HearthStoneDb db, int count) {
        List<CardDescr> cards = db.getCardDb().getByKeywords(Keywords.COLLECTIBLE);
        List<CardDescr> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(cards.get(RNG.nextInt(cards.size())));
        }
        return result;
    }

    private static CardDescr getCard(HearthStoneDb db, String cardName) {
        return db.getCardDb().getById(new CardId(cardName));
    }

    private static Card getCard(Player player, String cardName) {
        HearthStoneDb db = player.getWorld().getDb();
        return new Card(player, getCard(db, cardName));
    }

    private static void playCard(Player player, String cardName, PlayTargetRequest playTarget) {
        Card card = getCard(player, cardName);
        player.playCard(card, 0, playTarget);
        player.getWorld().endPhase();
    }

    private static void setupInitialWorld(World world) {
        HearthStoneDb db = world.getDb();

        Player player1 = world.getPlayer1();
        player1.getHero().setCurrentHp(29);
        player1.getHero().setCurrentArmor(2);
        player1.summonMinion(db.getMinionDb().getById(new MinionId("Sludge Belcher")));
        player1.equipWeapon(db.getWeaponDb().getById(new WeaponId("Fiery War Axe")));
        player1.getHero().getAttackTool().incUseCount();

        player1.getHand().addCard(getCard(db, "Moonfire"));

        player1.getManaResource().setManaCrystals(7);
        player1.setMana(0);
        player1.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Player player2 = world.getPlayer2();
        player2.getHero().setCurrentHp(26);
        player2.getHero().setCurrentArmor(0);
        BoardSide board2 = player2.getBoard();
        player2.summonMinion(db.getMinionDb().getById(new MinionId("Grim Patron")));
        board2.getAllMinions().get(0).getBody().damage(player1.getSpellDamage(1));
        board2.getAllMinions().get(1).getBody().damage(player1.getSpellDamage(1));
        board2.getAllMinions().get(2).getBody().damage(player1.getSpellDamage(3));

        player2.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Hand hand2 = player2.getHand();
        hand2.addCard(getCard(db, "Slam"));
        hand2.addCard(getCard(db, "Fiery War Axe"));
        hand2.addCard(getCard(db, "Death's Bite"));
        hand2.addCard(getCard(db, "Frothing Berserker"));
        hand2.addCard(getCard(db, "Dread Corsair"));
        hand2.addCard(getCard(db, "Dread Corsair"));
        hand2.addCard(getCard(db, "Warsong Commander"));
        hand2.withCards((card) -> card.decreaseManaCost(1));

        player2.getBoard().getDeck().putOnTop(getCard(db, "Whirlwind"));

        world.endPhase();

        player2.startNewTurn();

        player2.getManaResource().setManaCrystals(8);
        player2.setMana(8);

        player1.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Armor Up!")));
        player2.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Armor Up!")));
    }

    private static void setupInitialWorld2(World world) {
        HearthStoneDb db = world.getDb();

        Player player1 = world.getPlayer1();
        player1.getHero().setCurrentHp(23);
        player1.getHero().setCurrentArmor(0);
        player1.summonMinion(db.getMinionDb().getById(new MinionId("Grim Patron")));

        BoardSide board1 = player1.getBoard();
        board1.getAllMinions().get(0).getBody().damage(player1.getBasicDamage(1));
        board1.getAllMinions().get(0).getBody().damage(player1.getBasicDamage(1));
        board1.getAllMinions().get(2).getBody().damage(player1.getBasicDamage(1));

        player1.getHand().addCard(getCard(db, "Moonfire"));
        player1.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Player player2 = world.getPlayer2();
        player2.getHero().setCurrentHp(26);
        player2.getHero().setCurrentArmor(0);
        player2.summonMinion(db.getMinionDb().getById(new MinionId("Grim Patron")));

        BoardSide board2 = player2.getBoard();
        board2.getAllMinions().get(0).getBody().damage(player1.getBasicDamage(1));
        board2.getAllMinions().get(1).getBody().damage(player1.getBasicDamage(2));
        board2.getAllMinions().get(2).getBody().damage(player1.getBasicDamage(1));
        board2.getAllMinions().get(3).getBody().damage(player1.getBasicDamage(2));
        board2.getAllMinions().get(4).getBody().damage(player1.getBasicDamage(3));
        player2.summonMinion(db.getMinionDb().getById(new MinionId("Treant")));

        player2.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Hand hand2 = player2.getHand();
        hand2.addCard(getCard(db, "Unstable Ghoul"));
        hand2.addCard(getCard(db, "Emperor Thaurissan"));
        hand2.addCard(getCard(db, "Execute"));
        hand2.addCard(getCard(db, "Grim Patron"));
        hand2.addCard(getCard(db, "Execute"));
        hand2.addCard(getCard(db, "Warsong Commander"));
        hand2.addCard(getCard(db, "Frothing Berserker"));

        player2.getBoard().getDeck().putOnTop(getCard(db, "Acolyte of Pain"));

        world.endPhase();
        world.setCurrentPlayerId(world.getPlayer2().getPlayerId());
        player2.startNewTurn();

        player1.getManaResource().setManaCrystals(8);
        player1.setMana(0);

        player2.getManaResource().setManaCrystals(8);
        player2.setMana(8);

        player1.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Armor Up!")));
        player2.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Armor Up!")));
    }

    private static void setupInitialWorld3(World world) {
        HearthStoneDb db = world.getDb();

        Player player1 = world.getPlayer1();
        world.setCurrentPlayerId(player1.getPlayerId());

        player1.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Reinforce")));

        player1.summonMinion(db.getMinionDb().getById(new MinionId("Tirion Fordring")));
        player1.summonMinion(db.getMinionDb().getById(new MinionId("Tirion Fordring")));
        player1.summonMinion(db.getMinionDb().getById(new MinionId("Tirion Fordring")));

        player1.getHand().addCard(getCard(db, "Pyroblast"));
        player1.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Player player2 = world.getPlayer2();
        player2.getHero().setHeroPower(db.getHeroPowerDb().getById(new CardId("Dagger Mastery")));

        playCard(player2, "The Coin",
                new PlayTargetRequest(player2.getPlayerId()));
        playCard(player2, "Defias Ringleader",
                new PlayTargetRequest(player2.getPlayerId(), 0, null));
        playCard(player2, "SI:7 Agent",
                new PlayTargetRequest(player2.getPlayerId(), 0, player2.getBoard().getAllMinions().get(0).getTargetId()));
        playCard(player2, "Ancient Mage",
                new PlayTargetRequest(player2.getPlayerId(), 1, null));
        player2.summonMinion(db.getMinionDb().getById(new MinionId("Sylvanas Windrunner")));

        player2.getBoard().getDeck().setCards(getRandomCards(db, 10));

        Hand hand2 = player2.getHand();
        hand2.addCard(getCard(db, "Shadowstep"));
        hand2.addCard(getCard(db, "Shadowstep"));
        hand2.addCard(getCard(db, "Fan of Knives"));
        hand2.addCard(getCard(db, "Deadly Poison"));
        hand2.addCard(getCard(db, "Deadly Poison"));
        hand2.addCard(getCard(db, "Blade Flurry"));
        hand2.addCard(getCard(db, "Blade Flurry"));
        hand2.addCard(getCard(db, "Elven Archer"));
        hand2.addCard(getCard(db, "Preparation"));

        player2.getBoard().getDeck().putOnTop(getCard(db, "Emperor Cobra"));
        player2.getBoard().getDeck().putOnTop(getCard(db, "Headcrack"));

        player1.getHero().setCurrentHp(30);
        player1.getHero().setCurrentArmor(0);

        player2.getHero().setCurrentHp(1);
        player2.getHero().setCurrentArmor(0);

        world.endTurn();

        player1.getManaResource().setManaCrystals(10);
        player1.setMana(0);

        player2.getManaResource().setManaCrystals(10);
        player2.setMana(10);
    }

    public static void main(String[] args) throws Throwable {
        UiUtils.useLookAndFeel("Nimbus");

        HearthStoneDb db = HearthStoneDb.readDefault();

        SwingUtilities.invokeLater(() -> {
            PlayerId player1 = new PlayerId("Player1");
            PlayerId player2 = new PlayerId("Player2");

            World world = new World(db, player1, player2);
            setupInitialWorld3(world);

            WorldPlayPanel worldPlayPanel = new WorldPlayPanel(world, player2);

            JFrame mainFrame = new JFrame("HearthStone Emulator");
            mainFrame.getContentPane().setLayout(new GridLayout(1, 1));
            mainFrame.getContentPane().add(worldPlayPanel);

            mainFrame.pack();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
            mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        });
    }
}
