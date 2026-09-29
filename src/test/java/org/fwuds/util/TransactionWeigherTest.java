package org.fwuds.util;

import org.fwuds.model.QuantitativeTransaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionWeigherTest {
    @Test void usesItemCardinalityRatherThanQuantitySum() {
        WeightProvider fixed = (transaction, item) -> switch (item) {
            case 1, 3 -> 0.2;
            case 4 -> 0.4;
            case 5 -> 0.5;
            default -> 0.1;
        };
        var raw = new QuantitativeTransaction(0, new int[]{1,3,4,5}, new int[]{1,2,2,1});
        assertEquals(0.475, new TransactionWeigher(fixed).weigh(raw).twu(), 1e-12);
    }

    @Test void generatorIsDeterministicAndBatchSensitive() {
        DynamicWeightGenerator first = new DynamicWeightGenerator(42, 2);
        DynamicWeightGenerator second = new DynamicWeightGenerator(42, 2);
        assertEquals(first.weight(0, 7), second.weight(0, 7));
        assertEquals(first.weight(0, 7), first.weight(1, 7));
    }

    @Test void rejectsNonPositiveWeights() {
        var raw = new QuantitativeTransaction(0, new int[]{1}, new int[]{1});
        assertThrows(IllegalArgumentException.class, () -> new TransactionWeigher((transaction, item) -> 0.0).weigh(raw));
    }
}
