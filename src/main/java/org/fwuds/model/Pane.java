package org.fwuds.model;

import java.util.List;

/** A complete fixed-size group of weighted transactions supplied to a miner. */
public record Pane(List<WeightedTransaction> transactions) {
    public Pane {
        transactions = List.copyOf(transactions);
        if (transactions.isEmpty()) throw new IllegalArgumentException("A pane must not be empty");
    }
}
