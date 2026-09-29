package org.fwuds.model;

/** A transaction with the paper's transaction weighted utility (twu). */
public record WeightedTransaction(long sequence, int[] items, int[] quantities, double twu) {
    public WeightedTransaction {
        QuantitativeTransaction normalized = new QuantitativeTransaction(sequence, items, quantities);
        items = normalized.items();
        quantities = normalized.quantities();
        if (!Double.isFinite(twu) || twu <= 0.0) {
            throw new IllegalArgumentException("twu must be finite and positive");
        }
    }

    @Override public int[] items() { return items.clone(); }
    @Override public int[] quantities() { return quantities.clone(); }

    public boolean contains(Itemset itemset) {
        int transactionIndex = 0;
        for (int patternIndex = 0; patternIndex < itemset.size(); patternIndex++) {
            int wanted = itemset.itemAt(patternIndex);
            while (transactionIndex < items.length && items[transactionIndex] < wanted) transactionIndex++;
            if (transactionIndex == items.length || items[transactionIndex] != wanted) return false;
        }
        return true;
    }
}
