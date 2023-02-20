package com.tyron.javacompletion.model.util;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Build a {@link RangeMap} from non-overlapping {@link Range} instances. */
public class NestedRangeMapBuilder<V> {
    private static class Entry<V> implements Comparable<Entry<V>> {
        // Range is an open-close range, e.g. [lower, upper).
        private final Range<Integer> range;
        private final V value;

        private Entry(Range<Integer> range, V value) {
            this.range = range;
            this.value = value;
        }

        /**
         * Compare two {@link Entry} instances by their ranges.
         *
         * <p>Range A is less than range B if: a) the lower endpoint of A is less than that of B, or b)
         * the lower endpoint of A is equal to that of B and the upper endpoint is greater than that of
         * B. Otherwise, if the A and B are not equal, then A is greater than B.
         */
        public int compareTo(Entry<V> o) {
            int ret = Integer.compare(this.range.lowerEndpoint(), o.range.lowerEndpoint());
            if (ret != 0) {
                return ret;
            }
            return -Integer.compare(this.range.upperEndpoint(), o.range.upperEndpoint());
        }
    }

    private final List<Entry<V>> entries = new ArrayList<>();

    /**
     * Maps a range to a specified value.
     *
     * <p>All existing ranges must be disconnected with {@code range}, encloses {@code range}, or is
     * enclosed in {@code range}.
     */
    public NestedRangeMapBuilder put(Range<Integer> range, V value) {
        range = range.canonical(DiscreteDomain.integers());
        if (!range.isEmpty()) {
            entries.add(new Entry<>(range, value));
        }
        return this;
    }

    public ImmutableRangeMap<Integer, V> build() {
        Collections.sort(entries);
        ImmutableRangeMap.Builder<Integer, V> builder = new ImmutableRangeMap.Builder<>();
        // A stack of disconnected range and value entries. Ranges are in ascending order, e.g the first
        // range's upper endpoint is greater than or equal to the second range's lower endpoint.
        Deque<Entry<V>> stack = new ArrayDeque<>();
        for (Entry<V> entry : entries) {
            while (!stack.isEmpty()) {
                Entry<V> topEntry = stack.pollFirst();
                if (topEntry.range.encloses(entry.range)) {
                    if (topEntry.range.lowerEndpoint() < entry.range.lowerEndpoint()) {
                        builder.put(
                                Range.closedOpen(topEntry.range.lowerEndpoint(), entry.range.lowerEndpoint()),
                                topEntry.value);
                    }

                    if (topEntry.range.upperEndpoint() > entry.range.upperEndpoint()) {
                        stack.addFirst(
                                new Entry<>(
                                        Range.closedOpen(entry.range.upperEndpoint(), topEntry.range.upperEndpoint()),
                                        topEntry.value));
                    }
                    break;
                } else {
                    // Disconnected with top entry, add the whole range of the top entry to the builder
                    builder.put(topEntry.range, topEntry.value);
                }
            }
            stack.addFirst(entry);
        }

        // Add the rest entries to the builder
        for (Entry<V> entry : stack) {
            builder.put(entry.range, entry.value);
        }

        return builder.build();
    }
}