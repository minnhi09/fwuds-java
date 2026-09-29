package org.fwuds.model;

import java.util.Arrays;

/** Raw quantitative transaction before the dynamic weight regime is applied. */
public record QuantitativeTransaction(long sequence, int[] items, int[] quantities) {
    public QuantitativeTransaction {
        int[] sourceItems = items.clone();
        int[] sourceQuantities = quantities.clone();
        if (sequence < 0) throw new IllegalArgumentException("Sequence must not be negative");
        if (items.length == 0 || items.length != quantities.length) {
            throw new IllegalArgumentException("A transaction needs equally-sized, non-empty item and quantity arrays");
        }
        Integer[] order = new Integer[sourceItems.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (left, right) -> Integer.compare(sourceItems[left], sourceItems[right]));
        int[] sortedItems = new int[sourceItems.length];
        int[] sortedQuantities = new int[sourceItems.length];
        for (int i = 0; i < order.length; i++) {
            sortedItems[i] = sourceItems[order[i]];
            sortedQuantities[i] = sourceQuantities[order[i]];
            if (sortedItems[i] <= 0 || sortedQuantities[i] <= 0) {
                throw new IllegalArgumentException("Items and quantities must be positive");
            }
            if (i > 0 && sortedItems[i] == sortedItems[i - 1]) {
                throw new IllegalArgumentException("Duplicate item " + sortedItems[i] + " in transaction " + sequence);
            }
        }
        items = sortedItems;
        quantities = sortedQuantities;
    }

    @Override public int[] items() { return items.clone(); }
    @Override public int[] quantities() { return quantities.clone(); }
}
