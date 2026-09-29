package org.fwuds.cli;

final class CsvSupport {
    private CsvSupport() { }

    static String escape(Object raw) {
        String value = String.valueOf(raw);
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
