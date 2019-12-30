# Sunrise / Sunset calculation library
## based on the algorithm [found here](http://edwilliams.org/sunrise_sunset_algorithm.htm)


Sample Usage: 

```java
package com.example;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.monepic.astro.Location;
import com.monepic.astro.SunRiseSetCalculator;
import com.monepic.astro.Zenith;

public class Example {
    public static void main(String[] args) {
        Location newYork = new Location(40.781915, -73.965978);
        // results are in UTC
        Instant sunriseInstant = SunRiseSetCalculator.sunriseInstant(LocalDate.now(), Zenith.OFFICIAL, newYork);
        System.out.println(sunriseInstant.atZone(ZoneId.of("America/New_York")));
    }
}
```

The results seem good enough for simple purposes, although I've observed some anomalies around the polar regions at their midnight sun / polar night transitions.
