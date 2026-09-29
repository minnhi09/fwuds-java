package org.fwuds.cli;

import org.fwuds.model.MiningConfig;
import picocli.CommandLine.Option;

import java.nio.file.Path;

final class MiningOptions {
    @Option(names = "--algorithm", required = true, description = "ct, dwt, or oracle")
    String algorithm;

    @Option(names = "--input", required = true, description = "FIMI/SPMF transaction file")
    Path input;

    @Option(names = "--window-panes", required = true, description = "Number of panes in a window")
    int windowPanes;

    @Option(names = "--pane-size", required = true, description = "Transactions in each pane")
    int paneSize;

    @Option(names = "--weight-batch-size", required = true, description = "Transactions sharing a weight regime")
    int weightBatchSize;

    @Option(names = "--min-wus", required = true, description = "Threshold as a fraction in [0,1]")
    double minWus;

    @Option(names = "--seed", defaultValue = "42", description = "Deterministic weight seed")
    long seed;

    @Option(names = "--max-slides", defaultValue = "-1", description = "Stop after this many slides; -1 reads to EOF")
    long maxSlides;

    MiningConfig config() {
        if (maxSlides < -1) throw new IllegalArgumentException("max-slides must be -1 or non-negative");
        return new MiningConfig(windowPanes, paneSize, weightBatchSize, minWus, seed);
    }
}
