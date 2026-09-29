package org.fwuds.io;

import org.fwuds.model.Pane;
import org.fwuds.model.QuantitativeTransaction;
import org.fwuds.model.WeightedTransaction;
import org.fwuds.util.TransactionWeigher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PaneReader {
    private final TransactionFileReader source;
    private final TransactionWeigher weigher;
    private final int paneSize;

    public PaneReader(TransactionFileReader source, TransactionWeigher weigher, int paneSize) {
        this.source = source;
        this.weigher = weigher;
        this.paneSize = paneSize;
    }

    /** Returns null at EOF. An incomplete final pane is deliberately ignored. */
    public Pane read() throws IOException {
        List<WeightedTransaction> transactions = new ArrayList<>(paneSize);
        while (transactions.size() < paneSize) {
            QuantitativeTransaction transaction = source.read();
            if (transaction == null) return transactions.size() == paneSize ? new Pane(transactions) : null;
            transactions.add(weigher.weigh(transaction));
        }
        return new Pane(transactions);
    }
}
