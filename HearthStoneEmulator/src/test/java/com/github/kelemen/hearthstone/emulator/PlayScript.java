package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.PlayTargetRequest;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.cards.Card;
import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardId;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.parsing.TestDb;
import com.github.kelemen.hearthstone.emulator.weapons.Weapon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.*;

public final class PlayScript {
    private static final PlayerId PLAYER1_ID = new PlayerId("Player1");
    private static final PlayerId PLAYER2_ID = new PlayerId("Player2");

    private final HearthStoneDb db;

    private final List<ScriptAction> script;

    private PlayScript() {
        this.db = TestDb.getTestDb();
        this.script = new LinkedList<>();
    }

    public static void testScript(Consumer<PlayScript> scriptConfig) {
        PlayScript script = new PlayScript();
        scriptConfig.accept(script);
        script.executeScript();
    }

    private void addScriptAction(Function<State, UndoAction> scriptAction) {
        addScriptAction(false, scriptAction);
    }

    private void addScriptAction(boolean expectationCheck, Function<State, UndoAction> scriptAction) {
        script.add(new ScriptAction(expectationCheck, new Exception("Script config line"), scriptAction));
    }

    private static PlayerId parsePlayerName(String name) {
        if (name.equalsIgnoreCase("p1") || name.equalsIgnoreCase(PLAYER1_ID.getName())) {
            return PLAYER1_ID;
        }
        else if (name.equalsIgnoreCase("p2") || name.equalsIgnoreCase(PLAYER2_ID.getName())) {
            return PLAYER2_ID;
        }
        throw new IllegalArgumentException("Illegal player name: " + name);
    }

    public void adjustPlayer(String playerName, Function<? super Player, ? extends UndoAction> action) {
        PlayerId playerId = parsePlayerName(playerName);
        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);
            return action.apply(player);
        });
    }

    public void expectPlayer(String playerName, Consumer<? super Player> check) {
        PlayerId playerId = parsePlayerName(playerName);
        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            check.accept(player);
        });
    }

    public void expectMinion(String target, Consumer<? super Minion> check) {
        expect((state) -> {
            TargetableCharacter foundTarget = state.findTarget(target);
            assertNotNull("Minion", foundTarget);
            assertTrue("Minion", foundTarget instanceof Minion);

            check.accept((Minion)foundTarget);
        });
    }

    public void setCurrentPlayer(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);
        addScriptAction((state) -> {
            return state.playAgent.setCurrentPlayerId(playerId);
        });
    }

    public void endTurn() {
        addScriptAction((state) -> {
            return state.playAgent.endTurn();
        });
    }

    public void playMinionCard(String playerName, int cardIndex, int minionPos) {
        playMinionCard(playerName, cardIndex, minionPos, "");
    }

    public void playCard(String playerName, int cardIndex, String target) {
        playNonMinionCard(playerName, cardIndex, -1, target);
    }

    public void playCard(String playerName, int cardIndex) {
        playNonMinionCard(playerName, cardIndex, -1, "");
    }

    public void playMinionCard(String playerName, int cardIndex, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);

        if (minionPos < 0) {
            throw new IllegalArgumentException("Need minion drop location.");
        }

        playCard(playerId, cardIndex, minionPos, target);
    }

    private void playNonMinionCard(String playerName, int cardIndex, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);
        playCard(playerId, cardIndex, minionPos, target);
    }

    private void playCard(PlayerId playerId, int cardIndex, int minionPos, String target) {
        expectGameContinues();
        addScriptAction((state) -> {
            DeathResolutionResult playResolution
                    = state.playAgent.playCard(cardIndex, state.toPlayTarget(playerId, minionPos, target));
            UndoAction resolutionSetUndo = state.setPlayResolution(playResolution);

            return () -> {
                resolutionSetUndo.undo();
                playResolution.undo();
            };
        });
    }

    public void playMinionCard(String playerName, String cardName, int minionPos) {
        playMinionCard(playerName, cardName, minionPos, "");
    }

    public void playCard(String playerName, String cardName, String target) {
        playNonMinionCard(playerName, cardName, -1, target);
    }

    public void playCard(String playerName, String cardName) {
        playNonMinionCard(playerName, cardName, -1, "");
    }

    public void playMinionCard(String playerName, String cardName, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);

        if (minionPos < 0) {
            throw new IllegalArgumentException("Need minion drop location.");
        }

        playCard(playerId, cardName, minionPos, target);
    }

    private void playNonMinionCard(String playerName, String cardName, int minionPos, String target) {
        PlayerId playerId = parsePlayerName(playerName);
        playCard(playerId, cardName, minionPos, target);
    }

    private void playCard(PlayerId playerId, String cardName, int minionPos, String target) {
        CardDescr cardDescr = db.getCardDb().getById(new CardId(cardName));

        expectGameContinues();
        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);
            Hand hand = player.getHand();

            UndoAction addCardUndo = hand.addCard(cardDescr);
            int cardIndex = hand.getCardCount() - 1;

            DeathResolutionResult playResolution
                    = state.playAgent.playCard(cardIndex, state.toPlayTarget(playerId, minionPos, target));
            UndoAction resolutionSetUndo = state.setPlayResolution(playResolution);

            return () -> {
                resolutionSetUndo.undo();
                playResolution.undo();
                addCardUndo.undo();
            };
        });
    }

    public void expectGameContinues() {
        expect((state) -> {
            DeathResolutionResult lastPlayResolution = state.getLastPlayResolution();
            if (lastPlayResolution.isGameOver()) {
                fail("Unexpected game over: " + lastPlayResolution.getDeadPlayers());
            }
        });
    }

    public void expectHeroDeath(String... expectedDeadPlayerNames) {
        Set<PlayerId> expectedDeadPlayerIds = new HashSet<>();
        for (String playerName: expectedDeadPlayerNames) {
            expectedDeadPlayerIds.add(parsePlayerName(playerName));
        }

        expect((state) -> {
            DeathResolutionResult lastPlayResolution = state.getLastPlayResolution();
            if (!lastPlayResolution.isGameOver()) {
                fail("Expected game over.");
            }

            Set<PlayerId> deadPlayerIds = new HashSet<>(lastPlayResolution.getDeadPlayers());
            assertEquals("Dead players", expectedDeadPlayerIds, deadPlayerIds);
        });
    }

    private List<CardDescr> parseCards(String... cardNames) {
        List<CardDescr> result = new ArrayList<>(cardNames.length);
        for (String cardName: cardNames) {
            result.add(db.getCardDb().getById(new CardId(cardName)));
        }
        return result;
    }

    public void deck(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> newCards = parseCards(cardNames);

        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);
            Deck deck = player.getBoard().getDeck();

            return deck.setCards(newCards);
        });
    }

    public void decreaseManaCostOfHand(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);
        addScriptAction((state) -> {
            Hand hand = state.world.getPlayer(playerId).getHand();
            return hand.withCards((card) -> card.decreaseManaCost(1));
        });
    }

    public void addToHand(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> newCards = parseCards(cardNames);

        addScriptAction((state) -> {
            Hand hand = state.world.getPlayer(playerId).getHand();
            List<UndoAction> result = new LinkedList<>();
            for (CardDescr card: newCards) {
                result.add(0, hand.addCard(card));
            }

            return () -> result.forEach(UndoAction::undo);
        });
    }

    public void expectDeck(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> expectedCards = parseCards(cardNames);

        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            Deck deck = player.getBoard().getDeck();

            List<CardDescr> deckCards = deck.getCards();
            assertEquals("deck", expectedCards, deckCards);
        });
    }

    private void unexpectedSecrets(String[] expectedNames, List<Secret> actual) {
        List<String> actualNames = new ArrayList<>(actual.size());
        actual.forEach((secret) -> actualNames.add(secret.getSecretId().getName()));
        fail("The actual secrets are different than what was expected."
                + " Expected: " + Arrays.toString(expectedNames)
                + ". Actual: " + actualNames);
    }

    public void expectSecret(String playerName, String... secretNames) {
        PlayerId playerId = parsePlayerName(playerName);
        String[] secretNamesCopy = secretNames.clone();

        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            List<Secret> secrets = player.getSecrets().getSecrets();

            if (secrets.size() != secretNamesCopy.length) {
                unexpectedSecrets(secretNames, secrets);
            }

            for (int i = 0; i < secretNamesCopy.length; i++) {
                if (!secretNamesCopy[i].equals(secrets.get(i).getSecretId().getName())) {
                    unexpectedSecrets(secretNames, secrets);
                }
            }
        });
    }

    public void expectHand(String playerName, String... cardNames) {
        PlayerId playerId = parsePlayerName(playerName);
        List<CardDescr> expectedCards = parseCards(cardNames);

        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            Hand hand = player.getHand();

            List<Card> handCards = hand.getCards();
            List<CardDescr> handCardDescrs = new ArrayList<>(handCards.size());
            handCards.forEach((card) -> handCardDescrs.add(card.getCardDescr()));

            assertEquals("hand", expectedCards, handCardDescrs);
        });
    }

    public void setHeroHp(String playerName, int hp, int armor) {
        PlayerId playerId = parsePlayerName(playerName);
        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);
            Hero hero = player.getHero();

            int prevHp = hero.getCurrentHp();
            int prerArmo = hero.getCurrentArmor();

            hero.setCurrentArmor(armor);
            hero.setCurrentHp(hp);

            return () -> {
                hero.setCurrentHp(prevHp);
                hero.setCurrentArmor(prerArmo);
            };
        });
    }

    public void setMana(String playerName, int mana) {
        PlayerId playerId = parsePlayerName(playerName);
        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);

            int prevMana = player.getMana();
            player.setMana(mana);

            return () -> player.setMana(prevMana);
        });
    }

    public void expectedMana(String playerName, int expectedMana) {
        PlayerId playerId = parsePlayerName(playerName);
        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            assertEquals(expectedMana, player.getMana());
        });
    }

    private void expect(Consumer<State> check) {
        addScriptAction(true, (state) -> {
            check.accept(state);
            return () -> check.accept(state);
        });
    }

    public void expectHeroHp(String playerName, int expectedHp, int expectedArmor) {
        PlayerId playerId = parsePlayerName(playerName);
        expect((state) -> {
            Hero hero = state.world.getPlayer(playerId).getHero();
            assertEquals("hp", expectedHp, hero.getCurrentHp());
            assertEquals("armor", expectedArmor, hero.getCurrentArmor());
        });
    }

    public void expectNoWeapon(String playerName, int attack) {
        PlayerId playerId = parsePlayerName(playerName);
        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            Hero hero = player.getHero();
            assertNull("weapon", player.tryGetWeapon());
            assertEquals("attack", attack, hero.getAttackTool().getAttack());
        });
    }

    public void expectWeapon(String playerName, int expectedAttack, int expectedCharges) {
        PlayerId playerId = parsePlayerName(playerName);
        expect((state) -> {
            Player player = state.world.getPlayer(playerId);
            Hero hero = player.getHero();
            Weapon weapon = player.tryGetWeapon();
            assertNotNull("weapon", weapon);

            assertEquals("attack", expectedAttack, hero.getAttackTool().getAttack());
            assertEquals("charges", expectedCharges, weapon.getCharges());
        });
    }

    public void refreshAttacks() {
        refreshAttack("p1");
        refreshAttack("p2");
    }

    public void refreshAttack(String playerName) {
        PlayerId playerId = parsePlayerName(playerName);

        addScriptAction((state) -> {
            Player player = state.world.getPlayer(playerId);
            UndoAction refreshHeroUndo = player.getHero().refresh();
            UndoAction refreshBoardUndo = player.getBoard().refresh();
            return () -> {
                refreshBoardUndo.undo();
                refreshHeroUndo.undo();
            };
        });
    }

    public void addRoll(int possibilityCount, int rollResult) {
        addScriptAction((state) -> {
            return state.randomProvider.addRoll(possibilityCount, rollResult);
        });
    }

    public void addCardChoice(int choiceIndex, String... cardNames) {
        ExceptionHelper.checkArgumentInRange(choiceIndex, 0, cardNames.length - 1, "choiceIndex");

        String[] cardNamesCopy = cardNames.clone();
        ExceptionHelper.checkNotNullElements(cardNamesCopy, "cardNames");

        addScriptAction((state) -> {
            return state.userAgent.addChoice(choiceIndex, cardNamesCopy);
        });
    }

    public void expectBoard(String playerName, MinionExpectations... minionDescrs) {
        PlayerId playerId = parsePlayerName(playerName);

        MinionExpectations[] minionDescrsCopy = minionDescrs.clone();
        ExceptionHelper.checkNotNullElements(minionDescrsCopy, "minionDescrsCopy");

        expect((state) -> {
            BoardSide board = state.world.getPlayer(playerId).getBoard();
            List<Minion> minions = board.getAllMinions();

            if (minions.size() != minionDescrsCopy.length) {
                fail("The size of the board is different than expected."
                        + " Expected board: " + Arrays.toString(minionDescrsCopy) + ". Actual board: " + minions);
            }

            int index = 0;
            for (Minion minion: minions) {
                int minionIndex = index;
                minionDescrsCopy[index].verifyExpectations(minion, () -> "Index: " + minionIndex + ".");
                index++;
            }
        });
    }

    public void attack(String attacker, String target) {
        addScriptAction((state) -> {
            TargetId attackerId = state.findTargetId(attacker);
            TargetId targetId = state.findTargetId(target);

            DeathResolutionResult playResolution
                    = state.playAgent.attack(attackerId, targetId);
            UndoAction setStateUndo = state.setPlayResolution(playResolution);

            return () -> {
                setStateUndo.undo();
                playResolution.undo();
            };
        });
    }

    private void executeScript() {
        for (boolean changePlayers: new boolean[]{false, true}) {
            executeScript(changePlayers);
        }
    }

    private void executeScript(boolean changePlayers) {
        State state = new State(changePlayers, db);

        executeAllScriptActions(state);

        if (!state.randomProvider.rolls.isEmpty()) {
            throw new AssertionError("There were unnecessary rolls defined: " + state.randomProvider.rolls);
        }

        if (!state.userAgent.choices.isEmpty()) {
            throw new AssertionError("There were unnecessary card choices defined: " + state.userAgent.choices);
        }

        int scriptSize = script.size();
        for (int index1 = 0; index1 < scriptSize - 1; index1++) {
            if (script.get(index1).expectationCheck) {
                continue;
            }

            for (int index2 = index1 + 1; index2 <= scriptSize; index2++) {
                if (script.get(index2 - 1).expectationCheck) {
                    continue;
                }

                try {
                    executeScriptWithUndoTest(changePlayers, index1, index2);
                } catch (Throwable ex) {
                    ex.addSuppressed(new Exception("Undoing after", script.get(index1).stackTrace));
                    ex.addSuppressed(new Exception("Undoing before", script.get(index2 - 1).stackTrace));
                    throw ex;
                }
            }
        }
    }

    private void executeScriptWithUndoTest(boolean changePlayers, int index1, int index2) {
        State state = new State(changePlayers, db);

        List<ScriptAction> currentScript = new ArrayList<>(script);
        int scriptLength = currentScript.size();

        ExceptionHelper.checkArgumentInRange(index1, 0, index2 - 1, "index1");
        ExceptionHelper.checkArgumentInRange(index2, index1, scriptLength, "index2");

        List<UndoableResult<Throwable>> undos = new LinkedList<>();
        for (int i = 0; i < index2; i++) {
            ScriptAction action = currentScript.get(i);
            UndoAction undo;
            try {
                undo = action.doAction(state);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }

            if (i >= index1) {
                undos.add(0, new UndoableResult<>(action.stackTrace, undo));
            }
        }

        for (UndoableResult<Throwable> undo: undos) {
            try {
                undo.undo();
            } catch (Throwable ex) {
                AssertionError undoError = new AssertionError("Undoing action failed.", ex);
                undoError.addSuppressed(undo.getResult());
                throw undoError;
            }
        }

        for (int i = index1; i < scriptLength; i++) {
            ScriptAction action = currentScript.get(i);
            try {
                action.doAction(state);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }
        }
    }

    private void executeAllScriptActions(State state) {
        List<ScriptAction> currentScript = new ArrayList<>(script);

        for (ScriptAction action: currentScript) {
            try {
                action.doAction(state);
            } catch (Throwable ex) {
                ex.addSuppressed(action.stackTrace);
                throw ex;
            }
        }
    }

    private static final class CardChoiceDef {
        public final int choiceIndex;
        public final String[] cardNames;

        public CardChoiceDef(int choiceIndex, String[] cardNames) {
            this.choiceIndex = choiceIndex;
            this.cardNames = cardNames;
        }

        private void failExpectations(List<? extends CardDescr> cards) {
            throw new AssertionError("Unexpected card choices: " + cards
                        + ". Expected: " + Arrays.toString(cardNames));
        }

        public void assertSameCards(List<? extends CardDescr> cards) {
            if (cards.size() != cardNames.length) {
                failExpectations(cards);
            }

            int index = 0;
            for (CardDescr card: cards) {
                if (!Objects.equals(cardNames[index], card.getId().getName())) {
                    failExpectations(cards);
                }
                index++;
            }
        }

        public CardDescr getChoice(HearthStoneDb db) {
            return db.getCardDb().getById(new CardId(cardNames[choiceIndex]));
        }

        @Override
        public String toString() {
            return "CardChoiceDef{" + "choice=" + choiceIndex + ", cards=" + Arrays.toString(cardNames) + '}';
        }
    }

    private static final class RollDef {
        public final int possibilityCount;
        public final int rollResult;

        public RollDef(int possibilityCount, int rollResult) {
            ExceptionHelper.checkArgumentInRange(rollResult, 0, possibilityCount - 1, "rollResult");

            this.possibilityCount = possibilityCount;
            this.rollResult = rollResult;
        }

        @Override
        public String toString() {
            return "Roll{" + "possibilityCount=" + possibilityCount + ", rollResult=" + rollResult + '}';
        }
    }

    private static final class ScriptedRandomProvider implements RandomProvider {
        private final Deque<RollDef> rolls;
        private Deque<RollDef> rollRecorder;

        public ScriptedRandomProvider() {
            this.rolls = new LinkedList<>();
        }

        public UndoAction addRoll(int possibilityCount, int rollResult) {
            rolls.addLast(new RollDef(possibilityCount, rollResult));
            return () -> rolls.removeLast();
        }

        public void startRollRecording(Deque<RollDef> rolls) {
            if (rollRecorder != null) {
                throw new IllegalStateException("Nested roll recording.");
            }
            rollRecorder = rolls;
        }

        public void stopRollRecording() {
            rollRecorder = null;
        }

        public void addRecordedRolls(Deque<RollDef> recordedRolls) {
            for (RollDef roll: recordedRolls) {
                rolls.addFirst(roll);
            }
        }

        @Override
        public int roll(int bound) {
            RollDef roll = rolls.pollFirst();
            if (roll == null) {
                throw new AssertionError("Unexpected random roll: " + bound);
            }

            if (roll.possibilityCount != bound) {
                throw new AssertionError("Unexpected possibility count for random roll: " + bound
                        + ". Expected: " + roll.possibilityCount);
            }

            if (rollRecorder != null) {
                rollRecorder.addFirst(roll);
            }

            return roll.rollResult;
        }
    }

    private static final class ScriptedUserAgent implements UserAgent {
        private final HearthStoneDb db;
        private final Deque<CardChoiceDef> choices;
        private Deque<CardChoiceDef> choiceRecorder;

        public ScriptedUserAgent(HearthStoneDb db) {
            assert db != null;

            this.db = db;
            this.choices = new LinkedList<>();
        }

        public UndoAction addChoice(int choiceIndex, String[] cardNames) {
            choices.addLast(new CardChoiceDef(choiceIndex, cardNames));
            return () -> choices.removeLast();
        }

        public void startRollRecording(Deque<CardChoiceDef> choices) {
            if (choiceRecorder != null) {
                throw new IllegalStateException("Nested card choice recording.");
            }
            choiceRecorder = choices;
        }

        public void stopRollRecording() {
            choiceRecorder = null;
        }

        public void addRecordedChoices(Deque<CardChoiceDef> recordedChoices) {
            for (CardChoiceDef choice: recordedChoices) {
                choices.addFirst(choice);
            }
        }

        @Override
        public CardDescr selectCard(boolean allowCancel, List<? extends CardDescr> cards) {
            CardChoiceDef choice = choices.pollFirst();
            if (choice == null) {
                throw new AssertionError("Unexpected card choose request: " + cards);
            }
            choice.assertSameCards(cards);

            if (choiceRecorder != null) {
                choiceRecorder.addFirst(choice);
            }

            return choice.getChoice(db);
        }
    }

    private static final class State {
        private final ScriptedUserAgent userAgent;
        private final ScriptedRandomProvider randomProvider;
        private final WorldPlayAgent playAgent;
        private final World world;
        private final AtomicReference<DeathResolutionResult> lastPlayResolution;

        public State(boolean changePlayers, HearthStoneDb db) {
            this.randomProvider = new ScriptedRandomProvider();
            this.userAgent = new ScriptedUserAgent(db);
            this.world = changePlayers
                    ? new World(db, PLAYER2_ID, PLAYER1_ID)
                    : new World(db, PLAYER1_ID, PLAYER2_ID);
            this.world.setRandomProvider(randomProvider);
            this.world.setUserAgent(userAgent);
            this.playAgent = new WorldPlayAgent(world);
            this.lastPlayResolution = new AtomicReference<>(DeathResolutionResult.NO_DEATHS);
        }

        public DeathResolutionResult getLastPlayResolution() {
            return lastPlayResolution.get();
        }

        public UndoAction setPlayResolution(DeathResolutionResult newResult) {
            ExceptionHelper.checkNotNullArgument(newResult, "newResult");

            DeathResolutionResult prevResult = lastPlayResolution.getAndSet(newResult);
            return () -> lastPlayResolution.set(prevResult);
        }

        public TargetableCharacter findTarget(String targetId) {
            if (targetId.trim().isEmpty()) {
                return null;
            }

            String[] targetIdParts = targetId.split(":");
            if (targetIdParts.length < 2) {
                throw new IllegalArgumentException("Illegal target ID: " + targetId);
            }

            Player player = world.getPlayer(parsePlayerName(targetIdParts[0]));
            String targetName = targetIdParts[1].trim();
            if (targetName.equalsIgnoreCase("hero")) {
                return player.getHero();
            }

            int minionIndex = Integer.parseInt(targetName);
            Minion minion = player.getBoard().getAllMinions().get(minionIndex);
            return minion;
        }

        public TargetId findTargetId(String targetId) {
            TargetableCharacter target = findTarget(targetId);
            return target != null ? target.getTargetId() : null;
        }

        public PlayTargetRequest toPlayTarget(PlayerId player, int minionPos, String targetId) {
            return new PlayTargetRequest(player, minionPos, findTargetId(targetId));
        }
    }

    private static final class ScriptAction {
        private final Exception stackTrace;
        private final boolean expectationCheck;
        private final Function<State, UndoAction> action;

        public ScriptAction(boolean expectationCheck, Exception stackTrace, Function<State, UndoAction> action) {
            this.expectationCheck = expectationCheck;
            this.stackTrace = stackTrace;
            this.action = action;
        }

        public UndoAction doAction(State state) {
            Deque<RollDef> recordedRolls = new LinkedList<>();
            Deque<CardChoiceDef> recodedChoices = new LinkedList<>();

            state.userAgent.startRollRecording(recodedChoices);
            state.randomProvider.startRollRecording(recordedRolls);
            UndoAction actionUndo = action.apply(state);
            state.randomProvider.stopRollRecording();
            state.userAgent.stopRollRecording();

            return () -> {
                state.userAgent.startRollRecording(recodedChoices);
                state.randomProvider.startRollRecording(recordedRolls);
                actionUndo.undo();
                state.randomProvider.addRecordedRolls(recordedRolls);
                state.randomProvider.stopRollRecording();
                state.userAgent.addRecordedChoices(recodedChoices);
                state.userAgent.stopRollRecording();
            };
        }
    }
}
