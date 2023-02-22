/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
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