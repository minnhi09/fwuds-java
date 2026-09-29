package org.fwuds.model;

/** Immutable parameters shared by the input pipeline and every miner implementation. */
public record MiningConfig(int windowPanes, int paneSize, int weightBatchSize, double minWus, long seed) {
    public MiningConfig {
        if (windowPanes <= 0 || paneSize <= 0 || weightBatchSize <= 0) {
            throw new IllegalArgumentException("Window, pane, and weight-batch sizes must be positive");
        }
        if (!Double.isFinite(minWus) || minWus < 0.0 || minWus > 1.0) {
            throw new IllegalArgumentException("minWus must be in [0,1]");
        }
    }

    public int windowTransactions() { return Math.multiplyExact(windowPanes, paneSize); }
}
