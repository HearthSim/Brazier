package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class AuraAwarePropertyBase<T> implements Silencable {
    private final BuffDef<T> buffDef;
    private final T combinedView;
    private final List<BuffRef<T>> buffRefs;

    public AuraAwarePropertyBase(BuffDef<T> buffDef) {
        this.buffDef = buffDef;
        this.buffRefs = new ArrayList<>();
        this.combinedView = buffDef.viewCombinedBuffs(Collections.unmodifiableList(this.buffRefs));
    }

    private AuraAwarePropertyBase(AuraAwarePropertyBase<T> other) {
        this.buffDef = other.buffDef;
        this.buffRefs = new ArrayList<>(other.buffRefs.size());
        this.combinedView = other.buffDef.viewCombinedBuffs(Collections.unmodifiableList(this.buffRefs));
        for (BuffRef<T> buffRef: other.buffRefs) {
            if (!buffRef.external) {
                this.buffRefs.add(buffRef);
            }
        }
    }

    public UndoableUnregisterRef addRemovableBuff(BuffArg buffArg, T toAdd) {
        int priority = buffArg.getPriority();
        boolean external = buffArg.isExternal();

        int buffPos = findInsertPos(priority);

        BuffRef<T> buffRef = new BuffRef<>(priority, external, toAdd);
        buffRefs.add(buffPos, buffRef);
        return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                for (int i = buffRefs.size() - 1; i >= 0; i--) {
                    BuffRef<T> candidate = buffRefs.get(i);
                    if (candidate == buffRef) {
                        int candidateIndex = i;
                        buffRefs.remove(candidateIndex);
                        return () -> buffRefs.add(candidateIndex, candidate);
                    }
                }
                return UndoAction.DO_NOTHING;
            }

            @Override
            public void undo() {
                BuffRef<?> removed = buffRefs.remove(buffPos);
                if (removed != buffRef) {
                    throw new IllegalStateException("Undo was called in an illegal state.");
                }
            }
        });
    }

    private int findInsertPos(int priority) {
        for (int i = buffRefs.size() - 1; i >= 0; i--) {
            BuffRef<?> buffRef = buffRefs.get(i);
            if (buffRef.priority <= priority) {
                return i + 1;
            }
        }
        return 0;
    }

    public AuraAwarePropertyBase<T> copy() {
        return new AuraAwarePropertyBase<>(this);
    }

    private boolean hasNonExternalBuff() {
        for (BuffRef<?> buffRef: buffRefs) {
            if (!buffRef.external) {
                return true;
            }
        }
        return false;
    }

    @Override
    public UndoAction silence() {
        if (!hasNonExternalBuff()) {
            return UndoAction.DO_NOTHING;
        }

        List<BuffRef<T>> prevRefs = new ArrayList<>(buffRefs);
        buffRefs.clear();
        for (BuffRef<T> buffRef: prevRefs) {
            if (buffRef.external) {
                buffRefs.add(buffRef);
            }
        }
        return () -> {
            buffRefs.clear();
            buffRefs.addAll(prevRefs);
        };
    }

    public T getCombinedView() {
        return combinedView;
    }

    public static interface BuffDef<T> {
        public T viewCombinedBuffs(Collection<? extends BuffRef<T>> buffs);
    }

    public static final class BuffRef<T> {
        private final int priority;
        private final boolean external;
        private final T buff;

        public BuffRef(int priority, boolean external, T buff) {
            this.priority = priority;
            this.external = external;
            this.buff = buff;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isExternal() {
            return external;
        }

        public T getBuff() {
            return buff;
        }
    }
}
