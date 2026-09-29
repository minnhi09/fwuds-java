package org.fwuds.mining;

import org.fwuds.model.Itemset;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.Pane;
import org.fwuds.model.PatternResult;
import org.fwuds.model.WeightedTransaction;
import org.fwuds.model.WindowResult;
import org.fwuds.util.FwudsMath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Exponential reference implementation intended only for validation on small item universes. */
public final class BruteForceMiner implements FwudsMiner {
    private static final int MAX_ITEMS = 24;
    private final MiningConfig config;
    private final ArrayDeque<Pane> panes = new ArrayDeque<>();
    private long windowIndex;
    private WindowResult current;

    public BruteForceMiner(MiningConfig config) { this.config = config; }

    @Override public WindowResult initialize(List<Pane> initialWindow) {
        if (!panes.isEmpty()) throw new IllegalStateException("Miner is already initialized");
        validateInitialWindow(initialWindow);
        panes.addAll(initialWindow);
        current = mine();
        return current;
    }

    @Override public WindowResult slide(Pane newestPane) {
        requireInitialized();
        validatePane(newestPane);
        panes.removeFirst();
        panes.addLast(newestPane);
        windowIndex++;
        current = mine();
        return current;
    }

    @Override public WindowResult current() { requireInitialized(); return current; }

    private WindowResult mine() {
        List<WeightedTransaction> transactions = panes.stream().flatMap(p -> p.transactions().stream()).toList();
        int[] items = transactions.stream().flatMapToInt(t -> java.util.Arrays.stream(t.items())).distinct().sorted().toArray();
        if (items.length > MAX_ITEMS) throw new IllegalStateException("Brute-force oracle supports at most " + MAX_ITEMS + " items");
        double sumTwu = transactions.stream().mapToDouble(WeightedTransaction::twu).sum();
        List<PatternResult> results = new ArrayList<>();
        enumerate(items, 0, new TreeSet<>(), transactions, sumTwu, results);
        return new WindowResult(windowIndex, results, sumTwu);
    }

    private void enumerate(int[] items, int index, TreeSet<Integer> selected,
                           List<WeightedTransaction> transactions, double sumTwu, List<PatternResult> results) {
        for (int i = index; i < items.length; i++) {
            selected.add(items[i]);
            Itemset candidate = Itemset.of(selected);
            double numerator = transactions.stream().filter(t -> t.contains(candidate)).mapToDouble(WeightedTransaction::twu).sum();
            if (FwudsMath.meetsThreshold(numerator, sumTwu, config.minWus())) {
                results.add(new PatternResult(candidate, numerator / sumTwu));
            }
            enumerate(items, i + 1, selected, transactions, sumTwu, results);
            selected.remove(items[i]);
        }
    }

    private void validateInitialWindow(List<Pane> initialWindow) {
        if (initialWindow.size() != config.windowPanes()) {
            throw new IllegalArgumentException("Expected " + config.windowPanes() + " initial panes");
        }
        initialWindow.forEach(this::validatePane);
    }

    private void validatePane(Pane pane) {
        if (pane.transactions().size() != config.paneSize()) {
            throw new IllegalArgumentException("Expected pane size " + config.paneSize());
        }
    }

    private void requireInitialized() {
        if (current == null) throw new IllegalStateException("Miner has not been initialized");
    }
}
