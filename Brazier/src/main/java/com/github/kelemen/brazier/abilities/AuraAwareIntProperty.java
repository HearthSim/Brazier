package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Priorities;
import com.github.kelemen.brazier.Silencable;
import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.events.UndoableUnregisterRef;
import java.util.ArrayList;
import java.util.List;

public final class AuraAwareIntProperty implements Silencable {
    private final int baseValue;
    private final int minValue;

    private final List<BuffRef> buffRefs;

    public AuraAwareIntProperty(int baseValue) {
        this(baseValue, Integer.MIN_VALUE);
    }

    public AuraAwareIntProperty(int baseValue, int minValue) {
        this.baseValue = baseValue;
        this.minValue = minValue;

        this.buffRefs = new ArrayList<>();
    }

    private AuraAwareIntProperty(AuraAwareIntProperty other) {
        this.baseValue = other.baseValue;
        this.minValue = other.minValue;

        this.buffRefs = new ArrayList<>(other.buffRefs.size());
        for (BuffRef buffRef: other.buffRefs) {
            if (!buffRef.external) {
                this.buffRefs.add(buffRef);
            }
        }
    }

    public UndoAction setValueTo(int newValue) {
        return addRemovableBuff((prev) -> newValue);
    }

    public UndoAction addBuff(int toAdd) {
        return addRemovableBuff(toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int toAdd) {
        return addRemovableBuff((prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(IntPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.NORMAL_PRIORITY, false, toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(int toAdd) {
        return addExternalBuff((prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addExternalBuff(IntPropertyBuff toAdd) {
        return addRemovableBuff(Priorities.HIGH_PRIORITY, true, toAdd);
    }

    public UndoAction setValueTo(int priority, boolean external, int newValue) {
        return addRemovableBuff(priority, external, (prev) -> newValue);
    }

    public UndoAction addBuff(int priority, boolean external, int toAdd) {
        return addRemovableBuff(priority, external, toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int priority, boolean external, int toAdd) {
        return addRemovableBuff(priority, external, (prev) -> prev + toAdd);
    }

    public UndoableUnregisterRef addRemovableBuff(int priority, boolean external, IntPropertyBuff toAdd) {
        int buffPos = findInsertPos(priority);

        BuffRef buffRef = new BuffRef(priority, external, toAdd);
        buffRefs.add(buffPos, buffRef);
        return UndoableUnregisterRef.makeIdempotent(new UndoableUnregisterRef() {
            @Override
            public UndoAction unregister() {
                for (int i = buffRefs.size() - 1; i >= 0; i--) {
                    BuffRef candidate = buffRefs.get(i);
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
                BuffRef removed = buffRefs.remove(buffPos);
                if (removed != buffRef) {
                    throw new IllegalStateException("Undo was called in an illegal state.");
                }
            }
        });
    }

    private int findInsertPos(int priority) {
        for (int i = buffRefs.size() - 1; i >= 0; i--) {
            BuffRef buffRef = buffRefs.get(i);
            if (buffRef.priority <= priority) {
                return i + 1;
            }
        }
        return 0;
    }

    public AuraAwareIntProperty copy() {
        return new AuraAwareIntProperty(this);
    }

    private boolean hasNonExternalBuff() {
        for (BuffRef buffRef: buffRefs) {
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

        List<BuffRef> prevRefs = new ArrayList<>(buffRefs);
        buffRefs.clear();
        for (BuffRef buffRef: prevRefs) {
            if (buffRef.external) {
                buffRefs.add(buffRef);
            }
        }
        return () -> {
            buffRefs.clear();
            buffRefs.addAll(prevRefs);
        };
    }

    public int getValue() {
        int result = baseValue;
        for (BuffRef buffRef: buffRefs) {
            result = buffRef.adjustValue(result);
        }
        return result;
    }

    private static final class BuffRef {
        public final int priority;
        public final boolean external;
        private final IntPropertyBuff buff;

        public BuffRef(int priority, boolean external, IntPropertyBuff buff) {
            this.priority = priority;
            this.external = external;
            this.buff = buff;
        }

        public int adjustValue(int prevValue) {
            return buff.buffProperty(prevValue);
        }
    }
}
