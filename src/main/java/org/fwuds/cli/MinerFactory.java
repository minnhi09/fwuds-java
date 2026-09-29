package org.fwuds.cli;

import org.fwuds.mining.BruteForceMiner;
import org.fwuds.mining.FwudsMiner;
import org.fwuds.mining.ct.FwudsCtMiner;
import org.fwuds.mining.dwt.FwudsDwtMiner;
import org.fwuds.model.MiningConfig;

final class MinerFactory {
    private MinerFactory() { }

    static FwudsMiner create(String algorithm, MiningConfig config) {
        return switch (algorithm.toLowerCase(java.util.Locale.ROOT)) {
            case "ct" -> new FwudsCtMiner(config);
            case "dwt" -> new FwudsDwtMiner(config);
            case "brute-force", "oracle" -> new BruteForceMiner(config);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        };
    }
}
