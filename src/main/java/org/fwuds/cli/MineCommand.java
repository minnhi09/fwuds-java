package org.fwuds.cli;

import org.fwuds.model.PatternResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "mine", mixinStandardHelpOptions = true,
        description = "Mine FWUPs for every complete sliding window.")
final class MineCommand implements Callable<Integer> {
    @Mixin MiningOptions options;

    @Option(names = "--output", required = true, description = "Output pattern CSV")
    Path output;

    @Override public Integer call() throws Exception {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("dataset,algorithm,seed,window,pattern,wus,window_runtime_nanos,jvm\n");
            String dataset = options.input.getFileName().toString();
            String jvm = System.getProperty("java.vm.name") + " " + System.getProperty("java.version");
            MiningRun.RunStats stats = MiningRun.execute(options.algorithm, options.input, options.config(), options.maxSlides, timed -> {
                try {
                    for (PatternResult pattern : timed.result().patterns()) {
                        writer.write(String.join(",",
                                CsvSupport.escape(dataset), CsvSupport.escape(options.algorithm), Long.toString(options.seed),
                                Long.toString(timed.result().windowIndex()), CsvSupport.escape(pattern.itemset()),
                                Double.toString(pattern.wus()), Long.toString(timed.runtimeNanos()), CsvSupport.escape(jvm)));
                        writer.newLine();
                    }
                } catch (java.io.IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
            System.out.printf("windows=%d patterns=%d runtime_ms=%.3f peak_heap_mb=%.3f result_sha256=%s%n",
                    stats.windows(), stats.patterns(), stats.runtimeNanos() / 1_000_000.0,
                    stats.peakHeapBytes() / 1_048_576.0, stats.resultSha256());
        }
        return 0;
    }
}
