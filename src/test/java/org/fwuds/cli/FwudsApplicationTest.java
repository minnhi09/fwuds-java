package org.fwuds.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FwudsApplicationTest {
    @TempDir Path directory;

    @Test void mineCommandWritesCsv() throws Exception {
        Path input = directory.resolve("input.txt");
        Path output = directory.resolve("patterns.csv");
        Files.writeString(input, "1 2\n1 3\n1 2 3\n2 3\n");
        int status = new CommandLine(new FwudsApplication()).execute(
                "mine", "--algorithm", "ct", "--input", input.toString(),
                "--window-panes", "2", "--pane-size", "1", "--weight-batch-size", "2",
                "--min-wus", "0.2", "--seed", "42", "--max-slides", "1", "--output", output.toString());
        assertEquals(0, status);
        assertTrue(Files.readString(output).startsWith("dataset,algorithm,seed,window,pattern,wus"));
    }

    @Test void subcommandsExposeStandardHelp() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        CommandLine command = new CommandLine(new FwudsApplication());
        command.setOut(new PrintWriter(captured, true));

        assertEquals(0, command.execute("mine", "--help"));
        assertTrue(captured.toString().contains("--algorithm"));

        captured.reset();
        assertEquals(0, command.execute("datasets", "verify", "--help"));
        assertTrue(captured.toString().contains("--manifest"));
    }
}
