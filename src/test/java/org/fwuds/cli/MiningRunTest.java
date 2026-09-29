package org.fwuds.cli;

import org.fwuds.model.MiningConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningRunTest {
    @TempDir Path directory;

    @Test void resultHashIsStableAcrossEquivalentAlgorithms() throws Exception {
        Path input = directory.resolve("input.txt");
        Files.writeString(input, "1 2\n1 3\n1 2 3\n2 3\n");
        MiningConfig config = new MiningConfig(2, 1, 2, 0.2, 42);

        MiningRun.RunStats ct = MiningRun.execute("ct", input, config, 2, ignored -> { });
        MiningRun.RunStats dwt = MiningRun.execute("dwt", input, config, 2, ignored -> { });

        assertEquals(ct.patterns(), dwt.patterns());
        assertEquals(ct.resultSha256(), dwt.resultSha256());
    }
}
