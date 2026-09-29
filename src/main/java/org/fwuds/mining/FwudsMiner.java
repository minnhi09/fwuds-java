package org.fwuds.mining;

import org.fwuds.model.Pane;
import org.fwuds.model.WindowResult;

import java.util.List;

/** Stateful miner whose state represents one complete sliding window. */
public interface FwudsMiner {
    /** Builds the initial window; the list must contain exactly the configured pane count. */
    WindowResult initialize(List<Pane> initialWindow);

    /** Evicts the oldest pane and inserts one complete newest pane. */
    WindowResult slide(Pane newestPane);

    /** Returns the latest result after {@link #initialize(List)} or {@link #slide(Pane)}. */
    WindowResult current();
}
