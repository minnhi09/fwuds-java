package org.fwuds.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFileReaderTest {
    @TempDir Path directory;

    @Test void readsBinaryAndQuantitativeTokens() throws Exception {
        Path file = directory.resolve("data.txt");
        Files.writeString(file, "# comment\n3 1 2\n1:2 4:5\n");
        try (TransactionFileReader reader = new TransactionFileReader(file)) {
            var first = reader.read();
            assertArrayEquals(new int[]{1,2,3}, first.items());
            assertArrayEquals(new int[]{1,1,1}, first.quantities());
            var second = reader.read();
            assertArrayEquals(new int[]{1,4}, second.items());
            assertArrayEquals(new int[]{2,5}, second.quantities());
            assertNull(reader.read());
        }
    }

    @Test void rejectsDuplicateItems() throws Exception {
        Path file = directory.resolve("bad.txt");
        Files.writeString(file, "1 2 1\n");
        try (TransactionFileReader reader = new TransactionFileReader(file)) {
            var exception = assertThrows(java.io.IOException.class, reader::read);
            assertEquals(true, exception.getMessage().contains("Duplicate item"));
        }
    }
}
