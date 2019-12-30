package com.monepic.astro;

/**
 * <a href="https://en.m.wikipedia.org/wiki/Solar_zenith_angle">Solar zenith angle</a> to use for the sunrise and sunset calculations
 * @author monepic
 *
 */
public enum Zenith {
    OFFICIAL(90, 50),
    CIVIL(96, 0),
    NAUTICAL(102, 0),
    ASTRONOMICAL(108, 0)
    ;
    private final double decimalDegrees;

    private Zenith(int degrees, int minutes) {
        decimalDegrees = degrees + 1.0 * minutes / 60;
    }

    public double getDecimalDegrees() { return decimalDegrees; }
}
