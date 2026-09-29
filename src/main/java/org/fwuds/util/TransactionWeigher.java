package org.fwuds.util;

import org.fwuds.model.QuantitativeTransaction;
import org.fwuds.model.WeightedTransaction;

public final class TransactionWeigher {
    private final WeightProvider weights;

    public TransactionWeigher(WeightProvider weights) { this.weights = weights; }

    public WeightedTransaction weigh(QuantitativeTransaction transaction) {
        int[] items = transaction.items();
        int[] quantities = transaction.quantities();
        double total = 0.0;
        for (int i = 0; i < items.length; i++) {
            double weight = weights.weight(transaction.sequence(), items[i]);
            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException("Weight must be finite and positive for item " + items[i]);
            }
            total += weight * quantities[i];
        }
        return new WeightedTransaction(transaction.sequence(), items, quantities, total / items.length);
    }
}
