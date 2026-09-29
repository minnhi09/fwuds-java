package org.fwuds.util;

/** Traversal-order-independent dynamic weights, stable across Java releases. */
public final class DynamicWeightGenerator implements WeightProvider {
    private final long seed;
    private final int batchSize;

    public DynamicWeightGenerator(long seed, int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("Batch size must be positive");
        this.seed = seed;
        this.batchSize = batchSize;
    }

    @Override public double weight(long transactionSequence, int item) {
        long batch = transactionSequence / batchSize;
        long mixed = mix64(seed ^ mix64(batch + 0x9E3779B97F4A7C15L) ^ mix64(item));
        return 1 + (int) Long.remainderUnsigned(mixed, 10L);
    }

    static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
