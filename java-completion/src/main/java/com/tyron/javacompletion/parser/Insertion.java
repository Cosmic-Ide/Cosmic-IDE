package com.tyron.javacompletion.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Ordering;
import java.util.List;

@AutoValue
abstract class Insertion {
    private static final Ordering<Insertion> REVERSE_INSERTION =
            Ordering.natural().onResultOf((Insertion insertion) -> insertion.getPos()).reverse();

    public abstract int getPos();

    public abstract String getText();

    public static Insertion create(int pos, String text) {
        return new AutoValue_Insertion(pos, text);
    }

    public static CharSequence applyInsertions(CharSequence content, List<Insertion> insertions) {
        List<Insertion> reverseInsertions = REVERSE_INSERTION.immutableSortedCopy(insertions);

        StringBuilder sb = new StringBuilder(content);

        for (Insertion insertion : reverseInsertions) {
            sb.insert(insertion.getPos(), insertion.getText());
        }
        return sb;
    }
}