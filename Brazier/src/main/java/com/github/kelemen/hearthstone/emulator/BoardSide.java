package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.actions.BattleCryArg;
import com.github.kelemen.hearthstone.emulator.actions.PlayTarget;
import com.github.kelemen.hearthstone.emulator.actions.UndoAction;
import com.github.kelemen.hearthstone.emulator.actions.UndoBuilder;
import com.github.kelemen.hearthstone.emulator.actions.UndoableAction;
import com.github.kelemen.hearthstone.emulator.minions.Minion;
import com.github.kelemen.hearthstone.emulator.minions.MinionBody;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

public final class BoardSide {
    private final Player owner;

    private final int maxSize;
    private final RefList<MinionRef> minionRefs;

    private final Deck deck;

    private final Graveyard graveyard;

    public BoardSide(Player owner, int maxSize) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");
        ExceptionHelper.checkArgumentInRange(maxSize, 0, Integer.MAX_VALUE, "maxSize");

        this.owner = owner;
        this.deck = new Deck();
        this.maxSize = maxSize;
        this.minionRefs = new RefLinkedList<>();

        this.graveyard = new Graveyard();
    }

    /**
     * Refreshes the states of the minions on the board. That is,
     * sleeping minions will be awakened and their number of attacks will reset.
     *
     * @return returns an action which undoes everything this method did, assuming
     *   the undo action is called in the same state as was before calling this
     *   {@code regresh method}. This method never returns {@code null}.
     */
    public UndoAction refresh() {
        UndoBuilder result = new UndoBuilder();
        for (MinionRef minionRef: minionRefs) {
            result.addUndo(minionRef.minion.refresh());
        }
        result.addUndo(graveyard.refresh());
        return result;
    }

    public UndoAction refreshEndOfTurn() {
        UndoBuilder result = new UndoBuilder(minionRefs.size());
        for (MinionRef minionRef: minionRefs) {
            result.addUndo(minionRef.minion.refreshEndOfTurn());
        }
        return result;
    }

    /**
     * Applies the aura effects if they are not already applied. This is
     * only needed for health auras to atomically update all health affecting
     * auras.
     * <P>
     * This method is idempotent.
     *
     * @return an action which undoes everything this method did, assuming
     *   the undo action is called in the same state as was before calling this
     *   {@code regresh method}. This method never returns {@code null}.
     */
    public UndoAction applyAuras() {
        UndoBuilder result = new UndoBuilder(minionRefs.size());
        for (MinionRef minionRef: minionRefs) {
            result.addUndo(minionRef.minion.applyAuras());
        }
        return result;
    }

    public Player getOwner() {
        return owner;
    }

    public Deck getDeck() {
        return deck;
    }

    public Graveyard getGraveyard() {
        return graveyard;
    }

    private int getReservationCount() {
        int result = 0;
        for (MinionRef minionRef: minionRefs) {
            if (minionRef.needsSpace) {
                result++;
            }
        }
        return result;
    }

    public boolean isFull() {
        return getReservationCount() >= maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean hasNonStealthTaunt() {
        return findMinion((minion) -> {
            MinionBody body = minion.getBody();
            return body.isTaunt() && !body.isStealth();
        }) != null;
    }

    public Minion findMinion(TargetId target) {
        ExceptionHelper.checkNotNullArgument(target, "target");

        return findMinion((minion) -> target.equals(minion.getTargetId()));
    }

    public Minion findMinion(Predicate<? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        for (MinionRef minionRef: minionRefs) {
            Minion minion = minionRef.tryGetVisibleMinion();
            if (minion != null && filter.test(minion)) {
                return minion;
            }
        }
        return null;
    }

    private static boolean filterAliveMinion(Minion minion) {
        return !minion.isDead() && !minion.isScheduledToDestroy();
    }

    public void collectAliveMinions(List<? super Minion> result) {
        collectMinions(result, BoardSide::filterAliveMinion);
    }

    public void collectAliveMinions(List<? super Minion> result, Predicate<? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");
        collectMinions(result, (minion) -> filter.test(minion) && filterAliveMinion(minion));
    }

    public void collectMinions(List<? super Minion> result) {
        collectMinions(result, (minion) -> true);
    }

    public void collectMinions(List<? super Minion> result, Predicate<? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        for (MinionRef minionRef: minionRefs) {
            Minion minion = minionRef.tryGetVisibleMinion();
            if (minion != null && filter.test(minion)) {
                result.add(minion);
            }
        }
    }

    public UndoAction forAllMinions(Function<? super Minion, ? extends UndoAction> action) {
        ExceptionHelper.checkNotNullArgument(action, "action");

        int reservationCount = minionRefs.size();
        if (reservationCount == 0) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder(reservationCount);
        boolean applied = false;
        for (MinionRef minionRef: minionRefs) {
            Minion minion = minionRef.tryGetVisibleMinion();
            if (minion != null) {
                applied = true;
                result.addUndo(action.apply(minion));
            }
        }
        return applied ? result : UndoAction.DO_NOTHING;
    }

    public int countMinions(Predicate<? super Minion> filter) {
        ExceptionHelper.checkNotNullArgument(filter, "filter");

        int result = 0;
        for (MinionRef minionRef: minionRefs) {
            Minion minion = minionRef.tryGetVisibleMinion();
            if (minion != null && filter.test(minion)) {
                result++;
            }
        }
        return result;
    }

    public int getMinionCount() {
        return getReservationCount();
    }

    public List<Minion> getAliveMinions() {
        List<Minion> result = new ArrayList<>(minionRefs.size());
        collectAliveMinions(result);
        return result;
    }

    public List<Minion> getAllMinions() {
        List<Minion> result = new ArrayList<>(minionRefs.size());
        collectMinions(result);
        return result;
    }

    private int toBounds(int boardIndex) {
        if (boardIndex < 0) return 0;
        else if (boardIndex > minionRefs.size()) return minionRefs.size();
        else return boardIndex;
    }

    private UndoableResult<BoardReservationRef> tryReserveBoardFor(
            MinionDescr minion,
            Function<MinionRef, RefList.ElementRef<MinionRef>> addToList) {
        return tryReserveBoardFor(new Minion(owner, minion), addToList);
    }

    private UndoableResult<BoardReservationRef> tryReserveBoardFor(
            Minion minion,
            Function<MinionRef, RefList.ElementRef<MinionRef>> addToList) {

        if (isFull()) {
            return null;
        }

        MinionRef result = new MinionRef(minion);

        UndoAction setLocationRefUndo = minion.setLocationRef(result);

        RefList.ElementRef<MinionRef> listRef = addToList.apply(result);
        result.setReference(listRef);

        return new UndoableResult<>(result, () -> {
            result.getElementReference().remove();
            setLocationRefUndo.undo();
        });
    }

    private MinionRef findMinionRef(TargetId minionId) {
        ExceptionHelper.checkNotNullArgument(minionId, "minionId");
        for (RefList.ElementRef<MinionRef> ref = minionRefs.getFirstReference(); ref != null; ref = ref.getNext(1)) {
            MinionRef candidate = ref.getElement();
            if (candidate != null && minionId.equals(candidate.minion.getTargetId())) {
                return candidate;
            }
        }
        return null;
    }

    private World getWorld() {
        return getOwner().getWorld();
    }

    private static void removeFromBoard(Minion minion, UndoBuilder result) {
        Player owner = minion.getOwner();
        MinionRef prevRef = owner.getBoard().findMinionRef(minion.getTargetId());
        if (prevRef != null) {
            result.addUndo(prevRef.removeFromBoardList());
        }
    }

    public UndoAction takeOwnership(Minion minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");

        if (minion.isDestroyed()) {
            return UndoAction.DO_NOTHING;
        }

        // We must not check if it is already on our board because
        // in this case, HearthStone still tries to put the minion back.
        // Which might result in the death of the minion due to full board.

        if (isFull()) {
            return minion.poison();
        }

        UndoBuilder result = new UndoBuilder();

        removeFromBoard(minion, result);

        Player prevOwner = minion.getOwner();
        minion.setOwner(getOwner());
        result.addUndo(() -> minion.setOwner(prevOwner));

        UndoableResult<BoardReservationRef> boardPos = tryReservePosition(minion);
        // This shouldn't happen since we already checked that there is room to put the minion
        // but do the check anyway.
        if (boardPos == null) {
            return minion.poison();
        }
        result.addUndo(boardPos.getUndoAction());

        result.addUndo(minion.refresh());
        result.addUndo(minion.exhaust());

        result.addUndo(summonMinion(boardPos.getResult()));

        return result;
    }

    public UndoAction summonMinion(BoardReservationRef reservationRef) {
        ExceptionHelper.checkNotNullArgument(reservationRef, "reservationRef");
        return summonMinionUnsafe(reservationRef, null);
    }

    public UndoAction summonMinion(
            BoardReservationRef reservationRef,
            PlayTarget battleCryTarget) {
        ExceptionHelper.checkNotNullArgument(reservationRef, "reservationRef");
        ExceptionHelper.checkNotNullArgument(battleCryTarget, "battleCryTarget");
        return summonMinionUnsafe(reservationRef, battleCryTarget);
    }

    private UndoAction summonMinionUnsafe(
            BoardReservationRef reservationRef,
            PlayTarget battleCryTarget) {

        Minion minion = reservationRef.getMinion();

        World world = getWorld();
        WorldEvents events = world.getEvents();

        UndoBuilder result = new UndoBuilder();

        CompletableWorldActionEvents<Minion> summoningListeners = events.summoningListeners();
        UndoableResult<UndoableAction> summoningFinalizer = summoningListeners.triggerEvent(minion);
        result.addUndo(summoningFinalizer.getUndoAction());

        if (battleCryTarget != null) {
            BattleCryArg battleCryArg = new BattleCryArg(minion, battleCryTarget);
            result.addUndo(minion.getBaseDescr().executeBattleCriesNow(owner, battleCryArg));
        }
        result.addUndo(reservationRef.showMinion());

        result.addUndo(summoningFinalizer.getResult().doAction());

        return result;
    }

    public UndoableResult<BoardReservationRef> tryReservePosition(Minion minion, int index) {
        return tryReserveBoardFor(minion, (element) -> {
            return minionRefs.addGetReference(toBounds(index), element);
        });
    }

    public UndoableResult<BoardReservationRef> tryReservePosition(MinionDescr minionDescr, int index) {
        return tryReservePosition(new Minion(owner, minionDescr), index);
    }

    public UndoableResult<BoardReservationRef> tryReservePosition(Minion minion) {
        ExceptionHelper.checkNotNullArgument(minion, "minion");
        return tryReserveBoardFor(minion, minionRefs::addLastGetReference);
    }

    public UndoableResult<BoardReservationRef> tryReservePosition(MinionDescr minionDescr) {
        ExceptionHelper.checkNotNullArgument(minionDescr, "minionDescr");

        return tryReserveBoardFor(minionDescr, minionRefs::addLastGetReference);
    }

    private final class MinionRef
    implements
            BoardReservationRef, SummonLocationRef {
        private Minion minion;
        private boolean needsSpace;
        private final AtomicBoolean visible;

        private RefList.ElementRef<MinionRef> listRef;

        public MinionRef(Minion minion) {
            assert minion != null;

            this.minion = minion;
            this.needsSpace = true;
            this.visible = new AtomicBoolean(false);
        }

        public void setReference(RefList.ElementRef<MinionRef> newRef) {
            assert newRef.getElement() == this;
            this.listRef = newRef;
        }

        public RefList.ElementRef<MinionRef> getElementReference() {
            return listRef;
        }

        public UndoableResult<BoardReservationRef> tryReserveLeft(Minion newMinion) {
            return tryReserveBoardFor(newMinion, listRef::addBefore);
        }

        public UndoableResult<BoardReservationRef> tryReserveRight(Minion newMinion) {
            return tryReserveBoardFor(newMinion, listRef::addAfter);
        }

        private UndoAction summonSide(
                Minion summonedMinion,
                Function<MinionRef, RefList.ElementRef<MinionRef>> addToList) {

            UndoableResult<BoardReservationRef> reservationRef = tryReserveBoardFor(summonedMinion, addToList);
            if (reservationRef == null) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction summonUndo = summonMinion(reservationRef.getResult());
            return () -> {
                summonUndo.undo();
                reservationRef.undo();
            };
        }

        @Override
        public UndoAction replace(Minion summonedMinion) {
            boolean prevVisible = visible.getAndSet(false);
            boolean prevNeedsSpace = needsSpace;
            needsSpace = false;

            UndoableResult<BoardReservationRef> boardReservation = tryReserveBoardFor(summonedMinion, listRef::addAfter);

            UndoAction destroyUndo = minion.completeKillAndDeactivate(false);
            UndoAction removeUndo = removeFromBoardList();

            UndoAction summonUndo = boardReservation != null
                    ? summonMinion(boardReservation.getResult())
                    : UndoAction.DO_NOTHING;

            return () -> {
                summonUndo.undo();
                removeUndo.undo();
                destroyUndo.undo();
                if (boardReservation != null) {
                    boardReservation.undo();
                }

                needsSpace = prevNeedsSpace;
                visible.set(prevVisible);
            };
        }

        @Override
        public UndoAction summonLeft(Minion summonedMinion) {
            return summonSide(summonedMinion, listRef::addBefore);
        }

        @Override
        public UndoAction summonRight(Minion summonedMinion) {
            return summonSide(summonedMinion, listRef::addAfter);
        }

        public Minion tryGetVisibleMinion() {
            return visible.get() ? minion : null;
        }

        @Override
        public UndoAction showMinion() {
            if (minion.isDestroyed()) {
                return UndoAction.DO_NOTHING;
            }

            if (!visible.compareAndSet(false, true)) {
                return UndoAction.DO_NOTHING;
            }

            UndoAction activateUndo = minion.activatePassiveAbilities();
            return () -> {
                activateUndo.undo();
                visible.set(false);
            };
        }

        @Override
        public Minion getMinion() {
            return minion;
        }

        @Override
        public BoardLocationRef tryGetLeft() {
            RefList.ElementRef<MinionRef> prevRef = getElementReference().getPrevious(1);
            return prevRef != null ? prevRef.getElement() : null;
        }

        @Override
        public BoardLocationRef tryGetRight() {
            RefList.ElementRef<MinionRef> nextRef = getElementReference().getNext(1);
            return nextRef != null ? nextRef.getElement() : null;
        }

        @Override
        public UndoAction removeFromBoard() {
            return destroy(false);
        }

        @Override
        public UndoAction destroy() {
            return destroy(true);
        }

        @Override
        public boolean isOnBoard() {
            return !listRef.isRemoved();
        }

        private UndoAction removeFromBoardList() {
            if (listRef.isRemoved()) {
                return UndoAction.DO_NOTHING;
            }

            int index = listRef.getIndex();
            listRef.remove();
            return () -> setReference(minionRefs.addGetReference(index, this));
        }

        public UndoAction destroy(boolean triggerKill) {
            boolean prevVisible = visible.getAndSet(false);
            boolean prevNeedsSpace = needsSpace;
            needsSpace = false;

            UndoAction destroyUndo = minion.completeKillAndDeactivate(triggerKill);
            UndoAction removeUndo = removeFromBoardList();

            return () -> {
                removeUndo.undo();
                destroyUndo.undo();

                needsSpace = prevNeedsSpace;
                visible.set(prevVisible);
            };
        }
    }
}
