package org.fwuds.util;

public final class FwudsMath {
    public static final double EPSILON = 1.0e-12;
    private FwudsMath() { }

    public static boolean meetsThreshold(double numerator, double denominator, double threshold) {
        return numerator + EPSILON >= threshold * denominator;
    }
}
