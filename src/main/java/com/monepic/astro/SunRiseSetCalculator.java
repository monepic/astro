package com.monepic.astro;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Based on the algorithm found <a href="http://edwilliams.org/sunrise_sunset_algorithm.htm">here</a>
 * 
 * Results are in UTC
 * 
 * @author monepic
 */
public class SunRiseSetCalculator {

    public static Logger LOG = Logger.NULL;
    interface Logger {
        /** Logs to System.out */
        public static Logger CONSOLE = (s, args) -> { System.out.printf(s, args); System.out.println(); };
        /** Logs nowhere */
        public static Logger NULL = (s, args) -> {};
        static final Object[] EMPTY_ARRAY = {};

        default void log() { log(""); }
        default void log(Object s) { 
            log(String.valueOf(s), EMPTY_ARRAY); 
        }
        void log(String s, Object...args);
    };

    public static LocalTime sunrise(LocalDate date, Zenith zenith, Location location) {
        return sunrise(date, zenith, location.getLatitude(), location.getLongitude());
    }

    public static LocalTime sunrise(LocalDate date, Zenith zenith, double latitude, double longitude) {
        Instant sunrise = sunriseInstant(date, zenith, latitude, longitude);
        return sunrise == null ? null : sunrise.atOffset(ZoneOffset.UTC).toLocalTime();
    }

    public static LocalTime sunset(LocalDate date, Zenith zenith, Location location) {
        return sunset(date, zenith, location.getLatitude(), location.getLongitude());
    }

    public static LocalTime sunset(LocalDate date, Zenith zenith, double latitude, double longitude) {
        Instant sunset = sunsetInstant(date, zenith, latitude, longitude);
        return sunset == null ? null : sunset.atOffset(ZoneOffset.UTC).toLocalTime();
    }

    public static Instant sunriseInstant(LocalDate date, Zenith zenith, Location location) {
        return sunriseInstant(date, zenith, location.getLatitude(), location.getLongitude());
    }

    public static Instant sunriseInstant(LocalDate date, Zenith zenith, double latitude, double longitude) {
        return calculate(true, date, zenith, latitude, longitude);
    }

    public static Instant sunsetInstant(LocalDate date, Zenith zenith, Location location) {
        return sunsetInstant(date, zenith, location.getLatitude(), location.getLongitude());
    }

    public static Instant sunsetInstant(LocalDate date, Zenith zenith, double latitude, double longitude) {
        return calculate(false, date, zenith, latitude, longitude);
    }

    private static Instant calculate(boolean rising, LocalDate date, Zenith zenith, double latitude, double longitude) {
        int N = date.getDayOfYear();
        LOG.log("Day of year = %d", N);

        /*
		  Convert the longitude to hour value and calculate an approximate time
	      lngHour = longitude / 15
          if rising time is desired:
          t = N + ((6 - lngHour) / 24)
          if setting time is desired:
          t = N + ((18 - lngHour) / 24)
         */
        double lngHour = longitude / 15;
        double t = N + (((rising ? 6 : 18) - lngHour) / 24);
        LOG.log("Longitude hour = %f", lngHour);
        LOG.log("Approximate time = %f", t);

        /*
          Calculate the Sun's mean anomaly
          M = (0.9856 * t) - 3.289
         */
        double M = (0.9856 * t) - 3.289;
        LOG.log("Sun's mean anomaly = %f", M);

        /*
          Calculate the Sun's true longitude
          L = M + (1.916 * sin(M)) + (0.020 * sin(2 * M)) + 282.634
          NOTE: L potentially needs to be adjusted into the range [0,360] by adding/subtracting 360
         */
        double L = constrain(360, M + (1.916 * sin(M)) + (0.020 * sin(2 * M)) + 282.634);
        LOG.log("Sun's true longitude = %f", L);

        /*
          Calculate the Sun's right ascension
          RA = atan(0.91764 * tan(L))
          NOTE: RA potentially needs to be adjusted into the range [0,360) by adding/subtracting 360
         */
        double RA = constrain(360, atan(0.91764 * tan(L)));
        LOG.log("Sun's right ascension = %f", RA);

        /*
          The right ascension value needs to be in the same quadrant as L
          Lquadrant  = (floor( L/90)) * 90
          RAquadrant = (floor(RA/90)) * 90
          RA = RA + (Lquadrant - RAquadrant)
         */
        double Lquad = (Math.floor(L/90)) * 90;
        double RAquad = (Math.floor(RA/90)) * 90;
        double RAa = RA + (Lquad - RAquad);
        LOG.log("Sun's right ascension quadrant adjusted = %f", RAa);

        /*
	      The right ascension value needs to be converted into hours
          RA = RA / 15
         */
        double RAah = RAa / 15;
        LOG.log("Sun's right ascension in hours = %f", RAah);

        /*
          Calculate the Sun's declination
          sinDec = 0.39782 * sin(L)
          cosDec = cos(asin(sinDec))
         */
        double sinDec = 0.39782 * sin(L);
        double cosDec = cos(asin(sinDec));
        LOG.log("Sun's declination (sin, cos) (%f, %f)", sinDec, cosDec);

        /*
          Calculate the Sun's local hour angle
          cosH = (cos(zenith) - (sinDec * sin(latitude))) / (cosDec * cos(latitude))
          if (cosH >  1) 
           the sun never rises on this location (on the specified date)
          if (cosH < -1)
           the sun never sets on this location (on the specified date)
         */
        LOG.log("Zenith decimal deg = %f", zenith.getDecimalDegrees());
        double cosH = (cos(zenith.getDecimalDegrees()) - (sinDec * sin(latitude))) / (cosDec * cos(latitude));
        LOG.log("Sun's local hour angle %f", cosH);

        if (rising && cosH > 1) {
            // no sunrise on this day - always night
            LOG.log("There's no sunrise on this day (polar night)");
            return null;
        } else if (rising && cosH < -1) {
            // no sunrise on this day - always day
            LOG.log("There's no sunrise on this day (midnight sun)");
            return null;
        } else if (!rising && cosH > 1) {
            // no sunset on this day
            LOG.log("There's no sunset on this day (polar night)");
            return null;
        } else if (!rising && cosH < -1) {
            // no sunset on this day - always day
            LOG.log("There's no sunset on this day (midnight sun)");
            return null;
        }

        /*
          Finish calculating H and convert into hours
          if rising time is desired:
            H = 360 - acos(cosH)
          if setting time is desired:
            H = acos(cosH)
		  H = H / 15
         */
        double H = (rising ? (360 - acos(cosH)) : acos(cosH)) / 15;
        LOG.log("acos(cosH) = %f", acos(cosH));
        LOG.log("H = %f", H);

        /*
		  Calculate local mean time of rising/setting
          T = H + RA - (0.06571 * t) - 6.622
         */
        double T = H + RAah - (0.06571 * t) - 6.622;
        LOG.log("Local mean time of %s = %s", rising ? "sunrise" : "sunset", 
                LocalTime.ofSecondOfDay(Math.round((constrain(24,T) * 60 * 60))).toString());

        /*
          Adjust back to UTC
          UT = T - lngHour
          NOTE: UT potentially needs to be adjusted into the range [0,24] by adding/subtracting 24
         */

        // fiddle the dates to ensure the UTC date is correct
        Instant result = LocalDateTime
                .of(date, LocalTime.ofSecondOfDay(Math.round(constrain(24, T) * 60 * 60)))
                .toInstant(ZoneOffset.UTC)
                .minus(Math.round(lngHour * 60 * 60), ChronoUnit.SECONDS);
        LOG.log("%s = %s", rising ? "Sunrise" : "Sunset", result);
        LOG.log();
        return result;
    }

    private static double sin(double ang) {
        return Math.sin(Math.toRadians(ang));
    }
    private static double cos(double ang) {
        return Math.cos(Math.toRadians(ang));
    }
    private static double tan(double ang) {
        return Math.tan(Math.toRadians(ang));
    }
    private static double asin(double x) {
        return Math.toDegrees(Math.asin(x));
    }
    private static double acos(double x) {
        return Math.toDegrees(Math.acos(x));
    }
    private static double atan(double x) {
        return Math.toDegrees(Math.atan(x));
    }

    private static double constrain(double lim, double in) {
        if (in < 0) { return constrain(lim, in + lim); }
        if (in > lim) { return constrain(lim, in - lim); }
        return in;
    }
}
