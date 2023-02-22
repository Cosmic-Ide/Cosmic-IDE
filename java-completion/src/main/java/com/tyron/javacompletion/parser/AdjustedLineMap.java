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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.sun.source.tree.LineMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LineMap} adjusted with fixes done by {@link FileContentFixer}. The position in this line
 * map is the position in the fixed content, while the line and column numbers are the numbers in
 * the original line map.
 */
public class AdjustedLineMap implements LineMap {
    private static final Adjustment INITIAL_LINE_START_ADJUSTMENT = Adjustment.create(1, 0);
    private static final Adjustment INITIAL_COLUMN_ADJUSTMENT = Adjustment.create(1, 1);
    private static final List<Adjustment> DEFAULT_COLUMN_ADJUSTMENTS =
            ImmutableList.of(INITIAL_COLUMN_ADJUSTMENT);

    // Sorted original line number -> adjusted line start position mappings.
    // Last line is guaranteed to exist if there are insertions on it.
    private final List<Adjustment> lineStartAdjustments;

    // Map of original line number -> column adjustments of that line. Column adjustments are sorted
    // mappings of original column number -> adjusted column number.
    private final Map<Long, List<Adjustment>> columnAdjustments;

    private final LineMap originalLineMap;

    private AdjustedLineMap(
            LineMap originalLineMap,
            List<Adjustment> lineStartAdjustments,
            Map<Long, List<Adjustment>> columnAdjustments) {
        this.originalLineMap = originalLineMap;
        this.lineStartAdjustments = lineStartAdjustments;
        this.columnAdjustments = columnAdjustments;
    }

    @Override
    public long getStartPosition(long line) {
        Adjustment baseAdjustment = findOriginalLowerBound(lineStartAdjustments, line);
        long originalPos = originalLineMap.getStartPosition(line);
        long originalBasePos = originalLineMap.getStartPosition(baseAdjustment.getOriginal());
        return originalPos + (baseAdjustment.getAdjusted() - originalBasePos);
    }

    @Override
    public long getPosition(long line, long column) {
        long startPos = getStartPosition(line);
        Adjustment baseAdjustment = findOriginalLowerBound(getColumnAdjustments(line), column);
        long adjustedColumn = column + (baseAdjustment.getAdjusted() - baseAdjustment.getOriginal());
        return startPos + adjustedColumn - 1;
    }

    @Override
    public long getLineNumber(long pos) {
        return getLineNumberAndPosDelta(pos).lineNumber;
    }

    public LineMap getOriginalLineMap() {
        return originalLineMap;
    }

    private LineNumberAndPosDelta getLineNumberAndPosDelta(long pos) {
        List<Adjustment> adjustments = findAdjusted(lineStartAdjustments, pos);
        Adjustment lowerBound = adjustments.get(0);
        Adjustment upperBound = adjustments.size() > 1 ? adjustments.get(1) : null;
        long posDelta =
                lowerBound.getAdjusted() - originalLineMap.getStartPosition(lowerBound.getOriginal());
        long originalPos = pos - posDelta;
        long lineNumber;
        if (upperBound == null) {
            // No more insertion beyong lowerBound. originalPos is within the original length of the file
            // content.
            lineNumber = originalLineMap.getLineNumber(originalPos);
        } else {
            lineNumber = Integer.MAX_VALUE;
            try {
                lineNumber = originalLineMap.getLineNumber(originalPos);
            } catch (Exception e) {
                // Line start position + original length + line delta exceeds content length. Use upperBound
                // to get line number below.
            }
            if (upperBound != null && upperBound.getOriginal() <= lineNumber) {
                // This can happen if there are insertions on the original line and the column of the
                // adjusted
                // pos is greater than the length of the original line.
                lineNumber = upperBound.getOriginal() - 1;
            }
        }
        return new LineNumberAndPosDelta(lineNumber, posDelta);
    }

    @Override
    public long getColumnNumber(long pos) {
        LineNumberAndPosDelta lineNumberAndPosDelta = getLineNumberAndPosDelta(pos);
        long adjustedStartingPos =
                lineNumberAndPosDelta.posDelta
                        + originalLineMap.getStartPosition(lineNumberAndPosDelta.lineNumber);
        long adjustedColumn = pos - adjustedStartingPos + 1; // Columns start with 1.
        Adjustment baseAdjustment =
                findAdjusted(getColumnAdjustments(lineNumberAndPosDelta.lineNumber), adjustedColumn).get(0);
        return adjustedColumn - (baseAdjustment.getAdjusted() - baseAdjustment.getOriginal());
    }

    private List<Adjustment> getColumnAdjustments(long line) {
        if (columnAdjustments.containsKey(line)) {
            return columnAdjustments.get(line);
        }
        return DEFAULT_COLUMN_ADJUSTMENTS;
    }

    /**
     * Returns the last {@line Adjustment} whose original value is less or equal to {@code original}.
     */
    private static Adjustment findOriginalLowerBound(List<Adjustment> adjustments, long original) {
        Adjustment baseAdjustment = null;
        // We can use binary search, but typically the adjustments shouldn't be too much and linear
        // algorithm performs better.
        for (Adjustment adjustment : adjustments) {
            if (adjustment.getOriginal() <= original) {
                baseAdjustment = adjustment;
            }
        }
        return baseAdjustment;
    }

    /**
     * Returns the last {@line Adjustment} whose adjusted value is less or equal to {@code adjusted},
     * and the next element if available.
     *
     * @return A list of 1 or 2 elements.
     */
    private static List<Adjustment> findAdjusted(List<Adjustment> adjustments, long adjusted) {
        checkArgument(!adjustments.isEmpty(), "Adjustment cannot be empty");

        // We can use binary search, but typically the adjustments shouldn't be too much and linear
        // algorithm performs better.
        for (int i = 0; i < adjustments.size(); i++) {
            Adjustment adjustment = adjustments.get(i);
            if (adjustment.getAdjusted() > adjusted) {
                Adjustment lowerBound = (i > 0) ? adjustments.get(i - 1) : INITIAL_LINE_START_ADJUSTMENT;
                return ImmutableList.of(lowerBound, adjustment);
            }
        }
        return ImmutableList.of(adjustments.get(adjustments.size() - 1));
    }

    // Visible for AutoValue. Easier for debugging with generated toString();
    @AutoValue
    abstract static class Adjustment {
        abstract long getOriginal();

        abstract long getAdjusted();

        static Adjustment create(long original, long adjusted) {
            return new AutoValue_AdjustedLineMap_Adjustment(original, adjusted);
        }
    }

    private static class LineNumberAndPosDelta {
        private final long lineNumber;
        private final long posDelta;

        private LineNumberAndPosDelta(long lineNumber, long posDelta) {
            this.lineNumber = lineNumber;
            this.posDelta = posDelta;
        }
    }

    static class Builder {
        static final Ordering<Insertion> ORDER_BY_POS =
                Ordering.natural().onResultOf((Insertion insertion) -> insertion.getPos());

        private final List<Insertion> insertions = new ArrayList<>();
        private LineMap originalLineMap;

        Builder addInsertion(Insertion insertion) {
            insertions.add(insertion);
            return this;
        }

        Builder addInsertions(Collection<Insertion> insertions) {
            this.insertions.addAll(insertions);
            return this;
        }

        Builder setOriginalLineMap(LineMap originalLineMap) {
            this.originalLineMap = originalLineMap;
            return this;
        }

        AdjustedLineMap build() {
            checkNotNull(originalLineMap, "originalLineMap");
            List<Insertion> sortedInsertions = ORDER_BY_POS.immutableSortedCopy(insertions);
            List<Adjustment> lineStartAdjustments = new ArrayList<>();
            Map<Long, List<Adjustment>> columnAdjustments = new HashMap<>();
            List<Adjustment> currentColumntAdjustments = null;
            long currentLine = -1;
            long columnDelta = 0;
            long nextLineDelta = 0;
            long currentLineDelta = 0;
            boolean currentLineAdded = false;

            lineStartAdjustments.add(INITIAL_LINE_START_ADJUSTMENT);
            for (Insertion insertion : sortedInsertions) {
                long pos = insertion.getPos();
                long line = originalLineMap.getLineNumber(pos);
                long column = originalLineMap.getColumnNumber(pos);
                if (line != currentLine) {
                    if (currentLine > 0) {
                        lineStartAdjustments.add(
                                Adjustment.create(
                                        currentLine + 1,
                                        originalLineMap.getStartPosition(currentLine + 1) + nextLineDelta));
                    }
                    currentLineAdded = (line == currentLine + 1);
                    currentLine = line;
                    currentLineDelta = nextLineDelta;
                    columnDelta = 0;
                    currentColumntAdjustments = new ArrayList<>();
                    columnAdjustments.put(line, currentColumntAdjustments);
                    currentColumntAdjustments.add(INITIAL_COLUMN_ADJUSTMENT);
                }
                long delta = insertion.getText().length();
                nextLineDelta += delta;
                columnDelta += delta;
                currentColumntAdjustments.add(Adjustment.create(column + 1, column + 1 + columnDelta));
            }

            if (currentLine > 0) {
                try {
                    lineStartAdjustments.add(
                            Adjustment.create(
                                    currentLine + 1,
                                    originalLineMap.getStartPosition(currentLine + 1) + nextLineDelta));
                } catch (Exception e) {
                    // Current line is the last line, add current line.
                    if (!currentLineAdded) {
                        lineStartAdjustments.add(
                                Adjustment.create(
                                        currentLine, originalLineMap.getStartPosition(currentLine) + currentLineDelta));
                    }
                }
            }

            return new AdjustedLineMap(originalLineMap, lineStartAdjustments, columnAdjustments);
        }
    }
}