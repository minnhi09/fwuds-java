package org.fwuds.support;

import org.fwuds.model.Pane;
import org.fwuds.model.WeightedTransaction;

import java.util.List;

public final class PaperExample {
    private PaperExample() { }

    public static List<Pane> initialWindow() {
        return List.of(new Pane(List.of(t1(), t2())), new Pane(List.of(t3(), t4())));
    }

    public static Pane newestPane() { return new Pane(List.of(t5(), t6())); }

    public static WeightedTransaction t1() { return tx(0, new int[]{1,3,4,5}, new int[]{1,2,2,1}, 0.48); }
    public static WeightedTransaction t2() { return tx(1, new int[]{2,3,4,5}, new int[]{2,1,2,2}, 0.70); }
    public static WeightedTransaction t3() { return tx(2, new int[]{1,3,5}, new int[]{2,1,1}, 0.37); }
    public static WeightedTransaction t4() { return tx(3, new int[]{1,3,4}, new int[]{1,3,2}, 0.97); }
    public static WeightedTransaction t5() { return tx(4, new int[]{2,3,4}, new int[]{1,1,1}, 0.43); }
    public static WeightedTransaction t6() { return tx(5, new int[]{1,3,4,5}, new int[]{2,2,3,1}, 1.05); }

    private static WeightedTransaction tx(long id, int[] items, int[] quantities, double twu) {
        return new WeightedTransaction(id, items, quantities, twu);
    }
}
