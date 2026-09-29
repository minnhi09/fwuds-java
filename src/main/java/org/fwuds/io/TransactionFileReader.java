package org.fwuds.io;

import org.fwuds.model.QuantitativeTransaction;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Streaming reader for FIMI/SPMF transaction files and optional item:quantity tokens. */
public final class TransactionFileReader implements Closeable {
    private final BufferedReader reader;
    private long sequence;
    private long lineNumber;

    public TransactionFileReader(Path path) throws IOException {
        reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public QuantitativeTransaction read() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("%") || line.startsWith("@")) continue;
            String[] tokens = line.split("\\s+");
            List<Integer> items = new ArrayList<>(tokens.length);
            List<Integer> quantities = new ArrayList<>(tokens.length);
            for (String token : tokens) {
                String[] pair = token.split(":", -1);
                if (pair.length > 2 || pair[0].isBlank()) fail("Invalid token: " + token);
                try {
                    items.add(Integer.parseInt(pair[0]));
                    quantities.add(pair.length == 2 ? Integer.parseInt(pair[1]) : 1);
                } catch (NumberFormatException exception) {
                    fail("Invalid integer token: " + token);
                }
            }
            try {
                QuantitativeTransaction result = new QuantitativeTransaction(
                        sequence,
                        items.stream().mapToInt(Integer::intValue).toArray(),
                        quantities.stream().mapToInt(Integer::intValue).toArray());
                sequence++;
                return result;
            } catch (IllegalArgumentException exception) {
                fail(exception.getMessage());
            }
        }
        return null;
    }

    public long transactionsRead() { return sequence; }

    private void fail(String message) throws IOException {
        throw new IOException("Line " + lineNumber + ": " + message);
    }

    @Override public void close() throws IOException { reader.close(); }
}
