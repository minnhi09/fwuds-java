package org.fwuds.cli;

import org.fwuds.io.PaneReader;
import org.fwuds.io.TransactionFileReader;
import org.fwuds.mining.FwudsMiner;
import org.fwuds.model.MiningConfig;
import org.fwuds.model.Pane;
import org.fwuds.model.PatternResult;
import org.fwuds.model.WindowResult;
import org.fwuds.util.DynamicWeightGenerator;
import org.fwuds.util.TransactionWeigher;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

final class MiningRun {
    private MiningRun() { }

    static RunStats execute(String algorithm, Path input, MiningConfig config, long maxSlides,
                            Consumer<TimedWindowResult> resultConsumer) throws IOException {
        resetHeapPeaks();
        MessageDigest digest = sha256();
        long elapsedNanos = 0L;
        long patternCount = 0L;
        long windowCount = 0L;
        try (TransactionFileReader source = new TransactionFileReader(input)) {
            PaneReader panes = new PaneReader(source,
                    new TransactionWeigher(new DynamicWeightGenerator(config.seed(), config.weightBatchSize())),
                    config.paneSize());
            List<Pane> initial = new ArrayList<>(config.windowPanes());
            for (int i = 0; i < config.windowPanes(); i++) {
                Pane pane = panes.read();
                if (pane == null) throw new IOException("Input does not contain a complete initial window");
                initial.add(pane);
            }
            FwudsMiner miner = MinerFactory.create(algorithm, config);
            long started = System.nanoTime();
            WindowResult result = miner.initialize(initial);
            long duration = System.nanoTime() - started;
            elapsedNanos += duration;
            patternCount += result.patterns().size();
            windowCount++;
            updateDigest(digest, result);
            resultConsumer.accept(new TimedWindowResult(result, duration));
            Pane newest;
            long slides = 0L;
            while ((maxSlides < 0 || slides < maxSlides) && (newest = panes.read()) != null) {
                started = System.nanoTime();
                result = miner.slide(newest);
                duration = System.nanoTime() - started;
                elapsedNanos += duration;
                patternCount += result.patterns().size();
                windowCount++;
                updateDigest(digest, result);
                resultConsumer.accept(new TimedWindowResult(result, duration));
                slides++;
            }
        }
        return new RunStats(windowCount, patternCount, elapsedNanos, peakHeapBytes(), HexFormat.of().formatHex(digest.digest()));
    }

    private static void updateDigest(MessageDigest digest, WindowResult result) {
        digest.update(Long.toString(result.windowIndex()).getBytes(StandardCharsets.UTF_8));
        for (PatternResult pattern : result.patterns()) {
            digest.update(pattern.itemset().toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void resetHeapPeaks() {
        ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP).forEach(pool -> {
                    try { pool.resetPeakUsage(); } catch (UnsupportedOperationException ignored) { }
                });
    }

    private static long peakHeapBytes() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .mapToLong(pool -> Math.max(0L, pool.getPeakUsage().getUsed())).sum();
    }

    record TimedWindowResult(WindowResult result, long runtimeNanos) { }
    record RunStats(long windows, long patterns, long runtimeNanos, long peakHeapBytes, String resultSha256) { }
}
