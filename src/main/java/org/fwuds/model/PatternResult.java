package org.fwuds.model;

/** One frequent weighted utility pattern and its normalized utility in a window. */
public record PatternResult(Itemset itemset, double wus) implements Comparable<PatternResult> {
    public PatternResult {
        if (itemset == null || !Double.isFinite(wus)) throw new IllegalArgumentException("Invalid pattern result");
    }

    @Override public int compareTo(PatternResult other) { return itemset.compareTo(other.itemset); }
}
