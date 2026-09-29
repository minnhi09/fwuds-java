package org.fwuds.mining.dwt;

import java.util.List;

final class WunList {
    private final List<WunCode> codes;
    private final double totalWeight;

    WunList(List<WunCode> codes) {
        this.codes = List.copyOf(codes);
        this.totalWeight = codes.stream().mapToDouble(WunCode::weight).sum();
    }

    List<WunCode> codes() { return codes; }
    double totalWeight() { return totalWeight; }
    boolean isEmpty() { return codes.isEmpty(); }
}
