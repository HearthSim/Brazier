package com.github.kelemen.hearthstone.emulator.parsing;

import com.github.kelemen.hearthstone.emulator.HearthStoneDb;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.internal.AssumptionViolatedException;

public final class TestDb {
    private static final AtomicReference<HearthStoneDb> DB_REF = new AtomicReference<>(null);

    public static HearthStoneDb getTestDbUnsafe() throws IOException, ObjectParsingException {
        HearthStoneDb result = DB_REF.get();
        if (result == null) {
            result = HearthStoneDb.readDefault();
            if (!DB_REF.compareAndSet(null, result)) {
                result = DB_REF.get();
            }
        }
        return result;
    }

    public static HearthStoneDb getTestDb() {
        try {
            return getTestDbUnsafe();
        } catch (IOException | ObjectParsingException ex) {
            AssumptionViolatedException toThrow = new AssumptionViolatedException("TestDb is not available.");
            toThrow.addSuppressed(ex);
            throw toThrow;
        }
    }

    private TestDb() {
        throw new AssertionError();
    }
}
