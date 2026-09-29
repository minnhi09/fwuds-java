package org.fwuds.util;

@FunctionalInterface
public interface WeightProvider {
    double weight(long transactionSequence, int item);
}
