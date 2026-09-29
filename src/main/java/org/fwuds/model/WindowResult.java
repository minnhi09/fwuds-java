package org.fwuds.model;

import java.util.List;

public record WindowResult(long windowIndex, List<PatternResult> patterns, double sumTwu) {
    public WindowResult {
        patterns = patterns.stream().sorted().toList();
    }
}
