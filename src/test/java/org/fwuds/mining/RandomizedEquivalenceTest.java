package org.fwuds.mining;

import org.fwuds.mining.ct.FwudsCtMiner;
import org.fwuds.mining.dwt.FwudsDwtMiner;
import org.fwuds.model.Itemset;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.Pane;
import org.fwuds.model.PatternResult;
import org.fwuds.model.QuantitativeTransaction;
import org.fwuds.model.WindowResult;
import org.fwuds.util.DynamicWeightGenerator;
import org.fwuds.util.TransactionWeigher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomizedEquivalenceTest {
    @Test void optimizedAlgorithmsMatchOracleOnDeterministicSmallStreams() {
        for (long seed = 1; seed <= 20; seed++) verify(seed);
    }

    private static void verify(long seed) {
        MiningConfig config = new MiningConfig(2, 2, 3, 0.25, seed);
        TransactionWeigher weigher = new TransactionWeigher(new DynamicWeightGenerator(seed, config.weightBatchSize()));
        Random random = new Random(seed);
        List<Pane> panes = new ArrayList<>();
        long sequence = 0;
        for (int p = 0; p < 5; p++) {
            var transactions = new ArrayList<org.fwuds.model.WeightedTransaction>();
            for (int t = 0; t < config.paneSize(); t++) {
                TreeSet<Integer> items = new TreeSet<>();
                while (items.size() < 1 + random.nextInt(4)) items.add(1 + random.nextInt(5));
                int[] ids = items.stream().mapToInt(Integer::intValue).toArray();
                int[] quantities = java.util.Arrays.stream(ids).map(ignored -> 1 + random.nextInt(3)).toArray();
                transactions.add(weigher.weigh(new QuantitativeTransaction(sequence++, ids, quantities)));
            }
            panes.add(new Pane(transactions));
        }

        List<Pane> initial = panes.subList(0, 2);
        BruteForceMiner oracle = new BruteForceMiner(config);
        FwudsCtMiner ct = new FwudsCtMiner(config);
        FwudsDwtMiner dwt = new FwudsDwtMiner(config);
        assertEquivalent(oracle.initialize(initial), ct.initialize(initial), seed);
        assertEquivalent(oracle.current(), dwt.initialize(initial), seed);
        for (int i = 2; i < panes.size(); i++) {
            assertEquivalent(oracle.slide(panes.get(i)), ct.slide(panes.get(i)), seed);
            assertEquivalent(oracle.current(), dwt.slide(panes.get(i)), seed);
        }
    }

    private static void assertEquivalent(WindowResult expected, WindowResult actual, long seed) {
        Map<Itemset, Double> left = map(expected);
        Map<Itemset, Double> right = map(actual);
        assertEquals(left.keySet(), right.keySet(), "seed=" + seed + " window=" + expected.windowIndex());
        left.forEach((pattern, wus) -> assertEquals(wus, right.get(pattern), 1e-12,
                "seed=" + seed + " window=" + expected.windowIndex() + " pattern=" + pattern));
    }

    private static Map<Itemset, Double> map(WindowResult result) {
        return result.patterns().stream().collect(Collectors.toMap(PatternResult::itemset, PatternResult::wus));
    }
}
