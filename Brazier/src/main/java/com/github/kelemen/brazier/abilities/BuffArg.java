package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.Priorities;

public final class BuffArg {
    public static final BuffArg NORMAL_BUFF = ownedBuff(Priorities.NORMAL_PRIORITY);
    public static final BuffArg NORMAL_AURA_BUFF = externalBuff(Priorities.HIGH_PRIORITY);

    private final int priority;
    private final boolean external;

    public BuffArg(int priority, boolean external) {
        this.priority = priority;
        this.external = external;
    }

    public static BuffArg externalBuff(int priority) {
        return new BuffArg(priority, true);
    }

    public static BuffArg ownedBuff(int priority) {
        return new BuffArg(priority, false);
    }

    public void checkNormalBuff() {
        if (external || getPriority() != Priorities.NORMAL_PRIORITY) {
            throw new UnsupportedOperationException("Unsupported buff: " + this);
        }
    }

    public int getPriority() {
        return priority;
    }

    public boolean isExternal() {
        return external;
    }

    @Override
    public String toString() {
        return "BuffArg{" + "priority=" + priority + ", " + (external ? "external" : "owned") + '}';
    }
}
