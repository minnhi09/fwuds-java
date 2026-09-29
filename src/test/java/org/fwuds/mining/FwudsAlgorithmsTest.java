package org.fwuds.mining;

import org.fwuds.mining.ct.FwudsCtMiner;
import org.fwuds.mining.dwt.FwudsDwtMiner;
import org.fwuds.model.Itemset;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.PatternResult;
import org.fwuds.model.WindowResult;
import org.fwuds.support.PaperExample;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FwudsAlgorithmsTest {
    private static final MiningConfig CONFIG = new MiningConfig(2, 2, 3, 0.5, 42);

    @Test void circularTidsetsMatchPaperAcrossSlide() {
        FwudsCtMiner miner = new FwudsCtMiner(CONFIG);
        miner.initialize(PaperExample.initialWindow());
        assertEquals(List.of(1, 3, 4), miner.singletonCtsets().get(1));
        assertEquals(List.of(1, 2, 3, 4), miner.singletonCtsets().get(3));
        assertEquals(4, miner.currentLcTid());

        WindowResult result = miner.slide(PaperExample.newestPane());
        assertEquals(List.of(3, 4, 2), miner.singletonCtsets().get(1));
        assertEquals(List.of(3, 4, 1, 2), miner.singletonCtsets().get(3));
        assertEquals(2, miner.currentLcTid());
        assertEquals(2.39 / 2.82, byPattern(result).get(new Itemset(1)), 1e-12);
    }

    @Test void bothAlgorithmsEqualOracleForBothPaperWindows() {
        BruteForceMiner oracle = new BruteForceMiner(CONFIG);
        FwudsCtMiner ct = new FwudsCtMiner(CONFIG);
        FwudsDwtMiner dwt = new FwudsDwtMiner(CONFIG);

        assertEquivalent(oracle.initialize(PaperExample.initialWindow()), ct.initialize(PaperExample.initialWindow()));
        assertEquivalent(oracle.current(), dwt.initialize(PaperExample.initialWindow()));
        assertEquivalent(oracle.slide(PaperExample.newestPane()), ct.slide(PaperExample.newestPane()));
        assertEquivalent(oracle.current(), dwt.slide(PaperExample.newestPane()));
    }

    private static void assertEquivalent(WindowResult expected, WindowResult actual) {
        Map<Itemset, Double> left = byPattern(expected);
        Map<Itemset, Double> right = byPattern(actual);
        assertEquals(left.keySet(), right.keySet());
        left.forEach((pattern, wus) -> assertEquals(wus, right.get(pattern), 1e-12, pattern.toString()));
    }

    private static Map<Itemset, Double> byPattern(WindowResult result) {
        return result.patterns().stream().collect(Collectors.toMap(PatternResult::itemset, PatternResult::wus));
    }
}
