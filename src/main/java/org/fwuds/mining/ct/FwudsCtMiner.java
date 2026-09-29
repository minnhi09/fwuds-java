package org.fwuds.mining.ct;

import org.fwuds.mining.FwudsMiner;
import org.fwuds.model.Itemset;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.Pane;
import org.fwuds.model.PatternResult;
import org.fwuds.model.WeightedTransaction;
import org.fwuds.model.WindowResult;
import org.fwuds.util.FwudsMath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Paper-faithful Circular Tidset implementation of FWUDS-CT. */
public final class FwudsCtMiner implements FwudsMiner {
    private final MiningConfig config;
    private final int capacity;
    private final Map<Integer, ArrayDeque<Integer>> singletonCtsets = new HashMap<>();
    private final double[] twuByLcTid;
    private int currentLcTid;
    private long windowIndex;
    private boolean initialized;
    private WindowResult current;

    public FwudsCtMiner(MiningConfig config) {
        this.config = config;
        this.capacity = config.windowTransactions();
        this.twuByLcTid = new double[capacity + 1];
    }

    @Override public WindowResult initialize(List<Pane> initialWindow) {
        if (initialized) throw new IllegalStateException("Miner is already initialized");
        if (initialWindow.size() != config.windowPanes()) {
            throw new IllegalArgumentException("Expected " + config.windowPanes() + " initial panes");
        }
        initialWindow.forEach(this::insertPane);
        initialized = true;
        current = mine();
        return current;
    }

    @Override public WindowResult slide(Pane newestPane) {
        requireInitialized();
        validatePane(newestPane);
        removeOldestPane();
        insertPane(newestPane);
        windowIndex++;
        current = mine();
        return current;
    }

    @Override public WindowResult current() { requireInitialized(); return current; }

    public int currentLcTid() { return currentLcTid; }

    public Map<Integer, List<Integer>> singletonCtsets() {
        Map<Integer, List<Integer>> snapshot = new LinkedHashMap<>();
        singletonCtsets.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> snapshot.put(entry.getKey(), List.copyOf(entry.getValue())));
        return Map.copyOf(snapshot);
    }

    private void insertPane(Pane pane) {
        validatePane(pane);
        for (WeightedTransaction transaction : pane.transactions()) {
            currentLcTid = currentLcTid == capacity ? 1 : currentLcTid + 1;
            twuByLcTid[currentLcTid] = transaction.twu();
            for (int item : transaction.items()) {
                singletonCtsets.computeIfAbsent(item, ignored -> new ArrayDeque<>()).addLast(currentLcTid);
            }
        }
    }

    private void removeOldestPane() {
        int removeFrom = currentLcTid == capacity ? 1 : currentLcTid + 1;
        int removeTo = currentLcTid == capacity ? config.paneSize() : currentLcTid + config.paneSize();
        for (ArrayDeque<Integer> ctset : singletonCtsets.values()) {
            while (!ctset.isEmpty()) {
                int value = ctset.peekFirst();
                if (value >= removeFrom && value <= removeTo) ctset.removeFirst();
                else break;
            }
        }
        singletonCtsets.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        for (int lcTid = removeFrom; lcTid <= removeTo; lcTid++) twuByLcTid[lcTid] = 0.0;
    }

    private WindowResult mine() {
        double sumTwu = 0.0;
        for (int i = 1; i < twuByLcTid.length; i++) sumTwu += twuByLcTid[i];
        List<Candidate> level = new ArrayList<>();
        Map<Itemset, Double> results = new HashMap<>();
        for (Map.Entry<Integer, ArrayDeque<Integer>> entry : singletonCtsets.entrySet()) {
            List<Integer> tids = List.copyOf(entry.getValue());
            double numerator = numerator(tids);
            if (FwudsMath.meetsThreshold(numerator, sumTwu, config.minWus())) {
                Itemset itemset = new Itemset(entry.getKey());
                level.add(new Candidate(itemset, tids, numerator));
                results.put(itemset, numerator / sumTwu);
            }
        }
        level.sort(Comparator.comparing(Candidate::itemset));
        mineLevel(level, sumTwu, results);
        List<PatternResult> patterns = results.entrySet().stream()
                .map(entry -> new PatternResult(entry.getKey(), entry.getValue())).toList();
        return new WindowResult(windowIndex, patterns, sumTwu);
    }

    private void mineLevel(List<Candidate> candidates, double sumTwu, Map<Itemset, Double> results) {
        for (int i = 0; i < candidates.size(); i++) {
            Candidate left = candidates.get(i);
            List<Candidate> next = new ArrayList<>();
            for (int j = i + 1; j < candidates.size(); j++) {
                Candidate right = candidates.get(j);
                Itemset itemset = left.itemset().union(right.itemset());
                List<Integer> tids = intersect(left.tids(), right.tids());
                if (tids.isEmpty()) continue;
                double numerator = numerator(tids);
                if (FwudsMath.meetsThreshold(numerator, sumTwu, config.minWus())) {
                    results.put(itemset, numerator / sumTwu);
                    next.add(new Candidate(itemset, tids, numerator));
                }
            }
            if (next.size() > 1) mineLevel(next, sumTwu, results);
        }
    }

    /** Algorithm 2: chronological intersection despite circular identifiers. */
    public List<Integer> intersect(List<Integer> first, List<Integer> second) {
        List<Integer> intersection = new ArrayList<>(Math.min(first.size(), second.size()));
        int left = 0, right = 0;
        while (left < first.size() && right < second.size()) {
            int rawLeft = first.get(left);
            int rawRight = second.get(right);
            int comparableLeft = rawLeft <= currentLcTid ? rawLeft + capacity : rawLeft;
            int comparableRight = rawRight <= currentLcTid ? rawRight + capacity : rawRight;
            if (comparableLeft == comparableRight) {
                intersection.add(rawLeft);
                left++;
                right++;
            } else if (comparableLeft < comparableRight) left++;
            else right++;
        }
        return List.copyOf(intersection);
    }

    private double numerator(List<Integer> tids) {
        double total = 0.0;
        for (int lcTid : tids) total += twuByLcTid[lcTid];
        return total;
    }

    private void validatePane(Pane pane) {
        if (pane.transactions().size() != config.paneSize()) {
            throw new IllegalArgumentException("Expected pane size " + config.paneSize());
        }
    }

    private void requireInitialized() {
        if (!initialized) throw new IllegalStateException("Miner has not been initialized");
    }

    private record Candidate(Itemset itemset, List<Integer> tids, double numerator) { }
}
