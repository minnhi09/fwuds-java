package org.fwuds.mining.dwt;

import org.fwuds.mining.FwudsMiner;
import org.fwuds.model.Itemset;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.Pane;
import org.fwuds.model.PatternResult;
import org.fwuds.model.WeightedTransaction;
import org.fwuds.model.WindowResult;
import org.fwuds.util.FwudsMath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** DSWUN-tree/WUNList implementation of FWUDS-DWT (Algorithms 4-6). */
public final class FwudsDwtMiner implements FwudsMiner {
    private final MiningConfig config;
    private final Map<Integer, Integer> rank = new HashMap<>();
    private DswunTree tree;
    private boolean initialized;
    private long windowIndex;
    private WindowResult current;

    public FwudsDwtMiner(MiningConfig config) { this.config = config; }

    @Override public WindowResult initialize(List<Pane> initialWindow) {
        if (initialized) throw new IllegalStateException("Miner is already initialized");
        if (initialWindow.size() != config.windowPanes()) {
            throw new IllegalArgumentException("Expected " + config.windowPanes() + " initial panes");
        }
        initialWindow.forEach(this::validatePane);
        List<WeightedTransaction> transactions = initialWindow.stream()
                .flatMap(pane -> pane.transactions().stream()).toList();
        establishInitialOrder(transactions);
        tree = new DswunTree(rank);
        transactions.forEach(tree::insert);
        initialized = true;
        current = mine();
        return current;
    }

    @Override public WindowResult slide(Pane newestPane) {
        requireInitialized();
        validatePane(newestPane);
        appendLateItemRanks(newestPane.transactions());
        // Algorithm 5 inserts the new pane before evicting the oldest one.
        newestPane.transactions().forEach(tree::insert);
        tree.evictOldest(config.paneSize());
        windowIndex++;
        current = mine();
        return current;
    }

    @Override public WindowResult current() { requireInitialized(); return current; }

    public Map<Integer, Integer> itemRanks() { return Map.copyOf(rank); }

    private void establishInitialOrder(List<WeightedTransaction> transactions) {
        Map<Integer, Double> totals = new HashMap<>();
        for (WeightedTransaction transaction : transactions) {
            for (int item : transaction.items()) totals.merge(item, transaction.twu(), Double::sum);
        }
        List<Integer> items = new ArrayList<>(totals.keySet());
        items.sort(Comparator.<Integer>comparingDouble(totals::get).reversed().thenComparingInt(Integer::intValue));
        for (int i = 0; i < items.size(); i++) rank.put(items.get(i), i);
    }

    private void appendLateItemRanks(List<WeightedTransaction> transactions) {
        TreeSet<Integer> unseen = new TreeSet<>();
        for (WeightedTransaction transaction : transactions) {
            for (int item : transaction.items()) if (!rank.containsKey(item)) unseen.add(item);
        }
        for (int item : unseen) rank.put(item, rank.size());
    }

    private WindowResult mine() {
        double sumTwu = tree.sumTwu();
        Map<Integer, WunList> singletonLists = tree.generateWunLists();
        List<Candidate> singletons = new ArrayList<>();
        Map<Itemset, Double> results = new LinkedHashMap<>();
        singletonLists.forEach((item, list) -> {
            if (FwudsMath.meetsThreshold(list.totalWeight(), sumTwu, config.minWus())) {
                Itemset itemset = new Itemset(item);
                singletons.add(new Candidate(itemset, list, rank.get(item)));
                results.put(itemset, list.totalWeight() / sumTwu);
            }
        });
        singletons.sort(Comparator.comparingInt(Candidate::anchorRank));
        mineLevel(singletons, sumTwu, results);
        List<PatternResult> patterns = results.entrySet().stream()
                .map(entry -> new PatternResult(entry.getKey(), entry.getValue())).toList();
        return new WindowResult(windowIndex, patterns, sumTwu);
    }

    private void mineLevel(List<Candidate> candidates, double sumTwu, Map<Itemset, Double> results) {
        double minimumWeight = config.minWus() * sumTwu;
        for (int i = candidates.size() - 1; i >= 0; i--) {
            Candidate descendant = candidates.get(i);
            List<Candidate> next = new ArrayList<>();
            for (int j = i - 1; j >= 0; j--) {
                Candidate ancestor = candidates.get(j);
                WunList intersection = intersect(descendant.list(), ancestor.list(), minimumWeight);
                if (intersection.isEmpty() || !FwudsMath.meetsThreshold(intersection.totalWeight(), sumTwu, config.minWus())) {
                    continue;
                }
                Itemset itemset = descendant.itemset().union(ancestor.itemset());
                results.put(itemset, intersection.totalWeight() / sumTwu);
                next.add(new Candidate(itemset, intersection, ancestor.anchorRank()));
            }
            if (next.size() > 1) {
                next.sort(Comparator.comparingInt(Candidate::anchorRank));
                mineLevel(next, sumTwu, results);
            }
        }
    }

    /** Linear ancestor/descendant intersection with a safe remaining-weight pruning bound. */
    static WunList intersect(WunList descendants, WunList ancestors, double minimumWeight) {
        List<WunCode> result = new ArrayList<>();
        List<WunCode> left = descendants.codes();
        List<WunCode> right = ancestors.codes();
        int ancestorIndex = 0;
        double accumulated = 0.0;
        double processedLeft = 0.0;
        for (WunCode descendant : left) {
            while (ancestorIndex < right.size()) {
                WunCode ancestor = right.get(ancestorIndex);
                if (ancestor.pre() < descendant.pre() && ancestor.post() > descendant.post()) {
                    if (!result.isEmpty() && result.get(result.size() - 1).pre() == ancestor.pre()) {
                        WunCode previous = result.remove(result.size() - 1);
                        result.add(new WunCode(previous.pre(), previous.post(), previous.weight() + descendant.weight()));
                    } else {
                        result.add(new WunCode(ancestor.pre(), ancestor.post(), descendant.weight()));
                    }
                    accumulated += descendant.weight();
                    break;
                }
                if (ancestor.pre() >= descendant.pre()) break;
                ancestorIndex++;
            }
            processedLeft += descendant.weight();
            double leftRemaining = descendants.totalWeight() - processedLeft;
            if (accumulated + leftRemaining + FwudsMath.EPSILON < minimumWeight) return new WunList(List.of());
        }
        return new WunList(result);
    }

    int treeTransactionCount() { requireInitialized(); return tree.transactionCount(); }

    private void validatePane(Pane pane) {
        if (pane.transactions().size() != config.paneSize()) {
            throw new IllegalArgumentException("Expected pane size " + config.paneSize());
        }
    }

    private void requireInitialized() {
        if (!initialized) throw new IllegalStateException("Miner has not been initialized");
    }

    private record Candidate(Itemset itemset, WunList list, int anchorRank) { }
}
