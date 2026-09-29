package org.fwuds.model;

import java.util.Arrays;
import java.util.Collection;

/** Immutable, canonical itemset whose item identifiers are strictly increasing. */
public final class Itemset implements Comparable<Itemset> {
    private final int[] items;

    public Itemset(int... items) {
        if (items.length == 0) {
            throw new IllegalArgumentException("An itemset must not be empty");
        }
        this.items = items.clone();
        Arrays.sort(this.items);
        for (int i = 0; i < this.items.length; i++) {
            if (this.items[i] <= 0) {
                throw new IllegalArgumentException("Item identifiers must be positive");
            }
            if (i > 0 && this.items[i] == this.items[i - 1]) {
                throw new IllegalArgumentException("Duplicate item: " + this.items[i]);
            }
        }
    }

    public static Itemset of(Collection<Integer> values) {
        return new Itemset(values.stream().mapToInt(Integer::intValue).toArray());
    }

    public Itemset union(Itemset other) {
        int[] merged = new int[items.length + other.items.length];
        int i = 0, j = 0, k = 0;
        while (i < items.length || j < other.items.length) {
            int value;
            if (j == other.items.length || (i < items.length && items[i] < other.items[j])) value = items[i++];
            else if (i == items.length || other.items[j] < items[i]) value = other.items[j++];
            else { value = items[i]; i++; j++; }
            merged[k++] = value;
        }
        return new Itemset(Arrays.copyOf(merged, k));
    }

    public int size() { return items.length; }
    public int itemAt(int index) { return items[index]; }
    public int[] toArray() { return items.clone(); }

    @Override public int compareTo(Itemset other) {
        int limit = Math.min(items.length, other.items.length);
        for (int i = 0; i < limit; i++) {
            int comparison = Integer.compare(items[i], other.items[i]);
            if (comparison != 0) return comparison;
        }
        return Integer.compare(items.length, other.items.length);
    }

    @Override public boolean equals(Object candidate) {
        return candidate instanceof Itemset other && Arrays.equals(items, other.items);
    }

    @Override public int hashCode() { return Arrays.hashCode(items); }

    @Override public String toString() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) value.append(' ');
            value.append(items[i]);
        }
        return value.toString();
    }
}
