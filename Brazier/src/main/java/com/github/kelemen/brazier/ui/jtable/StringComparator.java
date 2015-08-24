package com.github.kelemen.brazier.ui.jtable;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import org.jtrim.utils.ExceptionHelper;

public final class StringComparator implements Comparator<String> {
    public static final StringComparator NULL_FIRST = new StringComparator(true);
    public static final StringComparator NULL_LAST = new StringComparator(false);

    public static final StringComparator DEFAULT_INSTANCE = NULL_FIRST;

    private final Collator cmp;
    private final int firstNullResult;
    private final int secondNullResult;

    public StringComparator() {
        this(true);
    }

    public StringComparator(boolean nullFirst) {
        this(Locale.getDefault(), nullFirst);
    }

    public StringComparator(Locale locale, boolean nullFirst) {
        ExceptionHelper.checkNotNullArgument(locale, "locale");

        this.cmp = Collator.getInstance(locale);
        if (nullFirst) {
            firstNullResult = -1;
            secondNullResult = 1;
        }
        else {
            firstNullResult = 1;
            secondNullResult = -1;
        }
    }

    @Override
    @SuppressWarnings("StringEquality")
    public int compare(String o1, String o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return firstNullResult;
        }
        if (o2 == null) {
            return secondNullResult;
        }

        return cmp.compare(o1, o2);
    }
}
