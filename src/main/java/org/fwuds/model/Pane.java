package org.fwuds.model;

import java.util.List;

public record Pane(List<WeightedTransaction> transactions) {
    public Pane {
        transactions = List.copyOf(transactions);
        if (transactions.isEmpty()) throw new IllegalArgumentException("A pane must not be empty");
    }
}
