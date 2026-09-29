package org.fwuds.cli;

import org.fwuds.io.TransactionFileReader;
import org.fwuds.model.QuantitativeTransaction;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "verify", description = "Verify downloaded datasets against their manifest.")
final class DatasetVerifyCommand implements Callable<Integer> {
    @Option(names = "--manifest", defaultValue = "data/datasets.csv") Path manifest;
    @Option(names = "--data-dir", defaultValue = "data/raw") Path dataDir;

    @Override public Integer call() throws Exception {
        int failures = 0;
        try (BufferedReader rows = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            String row = rows.readLine();
            while ((row = rows.readLine()) != null) {
                if (row.isBlank()) continue;
                String[] fields = row.split(",", -1);
                String name = fields[0];
                Path file = dataDir.resolve(fields[1]);
                long expectedTransactions = Long.parseLong(fields[3]);
                int expectedItems = Integer.parseInt(fields[4]);
                String expectedSha = fields[5];
                if (!Files.isRegularFile(file)) {
                    System.err.println(name + ": missing " + file);
                    failures++;
                    continue;
                }
                long transactions = 0;
                Set<Integer> items = new HashSet<>();
                try (TransactionFileReader reader = new TransactionFileReader(file)) {
                    QuantitativeTransaction transaction;
                    while ((transaction = reader.read()) != null) {
                        transactions++;
                        for (int item : transaction.items()) items.add(item);
                    }
                }
                String actualSha = sha256(file);
                Path localChecksum = Path.of(file.toString() + ".sha256");
                if (expectedSha.equals("-") && Files.isRegularFile(localChecksum)) {
                    expectedSha = Files.readString(localChecksum, StandardCharsets.US_ASCII).trim();
                }
                boolean metadataOk = transactions == expectedTransactions && items.size() == expectedItems;
                boolean hashOk = expectedSha.equals("-") || expectedSha.equalsIgnoreCase(actualSha);
                System.out.printf("%s: transactions=%d items=%d sha256=%s %s%n", name, transactions, items.size(), actualSha,
                        metadataOk && hashOk ? "OK" : "FAILED");
                if (!metadataOk || !hashOk) failures++;
            }
        }
        return failures == 0 ? 0 : 2;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
