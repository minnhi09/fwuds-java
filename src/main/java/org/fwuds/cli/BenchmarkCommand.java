package org.fwuds.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "benchmark", description = "Run repeatable end-to-end FWUDS measurements.")
final class BenchmarkCommand implements Callable<Integer> {
    @Mixin MiningOptions options;

    @Option(names = "--warmups", defaultValue = "1") int warmups;
    @Option(names = "--repetitions", defaultValue = "5") int repetitions;
    @Option(names = "--output", required = true) Path output;

    @Override public Integer call() throws Exception {
        if (warmups < 0 || repetitions <= 0) throw new IllegalArgumentException("Invalid warmup/repetition count");
        for (int i = 0; i < warmups; i++) {
            MiningRun.execute(options.algorithm, options.input, options.config(), options.maxSlides, ignored -> { });
            System.out.printf("warmup %d/%d complete%n", i + 1, warmups);
        }
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("dataset,algorithm,seed,run,windows,patterns,runtime_nanos,peak_heap_bytes,result_sha256,jvm,os\n");
            for (int run = 1; run <= repetitions; run++) {
                MiningRun.RunStats stats = MiningRun.execute(options.algorithm, options.input, options.config(), options.maxSlides, ignored -> { });
                writer.write(String.join(",",
                        CsvSupport.escape(options.input.getFileName()), CsvSupport.escape(options.algorithm),
                        Long.toString(options.seed), Integer.toString(run), Long.toString(stats.windows()),
                        Long.toString(stats.patterns()), Long.toString(stats.runtimeNanos()),
                        Long.toString(stats.peakHeapBytes()), stats.resultSha256(),
                        CsvSupport.escape(System.getProperty("java.vm.name") + " " + System.getProperty("java.version")),
                        CsvSupport.escape(System.getProperty("os.name") + " " + System.getProperty("os.version"))));
                writer.newLine();
                writer.flush();
                System.out.printf("run=%d runtime_ms=%.3f peak_heap_mb=%.3f hash=%s%n", run,
                        stats.runtimeNanos() / 1_000_000.0, stats.peakHeapBytes() / 1_048_576.0, stats.resultSha256());
            }
        }
        return 0;
    }
}
