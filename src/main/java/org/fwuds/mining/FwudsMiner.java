package org.fwuds.mining;

import org.fwuds.model.Pane;
import org.fwuds.model.WindowResult;

import java.util.List;

public interface FwudsMiner {
    WindowResult initialize(List<Pane> initialWindow);
    WindowResult slide(Pane newestPane);
    WindowResult current();
}
